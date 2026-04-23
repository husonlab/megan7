/*
 * MergeMappings.java Copyright (C) 2026 Daniel H. Huson
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

package megan.tools_updating;

import jloda.swing.util.ArgsOptions;
import jloda.util.*;
import jloda.util.progress.ProgressPercentage;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.main.MeganProperties;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * create a file that can be imported into the SQL mapping files.
 * Swallows accession version suffixes
 * Daniel Huson, 6.2020, 2.2022, 2.2026, 4.2026
 */
public class MergeMappings {
	/**
	 * run the program
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName("merge-mappings");
			PeakMemoryUsageMonitor.start();

			(new MergeMappings()).run(args);

			System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
			System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
			System.exit(0);
		} catch (Exception ex) {
			Basic.caught(ex);
			System.exit(1);
		}
	}

	/**
	 * run the program
	 */
	private void run(String[] args) throws Exception {
		final var options = new ArgsOptions(args, this, "Write out the mapping table to be imported into the mapping-DB");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setAuthors("Daniel H. Huson");
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setLatexDescription("This is used to merge multiple mapping tables in to a single table in preparation of creating a mapping database for MEGAN.");

		options.comment("Input");
		final var classificationNames = options.getOptionMandatory("-c", "classifications", "List of names of classifications", new String[0]);
		final var inputFiles = options.getOptionMandatory("-i", "input", "List of input .tab files for classifications", new String[0]);

		options.comment("Output");

		final var outputFile = options.getOption("-o", "output", "Output DB file", "tab.gz");
		var separator = options.getOption("-s", "separator", "Separator", new String[]{"tab", ";", ",", "|"}, "tab");
		if (separator.equalsIgnoreCase("tab"))
			separator = "\t";

		options.comment("Options");

		final var supportedOnly = options.getOption("-supp", "supportedOnly", "Only allow classification names supported by MEGAN", true);

		options.comment(ArgsOptions.OTHER);
		ProgramExecutorService.setNumberOfCoresToUse(options.getOption("-t", "threads", "Number of threads", 8));

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", megan.main.Megan7.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);
		//SetupAccessionForUltimateEdition.apply();

		if (classificationNames.length != inputFiles.length)
			throw new UsageException(String.format("Number of classifications (is %d) must equal number of input files (is %d)", classificationNames.length, inputFiles.length));

		// check that all input files exist:
		FileUtils.checkFileReadableNonEmpty(inputFiles);

		if (!outputFile.isBlank())
			FileUtils.checkFileWritable(true, outputFile);
		if (outputFile.isBlank())
			throw new IOException("No output file supplied");

		for (var i = 0; i < classificationNames.length; i++) {
			if (classificationNames[i].equalsIgnoreCase("ncbi"))
				classificationNames[i] = Classification.Taxonomy;
		}

		var outputClassifications = new BitSet();
		for (var i = 0; i < inputFiles.length; i++) {
			if (!supportedOnly || ClassificationManager.getAllSupportedClassifications().contains(classificationNames[i])) {
				outputClassifications.set(i);
			}
		}

		var currentAccessions = new String[classificationNames.length];
		var currentIds = new int[classificationNames.length];

		var progress = new ProgressPercentage("Processing files:", StringUtils.toString(inputFiles, ", "));
		var iterators = new FileLineIterator[classificationNames.length];
		for (var i = 0; i < classificationNames.length; i++) {
			iterators[i] = new FileLineIterator(inputFiles[i]);
			if (i == 0)
				progress.setMaximum(iterators[i].getMaximumProgress());
			while (iterators[i].hasNext()) {
				var line = iterators[i].next();
				var tokens = line.split("\t");
				if (tokens.length >= 2 && NumberUtils.isInteger(tokens[1]) && NumberUtils.parseInt(tokens[1]) != 0) {
					currentAccessions[i] = removeVersionSuffix(tokens[0]);
					currentIds[i] = NumberUtils.parseInt(tokens[1]);
					break;
				}
			}
			if (currentAccessions[i] == null)
				throw new IOException("No valid entry found in: " + inputFiles[i]);
		}

		var outputCounts = new int[classificationNames.length];
		var previous = "";

		try (var w = outputFile.isBlank() ? null : FileUtils.getOutputWriterPossiblyZIPorGZIP(outputFile)) {
			if (w != null)
				System.err.println("Writing file: " + outputFile);

			Line line;
			while ((line = nextLine(iterators, currentAccessions, currentIds)) != null) {
				if (line.getAccession().equals(previous))
					throw new IOException("Same accession reported multiple times: " + previous);
				else
					previous = line.getAccession();

				if (w != null) {
					line.write(w, separator, outputClassifications);
					for (var i : BitSetUtils.members(outputClassifications)) {
						if (line.getIds()[i] != 0)
							outputCounts[i]++;
					}
				}
				progress.setProgress(iterators[0].getProgress());
			}
		}
		for (var iterator : iterators) {
			iterator.close();
		}
		progress.close();

		if (!outputFile.isBlank()) {
			System.err.println("Output: " + StringUtils.toString("%,d", outputCounts, 0, outputCounts.length, "\t"));
		}
	}

