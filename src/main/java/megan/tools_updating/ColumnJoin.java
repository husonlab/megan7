/*
 * ColumnJoin.java Copyright (C) 2026 Daniel H. Huson
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
import megan.main.MeganProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * transfers classification using common column
 * Daniel Huson,1.2022, 2.2024
 */
public class ColumnJoin {
	/**
	 * transfers classification other accessions using common column
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName("column-join");
			PeakMemoryUsageMonitor.start();
			(new ColumnJoin()).run(args);
			System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
			System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
			System.exit(0);
		} catch (Exception ex) {
			Basic.caught(ex);
			System.exit(1);
		}
	}

	/**
	 * run
	 */
	private void run(String[] args) throws UsageException, IOException {
		final var options = new ArgsOptions(args, this, "ColumnJoin: join two mapping files via common column");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");
		options.setLatexDescription("This is used to join tables on a common column.");

		final var inputFileA = options.getOptionMandatory("-a", "inputA", "First input mapping file", "");
		final var inputFileB = options.getOptionMandatory("-b", "inputB", "Second input mapping file", "");

		final var outputFile = options.getOptionMandatory("-o", "output", "Output mapping file", "");

		final var outputUnusedB = options.getOption("-oa", "outputAdd", "Output additional unused data from inputB", "");

		var keyColumnA = options.getOption("-ka", "keyColumnA", "Key column for first input file (1-based)", 1, 1, 100);
		var valueColumnsA = BitSetUtils.asBitSet(options.getOption("-va", "valueColumnsA", "Value columns for first input file (1-based, if empty: report all non-key columns)", new ArrayList<>()));

		var keyColumnB = options.getOption("-kb", "keyColumnB", "Key column for second input file (1-based)", 1, 1, 100);
		var valueColumnsB = BitSetUtils.asBitSet(options.getOption("-vb", "valueColumnsB", "Value column for second input file (1-based, if empty: report all non-key columns)", new ArrayList<>()));

		var outputFormats = new String[]{"valueA_valueB", "key_valueA", "key_valueB"};
		var outputFormat = options.getOption("-of", "outputFormat", "Output format", outputFormats, outputFormats[0]);
		var outputCase = CollectionUtils.getIndex(outputFormat, outputFormats);

		final var reportNulls = options.getOption("-n", "null", "Report missing values as NULL", true);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", megan.main.Megan7.getDefaultPropertiesFile());

		var ensureUniqueKeys = options.getOption("-u", "uniqueKeys", "Ensure all keys are unique by dropping repeated ones", true);
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		FileUtils.checkFileReadableNonEmpty(inputFileA, inputFileB);
		FileUtils.checkFileWritable(outputFile, true);

		if (!outputUnusedB.isBlank())
			FileUtils.checkFileWritable(outputUnusedB, true);

		if (valueColumnsA.get(0))
			throw new UsageException("--valueColumnsA: must not contain 0");
		if (valueColumnsB.get(0))
			throw new UsageException("--valueColumnsB: must not contain 0");


		// need these to be 0-based below
		keyColumnA--;
		shiftDown(valueColumnsA);

		keyColumnB--;
		shiftDown(valueColumnsB);

		var countInA = 0L;
		var countInB = 0L;
		var countOut = 0L;
		var countDropped = 0L;
		var countUnusedB = 0L;

		var uniqueOutputKeys = (ensureUniqueKeys ? new HashSet<String>(100000000) : null);

		var unusedSet = (outputUnusedB.isBlank() ? null : new TreeSet<String>());

		try (var itA = new FileLineIterator(inputFileA, true);
			 var itB = new FileLineIterator(inputFileB);
			 var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(outputFile);
		) {
			System.err.println("Processing file: " + inputFileB);
			System.err.println("Writing file: " + outputFile);

			var keyA = ""; // current key A
			var valuesA = new ArrayList<String>(); // current values A
			Entry<String, String> nextEntryA = null; // next entry A

			var keyB = "";
			var valuesB = new ArrayList<String>();
			var notUsedValuesB = true;
			Entry<String, String> nextEntryB = null;

			var moveA = true;
			var moveB = true;

			while (true) {
				if (moveA) {
					// get next key and values for A:
					valuesA.clear();
					if (nextEntryA != null) {
						keyA = nextEntryA.getKey();
						valuesA.add(nextEntryA.getValue());
						nextEntryA = null;
					}
					while (true) {
						var pairA = nextPair(itA, keyColumnA, valueColumnsA, reportNulls);
						if (pairA == null)
							break;
						else if (valuesA.isEmpty()) {
							keyA = pairA.getKey();
							valuesA.add(pairA.getValue());
						} else if (pairA.getKey().equals(keyA)) {
							valuesA.add(pairA.getValue());
						} else {
							nextEntryA = pairA;
							countInA++;
							break;
						}
					}
					if (valuesA.isEmpty())
						break; // finished
				}
				if (moveB) {
					// get next key and values for B:
					if (notUsedValuesB) {
						if (unusedSet != null) {
							unusedSet.add(StringUtils.toString(valuesB, "\n"));
							countUnusedB += valuesB.size();
						}
					}
					notUsedValuesB = true;
					valuesB.clear();
					if (nextEntryB != null) {
						keyB = nextEntryB.getKey();
						valuesB.add(nextEntryB.getValue());
						nextEntryB = null;
					}
					while (true) {
						var pairB = nextPair(itB, keyColumnB, valueColumnsB, reportNulls);
						if (pairB == null)
							break;
						else if (valuesB.isEmpty()) {
							keyB = pairB.getKey();
							valuesB.add(pairB.getValue());
						} else if (pairB.getKey().equals(keyB)) {
							valuesB.add(pairB.getValue());
						} else {
							nextEntryB = pairB;
							countInB++;
							break;
						}
					}
					if (valuesB.isEmpty()) {
						break; // finished
					}
				}
				var compare = keyA.compareTo(keyB);
				if (compare < 0) {
					moveA = true;
					moveB = false;
				} else if (compare > 0) {
					moveA = false;
					moveB = true;
				} else { // same key, print out
					var hasValues = (!valuesA.isEmpty() && !valuesB.isEmpty());
					if (hasValues) {
						for (var valueA : valuesA) {
							for (var valueB : valuesB) {
								switch (outputCase) {
									case 0 -> {
										if (uniqueOutputKeys == null || uniqueOutputKeys.add(valueA)) {
											w.write(valueA + "\t" + valueB + "\n");
											countOut++;
										} else countDropped++;
									}
									case 1 -> {
										if (uniqueOutputKeys == null || uniqueOutputKeys.add(keyA)) {
											w.write(keyA + "\t" + valueA + "\n");
											countOut++;
										} else countDropped++;
									}
									case 2 -> {
										if (uniqueOutputKeys == null || uniqueOutputKeys.add(keyA)) {
											w.write(keyA + "\t" + valueB + "\n");
											countOut++;
										} else countDropped++;
									}
								}
							}
						}
						notUsedValuesB = false;
					}
					moveA = true;
					moveB = true;
				}
				w.flush();
			}
			if (notUsedValuesB) {
				if (unusedSet != null) {
					unusedSet.add(StringUtils.toString(valuesB, "\n"));
					countUnusedB += valuesB.size();
				}
			}
		}

		if (unusedSet != null) {
			try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(outputUnusedB);
				 var progress = new ProgressPercentage("Writing file: " + outputUnusedB)) {
				progress.setMaximum(unusedSet.size());
				for (var string : unusedSet) {
					if (uniqueOutputKeys == null || uniqueOutputKeys.add(string.split("\t")[0])) {
						w.write(string);
						w.write("\n");
					} else countDropped++;
					progress.incrementProgress();
				}
			}
		}
		System.err.printf("InputA: %,12d%n", countInA);
		System.err.printf("InputB: %,12d%n", countInB);
		System.err.printf("Output: %,12d%n", countOut);
		System.err.printf("UnusedB:%,12d%n", countUnusedB);
		if (ensureUniqueKeys)
			System.err.printf("Dropped:%,12d%n", countDropped);

	}

	/**
	 * get the next pair of entries
	 *
	 * @param it           the file iterator
	 * @param keyColumn    the key column (0-based)
	 * @param valueColumns the value columns (0-based) - if empty, will report all non-key-columns as value column
	 * @return key-string, value-string pair
	 */
	private static Entry<String, String> nextPair(FileLineIterator it, int keyColumn, BitSet valueColumns, boolean reportNulls) {
		while (it.hasNext()) {
			var tokens = it.next().split("\t");
			if (tokens.length > keyColumn) {
				return new Entry<>(tokens[keyColumn], getValuesAsString(tokens, keyColumn, valueColumns, reportNulls));
			}
		}
		return null;
	}

	private static String getValuesAsString(String[] tokens, int keyColumn, BitSet valueColumns, boolean reportNulls) {
		var buf = new StringBuilder();
		for (var i = 0; i < tokens.length; i++) {
			if (valueColumns.get(i) || (valueColumns.cardinality() == 0 && i != keyColumn)) {
				if (!buf.isEmpty())
					buf.append(("\t"));
				var value = tokens[i];
				if (reportNulls && value.isEmpty())
					buf.append("NULL");
				else
					buf.append(tokens[i]);
			}
		}
		return buf.toString();
	}

	private static void shiftDown(BitSet bitSet) {
		var previous = BitSetUtils.copy(bitSet);
		bitSet.clear();
		for (var index : BitSetUtils.members(previous)) {
			bitSet.set(index - 1);
		}
	}
}
