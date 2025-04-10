/*
 * AAdderRun.java Copyright (C) 2024 Daniel H. Huson
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

package megan.tools;

import jloda.swing.util.ArgsOptions;
import jloda.util.*;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import jloda.util.progress.ProgressPercentage;
import megan.classification.IdMapper;
import megan.genes.GeneItem;
import megan.genes.GeneItemCreator;
import megan.io.IInputReader;
import megan.io.InputReader;
import megan.main.Megan7;
import megan.main.MeganProperties;
import megan.main.Setup;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * add functional annotations to DNA alignments
 * Daniel Huson, 5.2018
 */
public class AAdderRun {
	/**
	 * add functional annotations to DNA alignments
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName("aadder-run");
			Setup.apply();

			PeakMemoryUsageMonitor.start();
			(new AAdderRun()).run(args);
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
	private void run(String[] args) throws CanceledException, IOException, UsageException {
		final ArgsOptions options = new ArgsOptions(args, this, "Adds functional accessions to DNA alignments");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2025. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");
		options.setLatexDescription("""
				This is used to add functional assignments to DNA alignments, using an index created using
				\\verb^aadder-build^.
				""");
		options.comment("Input Output");
		final String[] inputFiles = options.getOptionMandatory("-i", "input", "Input SAM file(s) (.gz ok)", new String[0]);
		final String indexDirectory = options.getOptionMandatory("-d", "index", "AAdd index directory", "");
		final String[] outputFiles = options.getOptionMandatory("-o", "output", "Output file(s) (.gz ok) or directory", new String[0]);
		options.comment(ArgsOptions.OTHER);
		final double minCoverageProportion = options.getOption("-c", "percentToCover", "Percent of alignment that must be covered by protein", 90.00) / 100.0;
		final boolean reportUnmappedAccessions = options.getOption("-rnf", "reportNotFound", "Report the names of DNA references for which no functional accession is available", false);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", Megan7.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		final File outputDir;
		if (outputFiles.length == 1 && ((new File(outputFiles[0])).isDirectory())) {
			outputDir = new File(outputFiles[0]);
		} else {
			outputDir = null;
			if (inputFiles.length != outputFiles.length)
				throw new UsageException("Number of output files doesn't match number of input files");
		}

		final Map<String, Pair<Long, IntervalTree<GeneItem>>> ref2PosAndTree;
		final File indexFile = new File(indexDirectory, "aadd.idx");

		try (InputReader ins = new InputReader(indexFile); ProgressPercentage progress = new ProgressPercentage("Reading file: " + indexFile)) {
			readAndVerifyMagicNumber(ins, AAdderBuild.MAGIC_NUMBER_IDX);
			final String creator = ins.readString();
			System.err.println("Index created by: " + creator);
			final int entries = ins.readInt();
			progress.setMaximum(entries);

			ref2PosAndTree = new HashMap<>(2 * entries);

			for (int t = 0; t < entries; t++) {
				final String dnaId = ins.readString();
				final long pos = ins.readLong();
				ref2PosAndTree.put(dnaId, new Pair<>(pos, null));
				progress.incrementProgress();
			}
		}

		final IntervalTree<GeneItem> emptyTree = new IntervalTree<>();

		final File dbFile = new File(indexDirectory, "aadd.dbx");

		try (InputReader dbxIns = new InputReader(dbFile)) {
			System.err.println("Opening file: " + dbFile);

			readAndVerifyMagicNumber(dbxIns, AAdderBuild.MAGIC_NUMBER_DBX);
			final String[] cNames = new String[dbxIns.readInt()];
			for (int i = 0; i < cNames.length; i++) {
				cNames[i] = dbxIns.readString();
			}
			final GeneItemCreator creator = new GeneItemCreator(cNames, new IdMapper[0]);

			for (int i = 0; i < inputFiles.length; i++) {
				File inputFile = new File(inputFiles[i]);
				final File outputFile;
				if (outputDir != null) {
					outputFile = new File(outputDir, inputFile.getName() + ".out");
				} else
					outputFile = new File(outputFiles[i]);
				if (inputFile.equals(outputFile))
					throw new IOException("Input file equals output file: " + inputFile);
				final boolean gzipOutput = outputFile.getName().toLowerCase().endsWith(".gz");

				long countLines = 0;
				long countAlignments = 0;
				long countAnnotated = 0;
				long countReferencesLoaded = 0;

				final Set<String> refNotFound = new HashSet<>();

				try (final FileLineIterator it = new FileLineIterator(inputFile, true);
					 final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(gzipOutput ? new GZIPOutputStream(new FileOutputStream(outputFile)) : new FileOutputStream(outputFile)))) {
					System.err.println("Writing file: " + outputFile);

					while (it.hasNext()) {
						final String aLine = it.next();
						if (aLine.startsWith("@"))
							w.write(aLine);
						else {

							final String[] tokens = StringUtils.split(aLine, '\t');

							if (tokens.length < 2 || tokens[2].equals("*")) {
								w.write(aLine);
							} else {
								final IntervalTree<GeneItem> tree;
								{
									final int pos = tokens[2].indexOf(".");
									final String ref = (pos > 0 ? tokens[2].substring(0, pos) : tokens[2]);

									final Pair<Long, IntervalTree<GeneItem>> pair = ref2PosAndTree.get(ref);

									if (pair != null) {
										if (pair.getSecond() == null && pair.getFirst() != 0) {
											dbxIns.seek(pair.getFirst());

											int intervalsLength = dbxIns.readInt();
											if (intervalsLength > 0) {
												tree = new IntervalTree<>();
												for (int t = 0; t < intervalsLength; t++) {
													final int start = dbxIns.readInt();
													final int end = dbxIns.readInt();
													final GeneItem geneItem = creator.createGeneItem();
													geneItem.read(dbxIns);
													tree.add(start, end, geneItem);
													//System.err.println(refIndex+"("+start+"-"+end+") -> "+geneItem);
												}
												countReferencesLoaded++;
											} else {
												tree = emptyTree;
											}
											pair.setSecond(tree);
										} else {
											tree = pair.getSecond();
										}
									} else {
										if (!refNotFound.contains(ref)) {
											refNotFound.add(ref);
											if (reportUnmappedAccessions)
												System.err.println("Reference not found: " + ref);
										}
										continue;
									}
								}

								final int startSubject = NumberUtils.parseInt(tokens[3]);
								final int endSubject = startSubject + getRefLength(tokens[5]) - 1;

								final Interval<GeneItem> refInterval = tree.getBestInterval(new Interval<GeneItem>(startSubject, endSubject, null), minCoverageProportion);

								String annotatedRef = tokens[2];
								if (refInterval != null) {
									final GeneItem geneItem = refInterval.getData();
									final String remainder;
									final int len = annotatedRef.indexOf(' ');
									if (len >= 0 && len < annotatedRef.length()) {
										remainder = annotatedRef.substring(len); // keep space...
										annotatedRef = annotatedRef.substring(0, len);
									} else
										remainder = "";
									annotatedRef += (annotatedRef.endsWith("|") ? "" : "|") + geneItem.getAnnotation(refInterval) + remainder;
								}
								for (int t = 0; t < tokens.length; t++) {
									if (t > 0)
										w.write('\t');
									if (t == 2 && !annotatedRef.equals(tokens[2])) {
										w.write(annotatedRef);
										countAnnotated++;
									} else
										w.write(tokens[t]);
								}
							}
							countAlignments++;
						}
						w.write("\n");
						countLines++;
					}
				}

				System.err.printf("Lines:     %,11d%n", countLines);
				System.err.printf("Alignments:%,11d%n", countAlignments);
				System.err.printf("Annotated: %,11d%n", countAnnotated);
				System.err.printf("(Loaded refs:%,9d)%n", countReferencesLoaded);
				if (refNotFound.size() > 0)
					System.err.printf("(Missing refs:%,8d)%n", refNotFound.size());
			}
		}
	}

	private static final Pattern pattern = Pattern.compile("[0-9]+[MDN]+");

	private static int getRefLength(String cigar) {
		final Matcher matcher = pattern.matcher(cigar);
		final ArrayList<String> pairs = new ArrayList<>();
		while (matcher.find())
			pairs.add(matcher.group());

		int length = 0;
		for (String p : pairs) {
			int num = Integer.parseInt(p.substring(0, p.length() - 1));
			length += num;
		}
		return length;

	}

	/**
	 * read and verify a magic number from a stream
	 */
	public static void readAndVerifyMagicNumber(IInputReader ins, byte[] expectedMagicNumber) throws IOException {
		byte[] magicNumber = new byte[expectedMagicNumber.length];
		if (ins.read(magicNumber, 0, magicNumber.length) != expectedMagicNumber.length || !Basic.equal(magicNumber, expectedMagicNumber)) {
			System.err.println("Expected: " + StringUtils.toString(expectedMagicNumber));
			System.err.println("Got:      " + StringUtils.toString(magicNumber));
			throw new IOException("Index is too old or incorrect file (wrong magic number). Please recompute index.");
		}
	}
}
