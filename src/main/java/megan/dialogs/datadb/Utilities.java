package megan.dialogs.datadb;

import jloda.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 * some basic utilities for sqlite
 * Daniel Huson, 1.2023
 */
public class Utilities {
	/**
	 * executes a list of commands
	 */
	public static void execute(Connection connection, String... commands) throws SQLException {
		if (false)
			System.err.println("execute:\n" + StringUtils.toString(commands, "\n"));
		var statement = connection.createStatement();
		{
			for (var q : commands) {
				statement.execute(q);
			}
		}
	}

	/**
	 * executes a list of commands
	 */
	public static void execute(Connection connection, Collection<String> commands) throws SQLException {
		execute(connection, commands.toArray(new String[0]));
	}

	/**
	 * queries the table tableName and returns the number of rows in that table
	 *
	 * @param tableName name of the table
	 * @return size of the table tableName
	 */
	public static long countRows(Connection connection, String tableName) throws SQLException {
		long count = 0L;

		try (var statement = connection.createStatement();
			 var rs = statement.executeQuery("SELECT count(*) AS q FROM " + tableName + ";")) {
			while (rs.next()) {
				count = rs.getLong("q"); // todo: is this correct?
			}
		}
		return count;
	}

}
