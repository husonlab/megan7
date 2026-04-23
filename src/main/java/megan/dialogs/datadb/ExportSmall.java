package megan.dialogs.datadb;

import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.classification.ClassificationManager;
import megan.core.Document;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * exports to SQL in small format
 * Daniel Huson, 1.2023
 */
public class ExportSmall {
	public static String CREATE_SAMPLES_TABLE = "CREATE TABLE samples (name TEXT PRIMARY KEY)  WITHOUT ROWID;";
	public static String INSERT_SAMPLE_TEMPLATE = "INSERT INTO samples VALUES ('%s');";
	public static String CREATE_CLASSIFICATIONS_TABLE = "CREATE TABLE classifications (name TEXT PRIMARY KEY) WITHOUT ROWID;";
	public static String INSERT_CLASSIFICATION_TEMPLATE = "INSERT INTO classifications VALUES ('%s');";

	public static void apply(Connection connection, Document document, ArrayList<String> classifications, Set<String> selectedReads, ProgressListener progress) throws SQLException, IOException {
		// setup one table per classification, one row per sample
		Utilities.execute(connection, CREATE_SAMPLES_TABLE, CREATE_CLASSIFICATIONS_TABLE);

		var numberOfSamples = document.getNumberOfSamples();
		for (var sample : document.getSampleNames()) {
			Utilities.execute(connection, String.format(INSERT_SAMPLE_TEMPLATE, sample));
		}

		var orderClassifications = new ArrayList<String>();

		for (var classificationName : document.getClassificationNames()) {
			if (StringUtils.containsIgnoreCase(classifications, classificationName) || StringUtils.containsIgnoreCase(classifications, ExportToSQLite.ALL)) {
				Utilities.execute(connection, String.format(INSERT_CLASSIFICATION_TEMPLATE, classificationName));

				var createTable = "CREATE TABLE " + classificationName + " (id NUMERIC PRIMARY KEY, name TEXT, '"
								  + StringUtils.toString(document.getSampleNames(), "' NUMERIC,'") + "' NUMERIC) WITHOUT ROWID;";
				Utilities.execute(connection, createTable);
				orderClassifications.add(classificationName);
			}
		}

		var count = 0;
		for (var classificationName : orderClassifications) {
			var classification = ClassificationManager.get(classificationName, true);
			var class2counts = selectedReads == null ? document.getDataTable().getClassification2Class2Counts().get(classificationName) : extract(document, classification.getName(), selectedReads, progress);
			if (class2counts != null) {
				connection.setAutoCommit(false);
				try {
					progress.setTasks("Exporting small", classificationName + " (" + (++count) + " of " + orderClassifications.size() + ")");
					progress.setProgress(0);
					progress.setMaximum(class2counts.size());

					var lines = new ArrayList<String>();
					for (var entry : class2counts.entrySet()) {
						var classId = entry.getKey();
						var className = classification.getName2IdMap().get(classId);
						if (className != null)
							className = className.replaceAll("[,']", "_");
						else
							className = String.valueOf(classId);
						var counts = entry.getValue();
						var buf = new StringBuilder();
						buf.append("INSERT INTO ").append(classificationName).append(" VALUES (").append(classId).append(",'").append(className).append("',");
						for (int i = 0; i < numberOfSamples; i++) {
							if (i > 0)
								buf.append(",");
							buf.append(i < counts.length ? StringUtils.removeTrailingZerosAfterDot(counts[i]) : "0");
						}
						buf.append(");");
						lines.add(buf.toString());
						if (lines.size() >= 10000) {
							Utilities.execute(connection, lines);
							lines.clear();
						}
						progress.incrementProgress();
					}
					Utilities.execute(connection, lines);
					progress.reportTaskCompleted();
				} finally {
					connection.commit();
					connection.setAutoCommit(true);
				}
			}
		}

		for (var name : orderClassifications) {
			System.err.printf("Table '" + name + "' row count: %,d%n", Utilities.countRows(connection, name));
		}
	}

	/**
	 * computes the summary for a selected set of reads
	 *
	 * @param doc
	 * @param classificationName
	 * @param selectedReads
	 * @param progress
	 * @return classification summary
	 * @throws SQLException
	 * @throws IOException
	 */
	public static Map<Integer, float[]> extract(Document doc, String classificationName, Set<String> selectedReads, ProgressListener progress) throws SQLException, IOException {
		var useCounts = doc.getReadAssignmentMode() == Document.ReadAssignmentMode.readCount;

		if (!useCounts)
			System.err.println("Summarizing " + classificationName + " using read lengths");

		var classificationBlock = doc.getConnector().getClassificationBlock(classificationName);
		var class2counts = new HashMap<Integer, float[]>();
		progress.setTasks("Exporting small", "Computing summary for " + classificationName);
		progress.setProgress(0);
		progress.setMaximum(classificationBlock.getKeySet().size());

		for (var classId : classificationBlock.getKeySet()) {
			if (classId != 0) {
				try (var it = doc.getConnector().getReadsIterator(classificationName, classId, 0, 10, false, false)) {
					while (it.hasNext()) {
						var readBlock = it.next();
						var name = readBlock.getReadName();
						if (selectedReads.contains(name)) {
							class2counts.computeIfAbsent(classId, k -> new float[1])[0] += (useCounts ? 1 : readBlock.getReadLength());
						}
					}
				}
			}
			progress.incrementProgress();
		}
		progress.reportTaskCompleted();
		return class2counts;
	}
}
