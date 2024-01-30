/*
 * ReadsExporter.java Copyright (C) 2024 Daniel H. Huson
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
package megan.dialogs.export;

import jloda.util.CanceledException;
import jloda.util.FileUtils;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.classification.ClassificationManager;
import megan.data.IConnector;
import megan.data.IReadBlock;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;

import static megan.data.Stats.count;

/**
 * export all reads  to a file (or those associated with the set of selected taxa, if any selected)
 * Daniel Huson, 6.2010
 */
public class ReadsExporter {
	/**
	 * export all matches in file
	 */
	public static long exportAll(IConnector connector, String fileName, ProgressListener progressListener) throws IOException {
		long total = 0;
		try {
			progressListener.setTasks("Export", "Writing all reads");

			try (var w = new BufferedWriter(new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(fileName)));
				 var it = connector.getAllReadsIterator(0, 10000, true, false)) {
				progressListener.setMaximum(it.getMaximumProgress());
				progressListener.setProgress(0);
				while (it.hasNext()) {
					total++;
					write(it.next(), w);
					progressListener.setProgress(it.getProgress());
				}
			}
		} catch (CanceledException ex) {
			System.err.println("USER CANCELED");
		}
		return total;
	}

	/**
	 * export all reads for given set of classids in the given classification
	 */
	public static long export(String classificationName, Collection<Integer> classIds, IConnector connector, String fileName, ProgressListener progressListener) throws IOException, CanceledException {
		long total = 0;
		BufferedWriter w = null;

		try {
			progressListener.setTasks("Export", "Writing selected reads");

			if (fileName.contains("%f")) {
				fileName = fileName.replaceAll("%f", FileUtils.getFileNameWithoutPathOrSuffix(connector.getFilename()));
			}

			final var useOneOutputFile = (!fileName.contains("%t") && !fileName.contains("%i"));
			final var classification = (!useOneOutputFile ? ClassificationManager.get(classificationName, true) : null);

			var maxProgress = 100000L * classIds.size();
			var currentProgress = 0L;
			progressListener.setMaximum(maxProgress);
			progressListener.setProgress(0);
			int countClassIds = 0;
			for (Integer classId : classIds) {
				if (useOneOutputFile) {
					if (w == null)
						w = new BufferedWriter(FileUtils.getOutputWriterPossiblyZIPorGZIP(fileName));
				} else {
					if (w != null)
						w.close();
					var cName = classification.getName2IdMap().get(classId);

					var fName = fileName.replaceAll("%t", StringUtils.toCleanName(cName)).replaceAll("%i", "" + classId);
					w = new BufferedWriter(FileUtils.getOutputWriterPossiblyZIPorGZIP(fName));
				}

				countClassIds++;
				currentProgress = 100000L * countClassIds;
				try (var it = connector.getReadsIterator(classificationName, classId, 0, 10000, true, false)) {
					long progressIncrement = 100000 / (it.getMaximumProgress() + 1);

					while (it.hasNext()) {
						total++;
						write(it.next(), w);
						progressListener.setProgress(currentProgress);
						currentProgress += progressIncrement;
						if (!count.apply((int) total))
							return total;
					}
				}
			}

		} catch (CanceledException ex) {
			System.err.println("USER CANCELED");
		} finally {
			if (w != null)
				w.close();
		}
		return total;
	}

	/**
	 * write the read
	 */
	private static void write(IReadBlock readBlock, Writer w) throws IOException {
		var header = readBlock.getReadHeader();
		if (header != null) {
			if (!header.startsWith(">"))
				w.write(">");
			w.write(header);
			if (!header.endsWith("\n"))
				w.write("\n");
		} else
			w.write(">null\n");
		var sequence = readBlock.getReadSequence();
		if (sequence != null) {
			if (sequence.endsWith("\n\n")) {
				w.write(sequence.substring(0, sequence.length() - 1));
			} else {
				w.write(sequence);
				if (!sequence.endsWith("\n"))
					w.write("\n");
			}
		} else
			w.write("null\n");
	}
}
