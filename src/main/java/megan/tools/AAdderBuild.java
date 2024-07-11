/*
 * AAdderBuild.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.swing.util.BasicSwing;
import jloda.swing.util.GFF3FileFilter;
import jloda.util.*;
import jloda.util.interval.Interval;
import jloda.util.progress.ProgressListener;
import jloda.util.progress.ProgressPercentage;
import megan.accessiondb.AccessAccessionMappingDatabase;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.genes.CDS;
import megan.genes.GeneItem;
import megan.genes.GeneItemCreator;
import megan.io.OutputWriter;
import megan.main.Megan7;
import megan.main.MeganProperties;
import megan.main.Setup;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * build the aadder index
 * Daniel Huson, 5.2018
 */
public class AAdderBuild {
	final public static byte[] MAGIC_NUMBER_IDX = "AAddIdxV0.1.".getBytes();
	final public static byte[] MAGIC_NUMBER_DBX = "AAddDbxV0.1.".getBytes();

	private final static String INDEX_CREATOR = "AADD";

	/**
	 * add functional annotations to DNA alignments
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName("aadder-build");
			Setup.apply();

			PeakMemoryUsageMonitor.start();
			(new AAdderBuild()).run(args);
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
	private void run(String[] args) throws CanceledException, IOException, UsageException, SQLException {
		final ArgsOptions options = new ArgsOptions(args, this, "Build the index for AAdder");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2024. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");
		options.setLatexDescription("""
				This is used to build an index that can be used to perform functional binning of reads
				that have been aligned against genomic sequence. It parses files in GFF3 format and creates an index
				to be used with the \\verb^aadder-run^ program.
				""");

		options.comment("Input Output");
		final List<String> gffFiles = options.getOptionMandatory("-igff", "inputGFF", "Input GFF3 files or directory (.gz ok)", new LinkedList<>());
		final String indexDirectory = options.getOptionMandatory("-d", "index", "Index directory", "");

		options.comment("Classification mapping:");

		final String mapDBFile = options.getOption("-mdb", "mapDB", "MEGAN mapping DB (file megan-map.mdb)", "");

		options.comment("Deprecated classification mapping options:");

		final HashMap<String, String> class2AccessionFile = new HashMap<>();

		final String acc2TaxaFile = options.getOption("-a2t", "acc2taxa", "Accession-to-Taxonomy mapping file", "");

		for (String cName : ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy()) {
			class2AccessionFile.put(cName, options.getOption("-a2" + cName.toLowerCase(), "acc2" + cName.toLowerCase(), "Accession-to-" + cName + " mapping file", ""));
		}

		options.comment(ArgsOptions.OTHER);
		final boolean lookInside = options.getOption("-ex", "extraStrict", "When given an input directory, look inside every input file to check that it is indeed in GFF3 format", false);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", Megan7.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		final Collection<String> mapDBClassifications = AccessAccessionMappingDatabase.getContainedClassificationsIfDBExists(mapDBFile);
		if (!mapDBClassifications.isEmpty() && StringUtils.hasPositiveLengthValue(class2AccessionFile))
			throw new UsageException("Illegal to use both --mapDB and ---acc2... options");

		if (!mapDBClassifications.isEmpty())
			ClassificationManager.setMeganMapDBFile(mapDBFile);

		// setup the gff file:
		setupGFFFiles(gffFiles, lookInside);

		// setup gene item creator, in particular accession mapping
		final GeneItemCreator creator;
		if (!mapDBFile.isEmpty())
			creator = setupCreator(mapDBFile);
		else
			creator = setupCreator(acc2TaxaFile, class2AccessionFile);

		// obtains the gene annotations:
		Map<String, ArrayList<Interval<GeneItem>>> dnaId2list = computeAnnotations(creator, gffFiles);

		saveIndex(INDEX_CREATOR, creator, indexDirectory, dnaId2list, dnaId2list.keySet());
	}

	/**
	 * setup the GFF files
	 */
	public static void setupGFFFiles(List<String> gffFiles, boolean lookInside) throws IOException {
		if (gffFiles.size() == 1) {
			final File file = new File(gffFiles.get(0));
			if (file.isDirectory()) {
				System.err.println("Looking for GFF3 files in directory: " + file);
				gffFiles.clear();
				for (File aFile : BasicSwing.getAllFilesInDirectory(file, new GFF3FileFilter(true, lookInside), true)) {
					gffFiles.add(aFile.getPath());
				}
				if (gffFiles.isEmpty())
					throw new IOException("No GFF files found in directory: " + file);
				else
					System.err.printf("Found: %,d%n", gffFiles.size());
			}
		}
	}

