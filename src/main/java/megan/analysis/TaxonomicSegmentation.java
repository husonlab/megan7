/*
 * TaxonomicSegmentation.java Copyright (C) 2024 Daniel H. Huson
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

package megan.analysis;


import jloda.util.CanceledException;
import jloda.util.CollectionUtils;
import jloda.util.Pair;
import jloda.util.StringUtils;
import jloda.util.interval.IntervalTree;
import jloda.util.progress.ProgressListener;
import megan.algorithms.IntervalTree4Matches;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.util.*;

/**
 * computes the taxonomic segmentation of a read
 * Daniel Huson, 8.2018
 */
public class TaxonomicSegmentation {
	private static final int defaultRank = 0; // use next down
	public static final float defaultSwitchPenalty = 10000f; // always non-negative
	public static final float defaultCompatibleFactor = 1f; // always non-negative
	public static final float defaultIncompatibleFactor = 0.2f; // always non-negative

	private int rank = defaultRank; // rank 0 means use rank one below class id rank
	private int classId = 0; // class id to which read is taxonomically assigned, or 0
	private final Map<Integer, Integer> tax2taxAtRank;
	private float switchPenalty = defaultSwitchPenalty;
	private float compatibleFactor = defaultCompatibleFactor;
	private float incompatibleFactor = defaultIncompatibleFactor;

	public String getParamaterString() {
		return "rank=" + TaxonomicLevels.getName(rank) + " classId=" + TaxonomyData.getName2IdMap().get(classId) + " switchPenalty=" + StringUtils.removeTrailingZerosAfterDot("" + switchPenalty)
			   + " compatibleFactor=" + StringUtils.removeTrailingZerosAfterDot("" + compatibleFactor)
			   + " incompatibleFactor=" + StringUtils.removeTrailingZerosAfterDot("" + incompatibleFactor);
	}

	/**
	 * constructor
	 */
	public TaxonomicSegmentation() {
		tax2taxAtRank = new HashMap<>();
	}

