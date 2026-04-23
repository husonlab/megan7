
/*
 * ExtractFromNR.java Copyright (C) 2026 Daniel H. Huson
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
import jloda.util.*;
import jloda.util.progress.ProgressPercentage;
import megan.main.MeganProperties;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * extract clustered sequences from nr
 * Daniel Huson, 2.2024
 */
public class ExtractFromNR {
	/**
	 * extract clustered sequences from nr
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName(ExtractFromNR.class.getSimpleName());
			PeakMemoryUsageMonitor.start();
			(new ExtractFromNR()).run(args);
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
		final var options = new ArgsOptions(args, this, "Extracts a clustered database from nr");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");
		options.setLatexDescription("This is used to extract a nr90 or n50 database from the full NCBI-nr database.");

		options.comment("Input and Output:");
		var inputFile = options.getOptionMandatory("-i", "input", "Input file (usually nr.gz)", "");
		var accessionsFile = options.getOptionMandatory("-a", "accessionList", "List of accessions to be extracted (obtained from e.g. megan-map-nr50-Feb2024.mdb)", "");
		var outputFile = options.getOption("-o", "output", "Output file", "stdout");

		options.comment("Options:");
		var prefix = options.getOptionMandatory("-ap", "accessionPrefix", "Database accession pattern (e.g. NCBInr50_)", "");
		var toUpper = options.getOption("-u", "upper", "Convert all sequence letters to upper case", true);

		options.comment(ArgsOptions.OTHER);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", megan.main.Megan7.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		FileUtils.checkFileReadableNonEmpty(inputFile);
		FileUtils.checkFileReadableNonEmpty(accessionsFile);
		FileUtils.checkAllFilesDifferent(inputFile, outputFile, accessionsFile);

		var accessions = new HashSet<String>(100000000);
		var found = new HashSet<String>(100000000);


		try (var it = new FileLineIterator(accessionsFile, true)) {
			while (it.hasNext()) {
				var line = it.next();
				if (line.startsWith(prefix)) {
					line = line.substring(prefix.length());
					accessions.add(line);
				}
			}
		}
		System.err.printf("Read :%,15d%n", accessions.size());

		var inputLines = 0L;
		var outputLines = 0L;

		try (var fastaIterator = new FastAFileIterator(inputFile);
			 var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(outputFile)) {
			try (var progress = new ProgressPercentage("Reading file: " + inputFile, fastaIterator.getMaximumProgress())) {
				while (fastaIterator.hasNext()) {
					var pair = fastaIterator.next();
					var line = pair.getFirst();
					var sequence = toUpper ? pair.getSecond().toUpperCase() : pair.getSecond();

					var nrAccession = getAccession(line.substring(1));
					if (accessions.contains(nrAccession)) {
						var accession = prefix + nrAccession;
						w.write(">" + accession + "\n");
						w.write(sequence);
						w.write("\n");
						outputLines++;
						found.add(nrAccession);
					}
					progress.setProgress(fastaIterator.getProgress());
					inputLines++;
				}
			}
		}

		System.err.printf("Input:%,15d%n", inputLines);
		System.err.printf("Output:%,14d%n", outputLines);
		if ((accessions.size() - found.size()) != 0) {
			System.err.printf("Missed:%,14d%n", (accessions.size() - found.size()));
			accessions.stream().filter(a -> !found.contains(a)).findAny()
					.ifPresent(one -> System.err.println("E.g. this one is missing: " + one));
		}
	}

	private static String determinePrefix(Connection connection, String prefixPattern) throws SQLException, IOException {
		var rs = connection.createStatement().executeQuery("SELECT Accession FROM mappings limit 1");
		if (rs.next()) {
			var accession = rs.getString(1);
			var matcher = Pattern.compile(prefixPattern).matcher(accession);
			if (matcher.matches())
				return matcher.group(1);
		}
		throw new IOException("Couldn't determine accession prefix");
	}

	private static String getAccession(String line) {
		if (line.startsWith(">"))
			line = line.substring(1);
		line = StringUtils.getFirstWord(line);
		var pos = line.indexOf('.');
		if (pos != -1)
			line = line.substring(0, pos);
		return line;
	}
}
