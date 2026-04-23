/*
 * MakeSeedTree.java Copyright (C) 2022. Daniel H. Huson
 *
 *  No usage, copying or distribution without explicit permission.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */
package megan.tools_updating;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import jloda.util.progress.ProgressPercentage;
import megan.classification.IdMapper;
import megan.main.MeganProperties;
import megan.util.TreeUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * builds the SEED tree
 * Daniel Huson, 12.2019
 */
public class MakeSeedTree {
	/**
	 * builds the SEED tree and mapping files
	 */
	public static void main(String[] args) {
		try {
			final long start = System.currentTimeMillis();
			(new MakeSeedTree()).run(args);
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
		if (args.length == 0 && System.getProperty("user.name").equals("huson")) {
			args = new String[]{
					"-v",
					"-i", "/Volumes/LaCie/anupam/SeedFromPatric/requireSubsystemFormat.txt",
					"-inr", "/Volumes/LaCie/anupam/SeedFromPatric/mergepatricnr",
					"-pm", "/Users/huson/classify/seed/seed-Jan2021/seed.map",
					"-pa", "/Users/huson/classify/seed/seed-Jan2021/acc2seed-Jan2021.map.gz",
					"-t", "/Users/huson/classify/seed/latest-seed/seed-Feb2022.tre",
					"-f", "/Users/huson/classify/seed/latest-seed/fig2id-Feb2022.map",
					"-a", "/Users/huson/classify/seed/latest-seed/acc2seed.map.gz"
			};
		}

		ResourceManager.insertResourceRoot(megan.resources.Resources.class);

		final var options = new ArgsOptions(args, this, "Build SEED tree and mapping files as required by MEGAN");
		options.comment("Input");
		final var inputHierarchyFile = options.getOptionMandatory("-i", "input", "File containing table of figs, names and paths (such as subsystem.txt)", "");
		final var removeSecondColumn = options.getOption("-rmc", "removeSecondColumn", "Remove second column from input", false);
		final var fig2nrFile = options.getOption("-inr", "id2nrFile", "Hash to Fig id to nr accession mapping file (second and third columns) (such as mergepatricnr)", "");
		final var oldMapFile = options.getOptionMandatory("-pm", "previousMap", "Input previous map file (usually old/seed.map)", "");
		final var oldAccFile = options.getOptionMandatory("-pa", "previousAcc", "Input previous accession map file (usually old/acc2seed.map)", "");

		options.comment("Output");
		final var treeFile = options.getOption("-t", "outputTree", "Output tree file", FileUtils.getFilePath(inputHierarchyFile, "seed.tre"));
		final var mapFile = options.getOption("-m", "outputMap", "Output map file", FileUtils.replaceFileSuffix(treeFile, ".map"));
		final var fig2idFile = options.getOption("-f", "outputFig2Id", "Output fig-to-id mapping file", FileUtils.getFilePath(inputHierarchyFile, "fig2id.map"));
		final var acc2idFile = options.getOption("-a", "outputAcc2Seed", "Accession to SEED map output file", "acc2seed.map");
		final var infoFile = FileUtils.replaceFileSuffix(treeFile, ".info");

		options.comment(ArgsOptions.OTHER);
		final var addFigNodes = options.getOption("-n", "addFigNodes", "Add fig nodes as leaves", false);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", megan.main.Megan7.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		FileUtils.checkFileReadableNonEmpty(inputHierarchyFile, fig2nrFile, oldMapFile, oldAccFile);
		FileUtils.checkFileWritable(true, treeFile, mapFile, acc2idFile, infoFile);

		var lastUsed = 0;
		final var old2id = new HashMap<String, Integer>();
		if (!oldMapFile.isEmpty()) {
			try (var it = new FileLineIterator(oldMapFile, true)) {
				while (it.hasNext()) {
					var tokens = StringUtils.split(it.next(), '\t');
					if (tokens.length >= 2 && NumberUtils.isInteger(tokens[0])) {
						var id = NumberUtils.parseInt(tokens[0]);
						old2id.put(toKey(tokens[1]), id);
						lastUsed = Math.max(lastUsed, id);
					}
				}
			}
			System.err.printf("%,d%n", old2id.size());
		}

		final var fig2id = new HashMap<String, Integer>(100000000);
		final var id2label = new HashMap<Integer, String>(1000000);
		final var id2node = new HashMap<Integer, Node>(1000000);
		final var tree = new PhyloTree();

		final Node root;
		{
			root = tree.newNode(1);
			tree.setLabel(root, "" + 1);
			tree.setRoot(root);
			id2label.put(1, "SEED-2022");
			id2node.put(1, root);
		}

		boolean skipFirstLine;
		{
			final var line = FileUtils.getFirstLineFromFile(new File(inputHierarchyFile));
			skipFirstLine = (line != null && line.startsWith("patric_id"));
		}
		// parse the top level hierarchy:
		// expected format:  fig_id, product, role_name, superclass, class, subclass, subsystem_name (tab separated),,,
		// token[0] - fig_id
		// token[2] - role name
		// token[3] ... - path
		try (var it = new FileLineIterator(inputHierarchyFile, true)) {
			while (it.hasNext()) {
				var line = it.next();
				if (skipFirstLine) {
					skipFirstLine = false;
					continue;
				}
				var tokens = StringUtils.split(line, '\t', 1000);

				if (removeSecondColumn) {
					var tmp = new String[tokens.length - 1];
					tmp[0] = tokens[0];
					System.arraycopy(tokens, 2, tmp, 1, tmp.length - 1);
					tokens = tmp;
				}
				if (tokens.length >= 3) {
					final var figName = tokens[0];

					final var functionalRoleName = cleanGreek(tokens[2]);
					final var path = new String[tokens.length - 3];
					System.arraycopy(tokens, 3, path, 0, tokens.length - 3);

					var prev = tree.getRoot();
					for (var label : path) {
						if (label.length() > 0) {
							label = cleanGreek(label);

							var id = old2id.get(toKey(label));
							if (id == null) {
								id = (++lastUsed);
								old2id.put(toKey(label), id);
							}

							var node = id2node.get(id);
							if (node == null) {
								node = tree.newNode();
								tree.setLabel(node, "" + id);
								id2node.put(id, node);
								id2label.put(id, label);
								tree.newEdge(prev, node);
							}
							prev = node;
						}
					}

					if (addFigNodes && !figName.isBlank()) {
						var id = old2id.get(toKey(figName));
						if (id == null) {
							id = (++lastUsed);
							old2id.put(toKey(figName), id);
						}
						fig2id.put(figName, id);
						final var node = tree.newNode();
						tree.setLabel(node, "" + id);
						tree.newEdge(prev, node);
						if (!functionalRoleName.isBlank())
							id2label.put(id, figName + " " + functionalRoleName);
						else
							id2label.put(id, figName);
						prev = node;
					}

					final int id = NumberUtils.parseInt(tree.getLabel(prev));
					fig2id.put(figName, id);
				}
			}
		}
		System.err.printf("Nodes:    %,10d%n", tree.getNumberOfNodes());
		System.err.printf("Edges:    %,10d%n", tree.getNumberOfEdges());

		// sort
		TreeUtils.sortNodesAlphabetically(tree, id2label, new NodeSet(tree));

		// add all special nodes
		addSpecialNodes(tree, id2node, id2label);

		try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(treeFile);
			 var progress = new ProgressPercentage(("Writing file: " + treeFile))) {
			w.write(tree.toBracketString(false) + ";");
			progress.incrementProgress();
		}

		try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(mapFile);
			 var progress = new ProgressPercentage("Writing file: " + mapFile, id2label.size())) {
			for (var id : new TreeSet<>(id2label.keySet())) {
				var label = id2label.get(id);
				w.write(id + "\t" + label + "\n");
				progress.incrementProgress();
			}
		}

