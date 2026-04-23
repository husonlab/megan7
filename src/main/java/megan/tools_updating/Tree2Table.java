/*
 * Tree2Table.java Copyright (C) 2026 Daniel H. Huson
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
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.main.MeganProperties;
import megan.viewer.TaxonomicLevels;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * writes a rooted trees as a parent table
 * Daniel Huson, 5.2015, 1.2022
 */
public class Tree2Table {
	/**
	 * writes a rooted trees as a parent table
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName(Tree2Table.class.getSimpleName());

			PeakMemoryUsageMonitor.start();
			(new Tree2Table()).run(args);
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
		final var options = new ArgsOptions(args, this, "Writes a rooted tree as a parent table");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setAuthors("Daniel H. Huson");
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setLatexDescription("Writes a rooted tree as a parent table.");

		var whichTaxonomy = options.getOption("-t", "taxonomy", "Which taxonomy", List.of("NCBI", "GTDB"), "GTDB");

		var outputFile = options.getOption("-o", "output", "Output file", whichTaxonomy.toLowerCase() + ".tsv");

		options.comment("Options:");
		var reportParent = options.getOption("-p", "parent", "Report parent", true);
		var reportId = options.getOption("-d", "id", "Report id", true);
		var reportRank = options.getOption("-r", "ranme", "Report rank", true);
		var reportName = options.getOption("-n", "name", "Report name", true);
		options.comment(ArgsOptions.OTHER);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", megan.main.Megan7.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");

		var classification = switch (whichTaxonomy.toUpperCase()) {
			case "NCBI" -> ClassificationManager.get(Classification.Taxonomy, true);
			case "GTDB" -> ClassificationManager.get("GTDB", true);
			default -> throw new UsageException("--taxonomy: " + whichTaxonomy + ": invalid");
		};


		var numberOfLines = 0;
		try (var w = new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(outputFile))) {
			System.err.println("Writing file: " + outputFile);

			{
				w.write("#");
				var first = true;
				if (reportId) {
					w.write("id");
					first = false;
				}
				if (reportParent) {
					if (first)
						first = false;
					else
						w.write("\t");
					w.write("parent");
				}
				if (reportRank) {
					if (first)
						first = false;
					else
						w.write("\t");
					w.write("rank");
				}
				if (reportName) {
					if (first)
						first = false;
					else
						w.write("\t");
					w.write("name");
				}
				w.write("\n");
			}

			for (var v : classification.getFullTree().nodes()) {
				var first = true;
				if (reportId) {
					if (first)
						first = false;
					else
						w.write("\t");
					var id = (int) v.getInfo();
					w.write(String.valueOf(id));
				}
				if (reportParent) {
					if (first)
						first = false;
					else
						w.write("\t");

					var pid = (int) (v.getInDegree() > 0 ? v.getParent().getInfo() : 0);
					w.write(String.valueOf(pid));
				}
				if (reportRank) {
					if (first)
						first = false;
					else
						w.write("\t");
					var id = (int) v.getInfo();
					var rank = classification.getId2Rank().get(id);
					var code = (rank == null ? null : TaxonomicLevels.getOneLetterCodeFromRank(rank));
					w.write(code == null ? "" : code);
				}
				if (reportName) {
					if (first)
						first = false;
					else
						w.write("\t");
					var id = (int) v.getInfo();
					var name = classification.getName2IdMap().get(id);
					w.write(name);
				}
				if (!first) {
					w.write("\n");
					numberOfLines++;
				}
			}

		}
		System.err.printf("Count:%,15d%n", numberOfLines);
	}
}
