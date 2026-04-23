package megan.tools_updating;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.util.TreeUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * make the EggNOG 6 tree
 * Daniel Huson, 2024, 4.2026
 */
public class MakeEggNOGTree6 {
	public static final int KOG_ID_OFFSET = 1000000;
	public static final int ARCOG_ID_OFFSET = 10000000;


	/**
	 * builds the COG tree and LABEL-to-Integer mapping file
	 */
	public static void main(String[] args) {
		try {
			ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

			final long start = System.currentTimeMillis();
			(new MakeEggNOGTree6()).run(args);
			System.err.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s");
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

		final ArgsOptions options = new ArgsOptions(args, this, "Make eggNOG tree file as required by MEGAN");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2026. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");

		// -ic cog-20.def.tab -ik kog -ia ar14.arCOGdef19.tab -t eggnog.tre

		options.comment("Input");
		var cogDefFile = options.getOptionMandatory("-ic", "inputCOG", "Input COG file (https://ftp.ncbi.nih.gov/pub/COG/COG2020/data/cog-20.def.tab)", "cog-20.def.tab");
		var kogDefFile = options.getOption("-ik", "inputKOG", "Input KOG file (https://ftp.ncbi.nih.gov/pub/COG/KOG/kog)", "kog");
		var arCogDefFile = options.getOption("-ia", "inputArCOG", "Input arcCOG file (https://ftp.ncbi.nih.gov/pub/wolf/COGs/arCOG/ar14.arCOGdef19.tab)", "ar14.arCOGdef19.tab");
		var oldMapFile = options.getOption("-om", "oldMap", "Input oldmap file (usually eggnog.map-old)", "");

		options.comment("Output");
		final String treeFile = options.getOption("-t", "tree", "Output tree file", FileUtils.getFilePath(cogDefFile, "eggnog6.tre"));
		final String mapFile = options.getOption("-m", "map", "Output map file", FileUtils.replaceFileSuffix(treeFile, ".map"));

		options.done();

		FileUtils.checkFileReadableNonEmpty(cogDefFile);
		if (!kogDefFile.isBlank())
			FileUtils.checkFileReadableNonEmpty(kogDefFile);
		if (!arCogDefFile.isBlank())
			FileUtils.checkFileReadableNonEmpty(arCogDefFile);
		if (!oldMapFile.isBlank())
			FileUtils.checkFileReadableNonEmpty(oldMapFile);

		var idNodeMap = new HashMap<Integer, Node>();
		var idLabelMap = new TreeMap<Integer, String>();
		var letterIdMap = new HashMap<Character, Integer>();
		var tree = new PhyloTree();
		tree.setRoot(addNode(tree, null, -100, "EggNOG6", letterIdMap, idNodeMap, idLabelMap));

		// setup the top part of the tree:
		{
			var v = addNode(tree, tree.getRoot(), -101, "informationStorageAndProcessing", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -103, "[A] RNA processing and modification", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -106, "[B] Chromatin structure and dynamics", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -102, "[J] Translation, ribosomal structure and biogenesis", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -104, "[K] Transcription", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -105, "[L] Replication, recombination and repair", letterIdMap, idNodeMap, idLabelMap);
		}
		{
			var v = addNode(tree, tree.getRoot(), -107, "cellularProcessesAndSignaling", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -108, "[D] Cell cycle control, cell division, chromosome partitioning", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -109, "[Y] Nuclear structure", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -110, "[V] Defense mechanisms", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -111, "[T] Signal transduction mechanisms", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -112, "[M] Cell wall/membrane/envelope biogenesis", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -113, "[N] Cell motility", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -114, "[Z] Cytoskeleton", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -115, "[W] Extracellular structures", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -116, "[U] Intracellular trafficking, secretion, and vesicular transport", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -117, "[O] Posttranslational modification, protein turnover, chaperones", letterIdMap, idNodeMap, idLabelMap);
		}
		{
			var v = addNode(tree, tree.getRoot(), -118, "metabolism", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -119, "[C] Energy production and conversion", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -120, "[G] Carbohydrate transport and metabolism", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -121, "[E] Amino acid transport and metabolism", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -122, "[F] Nucleotide transport and metabolism", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -123, "[H] Coenzyme transport and metabolism", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -124, "[I] Lipid transport and metabolism", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -125, "[P] Inorganic ion transport and metabolism", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -126, "[Q] Secondary metabolites biosynthesis, transport and catabolism", letterIdMap, idNodeMap, idLabelMap);
		}
		{
			var v = addNode(tree, tree.getRoot(), -127, "poorlyCharacterized", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -128, "[R] General function prediction only", letterIdMap, idNodeMap, idLabelMap);
			addNode(tree, v, -129, "[S] Function unknown", letterIdMap, idNodeMap, idLabelMap);
		}
		{
			addNode(tree, tree.getRoot(), -130, "[X] Mobilome: prophages, transposons", letterIdMap, idNodeMap, idLabelMap);
		}

		if (!cogDefFile.isBlank()) {
			processCogDefFile(cogDefFile, tree, letterIdMap, idNodeMap, idLabelMap);
		}

		if (!arCogDefFile.isBlank()) {
			processArCogDefFile(arCogDefFile, tree, letterIdMap, idNodeMap, idLabelMap);
		}

		if (!kogDefFile.isBlank()) {
			processKOGFile(kogDefFile, tree, letterIdMap, idNodeMap, idLabelMap);
		}

		for (var v : tree.getRoot().children())
			TreeUtils.sortNodesAlphabeticallyRec(v, idLabelMap, new NodeSet(tree));

		addNode(tree, tree.getRoot(), -1, "No hits", letterIdMap, idNodeMap, idLabelMap);

		addNode(tree, tree.getRoot(), -2, "Not assigned", letterIdMap, idNodeMap, idLabelMap);
		addNode(tree, tree.getRoot(), -3, "Low complexity", letterIdMap, idNodeMap, idLabelMap);

		try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(treeFile)) {
			System.err.println("Writing file: " + treeFile);
			w.write(tree.toBracketString(false) + ";");
			System.err.printf("Nodes: %,d, edges: %,d%n", tree.getNumberOfNodes(), tree.getNumberOfEdges());
		}

		try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(mapFile)) {
			System.err.println("Writing file: " + mapFile);
			for (var id : idLabelMap.keySet()) {
				w.write(String.format("%d\t%s%n", id, idLabelMap.get(id)));
			}
			System.err.printf("Lines: %,d%n", idLabelMap.size());
		}