	/**
	 * computes the segmentation
	 */
	public ArrayList<Segment> computeTaxonomicSegmentation(ProgressListener progress, IReadBlock readBlock) throws CanceledException {
		final var originalSequence = readBlock.getReadSequence();
		if (originalSequence == null)
			return null;

		progress.setSubtask("Computing intervals");

		final var intervals = IntervalTree4Matches.computeIntervalTree(readBlock, null, progress); // todo: use dominated only?

		final var positions = new TreeSet<Integer>();
		for (var interval : intervals) {
			positions.add(interval.getStart() + 1);
			positions.add(interval.getEnd() - 1);
		}

		final var columns = computeDPColumns(intervals, positions);

		final var allTaxa = new TreeSet<Integer>();
		for (var matchBlock : intervals.values()) {
			if (matchBlock.getTaxonId() > 0) {
				final var tax = getTaxonAtRank(matchBlock.getTaxonId());
				if (tax > 0)
					allTaxa.add(tax);
				else if (classId > 0 && TaxonomyData.isAncestor(tax, classId)) // if alignment lies on or above the target taxon, use it
					allTaxa.add(classId);
			}
		}

		if (allTaxa.size() == 0)
			return new ArrayList<>();

		progress.setSubtask("Running dynamic program");
		progress.setMaximum(columns.size());
		progress.setProgress(0);

		final var tax2row = new HashMap<Integer, Integer>();
		{
			var row = 0;
			for (var tax : allTaxa) {
				tax2row.put(tax, row++);
			}
		}

		final var scoreMatrix = new float[columns.size()][allTaxa.size()];
		final var traceBackMatrix = new int[columns.size()][allTaxa.size()];

		var verbose = false;
		{
			for (var col = 1; col < columns.size(); col++) { // skip the first point
				final var column = columns.get(col);
				if (verbose)
					System.err.println(String.format("DPColumn@ %,d", column.getPos()) + ":");
				for (var tax : allTaxa) {
					if (tax != classId) { // don't use current assigned class, just need to use its alignments
						final var row = tax2row.get(tax);

						var maxScore = -10000000.0f;
						var maxScoreTax = 0;

						for (var otherTax : allTaxa) {
							final var otherRow = tax2row.get(otherTax);

							if (otherTax.equals(tax)) { // look at staying with tax
								final float score;
								if (column.getScore(tax) > 0 || column.getScore(classId) > 0) {
									score = scoreMatrix[col - 1][row] + compatibleFactor * Math.max(column.getScore(tax), column.getScore(classId));
								} else {
									score = scoreMatrix[col - 1][row] - incompatibleFactor * column.getMinAlignmentScore();
								}
								if (score > maxScore) {
									maxScore = score;
									maxScoreTax = tax;
								}

							} else { // other != tax, look at switching from tax to other
								final float score;
								if (column.getScore(otherTax) > 0 || column.getScore(classId) > 0)
									score = scoreMatrix[col - 1][otherRow] - switchPenalty + compatibleFactor * Math.max(column.getScore(otherTax), column.getScore(classId));
								else
									score = scoreMatrix[col - 1][otherRow] - switchPenalty - incompatibleFactor * column.getMinAlignmentScore();

								if (score > maxScore) {
									maxScore = score;
									maxScoreTax = otherTax;
								}
							}
						}

						if (verbose)
							System.err.printf("Traceback %d (%s) %.1f from %d (%s) %.1f%n", tax, TaxonomyData.getName2IdMap().get(tax),
									maxScore, maxScoreTax, TaxonomyData.getName2IdMap().get(maxScoreTax), scoreMatrix[col - 1][tax2row.get(maxScoreTax)]);

						scoreMatrix[col][row] = maxScore;
						traceBackMatrix[col][row] = maxScoreTax;
					}
					progress.incrementProgress();
				}
			}
		}

		final List<Pair<Float, Integer>> bestScores;
		if (columns.size() > 0)
			bestScores = computeBestScores(allTaxa, tax2row, scoreMatrix, 0.1);
		else
			bestScores = new ArrayList<>();
		if (verbose) {
			System.err.println("Best scores and taxa:");
			for (var pair : bestScores) {
				System.err.printf("%d (%s): %.1f%n", pair.getSecond(), TaxonomyData.getName2IdMap().get(pair.getSecond()), pair.getFirst());
			}
		}

		// trace back:
		final var segments = new ArrayList<Segment>();

		if (bestScores.size() > 0) {
			var tax = bestScores.get(0).getSecond();
			var row = tax2row.get(tax);
			var col = scoreMatrix.length - 1;

			while (col > 0) {
				final var currentColumn = columns.get(col);
				var prevCol = col - 1;
				while (prevCol > 0 && traceBackMatrix[prevCol][row] == tax)
					prevCol--;
				final var prevColumn = columns.get(prevCol);
				if (tax > 0)
					segments.add(new Segment(prevColumn.getPos(), currentColumn.getPos(), tax));
				if (prevCol > 0) {
					tax = traceBackMatrix[prevCol][row];
					row = tax2row.get(tax);
				}
				col = prevCol;
			}
		}
		// reverse:
		CollectionUtils.reverseInPlace(segments);

		//System.err.println(">" + Basic.swallowLeadingGreaterSign(readBlock.getReadName()) + ": segments: " + Basic.toString(segments, " "));

		return segments;
	}


	/**
	 * compute the columns for the DP
	 *
	 * @return DP data points
	 */
	private ArrayList<DPColumn> computeDPColumns(IntervalTree<IMatchBlock> intervals, TreeSet<Integer> positions) {
		final ArrayList<DPColumn> columns = new ArrayList<>();

		DPColumn prevColumn = null;


		for (var pos : positions) {
			final var column = new DPColumn(pos);
			if (prevColumn != null) {
				for (var interval : intervals.getIntervals(pos)) {
					final var matchBlock = interval.getData();
					final var segmentLength = pos - prevColumn.getPos() + 1;
					if (segmentLength >= 5) {
						final var score = matchBlock.getBitScore() * segmentLength / matchBlock.getLength();
						final var tax = getTaxonAtRank(matchBlock.getTaxonId());
						if (tax > 0)
							column.add(tax, score);
						else if (classId > 0 && TaxonomyData.isAncestor(tax, classId)) // if alignment lies on or above the target taxon, use it
							column.add(classId, score);
					}
				}
				if (column.getTaxa().size() > 0)
					columns.add(column);
			}
			prevColumn = column;
		}
		return columns;
	}


