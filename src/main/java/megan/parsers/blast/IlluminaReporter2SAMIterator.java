/*
 * IlluminaReporter2SAMIterator.java Copyright (C) 2024 Daniel H. Huson
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
package megan.parsers.blast;

import jloda.swing.window.NotificationsInSwing;
import jloda.util.NumberUtils;
import jloda.util.Pair;
import jloda.util.StringUtils;
import megan.util.IlluminaReporterFileFilter;

import java.io.IOException;
import java.util.TreeSet;


/**
 * parses a Illumina reporter output  into SAM format
 * <p>
 * Format:
 * >M03141:11:000000000-AKT4M:1:1101:8857:1086
 * Bacteria;1.00;Bacteroidetes;1.00;Bacteroidia;1.00;Bacteroidales;1.00;Bacteroidaceae;1.00;Bacteroides;1.00;stercorirosoris;0.98
 * >M03141:11:000000000-AKT4M:1:1101:10834:1096
 * Bacteria;1.00;Firmicutes;1.00;Clostridia;1.00;Clostridiales;1.00;;1.00;;1.00;;0.98
 * <p>
 * Daniel Huson, 1.2016
 */
public class IlluminaReporter2SAMIterator extends SAMIteratorBase implements ISAMIterator {
	private final TreeSet<Match> matches = new TreeSet<>(new Match());
	private final Pair<byte[], Integer> matchesTextAndLength = new Pair<>(new byte[10000], 0);

	/**
	 * constructor
	 */
	public IlluminaReporter2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
		super(fileName, maxNumberOfMatchesPerRead);
		if (!IlluminaReporterFileFilter.getInstance().accept(fileName)) {
			NotificationsInSwing.showWarning("Might not be in Illumina reporter format: " + fileName);
		}
	}

	/**
	 * is there more data?
	 *
	 * @return true, if more data available
	 */
	@Override
	public boolean hasNext() {
		return hasNextLine();
	}

	/**
	 * gets the next matches
	 *
	 * @return number of matches
	 */
	public int next() {
		if (!hasNextLine())
			return -1;

		matchesTextAndLength.setSecond(0);

		String line = nextLine();
		while (hasNextLine() && !line.startsWith(">")) {
			line = nextLine();
		}

		if (line == null || !line.startsWith(">"))
			return -1;

		final String queryName = StringUtils.getReadName(line);
		if (!hasNextLine())
			return -1;
		line = nextLine();

		final String[] tokens = StringUtils.split(line, ';');

		int matchId = 0; // used to distinguish between matches when sorting
		matches.clear();


		StringBuilder path = new StringBuilder();
		// add one match block for each percentage given:
		try {
			path.append("root").append(";");
			int whichToken = 0;
			while (whichToken < tokens.length && tokens[whichToken].length() > 0) {
				String name = tokens[whichToken++];
				if (whichToken >= 2 && Character.isLowerCase(name.charAt(0)) && whichToken == tokens.length - 1)
					name = tokens[whichToken - 3] + " " + name; // make binomial name

				if (!name.equalsIgnoreCase("root"))
					path.append(name).append(";");

				String ref = StringUtils.toString(tokens, 0, whichToken, ";") + ";";
				String scoreString = tokens[whichToken++];
				float bitScore = 100 * NumberUtils.parseFloat(scoreString);

				if (matches.size() < getMaxNumberOfMatchesPerRead() || bitScore > matches.last().bitScore) {
					Match match = new Match();
					match.bitScore = bitScore;
					match.id = matchId++;

					match.samLine = makeSAM(queryName, path.toString(), bitScore, ref);
					matches.add(match);
					if (matches.size() > getMaxNumberOfMatchesPerRead())
						matches.remove(matches.last());
				}
			}
		} catch (Exception ex) {
			System.err.println("Error parsing file near line: " + getLineNumber());
			if (incrementNumberOfErrors() >= getMaxNumberOfErrors())
				throw new RuntimeException("Too many errors");
		}

		return getPostProcessMatches().apply(queryName, matchesTextAndLength, isParseLongReads(), null, matches, null);
	}

	/**
	 * gets the matches text
	 *
	 * @return matches text
	 */
	@Override
	public byte[] getMatchesText() {
		return matchesTextAndLength.getFirst();
	}

	/**
	 * length of matches text
	 *
	 * @return length of text
	 */
	@Override
	public int getMatchesTextLength() {
		return matchesTextAndLength.getSecond();
	}

	/**
	 * make a SAM line
	 */
	private String makeSAM(String queryName, String refName, float bitScore, String line) {
		return String.format("%s\t0\t%s\t0\t255\t*\t*\t0\t0\t*\t*\tAS:i:%d\t", queryName, refName, Math.round(bitScore)) + String.format("AL:Z:%s\t", StringUtils.replaceSpaces(line, ' '));
	}
}
