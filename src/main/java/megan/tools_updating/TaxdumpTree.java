
/*
 * TaxdumpTree.java Copyright (C) 2026 Daniel H. Huson
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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.NewickIO;
import jloda.phylo.PhyloTree;
import jloda.swing.util.ArgsOptions;
import jloda.util.*;
import megan.main.MeganProperties;

import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;

/**
 * program to create all  ncbi.tre and  ncbi.map files required by MEGAN from the NCBI taxdmp.zip file
 * Daniel Huson, 11.2010
 */
public class TaxdumpTree {
	// maps numbers to ranks
	private final static Map<Integer, String> rankId2Rank = new HashMap<>();
	private final static Map<String, Integer> rank2RankId = new HashMap<>();

	static {
		rankId2Rank.put(100, "species");
		rankId2Rank.put(101, "subspecies");
		rankId2Rank.put(99, "species group");
		rankId2Rank.put(98, "genus");
		rankId2Rank.put(1, "kingdom");
		rankId2Rank.put(2, "phylum");
		rankId2Rank.put(3, "class");
		rankId2Rank.put(4, "order");
		rankId2Rank.put(5, "family");
		rankId2Rank.put(0, "no rank");
		rankId2Rank.put(90, "varietas");
		for (Integer id : rankId2Rank.keySet()) {
			rank2RankId.put(rankId2Rank.get(id), id);
		}
	}

	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramName("taxdump-tree");

