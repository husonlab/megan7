/*
 * NeighborNet.java Copyright (C) 2024 Daniel H. Huson
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
package megan.clusteranalysis.nnet;

import jloda.util.CanceledException;
import jloda.util.progress.ProgressListener;
import megan.clusteranalysis.tree.Distances;
import megan.clusteranalysis.tree.Taxa;

import java.util.Objects;
import java.util.Stack;

/**
 * the neighbor-net algorithm
 * Dave Bryant and Daniel Huson, 9.2007
 */
public class NeighborNet {
	private int[] ordering;

	/**
	 * run neighbor-net
	 *
	 * @return splits
	 */
	public SplitSystem apply(ProgressListener progressListener, Taxa taxa, Distances distances) throws CanceledException {
		progressListener.setTasks("Computing clustering", "Neighbor-Net");
		System.err.println("Bryant and Moulton (2004)");
		ordering = new int[taxa.size() + 1];
		if (taxa.size() > 3)
			runNeighborNet(progressListener, taxa.size(), setupMatrix(distances), ordering);
		else
			return new SplitSystem();

		return CircularSplitWeights.compute(ordering, distances, true, 0.0001f);
	}

	/**
	 * gets the circular ordering
	 */
	public int[] getOrdering() {
		return ordering;
	}

	/**
	 * Sets up the working matrix. The original distance matrix is enlarged to
	 * handle the maximum number of nodes
	 *
	 * @param dist Distance block
	 * @return a working matrix of appropriate cardinality
	 */
	private static double[][] setupMatrix(Distances dist) {
		final int ntax = dist.getNtax();
		final int max_num_nodes = Math.max(3, 3 * ntax - 5);
		final double[][] mat = new double[max_num_nodes][max_num_nodes];
		/* Copy the distance matrix into a larger, scratch distance matrix */
		for (int i = 1; i <= ntax; i++) {
			for (int j = 1; j <= ntax; j++)
				mat[i][j] = dist.get(i, j);
		}
		return mat;
	}

	/**
	 * Run the neighbor net algorithm
	 */
	private void runNeighborNet(ProgressListener progressListener, int ntax, double[][] mat, int[] ordering) throws CanceledException {
		NetNode netNodes = new NetNode();

		/* Nodes are stored in a doubly linked list that we set up here */
		for (int i = ntax; i >= 1; i--) /* Initially, all singleton nodes are active */ {
			NetNode taxNode = new NetNode();
			taxNode.id = i;
			taxNode.next = netNodes.next;
			netNodes.next = taxNode;
		}

		for (NetNode taxNode = netNodes; taxNode.next != null; taxNode = taxNode.next)
			/* Set up links in other direction */
			taxNode.next.prev = taxNode;

		/* Perform the agglomeration step */
		final Stack<NetNode> joins = joinNodes(progressListener, mat, netNodes, ntax);
		expandNodes(progressListener, joins, netNodes, ordering);
	}

