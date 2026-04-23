package megan.dialogs.datadb;

import jloda.util.progress.ProgressListener;
import megan.core.Document;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

/**
 * export all sequences
 * Daniel Huson, 1.2023
 */
public class ExportXLarge {
	public static String CREATE_SEQUENCES_TABLE = "CREATE TABLE sequences (name TEXT PRIMARY KEY, sequence TEXT) WITHOUT ROWID;";
	public static String INSERT_SEQUENCE = "INSERT INTO sequences VALUES ('%s','%s');";

	public static void apply(Connection connection, Document document, Set<String> selectedReads, ProgressListener progress) throws IOException, SQLException {

		Utilities.execute(connection, "DROP TABLE IF EXISTS sequences;", CREATE_SEQUENCES_TABLE);

		connection.setAutoCommit(false);
		try (var it = document.getConnector().getAllReadsIterator(0, 10, true, false)) {
			progress.setTasks("Exporting x-large", "sequences");
			progress.setProgress(0);
			progress.setMaximum(it.getMaximumProgress());
			var lines = new ArrayList<String>();
			while (it.hasNext()) {
				var readBlock = it.next();
				var name = readBlock.getReadName();
				if (selectedReads == null || selectedReads.contains(name)) {
					lines.add(String.format(INSERT_SEQUENCE, name, readBlock.getReadSequence()));
					if (lines.size() == 10000) {
						Utilities.execute(connection, lines);
						lines.clear();
					}
				}
				progress.setProgress(it.getProgress());
			}
			if (lines.size() > 0) {
				Utilities.execute(connection, lines);
			}
		} finally {
			connection.commit();
			connection.setAutoCommit(true);
		}
		progress.reportTaskCompleted();
		System.err.printf("Table 'sequences' row count: %,d%n", Utilities.countRows(connection, "sequences"));
	}
}
