/*
 * MakeEggNOG2Id6.java Copyright (C) 2024. Daniel H. Huson
 *
 *  No usage, copying or distribution without explicit permission.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package megan.tools_updating;

import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;

import java.io.IOException;

import static megan.tools_updating.MakeEggNOGTree6.ARCOG_ID_OFFSET;
import static megan.tools_updating.MakeEggNOGTree6.KOG_ID_OFFSET;

/**
 * make the eggNOG to Id mapping version 6
 * Daniel Huson, 2024, 4.2026
 */
public class MakeEggNOG2Id6 {

	/**
	 * builds the eggnog accession to id mapping
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);
			PeakMemoryUsageMonitor.start();
			(new MakeEggNOG2Id6()).run(args);
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
		ResourceManager.insertResourceRoot(megan.resources.Resources.class);

		final ArgsOptions options = new ArgsOptions(args, this, "Make eggNOG accession to id mapping");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");

		options.comment("Input and output");
		var inputFile = options.getOptionMandatory("-i", "input", "Input file (http://eggnog6.embl.de/download/eggnog_6.0/e6.seq2ogs.tsv)", "e6.seq2ogs.tsv");
		var outputFile = options.getOptionMandatory("-o", "output", "Output file containing accession to id map", "eggnog2id.map.gz");

		options.done();

		var countIn = 0L;
		var countOut = 0L;

		try (var it = new FileLineIterator(inputFile, true);
			 var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(outputFile)) {
			while (it.hasNext()) {
				// 336983.ENSCANP00000003193       6HZ3J,5QNNJ,7MVEZ,9FH2V,H47Y3,4R1PH,4ZU6I,987RA,HU7SA,EFZVP,BVFPY,7553C,KOG1756,8ZDAI,9EKPV,93KAN
				var line = it.next();
				var split = line.indexOf('\t');
				if (split > 0) {
					countIn++;
					var accession = line.substring(0, split);
					var cogId = 0;
					for (var label : line.split(",")) {
						if (label.startsWith("COG")) {
							cogId = Integer.parseInt(label.substring(3));
							break;
						} else if (label.startsWith("KOG")) {
							cogId = KOG_ID_OFFSET + Integer.parseInt(label.substring(3));
							break;
						} else if (label.startsWith("arCOG")) {
							cogId = ARCOG_ID_OFFSET + Integer.parseInt(label.substring(5));
							break;
						}
					}
					if (cogId > 0) {
						w.write(accession + "\t" + cogId + "\n");
						countOut++;
					}
				}
			}
		}
		System.err.printf("In: %,13d%n", countIn);
		System.err.printf("Out: %,12d%n", countOut);
	}
}