	/**
	 * Agglomerates the nodes
	 */
	private Stack<NetNode> joinNodes(ProgressListener progressListener, double[][] mat, NetNode netNodes, int num_nodes) throws CanceledException {
		//System.err.println("joinNodes");

		final Stack<NetNode> joins = new Stack<>();
		int num_active = num_nodes;
		int num_clusters = num_nodes;

		while (num_active > 3) {
            /* Special case
            If we let this one go then we get a divide by zero when computing Qpq */
			if (num_active == 4 && num_clusters == 2) {
				final NetNode p = netNodes.next;
				final NetNode q;
				if (p.next != p.nbr)
					q = p.next;
				else
					q = p.next.next;
				if (mat[p.id][q.id] + mat[p.nbr.id][q.nbr.id] < mat[p.id][q.nbr.id] + mat[p.nbr.id][q.id]) {
					join3way(p, q, q.nbr, joins, mat, netNodes, num_nodes);
				} else {
					join3way(p, q.nbr, q, joins, mat, netNodes, num_nodes);
				}
				break;
			}

            /* Compute the "averaged" sums s_i from each cluster to every other cluster.

      To Do: 2x speedup by using symmetry*/

			for (NetNode p = netNodes.next; p != null; p = p.next)
				p.Sx = 0.0;

			for (NetNode p = netNodes.next; p != null; p = p.next) {
				if (p.nbr == null || p.nbr.id > p.id) {
					for (NetNode q = p.next; q != null; q = q.next) {
						if (q.nbr == null || (q.nbr.id > q.id) && (q.nbr != p)) {
							final double dpq;
							if ((p.nbr == null) && (q.nbr == null))
								dpq = mat[p.id][q.id];
							else if ((p.nbr != null) && (q.nbr == null))
								dpq = (mat[p.id][q.id] + mat[p.nbr.id][q.id]) / 2.0;
							else if (p.nbr == null)
								dpq = (mat[p.id][q.id] + mat[p.id][q.nbr.id]) / 2.0;
							else
								dpq = (mat[p.id][q.id] + mat[p.id][q.nbr.id] + mat[p.nbr.id][q.id] + mat[p.nbr.id][q.nbr.id]) / 4.0;

							p.Sx += dpq;
							if (p.nbr != null)
								p.nbr.Sx += dpq;
							q.Sx += dpq;
							if (q.nbr != null)
								q.nbr.Sx += dpq;
						}
					}
					if (progressListener != null)
						progressListener.checkForCancel();
				}
			}

			NetNode Cx = null;
			NetNode Cy = null;
			/* Now minimize (m-2) D[C_i,C_k] - Sx - Sy */
			double best = 0;
			for (NetNode p = netNodes.next; p != null; p = p.next) {
				if ((p.nbr != null) && (p.nbr.id < p.id)) /* We only evaluate one node per cluster */
					continue;
				for (NetNode q = netNodes.next; q != p; q = q.next) {
					if ((q.nbr != null) && (q.nbr.id < q.id)) /* We only evaluate one node per cluster */
						continue;
					if (q.nbr == p) /* We only evaluate nodes in different clusters */
						continue;
					final double Dpq;
					if ((p.nbr == null) && (q.nbr == null))
						Dpq = mat[p.id][q.id];
					else if ((p.nbr != null) && (q.nbr == null))
						Dpq = (mat[p.id][q.id] + mat[p.nbr.id][q.id]) / 2.0;
					else if (p.nbr == null)
						Dpq = (mat[p.id][q.id] + mat[p.id][q.nbr.id]) / 2.0;
					else
						Dpq = (mat[p.id][q.id] + mat[p.id][q.nbr.id] + mat[p.nbr.id][q.id] + mat[p.nbr.id][q.nbr.id]) / 4.0;
					final double Qpq = ((double) num_clusters - 2.0) * Dpq - p.Sx - q.Sx;
					/* Check if this is the best so far */
					if ((Cx == null || (Qpq < best)) && (p.nbr != q)) {
						Cx = p;
						Cy = q;
						best = Qpq;
					}
				}
			}

			/* Find the node in each cluster */
			NetNode x = Cx;
			NetNode y = Cy;

			if (Objects.requireNonNull(Cx).nbr != null || Objects.requireNonNull(Cy).nbr != null) {
				Cx.Rx = ComputeRx(Cx, Cx, Cy, mat, netNodes);
				if (Cx.nbr != null)
					Cx.nbr.Rx = ComputeRx(Cx.nbr, Cx, Cy, mat, netNodes);
				Objects.requireNonNull(Cy).Rx = ComputeRx(Cy, Cx, Cy, mat, netNodes);
				if (Cy.nbr != null)
					Cy.nbr.Rx = ComputeRx(Cy.nbr, Cx, Cy, mat, netNodes);
			}

			int m = num_clusters;
			if (Cx.nbr != null)
				m++;
			if (Cy.nbr != null)
				m++;

			best = ((double) m - 2.0) * mat[Cx.id][Cy.id] - Cx.Rx - Cy.Rx;
			if (Cx.nbr != null) {
				final double Qpq = ((double) m - 2.0) * mat[Cx.nbr.id][Cy.id] - Cx.nbr.Rx - Cy.Rx;
				if (Qpq < best) {
					x = Cx.nbr;
					y = Cy;
					best = Qpq;
				}
			}
			if (Cy.nbr != null) {
				final double Qpq = ((double) m - 2.0) * mat[Cx.id][Cy.nbr.id] - Cx.Rx - Cy.nbr.Rx;
				if (Qpq < best) {
					x = Cx;
					y = Cy.nbr;
					best = Qpq;
				}
			}
			if ((Cx.nbr != null) && (Cy.nbr != null)) {
				final double Qpq = ((double) m - 2.0) * mat[Cx.nbr.id][Cy.nbr.id] - Cx.nbr.Rx - Cy.nbr.Rx;
				if (Qpq < best) {
					x = Cx.nbr;
					y = Cy.nbr;
				}
			}

			/* We perform an agglomeration... one of three types */
			if ((null == Objects.requireNonNull(x).nbr) && (null == Objects.requireNonNull(y).nbr)) {   /* Both vertices are isolated...add edge {x,y} */
				join2way(x, y);
				num_clusters--;
			} else if (null == x.nbr) {     /* X is isolated,  Y  is not isolated*/
				join3way(x, y, y.nbr, joins, mat, netNodes, num_nodes);
				num_nodes += 2;
				num_active--;
				num_clusters--;
			} else if ((null == Objects.requireNonNull(y).nbr) || (num_active == 4)) { /* Y is isolated,  X is not isolated
                                                        OR theres only four active nodes and none are isolated */
				join3way(y, x, x.nbr, joins, mat, netNodes, num_nodes);
				num_nodes += 2;
				num_active--;
				num_clusters--;
			} else {  /* Both nodes are connected to others and there are more than 4 active nodes */
				num_nodes = join4way(x.nbr, x, y, y.nbr, joins, mat, netNodes, num_nodes);
				num_active -= 2;
				num_clusters--;
			}
		}
		return joins;
	}