	/**
	 * determine the best scores seen
	 *
	 * @return best scores and taxa seen
	 */
	private List<Pair<Float, Integer>> computeBestScores(Set<Integer> taxa, Map<Integer, Integer> tax2row, float[][] scoreMatrix, double topProportion) {
		List<Pair<Float, Integer>> list = new ArrayList<>();

		final var col = scoreMatrix.length - 1;
		for (var tax : taxa) {
			list.add(new Pair<>(scoreMatrix[col][tax2row.get(tax)], tax));
		}
		if (list.size() > 1) {
			list.sort((a, b) -> {
				if (a.getFirst() > b.getFirst())
					return -1;
				else if (a.getFirst() < b.getFirst())
					return 1;
				else
					return a.getSecond().compareTo(b.getSecond());
			});
			var bestScore = list.get(0).getFirst();
			for (var i = 1; i < list.size(); i++) {
				if (list.get(i).getFirst() < topProportion * bestScore) {
					list = list.subList(0, i); // remove the remaining items
					break;
				}
			}
		}
		return list;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		if (rank != this.rank) {
			tax2taxAtRank.clear();
			this.rank = rank;
		}
	}

	public int getClassId() {
		return classId;
	}

	public void setClassId(int classId) {
		this.classId = classId;
	}

	public float getSwitchPenalty() {
		return switchPenalty;
	}

	public void setSwitchPenalty(float switchPenalty) {
		if (switchPenalty < 0)
			throw new IllegalArgumentException("negative switchPenalty");
		this.switchPenalty = switchPenalty;
	}

	public float getCompatibleFactor() {
		return compatibleFactor;
	}

	public void setCompatibleFactor(float compatibleFactor) {
		if (compatibleFactor < 0)
			throw new IllegalArgumentException("negative compatibleFactor");
		this.compatibleFactor = compatibleFactor;
	}

	public float getIncompatibleFactor() {
		return incompatibleFactor;
	}

	public void setIncompatibleFactor(float incompatibleFactor) {
		if (incompatibleFactor < 0)
			throw new IllegalArgumentException("negative incompatibleFactor");
		this.incompatibleFactor = incompatibleFactor;
	}

	/**
	 * gets the ancestor tax id at the set rank or 0
	 *
	 * @return ancestor or 0
	 */
	private Integer getTaxonAtRank(Integer taxonId) {
		if (taxonId == 0)
			return 0;

		if (rank == 0 || TaxonomyData.getTaxonomicRank(taxonId) == rank)
			return taxonId;

		if (tax2taxAtRank.containsKey(taxonId))
			return tax2taxAtRank.get(taxonId);

		var ancestorId = taxonId;
		var v = TaxonomyData.getTree().getANode(ancestorId);
		while (v != null) {
			var vLevel = TaxonomyData.getTaxonomicRank(ancestorId);
			if (vLevel == rank) {
				tax2taxAtRank.put(taxonId, ancestorId);
				return ancestorId;
			}
			if (v.getInDegree() > 0) {
				v = v.getFirstInEdge().getSource();
				ancestorId = (Integer) v.getInfo();
			} else
				break;
		}
		return 0;
	}

	/**
	 * a segment with tax id
	 */
	public record Segment(int start, int end, int tax) {

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		public int getTaxon() {
			return tax;
		}

		public String toString() {
			return String.format("%,d-%,d: %d (%s)", start, end, tax, TaxonomyData.getName2IdMap().get(tax));
		}
	}

	/**
	 * a column in the dynamic program: all alignments that are available at the given position
	 */
	private static class DPColumn {
		private final int pos;
		private final Map<Integer, Float> taxon2AlignmentScore; // todo: replace by array and rows
		private float minAlignmentScore = 0;

		DPColumn(int pos) {
			this.pos = pos;
			taxon2AlignmentScore = new TreeMap<>();
		}

		void add(int tax, float score) {
			if (score <= 0)
				throw new RuntimeException("Score must be positive, got: " + score); // should never happen
			final var prev = taxon2AlignmentScore.get(tax);
			if (prev == null || prev < score)
				taxon2AlignmentScore.put(tax, score);
			if (minAlignmentScore == 0 || score < minAlignmentScore)
				minAlignmentScore = score;
		}

		Set<Integer> getTaxa() {
			return taxon2AlignmentScore.keySet();
		}

		float getScore(int tax) {
			return taxon2AlignmentScore.getOrDefault(tax, 0f);
		}

		int getPos() {
			return pos;
		}

		float getMinAlignmentScore() {
			return minAlignmentScore;
		}

		public String toString() {
			final var buf = new StringBuilder(String.format("[%,d-%.1f-%d", pos, minAlignmentScore, taxon2AlignmentScore.size()));
			for (var tax : getTaxa()) {
				buf.append(String.format(" %d (%s)-%.1f", tax, TaxonomyData.getName2IdMap().get(tax), getScore(tax)));
			}
			buf.append("]");
			return buf.toString();
		}
	}
}
