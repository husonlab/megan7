package megan.dialogs.datadb;

import jloda.util.CanceledException;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.data.IConnector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * export medium leave of detail
 * Daniel Huson, 1.2023
 */
public class ExportMedium {

	public static void apply(Connection connection, IConnector connector, ArrayList<String> classifications, Set<String> selectedReads, ProgressListener progress) throws SQLException, IOException {

		var orderClassifications = new ArrayList<String>();

		for (var classificationName : connector.getAllClassificationNames()) {
			if (StringUtils.containsIgnoreCase(classifications, classificationName) || StringUtils.containsIgnoreCase(classifications, ExportToSQLite.ALL)) {
				orderClassifications.add(classificationName);
			}
		}

		var count = 0;
		for (String classificationName : orderClassifications) {
			var tableName = "read_to_" + classificationName;
			Utilities.execute(connection, "DROP TABLE IF EXISTS " + tableName + ";", "CREATE TABLE " + tableName + " (name TEXT KEY," + classificationName + ");");
			var classificationBlock = connector.getClassificationBlock(classificationName);
			progress.setTasks("Exporting medium", tableName + " (" + (++count) + " of " + orderClassifications.size() + ")");
			progress.setProgress(0);
			progress.setMaximum(classificationBlock.getKeySet().size());

			for (var classId : classificationBlock.getKeySet()) {
				if (classId != 0) {
					connection.setAutoCommit(false);
					try (var statement = connection.prepareStatement("INSERT INTO " + tableName + " (name," + classificationName + ") VALUES (?, ?);")) {
						try (var it = connector.getReadsIterator(classificationName, classId, 0, 10, false, false)) {
							while (it.hasNext()) {
								var name = it.next().getReadName();
								if (selectedReads == null || selectedReads.contains(name)) {
									statement.setString(1, name);
									statement.setInt(2, classId);
									statement.execute();
								}
							}
						}
					} finally {
						connection.commit();
						connection.setAutoCommit(true);
					}
				}
				progress.incrementProgress();
			}
			progress.reportTaskCompleted();
			System.err.printf("Table '" + tableName + "' row count: %,d%n", Utilities.countRows(connection, tableName));
		}
	}

	/**
	 * Creates the joining query variably and then performs the join. Resulting table is called ID_mappings (= mappings)
	 */
	private static void joinTables(Connection connection, List<String> tableNames, String newTableName, ProgressListener progress) throws SQLException, CanceledException {
		System.err.println("Joining tables...");

		Utilities.execute(connection, "DROP TABLE IF EXISTS " + newTableName + ";", "DROP TABLE IF EXISTS names;");

		// create table of names
		Utilities.execute(connection, "CREATE TABLE names AS SELECT name FROM " + StringUtils.toString(tableNames, " UNION SELECT name FROM ") + ";");

		// create target table
		var columnNames = tableNames.stream().map(s -> s.replaceAll("map", "")).collect(Collectors.toList());
		Utilities.execute(connection, "CREATE TABLE " + newTableName + " (name," + StringUtils.toString(columnNames, ",") + ")");

		var fillMappingCommand = new StringBuilder(" INSERT INTO " + newTableName + " SELECT * FROM names AS n ");

		progress.setTasks("Exporting medium", "Joining tables");
		progress.setProgress(0);
		progress.setMaximum(tableNames.size());
		for (var table : tableNames) {
			fillMappingCommand.append("LEFT OUTER JOIN ").append(table).append(" USING (").append("name").append(") ");
			progress.incrementProgress();
		}
		fillMappingCommand.append(";"); // finishing the query
		// executing the queries
		Utilities.execute(connection, fillMappingCommand.toString());

		Utilities.execute(connection, "DROP TABLE IF EXISTS names;");

		progress.reportTaskCompleted();
	}
}
