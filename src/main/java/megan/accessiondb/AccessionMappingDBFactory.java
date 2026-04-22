/*
 * AccessionMappingDBFactory.java Copyright (C) 2026 Daniel H. Huson
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

import jloda.util.FileUtils;

import java.io.IOException;
import java.sql.SQLException;

/**
 * creates an SQL-based accession mapper
 * Daniel Huson, 2.2026
 */
public final class AccessionMappingDBFactory {
	public enum DbType {SQLITE, DUCKDB, UNKNOWN}

	public static AccessionMappingDB open(String file) throws IOException {
		if (!FileUtils.fileExistsAndIsNonEmpty(file))
			throw new IOException("File not found or unreadable: " + file);

		try {
			DbType type = detect(file);
			return switch (type) {
				case SQLITE -> new AccessSQLiteMappingDB(file);
				case DUCKDB -> throw new IOException("DuckDB accession mapping not supported");
				case UNKNOWN -> throw new IOException("Unknown accession mapping");
			};
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}


	static DbType detect(String file) throws IOException {
		// 1) magic header sniff
		try (var in = new java.io.FileInputStream(file)) {
			var header = in.readNBytes(16);

			// SQLite header: "SQLite format 3\0"
			if (startsWith(header, "SQLite format 3\u0000".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
				return DbType.SQLITE;

			// DuckDB files start with "DUCK" (magic) in current formats.
			// Prefer to treat this as a hint; fall back to trial open if unknown.
			if (header.length >= 4 &&
				header[0] == 'D' && header[1] == 'U' && header[2] == 'C' && header[3] == 'K')
				return DbType.DUCKDB;
		}
		return DbType.UNKNOWN;
	}

	private static boolean startsWith(byte[] a, byte[] prefix) {
		if (a.length < prefix.length) return false;
		for (int i = 0; i < prefix.length; i++) if (a[i] != prefix[i]) return false;
		return true;
	}
}