	/**
	 * agglomerate 2 nodes
	 *
	 * @param x one node
	 * @param y other node
	 */
	private void join2way(NetNode x, NetNode y) {
		x.nbr = y;
		y.nbr = x;
	}

	/**
	 * agglomerate 3 nodes.
	 * Note that this version doesn't rescan num_nodes, you need to
	 * num_nodes+=2 after calling this!
	 *
	 * @param x one node
	 * @param y other node
	 * @param z other node
	 * @return one of the new nodes
	 */
	private NetNode join3way(NetNode x, NetNode y, NetNode z, Stack<NetNode> joins, double[][] mat, NetNode netNodes, int num_nodes) {
		/* Agglomerate x,y, and z to give TWO new nodes, u and v */
/* In terms of the linked list: we replace x and z
       by u and v and remove y from the linked list.
  	 and replace y with the new node z
    Returns a pointer to the node u */
//printf("Three way: %d, %d, and %d\n",x.id,y.id,z.id);

		NetNode u = new NetNode();
		u.id = num_nodes + 1;
		u.ch1 = x;
		u.ch2 = y;

		NetNode v = new NetNode();
		v.id = num_nodes + 2;
		v.ch1 = y;
		v.ch2 = z;

		/* Replace x by u in the linked list */
		u.next = x.next;
		u.prev = x.prev;
		if (u.next != null)
			u.next.prev = u;
		if (u.prev != null)
			u.prev.next = u;

		/* Replace z by v in the linked list */
		v.next = z.next;
		v.prev = z.prev;
		if (v.next != null)
			v.next.prev = v;
		if (v.prev != null)
			v.prev.next = v;

		/* Remove y from the linked list */
		if (y.next != null)
			y.next.prev = y.prev;
		if (y.prev != null)
			y.prev.next = y.next;

		/* Add an edge between u and v, and add u into the list of amalgamations */
		u.nbr = v;
		v.nbr = u;

		/* Update distance matrix */

		for (NetNode p = netNodes.next; p != null; p = p.next) {
			mat[u.id][p.id] = mat[p.id][u.id] = (2.0 / 3.0) * mat[x.id][p.id] + mat[y.id][p.id] / 3.0;
			mat[v.id][p.id] = mat[p.id][v.id] = (2.0 / 3.0) * mat[z.id][p.id] + mat[y.id][p.id] / 3.0;
		}
		mat[u.id][u.id] = mat[v.id][v.id] = 0.0;

		joins.push(u);

		return u;
	}