		var infoFile = FileUtils.replaceFileSuffix(treeFile, ".info");
		try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(infoFile)) {
			System.err.println("Writing file: " + infoFile);
			w.write("created: " + (new Date()) + "\n");
			w.write("Cite: Powell et al (2014) NAR 42 D231-239.\n");
		}


	}

	private void processCogDefFile(String file, PhyloTree tree, HashMap<Character, Integer> letterIdMap, Map<Integer, Node> idNodeMap, Map<Integer, String> idLabelMap) throws IOException {
		// format:
		// COG0001	H	Glutamate-1-semialdehyde aminotransferase	HemL	Heme biosynthesis		2CFB
		try (var it = new FileLineIterator(file, true)) {
			while (it.hasNext()) {
				var line = it.next();
				var tokens = StringUtils.split(line, '\t');
				if (tokens.length >= 4) {
					var cog = tokens[0];
					if (cog.startsWith("COG")) {
						var cogId = Integer.parseInt(cog.substring(3)); // 3=length of "COG"
						var letters = tokens[1];
						var text = tokens[2];
						if (!tokens[3].isBlank() && !tokens[3].equals("-"))
							text += " (" + tokens[3] + ")";
						if (!letters.isEmpty()) {
							for (var i = 0; i < letters.length(); i++) {
								var letter = letters.charAt(i);
								var letterId = letterIdMap.get(letter);
								if (letterId == null) {
									throw new IOException("Unknown letter in line: " + line);
								}
								var letterNode = idNodeMap.get(letterId);
								addNode(tree, letterNode, cogId, cog + " " + text, letterIdMap, idNodeMap, idLabelMap);
							}
						}
					}
				}
			}

		}
	}

	private void processArCogDefFile(String file, PhyloTree tree, HashMap<Character, Integer> letterIdMap, Map<Integer, Node> idNodeMap, Map<Integer, String> idLabelMap) throws IOException {
		// format:
		// arCOG00001	K	-	Transcriptional regulator, PadR family	COG01695	pfam03551

		try (var it = new FileLineIterator(file, true)) {
			while (it.hasNext()) {
				var line = it.next();
				var tokens = StringUtils.split(line, '\t');
				if (tokens.length >= 4) {
					var cog = tokens[0];
					if (cog.startsWith("arCOG")) {
						var cogId = ARCOG_ID_OFFSET + Integer.parseInt(cog.substring(5)); // 5=length of arCOG
						var letters = tokens[1];
						var text = tokens[3];
						if (!tokens[2].isBlank() && !tokens[2].equals("-"))
							text += " (" + tokens[2] + ")";
						if (!letters.isEmpty()) {
							for (var i = 0; i < letters.length(); i++) {
								var letter = letters.charAt(i);
								var letterId = letterIdMap.get(letter);
								if (letterId == null)
									throw new IOException("Unknown letter in line: " + line);
								var letterNode = idNodeMap.get(letterId);
								addNode(tree, letterNode, cogId, cog + " " + text, letterIdMap, idNodeMap, idLabelMap);
							}
						}
					}
				}
			}
		}
	}

	private void processKOGFile(String file, PhyloTree tree, HashMap<Character, Integer> letterIdMap, Map<Integer, Node> idNodeMap, Map<Integer, String> idLabelMap) throws IOException {
		// format:
		// arCOG00001	K	-	Transcriptional regulator, PadR family	COG01695	pfam03551

		try (var it = new FileLineIterator(file, true)) {
			var regex = "\\[(.*?)\\]\\s(\\w+)\\s(.*)";
			var pattern = Pattern.compile(regex);

			while (it.hasNext()) {
				var line = it.next();

				var matcher = pattern.matcher(line);

				if (matcher.find()) {
					var letters = matcher.group(1);
					var cog = matcher.group(2);
					if (cog.startsWith("KOG")) {
						var cogId = KOG_ID_OFFSET + Integer.parseInt(cog.substring(3)); // 3=length of "KOG"
						var text = matcher.group(3);
						if (!letters.isEmpty()) {
							for (var i = 0; i < letters.length(); i++) {
								var letter = letters.charAt(i);
								var letterId = letterIdMap.get(letter);
								if (letterId == null)
									throw new IOException("Unknown letter in line: " + line);
								var letterNode = idNodeMap.get(letterId);
								addNode(tree, letterNode, cogId, cog + " " + text, letterIdMap, idNodeMap, idLabelMap);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * add a node to the eggNOG tree
	 */
	private static Node addNode(PhyloTree tree, Node parent, int id, String label, Map<Character, Integer> letterIdMap, Map<Integer, Node> idNodeMap, Map<Integer, String> idLabelMap) {
		var v = tree.newNode(id);
		tree.setLabel(v, String.valueOf(id));
		idLabelMap.put(id, label);
		idNodeMap.put(id, v);
		if (label.length() >= 3 && label.charAt(0) == '[' && Character.isAlphabetic(label.charAt(1)) && label.charAt(2) == ']')
			letterIdMap.put(label.charAt(1), id);
		if (parent != null)
			tree.newEdge(parent, v);
		return v;
	}

}
