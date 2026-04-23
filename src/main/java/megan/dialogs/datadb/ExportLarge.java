package megan.dialogs.datadb;

import jloda.util.BitSetUtils;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.algorithms.ActiveMatches;
import megan.classification.Classification;
import megan.core.Document;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Set;

public class ExportLarge {
	public static String CREATE_MATCHES_TABLE = "CREATE TABLE matches (name TEXT, accession TEXT, bitscore NUMERIC, expected NUMERIC, identity NUMERIC, length NUMERIC, qstart INTEGER, qend INTEGER);";
	public static String INSERT_MATCH_TEMPLATE = "INSERT INTO matches VALUES ('%s','%s',%s,%s,%s,%d,%d,%d);";

	public static void apply(Connection connection, Document doc, Set<String> selectedReads, ProgressListener progress) throws IOException, SQLException {

		Utilities.execute(connection, "DROP TABLE IF EXISTS matches;", CREATE_MATCHES_TABLE);

		connection.setAutoCommit(false);
		try (var it = doc.getConnector().getAllReadsIterator(0, 10, false, true)) {
			progress.setTasks("Exporting large", "matches");
			progress.setProgress(0);
			progress.setMaximum(it.getMaximumProgress());
			var lines = new ArrayList<String>();
			while (it.hasNext()) {
				var readBlock = it.next();
				var name = readBlock.getReadName();
				if (selectedReads == null || selectedReads.contains(name)) {
					final BitSet activeMatches = new BitSet();
					if (false)
						ActiveMatches.compute(doc.getMinScore(), doc.isLongReads() ? 100 : doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, Classification.Taxonomy, activeMatches);
					else {
						for (var m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++)
							activeMatches.set(m);
					}
					for (var m : BitSetUtils.members(activeMatches)) {
						var matchBlock = readBlock.getMatchBlock(m);
						var accession = matchBlock.getTextFirstWord().replaceAll("\\.[0-9]*", "");
						var bitScore = matchBlock.getBitScore();
						var expected = matchBlock.getExpected();
						var percentIdentity = matchBlock.getPercentIdentity();
						var alignmentLength = matchBlock.getLength();
						var queryStart = matchBlock.getAlignedQueryStart();
						var queryEnd = matchBlock.getAlignedQueryEnd();
						lines.add(String.format(INSERT_MATCH_TEMPLATE, name, accession, StringUtils.removeTrailingZerosAfterDot(bitScore),
								StringUtils.removeTrailingZerosAfterDot(expected), StringUtils.removeTrailingZerosAfterDot(percentIdentity), alignmentLength, queryStart, queryEnd));
						if (lines.size() == 10000) {
							Utilities.execute(connection, lines);
							lines.clear();
						}
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
		System.err.printf("Table 'matches' row count: %,d%n", Utilities.countRows(connection, "matches"));
	}
}
