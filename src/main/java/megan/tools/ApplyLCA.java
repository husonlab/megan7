/*
 * ApplyLCA.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.swing.util.ProgramProperties;
import jloda.util.Basic;
import jloda.util.FileUtils;
import jloda.util.NumberUtils;
import megan.algorithms.AssignmentUsingLCA;
import megan.classification.Classification;
import megan.main.Megan7;
import megan.main.MeganProperties;
import megan.main.Setup;

import java.io.*;

/**
 * applies the LCA to input lines
 */
public class ApplyLCA {
	/**
	 * apply the LCA
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName("ApplyLCA");
			Setup.apply();

			long start = System.currentTimeMillis();
			(new ApplyLCA()).run(args);
			System.err.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s");
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
		final ArgsOptions options = new ArgsOptions(args, this, "Applies the LCA to taxon-ids");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2025. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");

		final String inputFile = options.getOptionMandatory("-i", "input", "Input  file (stdin ok)", "");
		final String outputFile = options.getOption("-o", "output", "Output file (stdout, .gz ok)", "stdout");
		String separator = options.getOption("-s", "Separator", "Separator character (or detect)", "detect");
		final boolean firstLineIsHeader = options.getOption("-H", "hasHeaderLine", "Has header line", true);
		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", Megan7.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		final AssignmentUsingLCA assignmentUsingLCA = new AssignmentUsingLCA(Classification.Taxonomy, false, 0);

		final Writer w = new BufferedWriter(new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(outputFile)));
		try (BufferedReader r = new BufferedReader(inputFile.equals("stdin") ? new InputStreamReader(System.in) : new FileReader(inputFile))) {
			String line;
			boolean first = true;
			int lineNumber = 0;
			while ((line = r.readLine()) != null) {
				lineNumber++;
				if (first) {
					first = false;
					if (separator.equals("detect")) {
						if (line.contains("\t"))
							separator = "\t";
						else if (line.contains(","))
							separator = ",";
						else if (line.contains(";"))
							separator = ";";
						else
							throw new IOException("Can't detect separator (didn't find tab, comma or semi-colon in first line)");
						if (firstLineIsHeader) {
							w.write(line + "\n");
							continue;
						}
					}
				}
				final String[] tokens = line.split("\\s*" + separator + "\\s*");
				if (tokens.length > 0) {
					int taxonId = -1;
					for (int i = 1; i < tokens.length; i++) {
						final String token = tokens[i].trim();
						if (!NumberUtils.isInteger(token)) {
							taxonId = 0;
							break;
						} else {
							final int id = NumberUtils.parseInt(token);
							if (id > 0) {
								taxonId = (taxonId == -1 ? id : assignmentUsingLCA.getLCA(taxonId, id));
							}
						}
					}
					w.write(tokens[0] + separator + taxonId + "\n");
				}
			}
			w.flush();
		} finally {
			if (!outputFile.equalsIgnoreCase("stdout"))
				w.close();
		}
	}
}
