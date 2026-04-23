/*
 * FastaHash.java Copyright (C) 2026 Daniel H. Huson
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

import jloda.seq.FastAFileIterator;
import jloda.swing.util.ArgsOptions;
import jloda.thirdparty.HexUtils;
import jloda.util.*;
import jloda.util.progress.ProgressPercentage;
import megan.classification.util.TaggedValueIterator;
import megan.main.MeganProperties;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * compute MD5 hashes for sequences
 * Daniel Huson, 5.2015, 1.2022
 */
public class FastaHash {
	/**
	 * compute MD5 hashes for sequences
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName(FastaHash.class.getSimpleName());
			PeakMemoryUsageMonitor.start();
			(new FastaHash()).run(args);
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
	private void run(String[] args) throws UsageException, IOException, NoSuchAlgorithmException {
		final var options = new ArgsOptions(args, this, "FastaHash: Computes a sequence to hash mapping");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");
		options.setLatexDescription("This is used to hash sequences provided in FastA records.");

		options.comment("Input and Output:");
		var inputFiles = options.getOptionMandatory("-i", "input", "Input file(s) (or directory)", new String[0]);
		var inputSuffix = options.getOption("-is", "suffix", "Input file suffix (if directory name given)", "");
		var toUpper = options.getOption("-u", "upper", "Convert all sequence letters to upper case", true);
		var outputFile = options.getOption("-o", "output", "Output file", "stdout");

		options.comment("Options:");
		var mapFirstWords = options.getOption("-mf", "first", "Map first word in header lines to hashes", true);
		var mapWordsAfterSOH = options.getOption("-mo", "other", "Map all words following an ASCII SOH character (code 1)", false);
		var mapTags = options.getOption("-pa", "parseTags", "Map tag-defined accessions to hashes", false);
		var tags = options.getOption("-pt", "tags", "Tags to parse", mapTags || options.isDoHelp() ? new String[]{"ref|", "gb|"} : new String[0]);

		var ignoreHyphenInFirstWord = options.getOption("xh", "excludeHyphen", "exclude entries when the first accession contains a hyphen", true);

		var mapHeaders = options.getOption("-mh", "mapHeader", "Map header lines to hashes (supersedes above options)", false);
		var mapSequences = options.getOption("-ms", "mapSequence", "Map sequences line to hashes (supersedes above options)", false);

		var flip = options.getOption("-f", "flip", "Report hash first, then key", false);

		options.comment("Taxonomy:");
		var reportTaxonomyFromHeaderLines = options.getOption("-tx", "taxonomy", "Extract taxon id from header line and report", false);
		var taxonomyPattern = options.getOption("-tp", "taxonomyPattern", "A regular expression for finding taxon ids", "TaxID=(\\d+)");

		options.comment(ArgsOptions.OTHER);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", megan.main.Megan7.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		var inputFileList = getFiles(List.of(inputFiles), inputSuffix);

		if (mapSequences)
			mapHeaders = false;

		if (mapHeaders || mapSequences) {
			mapFirstWords = false;
			mapWordsAfterSOH = false;
			mapTags = false;
			tags = new String[0];
		}

		var pattern = (reportTaxonomyFromHeaderLines ? Pattern.compile(taxonomyPattern) : null);

		final var accessionIterator = (mapFirstWords || (mapTags && tags.length > 0) ? new TaggedValueIterator(mapFirstWords, true, tags) : null);
		if (accessionIterator != null)
			accessionIterator.setAttemptWordsAfterSOH(mapWordsAfterSOH);

		final var md = MessageDigest.getInstance("MD5");

		var numberOfLines = 0L;
		try (var w = new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(outputFile))) {
			System.err.println("Writing file: " + outputFile);
			for (var inputFile : inputFileList) {
				try (var fastaIterator = new FastAFileIterator(inputFile.getPath())) {
					try (var progress = new ProgressPercentage("Reading file: " + inputFile, fastaIterator.getMaximumProgress())) {
						while (fastaIterator.hasNext()) {
							var pair = fastaIterator.next();
							var line = pair.getFirst();

							if (ignoreHyphenInFirstWord && hasHyphenInFirstWord(line))
								continue;

							var sequence = toUpper ? pair.getSecond().toUpperCase() : pair.getSecond();
							var digest = HexUtils.encodeHexString(md.digest(sequence.getBytes(StandardCharsets.UTF_8)));

							Integer taxId = null;
							if (pattern != null) {
								var matcher = pattern.matcher(line);
								if (matcher.find()) {
									taxId = NumberUtils.parseInt(matcher.group(1));
								}
							}

							if (accessionIterator != null) {
								accessionIterator.restart(line);
								while (accessionIterator.hasNext()) {
									var accession = accessionIterator.next();
									if (!flip)
										w.write(String.format("%s\t%s", accession, digest));
									else
										w.write(String.format("%s\t%s", digest, accession));
									w.write(taxId == null ? "\n" : "\t" + taxId + "\n");

									numberOfLines++;
								}
							} else if (mapHeaders) {
								if (!flip)
									w.write(String.format("%s\t%s\n", line.substring(1).trim(), digest));
								else
									w.write(String.format("%s\t%s\n", digest, line.substring(1).trim()));
								numberOfLines++;
							} else if (mapSequences) {
								if (!flip)
									w.write(String.format("%s\t%s\n", sequence, digest));
								else
									w.write(String.format("%s\t%s\n", digest, sequence));
								numberOfLines++;
							}
							progress.setProgress(fastaIterator.getProgress());
						}
					}
				}
			}
		}
		System.err.printf("Count:%,15d%n", numberOfLines);
	}

	/**
	 * gets all files in a list of files and/or directories
	 *
	 * @param filesAndOrDirectories list
	 * @param requiredSuffix        required in directories
	 * @return list of unique files
	 * @throws IOException if a file is empty or unreadable
	 */
	public static List<File> getFiles(Collection<String> filesAndOrDirectories, String requiredSuffix) throws IOException {
		var list = new ArrayList<File>();
		{
			var seen = new HashSet<File>();
			for (var entry : filesAndOrDirectories) {
				if (FileUtils.isDirectory(entry)) {
					for (var file : (new File(entry)).listFiles((dir, name) -> name.endsWith(requiredSuffix))) {
						if (seen.contains(file))
							System.err.println("File occurs more than once: " + file);
						else {
							seen.add(file);
							FileUtils.checkFileReadableNonEmpty(file.getPath());
							list.add(file);
						}
					}
				} else {
					var file = new File(entry);
					if (seen.contains(file))
						System.err.println("File occurs more than once: " + file);
					else {
						seen.add(file);
						FileUtils.checkFileReadableNonEmpty(file.getPath());
						list.add(file);
					}
				}
			}
		}
		return list;
	}

	public static boolean hasHyphenInFirstWord(String headerLine) {
		var insideWord = false;
		for (var i = 0; i < headerLine.length(); i++) {
			char ch = headerLine.charAt(i);
			if (!insideWord) {
				if (!Character.isWhitespace(ch) && ch != '>')
					insideWord = true;
			} else {
				if (Character.isWhitespace(ch))
					break;
				else if (ch == '-')
					return true;
			}
		}
		return false;
	}
}
