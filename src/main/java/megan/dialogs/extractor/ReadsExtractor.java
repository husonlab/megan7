/*
 * ReadsExtractor.java Copyright (C) 2024 Daniel H. Huson
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
package megan.dialogs.extractor;

import jloda.swing.util.ProgramProperties;
import jloda.util.CanceledException;
import jloda.util.FileUtils;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.ClassificationType;
import megan.core.Document;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * extract reads using the IConnector
 * Daniel Huson, 4.2010
 */
public class ReadsExtractor {


	/**
	 * extract all reads belonging to a given set of taxon ids
	 */
	public static int extractReadsByTaxonomy(final ProgressListener progressListener, final Set<Integer> taxIds,
											 final String outDirectory, final String outFileName, final Document doc, final boolean summarized) throws IOException, CanceledException {
		final Map<Integer, String> classId2Name = new HashMap<>();
		final Map<Integer, Collection<Integer>> classId2Descendants = new HashMap<>();
		for (Integer id : taxIds) {
			classId2Name.put(id, TaxonomyData.getName2IdMap().get(id));
			if (summarized)
				classId2Descendants.put(id, TaxonomyData.getTree().getAllDescendants(id));
		}
		return extractReads(progressListener, ClassificationType.Taxonomy.toString(), taxIds, classId2Name, classId2Descendants, outDirectory, outFileName, doc, summarized);
	}

	/**
	 * extract all reads belonging to a given set of  ids
	 */
	public static int extractReadsByFViewer(final String cName, final ProgressListener progressListener, final Collection<Integer> classIds,
											final String outDirectory, final String outFileName, final Document doc, boolean summarized) throws IOException, CanceledException {

		final var classification = ClassificationManager.get(cName, true);
		var classId2Name = new HashMap<Integer, String>();
		var classId2Descendants = new HashMap<Integer, Collection<Integer>>();
		for (var id : classIds) {
			classId2Name.put(id, classification.getName2IdMap().get(id));
			classId2Descendants.put(id, classification.getFullTree().getAllDescendants(id));
		}
		return extractReads(progressListener, cName, classIds, classId2Name, classId2Descendants, outDirectory, outFileName, doc, summarized);
	}

	/**
	 * extracts all reads for the given classes
	 */
	private static int extractReads(final ProgressListener progress, final String classificationName, final Collection<Integer> classIds, final Map<Integer, String> classId2Name,
									Map<Integer, Collection<Integer>> classId2Descendants,
									final String outDirectory, String fileName, final Document doc, final boolean summarized) throws IOException, CanceledException {
		progress.setSubtask("Extracting by " + classificationName);

		if (outDirectory.length() > 0) {
			fileName = new File(outDirectory, fileName).getPath();
		}

		final var connector = doc.getConnector();

		if (fileName.contains("%f")) {
			fileName = fileName.replaceAll("%f", FileUtils.getFileNameWithoutPathOrSuffix(connector.getFilename()));
		}

		final var useOneOutputFile = !(fileName.contains("%t") || fileName.contains("%i"));

		var numberOfReads = 0;

		final var classificationBlock = connector.getClassificationBlock(classificationName);

		if (classificationBlock == null)
			return 0;

		BufferedWriter w;
		if (useOneOutputFile) {
			w = new BufferedWriter(new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(fileName)));
			System.err.println("Writing to: " + fileName);
		} else {
			w = null;
		}

		final var maxProgress = 100000L * classIds.size();

		progress.setMaximum(maxProgress);
		progress.setProgress(0L);

		var countClassIds = 0;
		try {

			for (var classId : classIds) {
				countClassIds++;

				final var all = new HashSet<Integer>();
				all.add(classId);
				if (summarized && classId2Descendants.get(classId) != null)
					all.addAll(classId2Descendants.get(classId));

				var first = true;
				final var reportTaxa = classificationName.equals(Classification.Taxonomy) && ProgramProperties.get("report-taxa-in-extract-reads", false);

				try (var it = connector.getReadsIteratorForListOfClassIds(classificationName, all, 0, 10000, true, false)) {
					while (it.hasNext()) {
						if (first) {
							if (!useOneOutputFile) {
								if (w != null)
									w.close();
								final var cName = classId2Name.get(classId);
								var fName = fileName.replaceAll("%t", StringUtils.toCleanName(cName)).replaceAll("%i", "" + classId);
								w = new BufferedWriter(new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(fName)));
							}
							first = false;
						}

						final var readBlock = it.next();
						var readHeader = readBlock.getReadHeader().trim();
						if (!readHeader.startsWith(">"))
							w.write(">");
						w.write(readHeader);
						if (reportTaxa && classId > 0) {
							if (!readHeader.endsWith("|"))
								w.write("|");
							w.write("tax|" + classId);
						}
						w.write("\n");
						var readData = readBlock.getReadSequence();
						if (readData != null) {
							w.write(readData);
							if (!readData.endsWith("\n"))
								w.write("\n");
						}
						w.flush();
						numberOfReads++;
						progress.setProgress((long) (100000.0 * (countClassIds + (double) it.getProgress() / it.getMaximumProgress())));
					}
				}
			}
		} catch (CanceledException ex) {
			System.err.println("USER CANCELED");
		} finally {
			if (w != null)
				w.close();
		}
		return numberOfReads;
	}
}