		try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(fig2idFile);
			 var progress = new ProgressPercentage("Writing file: " + fig2idFile, fig2id.size())) {
			for (var fig : new TreeSet<>(fig2id.keySet())) {
				w.write(fig + "\t" + fig2id.get(fig) + "\n");
				progress.incrementProgress();
			}
		}

		try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(infoFile)) {
			System.err.println("Writing file: " + infoFile);
			w.write("created: " + (new Date()) + "\n");
			w.write("Cite: Overbeek et al (2014) NAR 44 D206-214.\n");
		}

		try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(FileUtils.replaceFileSuffix(treeFile, "-labeled.tre"));
			 var progress = new ProgressPercentage(("Writing file: " + FileUtils.replaceFileSuffix(treeFile, "-labeled.tre")))) {
			for (var v : tree.nodes()) {
				tree.setLabel(v, StringUtils.toCleanName(id2label.get(NumberUtils.parseInt(tree.getLabel(v)))));
			}
			w.write(tree.toBracketString(false) + ";");
			progress.incrementProgress();
		}

		if (!fig2nrFile.isBlank()) {
			final var acc2id = new TreeMap<String, Integer>();
			if (!oldAccFile.isBlank()) {
				try (var it = new FileLineIterator(oldAccFile, true)) {
					while (it.hasNext()) {
						final var tokens = StringUtils.split(it.next(), '\t');
						if (tokens.length == 2 && !tokens[0].isEmpty() && NumberUtils.isInteger(tokens[1])) {
							final var id = NumberUtils.parseInt(tokens[1]);
							if (id != 0) {
								acc2id.put(tokens[0], id);
							}
						}
					}
				}
				System.err.printf("Previous: %,d%n", acc2id.size());
			}
			final var previousCount = acc2id.size();

			try (var it = new FileLineIterator(fig2nrFile, true)) {
				while (it.hasNext()) {
					final var tokens = StringUtils.split(it.next(), '\t', 10, true);
					if (tokens.length == 2) {
						var id = fig2id.get(tokens[0]);
						if (id != null && id != 0) {
							acc2id.put(tokens[1], id);
						}
					} else if (tokens.length == 3) {
						var id = fig2id.get(tokens[1]);
						if (id != null && id != 0) {
							acc2id.put(tokens[2], id);
						}
					}
				}
				try (var w = FileUtils.getOutputWriterPossiblyZIPorGZIP(acc2idFile);
					 var progress = new ProgressPercentage("Writing file: " + acc2idFile, acc2id.size())) {
					for (var key : acc2id.keySet()) {
						w.write(String.format("%s\t%d\n", key, acc2id.get(key)));
						progress.incrementProgress();
					}
				}
				System.err.printf("Added: %,d%n", (acc2id.size() - previousCount));
			}
		}
	}

	/**
	 * replace some Greek codes by names
	 *
	 * @return cleaned string
	 */
	private String cleanGreek(String str) {
		return str.replaceAll("&#945;", "alpha").replaceAll("&#946;", "beta").replaceAll("&#954;", "kappa").replaceAll("&#963;", "sigma").trim();
	}

	/**
	 * gets the old id or the next unused
	 *
	 * @return old id or next unused
	 */
	private int getId(String label, Single<Integer> currentId, Map<String, Integer> old2id) {
		if (old2id.containsKey(label))
			return old2id.get(label);
		do {
			currentId.set(currentId.get() + 1);
		}
		while (old2id.containsValue(currentId.get()));
		return currentId.get();
	}

	/**
	 * add all special nodes
	 */
	private void addSpecialNodes(PhyloTree tree, Map<Integer, Node> id2node, Map<Integer, String> id2name) {
		final var unclassified = tree.newNode(IdMapper.UNCLASSIFIED_ID);
		tree.setLabel(unclassified, "" + IdMapper.UNCLASSIFIED_ID);
		tree.newEdge(tree.getRoot(), unclassified);
		id2name.put(IdMapper.UNCLASSIFIED_ID, IdMapper.UNCLASSIFIED_LABEL);
		id2node.put(IdMapper.UNCLASSIFIED_ID, unclassified);

		final var unassigned = tree.newNode(IdMapper.UNASSIGNED_ID);
		tree.setLabel(unassigned, "" + IdMapper.UNASSIGNED_ID);
		tree.newEdge(tree.getRoot(), unassigned);
		id2name.put(IdMapper.UNASSIGNED_ID, IdMapper.UNASSIGNED_LABEL);
		id2node.put(IdMapper.UNASSIGNED_ID, unassigned);

		final var noHits = tree.newNode(IdMapper.NOHITS_ID);
		tree.setLabel(noHits, "" + IdMapper.NOHITS_ID);
		tree.newEdge(tree.getRoot(), noHits);
		id2name.put(IdMapper.NOHITS_ID, IdMapper.NOHITS_LABEL);
		id2node.put(IdMapper.NOHITS_ID, noHits);

		final var lowComplexity = tree.newNode(IdMapper.LOW_COMPLEXITY_ID);
		tree.setLabel(lowComplexity, "" + IdMapper.LOW_COMPLEXITY_ID);
		tree.newEdge(tree.getRoot(), lowComplexity);
		id2name.put(IdMapper.LOW_COMPLEXITY_ID, IdMapper.LOW_COMPLEXITY_LABEL);
		id2node.put(IdMapper.LOW_COMPLEXITY_ID, lowComplexity);
	}

	private String toKey(String str) {
		return str.toLowerCase();
	}
}