	public static GeneItemCreator setupCreator(String mapDBFile) throws IOException, SQLException {
		final AccessAccessionMappingDatabase database = new AccessAccessionMappingDatabase(mapDBFile);
		final ArrayList<String> classificationNames = new ArrayList<>();
		for (String cName : ClassificationManager.getAllSupportedClassifications()) {
			if (database.getSize(cName) > 0)
				classificationNames.add(cName);
		}
		return new GeneItemCreator(classificationNames.toArray(new String[0]), database);
	}

	/**
	 * setup the gene item creator
	 *
	 * @return gene item creator
	 */
	public static GeneItemCreator setupCreator(String acc2TaxaFile, Map<String, String> class2AccessionFile) throws IOException {
		final String[] cNames;
		{
			final ArrayList<String> list = new ArrayList<>();
			if (acc2TaxaFile != null && !acc2TaxaFile.isEmpty())
				list.add(Classification.Taxonomy);
			for (String cName : class2AccessionFile.keySet())
				if (!class2AccessionFile.get(cName).isEmpty() && !list.contains(cName))
					list.add(cName);
			cNames = list.toArray(new String[0]);
		}

		final IdMapper[] idMappers = new IdMapper[cNames.length];

		for (int i = 0; i < cNames.length; i++) {
			final String cName = cNames[i];
			idMappers[i] = ClassificationManager.get(cName, true).getIdMapper();
			if (cName.equals(Classification.Taxonomy) && acc2TaxaFile != null && !acc2TaxaFile.isEmpty())
				idMappers[i].loadMappingFile(acc2TaxaFile, IdMapper.MapType.Accession, false, new ProgressPercentage());
			else
				idMappers[i].loadMappingFile(class2AccessionFile.get(cName), IdMapper.MapType.Accession, false, new ProgressPercentage());
		}
		return new GeneItemCreator(cNames, idMappers);
	}

	/**
	 * compute annotations
	 */
	public static Map<String, ArrayList<Interval<GeneItem>>> computeAnnotations(GeneItemCreator creator, Collection<String> gffFiles) throws IOException, CanceledException {
		Map<String, ArrayList<Interval<GeneItem>>> dnaId2list = new HashMap<>();

		final Collection<CDS> annotations = CDS.parseGFFforCDS(gffFiles, new ProgressPercentage("Processing GFF files"));

		try (ProgressListener progress = new ProgressPercentage("Building annotation list", annotations.size())) {
			for (CDS cds : annotations) {
				ArrayList<Interval<GeneItem>> list = dnaId2list.computeIfAbsent(cds.getDnaId(), k -> new ArrayList<>());
				final GeneItem geneItem = creator.createGeneItem();
				final String accession = cds.getProteinId();
				geneItem.setProteinId(accession.getBytes());
				geneItem.setReverse(cds.isReverse());
				list.add(new Interval<>(cds.getStart(), cds.getEnd(), geneItem));
				progress.incrementProgress();
			}
		}
		return dnaId2list;
	}

	/**
	 * save the index
	 */
	public static void saveIndex(String indexCreator, GeneItemCreator creator, String indexDirectory, Map<String, ArrayList<Interval<GeneItem>>> dnaId2list, Iterable<String> dnaIdOrder) throws IOException {
		// writes the index file:
		long totalRefWithAGene = 0;

		final File indexFile = new File(indexDirectory, "aadd.idx");
		final File dbFile = new File(indexDirectory, "aadd.dbx");
		try (OutputWriter idxWriter = new OutputWriter(indexFile); OutputWriter dbxWriter = new OutputWriter(dbFile);
			 ProgressPercentage progress = new ProgressPercentage("Writing files: " + indexFile + "\n               " + dbFile, dnaId2list.size())) {

			idxWriter.write(MAGIC_NUMBER_IDX);
			idxWriter.writeString(indexCreator);
			idxWriter.writeInt(dnaId2list.size());

			dbxWriter.write(MAGIC_NUMBER_DBX);
			// write the list of classifications:
			dbxWriter.writeInt(creator.numberOfClassifications());
			for (String cName : creator.cNames()) {
				dbxWriter.writeString(cName);
			}

			for (String dnaId : dnaIdOrder) {
				idxWriter.writeString(dnaId);
				final ArrayList<Interval<GeneItem>> list = dnaId2list.get(dnaId);
				if (list == null) {
					idxWriter.writeLong(0); // no intervals
				} else {
					idxWriter.writeLong(dbxWriter.getPosition()); // position of intervals in DB file

					dbxWriter.writeInt(list.size());
					for (Interval<GeneItem> interval : CollectionUtils.randomize(list, 666)) { // need to save in random order
						dbxWriter.writeInt(interval.getStart());
						dbxWriter.writeInt(interval.getEnd());
						interval.getData().write(dbxWriter);
					}
					totalRefWithAGene++;
				}
				progress.incrementProgress();
			}
		}

		System.err.printf("Reference sequences with at least one annotation: %,d of %,d%n", totalRefWithAGene, dnaId2list.size());
	}
}
