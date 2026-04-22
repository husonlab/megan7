/*
 * ModifyAccessionMappingDatabase.java Copyright (C) 2024 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package megan.accessiondb;

import jloda.util.FileLineIterator;
import jloda.util.NumberUtils;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Modify mapping database (SQLite or DuckDB)
 * Daniel Huson, 12.2019; extended for DuckDB 2.2026
 */
public class ModifyAccessionMappingDatabase {
	protected final String databaseFile;

	// SQLite-only config (null for DuckDB)
	protected final SQLiteConfig config;

	protected final AccessionMappingDBFactory.DbType dbType;

	/**
	 * constructor
	 */
	public ModifyAccessionMappingDatabase(String databaseFile) throws IOException, SQLException {
		this.databaseFile = databaseFile;
		this.dbType = AccessionMappingDBFactory.detect(databaseFile);

		System.err.println("Database '" + databaseFile + "', current contents: ");
		try (var accessionDB = AccessionMappingDBFactory.open(databaseFile)) {
			System.err.println(accessionDB.getInfo());
		}

		if (dbType == AccessionMappingDBFactory.DbType.SQLITE) {
			config = new SQLiteConfig();
			config.setCacheSize(10000);
			config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
			config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
			config.setJournalMode(SQLiteConfig.JournalMode.WAL);
		} else {
			config = null;
		}
	}

	private Connection openWriteConnection() throws SQLException {
		return switch (dbType) {
			case SQLITE -> config.createConnection("jdbc:sqlite:" + this.databaseFile);
			// case DUCKDB -> DriverManager.getConnection("jdbc:duckdb:" + this.databaseFile);
			default -> null;
		};
	}

	/**
	 * executes a list of commands
	 *
	 * @param commands String[] of complete queries
	 */
	private void execute(String... commands) throws SQLException {
		try (Connection connection = openWriteConnection();
			 Statement statement = connection.createStatement()) {
			for (String q : commands) {
				statement.execute(q);
			}
		}
	}

	/**
	 * adds a new column
	 */
	public void addNewColumn(String classificationName, String inputFile, String description) throws SQLException, IOException {
		if (classificationName == null)
			throw new NullPointerException("classificationName");

		int count = 0;

		// NOTE: don't quote identifiers with single quotes (that makes them string literals).
		// Use bare identifiers (or double quotes if needed).
		final String alter = "ALTER TABLE mappings ADD COLUMN " + classificationName + " INTEGER;";

		final String updateSql = "UPDATE mappings SET " + classificationName + "=? WHERE Accession=?";

		try (Connection connection = openWriteConnection();
			 Statement statement = connection.createStatement()) {

			// For DuckDB you may want to avoid preserve insertion order for faster bulk-ish updates
			if (dbType == AccessionMappingDBFactory.DbType.DUCKDB) {
				try {
					statement.execute("SET preserve_insertion_order=false;");
				} catch (SQLException ignored) {
				}
			}

			statement.execute(alter);

			connection.setAutoCommit(false);

			try (PreparedStatement update = connection.prepareStatement(updateSql)) {
				try (FileLineIterator it = new FileLineIterator(inputFile, true)) {
					while (it.hasNext()) {
						final String[] tokens = it.next().split("\t");
						final String accession = tokens[0];
						final int value = NumberUtils.parseInt(tokens[1]);
						if (value != 0) {
							update.setInt(1, value);
							update.setString(2, accession);
							update.executeUpdate();
							count++;
						}
					}
				}
			}

			System.err.println("Committing...");
			connection.commit();
			connection.setAutoCommit(true);

			// Insert info row
			// (Assumes schema: info(id TEXT PRIMARY KEY, info_string TEXT, size BIGINT/INTEGER))
			statement.execute("INSERT INTO info VALUES ('" + classificationName + "', '" + description.replace("'", "''") + "', " + count + ");");
		}
	}
}