			PeakMemoryUsageMonitor.start();
			(new TaxdumpTree()).run(args);
			System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
			System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
			System.exit(0);
		} catch (Exception ex) {
			Basic.caught(ex);
			System.exit(1);
		}
	}

	/**
	 * create all files ncbi.tre, ncbi.map and ncbi.lvl required by MEGAN from the NCBI taxdmp.zip file
	 */
	private void run(String[] args) throws UsageException, IOException {
		final var options = new ArgsOptions(args, this, "computes the NCBI taxonomy files for MEGAN");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setAuthors("Daniel H. Huson");
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setLatexDescription("This is used to create the ncbi.tre and ncbi.map tree and mapping files from an NCBI taxdump file.");

		var taxDumpFile = options.getOptionMandatory("-i", "input", "taxdump file (Zip file, usually from:  ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdmp.zip)", "");
		var treeFile = options.getOption("-t", "tree", "Output tree file", FileUtils.getFilePath(taxDumpFile, "ncbi.tre"));

		var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", megan.main.Megan7.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		final var ncbiTreeFromTaxDump = new TaxdumpTree();
		ncbiTreeFromTaxDump.createOutputFiles(new File(taxDumpFile), treeFile);
	}

	/**
	 * create the output files
	 */
	private void createOutputFiles(File taxDumpFile, String treeFile) throws IOException {
		final var taxonId2Name = new TreeMap<Integer, String>();
		final var taxonIds = new LinkedList<Integer>();
		final var synonym2TaxonId = new TreeMap<String, Integer>();
		final var taxonId2TaxonRank = new HashMap<Integer, Integer>();
		final var taxonId2ParentId = new HashMap<Integer, Integer>();
		var tree = new PhyloTree();

		try (var zipFile = new ZipFile(taxDumpFile, ZipFile.OPEN_READ)) {
			System.err.println("Parsing names.dmp");
			try (var namesReader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipFile.getEntry("names.dmp"))))) {
				parseNames(namesReader, taxonId2Name, synonym2TaxonId, taxonIds);
			}
			System.err.printf("Distinct taxa: %,d%n", taxonId2Name.size());
			System.err.printf("Synonyms: %,d%n", synonym2TaxonId.size());

			writeSynonymsFile(synonym2TaxonId);
			synonym2TaxonId.clear();

			System.err.println("Parsing nodes.dmp");
			try (var nodesReader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipFile.getEntry("nodes.dmp"))))) {
				parseNodes(nodesReader, taxonId2TaxonRank, taxonId2ParentId);
			}
			System.err.printf("Nodes: %,d%n", taxonId2ParentId.size());
		}

		System.err.println("Building tree");
		buildTree(taxonIds, taxonId2ParentId, tree);
		sortChildrenOfRoot(tree);
		sortAllOtherNodesAlphabetically(tree, taxonId2Name);
		//reverseTree(tree);
		checkTree(tree);

		var rootId = NumberUtils.parseInt(tree.getLabel(tree.getRoot()));
		taxonId2Name.put(rootId, "NCBI");

		System.err.printf("Tree has %,d nodes and %,d edges%n", tree.getNumberOfNodes(), tree.getNumberOfEdges());

		System.err.println("Writing files");
		writeTreeFile(treeFile, tree);
		writeMappingFile(FileUtils.replaceFileSuffix(treeFile, ".map"), taxonId2Name, taxonId2TaxonRank);
		writeLevelsFile(FileUtils.replaceFileSuffix(treeFile, ".lvl"));
		writeInfoFile(FileUtils.replaceFileSuffix(treeFile, ".info"));
	}

	/**
	 * parse the nodes-dump file to get mappings from names to ids
	 */
	private void parseNames(BufferedReader reader, SortedMap<Integer, String> taxonId2Name, SortedMap<String, Integer> synonym2TaxonId, List<Integer> taxonIds) throws IOException {
		Set<String> seen = new HashSet<>();

		String aLine;
		while ((aLine = reader.readLine()) != null) {
			aLine = aLine.trim();
			if (aLine.isEmpty() || aLine.startsWith("#"))
				continue;
			String[] tokens = StringUtils.split(aLine, '|');
			if (tokens.length == 4) {
				Integer taxonId = Integer.parseInt(tokens[0]);
				String name = tokens[1];

				if (!tokens[2].isEmpty())
					name = "!!!" + tokens[2]; // unique name, temporarly label with !!! for attempt to remove <bla> below
				String type = tokens[3];

				if (type.equalsIgnoreCase("scientific name")) {
					if (taxonId2Name.get(taxonId) != null)
						System.err.println("TaxonId " + taxonId + " already assigned to: " + taxonId2Name.get(taxonId) + ", now reassigned to: " + name);
					else {
						taxonId2Name.put(taxonId, name);
						taxonIds.add(taxonId);
						if (seen.contains(name))
							System.err.println("Name already seen: " + name);
						else
							seen.add(name);
					}
				} else // synonym
					synonym2TaxonId.put(name, taxonId);
			}
		}
		// remove additional stuff from unique names if not necessary
		for (var taxonId : taxonIds) {
			var name = taxonId2Name.get(taxonId);
			if (name.startsWith("!!!")) {
				boolean done = false;
				if (name.contains(" <") && name.endsWith(">")) {
					var shortName = name.substring(3, name.lastIndexOf(" <")).trim();
					if (!seen.contains(shortName)) {
						// System.err.println(name + " -> " + shortName);
						taxonId2Name.put(taxonId, shortName);
						seen.remove(name);
						seen.add(shortName);
						synonym2TaxonId.put(name.substring(3), taxonId);
						done = true;
					}
				}
				if (!done) {
					taxonId2Name.put(taxonId, name.substring(3));
					seen.remove(name);
					seen.add(name.substring(3));
				}
			}
		}
	}

	/**
	 * parses the nodes dump file, creates the ncbi tree and files the taxonid2taxon rank map
	 */
	private void parseNodes(BufferedReader reader, Map<Integer, Integer> taxonId2Rank, Map<Integer, Integer> taxonId2ParentId) throws IOException {
		String aLine;
		while ((aLine = reader.readLine()) != null) {
			aLine = aLine.trim();
			if (aLine.isEmpty() || aLine.startsWith("#"))
				continue;
			var tokens = StringUtils.split(aLine, '|');
			if (tokens.length == 13) {
				Integer taxonId = Integer.parseInt(tokens[0]);
				Integer parentId = Integer.parseInt(tokens[1]);
				taxonId2ParentId.put(taxonId, parentId);
				String rankName = tokens[2];
				if (rank2RankId.get(rankName) != null)
					taxonId2Rank.put(taxonId, rank2RankId.get(rankName));
				else
					taxonId2Rank.put(taxonId, 0);
			}
		}
	}

	/**
	 * parses the nodes dump file, creates the ncbi tree and files the taxonid2taxon rank map
	 */
	private void buildTree(List<Integer> taxonIds, Map<Integer, Integer> taxonId2ParentId, PhyloTree tree) {
		var taxonId2Node = new HashMap<Integer, Node>();
		for (var taxonId : taxonIds) {
			var v = tree.newNode();
			tree.setLabel(v, "" + taxonId);
			taxonId2Node.put(taxonId, v);
			if (taxonId == 1)
				tree.setRoot(v);
		}

		for (var taxonId : taxonIds) {
			var parentId = taxonId2ParentId.get(taxonId);
			if (!taxonId.equals(parentId)) {
				var v = taxonId2Node.get(parentId);
				var w = taxonId2Node.get(taxonId);
				if (v == null)
					System.err.println("Warning: parentId has no node: " + parentId);
				else if (w == null)
					System.err.println("Warning: taxonId has no node: " + taxonId);
				else
					tree.newEdge(v, w);
			}
		}
	}

	/**
	 * makes sure the computed graph is a proper tree
	 */
	private void checkTree(PhyloTree tree) throws IOException {
		try (var visited = tree.newNodeSet()) {
			final var stack = new Stack<Node>();
			stack.push(tree.getRoot());
			visited.add(tree.getRoot());
			while (!stack.isEmpty()) {
				final var v = stack.pop();
				if (v.getInDegree() > 1)
					throw new IOException("Computed graph has node with indegree " + v.getInDegree());

				for (var w : v.children()) {
					if (visited.contains(w)) {
						throw new IOException("Computed graph is not a tree, has cycles");
					}
					visited.add(w);
					stack.push(w);
				}
			}
			if (visited.size() != tree.getNumberOfNodes())
				throw new IOException("Computed graph is not connected, not all nodes reachable from root");
		}
	}

	/**
	 * sort the children of the root so that cellular organisms come first
	 */
	private void sortChildrenOfRoot(PhyloTree tree) {
		var v = tree.getRoot();
		// cellular organisms to come first
		var newOrder = new Edge[v.getDegree()];
		int count = 1;
		for (var e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
			if (tree.getLabel(e.getTarget()).equals("131567")) // cellular organisms
				newOrder[0] = e;
			else
				newOrder[count++] = e;
		}
		if (newOrder[0] != null) {
			v.rearrangeAdjacentEdges(Arrays.asList(newOrder));
		}
	}

	/**
	 * sort all nodes alphabetically, except for the root node and the 'cellular organisms' nodes
	 */
	private void sortAllOtherNodesAlphabetically(PhyloTree tree, Map<Integer, String> taxonId2Name) {
		for (var v = tree.getFirstNode(); v != null; v = v.getNext()) {
			if (v != tree.getRoot() && !tree.getLabel(v).equals("131567")) {
				var pairs = new TreeSet<Pair<String, Edge>>((stringEdgePair, stringEdgePair1) -> {
					var value = stringEdgePair.getFirst().compareTo(stringEdgePair1.getFirst());
					if (value != 0)
						return value;
					var e = stringEdgePair.getSecond();
					var e1 = stringEdgePair1.getSecond();
					return Integer.compare(e.getId(), e1.getId());
				});

				for (var e : v.outEdges()) {
					Node w = e.getTarget();
					pairs.add(new Pair<>(taxonId2Name.get(Integer.parseInt(tree.getLabel(w))).toLowerCase(), e));
				}
				final var newOrder = new LinkedList<Edge>();
				newOrder.add(v.getFirstInEdge());
				for (var pair : pairs) {
					newOrder.add(pair.getSecond());
				}
				v.rearrangeAdjacentEdges(newOrder);
			}
		}
	}

	/**
	 * creates and writes the synonyms files
	 */
	private void writeSynonymsFile(SortedMap<String, Integer> synonym2taxonId) throws IOException {
		System.err.print("Writing " + "synonyms.map" + ": ");
		var lines = 0;
		try (var w = new BufferedWriter(new FileWriter("synonyms.map"))) {
			for (var name : synonym2taxonId.keySet()) {
				w.write(name + "\t" + synonym2taxonId.get(name) + "\n");
				lines++;
			}
		}
		System.err.println(lines);
	}

	/**
	 * write the tree file
	 */
	private void writeTreeFile(String fileName, PhyloTree tree) throws IOException {
		System.err.print("Writing " + fileName + ": ");
		try (var w = new BufferedWriter(new FileWriter(fileName))) {
			w.write(NewickIO.toString(tree, false) + ";\n");
		}
		System.err.println("done");
	}

	/**
	 * write the mapping file
	 */
	private void writeMappingFile(String fileName, SortedMap<Integer, String> taxonId2ScientificName, Map<Integer, Integer> taxonId2taxonRank) throws IOException {
		System.err.print("Writing " + fileName + ": ");
		var lines = 0;
		try (var w = new BufferedWriter(new FileWriter(fileName))) {
			for (var taxonId : taxonId2ScientificName.keySet()) {
				w.write(taxonId + "\t" + taxonId2ScientificName.get(taxonId) + "\t" + (-1) + "\t" + taxonId2taxonRank.get(taxonId) + "\n");
				lines++;
			}
		}
		System.err.printf("%,d%n", lines);
	}

	/**
	 * writes the levels definition file
	 * todo: this is not really necessary, remove from MEGAN
	 */
	private void writeLevelsFile(String fileName) throws IOException {
		System.err.print("Writing " + fileName + ": ");
		try (var w = new BufferedWriter(new FileWriter(fileName))) {
			for (var rankId : TaxdumpTree.rankId2Rank.keySet()) {
				w.write(rankId + "\t" + TaxdumpTree.rankId2Rank.get(rankId) + "\n");
			}
		}
		System.err.println("done");
	}

	/**
	 * generate an info file
	 */
	private void writeInfoFile(String fileName) throws IOException {
		System.err.print("Writing " + fileName + ": ");
		try (var w = new BufferedWriter(new FileWriter(fileName))) {
			w.write("created: " + (new Date()) + "\n");
			w.write("Cite: Benson et al (2005) NAR 33 D34–38.");
		}
		System.err.println("done");
	}
}