	/**
	 * Agglomerate four nodes
	 *
	 * @param x2 a node
	 * @param x  a node
	 * @param y  a node
	 * @param y2 a node
	 * @return the new number of nodes
	 */
	private int join4way(NetNode x2, NetNode x, NetNode y, NetNode y2, Stack<NetNode> joins, double[][] mat, NetNode netNodes, int num_nodes) {
/* Replace x2,x,y,y2 by with two vertices... performed using two
       3 way amalgamations */

		NetNode u;

		u = join3way(x2, x, y, joins, mat, netNodes, num_nodes); /* Replace x2,x,y by two nodes, equalOverShorterOfBoth to x2_prev.next and y_prev.next. */
		num_nodes += 2;
		join3way(u, u.nbr, y2, joins, mat, netNodes, num_nodes); /* z = y_prev . next */
		num_nodes += 2;
		return num_nodes;
	}

	/**
	 * Computes the Rx
	 *
	 * @param z        a node
	 * @param Cx       a node
	 * @param Cy       a node
	 * @param mat      the distances
	 * @param netNodes the net nodes
	 * @return the Rx value
	 */
	private double ComputeRx(NetNode z, NetNode Cx, NetNode Cy, double[][] mat, NetNode netNodes) {
		double Rx = 0.0;

		for (NetNode p = netNodes.next; p != null; p = p.next) {
			if (p == Cx || p == Cx.nbr || p == Cy || p == Cy.nbr || p.nbr == null)
				Rx += mat[z.id][p.id];
			else /* p.nbr != null */
				Rx += mat[z.id][p.id] / 2.0; /* We take the average of the distances */
		}
		return Rx;
	}

	/**
	 * Expands the net nodes to obtain the ordering, quickly
	 *
	 * @param joins    stack of amalagations
	 * @param netNodes the net nodes
	 * @param ordering the ordering
	 */
	private void expandNodes(ProgressListener progressListener, Stack<NetNode> joins, NetNode netNodes, int[] ordering) throws CanceledException {
		//System.err.println("expandNodes");
		/* Set up the circular order for the first three nodes */
		NetNode x = netNodes.next;
		NetNode y = x.next;
		NetNode z = y.next;
		z.next = x;
		x.prev = z;

		/* Now do the rest of the expansions */
		while (!joins.empty()) {
/* Find the three elements replacing u and v. Swap u and v around if v comes before u in the
          circular ordering being built up */
			NetNode u = (joins.pop());
			// System.err.println("POP: u="+u);
			NetNode v = u.nbr;
			x = u.ch1;
			y = u.ch2;
			z = v.ch2;
			if (v != u.next) {
				NetNode tmp = u;
				u = v;
				v = tmp;
				tmp = x;
				x = z;
				z = tmp;
			}

			/* Insert x,y,z into the circular order */
			x.prev = u.prev;
			x.prev.next = x;
			x.next = y;
			y.prev = x;
			y.next = z;
			z.prev = y;
			z.next = v.next;
			z.next.prev = z;
			if (progressListener != null)
				progressListener.checkForCancel();
		}

		/* When we exit, we know that the point x points to a node in the circular order */
		/* We loop through until we find the node after taxa zero */
		while (x.id != 1) {
			x = x.next;
		}

		/* extract the ordering */
		NetNode a = x;
		int t = 0;
		do {
			// System.err.println("a="+a);
			ordering[++t] = a.id;
			a = a.next;
		} while (a != x);

	}
}

/* A node in the net */

class NetNode {
	int id = 0;
	NetNode nbr = null; // adjacent node
	NetNode ch1 = null; // first child
	NetNode ch2 = null; // second child
	NetNode next = null; // next in list of active nodes
	NetNode prev = null; // prev in list of active nodes
	double Rx = 0;
	double Sx = 0;

	public String toString() {
		String str = "[id=" + id;
		str += " nbr=" + (nbr == null ? "null" : ("" + nbr.id));
		str += " ch1=" + (ch1 == null ? "null" : ("" + ch1.id));
		str += " ch2=" + (ch2 == null ? "null" : ("" + ch2.id));
		str += " prev=" + (prev == null ? "null" : ("" + prev.id));
		str += " next=" + (next == null ? "null" : ("" + next.id));
		str += " Rx=" + Rx;
		str += " Sx=" + Sx;
		str += "]";
		return str;
	}
}
