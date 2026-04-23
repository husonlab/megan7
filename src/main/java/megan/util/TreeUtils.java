package megan.util;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

/**
 * Tree utilities
 * <p>
 * daniel huson, 4.2021
 */
public class TreeUtils {
	/**
	 * sort all nodes alphabetically
	 */
	public static void sortNodesAlphabetically(PhyloTree tree, Map<Integer, String> id2name, NodeSet ignore) {
		for (var v : tree.nodes()) {
			if (ignore == null || !ignore.contains(v)) {
				var pairs = new ArrayList<Pair<String, Edge>>(v.getDegree());
				for (var e : v.outEdges()) {
					pairs.add(new Pair<>(id2name.get(Integer.parseInt(e.getTarget().getLabel())).toUpperCase(), e));
				}
				pairs.sort(Comparator.comparing((Function<Pair<String, Edge>, String>) Pair::getFirst).thenComparingInt(pair -> pair.getSecond().getId()));

				var newOrder = new ArrayList<Edge>(v.getOutDegree());
				if (v.getInDegree() > 0)
					newOrder.add(v.getFirstInEdge());
				for (Pair<String, Edge> pair : pairs) {
					newOrder.add(pair.getSecond());
				}
				v.rearrangeAdjacentEdges(newOrder);
			}
		}
	}

	/**
	 * sort all nodes alphabetically
	 */
	public static void sortNodesAlphabetically(PhyloTree tree, NodeSet ignore) {
		for (var v : tree.nodes()) {
			if (ignore == null || !ignore.contains(v)) {
				var pairs = new ArrayList<Pair<String, Edge>>(v.getDegree());
				for (var e : v.outEdges()) {
					pairs.add(new Pair<>(e.getTarget().getLabel().toUpperCase(), e));
				}
				pairs.sort(Comparator.comparing((Function<Pair<String, Edge>, String>) Pair::getFirst).thenComparingInt(pair -> pair.getSecond().getId()));

				var newOrder = new ArrayList<Edge>(v.getOutDegree());
				if (v.getInDegree() > 0)
					newOrder.add(v.getFirstInEdge());
				for (Pair<String, Edge> pair : pairs) {
					newOrder.add(pair.getSecond());
				}
				v.rearrangeAdjacentEdges(newOrder);
			}
		}
	}

	/**
	 * sort all nodes alphabetically
	 */
	public static void sortNodesAlphabeticallyRec(Node v, Map<Integer, String> id2name, NodeSet ignore) {
		if (ignore == null || !ignore.contains(v)) {
			var pairs = new ArrayList<Pair<String, Edge>>(v.getDegree());
			for (var e : v.outEdges()) {
				pairs.add(new Pair<>(id2name.get(Integer.parseInt(e.getTarget().getLabel())).toUpperCase(), e));
			}
			pairs.sort(Comparator.comparing((Function<Pair<String, Edge>, String>) Pair::getFirst).thenComparingInt(pair -> pair.getSecond().getId()));

			var newOrder = new ArrayList<Edge>(v.getOutDegree());
			if (v.getInDegree() > 0)
				newOrder.add(v.getFirstInEdge());
			for (Pair<String, Edge> pair : pairs) {
				newOrder.add(pair.getSecond());
			}
			v.rearrangeAdjacentEdges(newOrder);
			for (var w : v.children()) {
				sortNodesAlphabeticallyRec(w, id2name, ignore);
			}
		}
	}
}