	/**
	 * retrieve the next line to write. Can handle the case that a file contains multiple entries for the same accession.
	 * It will use (only) the first non-0 entry for a given accession. Assumes the input tables are sorted and will throw an exception if they are not
	 *
	 * @param iterators         iterators over the different input files
	 * @param currentAccessions keeps the current accession for each input
	 * @param currentIds        keeps the current id for each input
	 * @return the next line to report
	 * @throws IOException indicates that there has been a problem accessing a file or file is not sorted
	 */
	private Line nextLine(FileLineIterator[] iterators, String[] currentAccessions, int[] currentIds) throws IOException {
		var smallestAccession = Arrays.stream(currentAccessions).filter(s -> !s.isBlank()).min(String::compareTo);
		if (smallestAccession.isPresent()) {
			// copy result
			var accession = smallestAccession.get();
			var ids = new int[currentAccessions.length];
			for (var i = 0; i < currentAccessions.length; i++) {
				if (accession.equals(currentAccessions[i]))
					ids[i] = currentIds[i];
			}
			var result = new Line(accession, ids);
			// move on to next
			for (var i = 0; i < currentAccessions.length; i++) {
				while (currentAccessions[i].equals(accession)) { // there might be more than one match in succession
					currentAccessions[i] = "";
					currentIds[i] = 0;
					while (iterators[i].hasNext()) { // find the next line that has a non-0 id
						var tokens = iterators[i].next().split("\t");
						if (tokens.length >= 2) {
							var itAccession = removeVersionSuffix(tokens[0]);
							if (itAccession.compareTo(accession) < 0)
								throw new IOException(String.format("Input file %d: not sorted, %s encountered before %s", i, accession, itAccession));
							var id = NumberUtils.parseInt(tokens[1]);
							if (id != 0) {
								currentAccessions[i] = itAccession;
								currentIds[i] = id;
								break;
							}
						}
					}
				}
			}
			return result;
		} else
			return null;
	}

	private static String removeVersionSuffix(String accession) {
		return accession.replaceFirst("\\.[0-9]+$", "");
	}

	private static class Line {
		private final String accession;
		private final int[] ids;

		public Line(String accession, int[] ids) {
			this.accession = accession;
			this.ids = ids;
		}

		public String getAccession() {
			return accession;
		}

		public int[] getIds() {
			return ids;
		}

		public void write(Writer w, String separator, BitSet activeClassifications) throws IOException {
			w.write(accession);
			for (var i : BitSetUtils.members(activeClassifications)) {
				var id = ids[i];
				if (id != 0) {
					w.write(separator + id);
				} else
					w.write(separator);
			}
			w.write("\n");
		}
	}
}
