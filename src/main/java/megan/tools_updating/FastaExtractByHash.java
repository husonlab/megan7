
/*
 * FastaExtractByHash.java Copyright (C) 2026 Daniel H. Huson
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
import megan.main.MeganProperties;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;

/**
 * extracts sequences by hash values
 * Daniel Huson, 2.2024
 */
public class FastaExtractByHash {
	/**
	 * extracts sequences by hash values
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName("fasta-extract-by-hash");
			PeakMemoryUsageMonitor.start();
			(new FastaExtractByHash()).run(args);
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
		final var options = new ArgsOptions(args, this, "FastaExtractByHash: Extracts sequences from a Fasta file using hashes");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");
		options.setLatexDescription("This is used to extract FastA records from a set of files based on sequence hashes.");

		options.comment("Input and Output");
		var inputFiles = options.getOptionMandatory("-i", "input", "FastA input file(s) (or directory)", new String[0]);
		final var inputSuffix = options.getOption("-is", "inputSuffix", "Input file suffix (if directory name given)", "");
		final var accHashFile = options.getOptionMandatory("-a", "accHashFile", "Input accession-to-hash file", "");
		final var outputFile = options.getOption("-o", "output", "Output file", "stdout");
		options.comment("Options");
		final var prefix = options.getOption("-p", "prefix", "Prefix for accession label", "");
		final var suffix = options.getOption("-s", "suffix", "Suffix for accession label", "");
		final var toUpper = options.getOption("-u", "upper", "Convert all sequence letters to upper case", true);
		var flip = options.getOption("-f", "flip", "Input hash file is flipped, namely hash-to-accession", false);
		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", megan.main.Megan7.getDefaultPropertiesFile());
		options.done();


		MeganProperties.initializeProperties(propertiesFile);

		var inputFileList = FastaHash.getFiles(List.of(inputFiles), inputSuffix);

		var hashAccessionMap = new HashMap<String, String>(100000000);
		{
			var numberOfLines = 0;
			try (var it = new FileLineIterator(accHashFile, true)) {
				while (it.hasNext()) {
					var tokens = StringUtils.split(it.next(), '\t');
					if (tokens.length == 2) {
						if (flip) {
							hashAccessionMap.put(tokens[1], tokens[0]);
						} else {
							hashAccessionMap.put(tokens[0], tokens[1]);
						}
					}
					numberOfLines++;
				}
			}
			System.err.printf("Input: %,12d%n", numberOfLines);
			System.err.printf("Values:%,12d%n", hashAccessionMap.size());
		}

		{
			final var md = MessageDigest.getInstance("MD5");
			var inCount = 0L;
			var outCount = 0L;
			try (var w = new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(outputFile))) {
				System.err.println("Writing file: " + outputFile);
				for (var inputFile : inputFileList) {
					try (var fastaIterator = new FastAFileIterator(inputFile.getPath())) {
						try (var progress = new ProgressPercentage("Reading file: " + inputFile, fastaIterator.getMaximumProgress())) {
							while (fastaIterator.hasNext()) {
								var pair = fastaIterator.next();
								var sequence = toUpper ? pair.getSecond().toUpperCase() : pair.getSecond();
								var digest = HexUtils.encodeHexString(md.digest(sequence.getBytes(StandardCharsets.UTF_8)));
								var accession = hashAccessionMap.get(digest);
								if (accession != null) {
									w.write(">%s%s%s%n".formatted(prefix, accession, suffix));
									w.write(pair.getSecond());
									w.write("\n");
									outCount++;
								}
								inCount++;
								progress.setProgress(fastaIterator.getProgress());
							}
						}
					}
				}
			}
			System.err.printf("Input: %,12d%n", inCount);
			System.err.printf("Output:%,12d%n", outCount);
		}
	}
}
