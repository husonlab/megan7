/*
 * Mothur2SAMIterator.java Copyright (C) 2024 Daniel H. Huson
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
import megan.util.MothurFileFilter;

import java.io.IOException;
import java.util.TreeSet;


/**
 * parses a mothur file into SAM format
 * Daniel Huson, 4.2015
 */
public class Mothur2SAMIterator extends SAMIteratorBase implements ISAMIterator {
	private final TreeSet<Match> matches = new TreeSet<>(new Match());
	private final Pair<byte[], Integer> matchesTextAndLength = new Pair<>(new byte[10000], 0);

	/**
	 * constructor
	 */
	public Mothur2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
		super(fileName, maxNumberOfMatchesPerRead);
		if (!MothurFileFilter.getInstance().accept(fileName)) {
			NotificationsInSwing.showWarning("Might not be a MOTHUR analysis file: " + fileName);
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

		// Format: ASF360\tBacteria(100);Firmicutes(100);Bacilli(100);Lactobacillales(100);Lactobacillaceae(100);Lactobacillus(100);

		String line = nextLine();
		boolean found = false;
		while (line != null && !found) {
			if (StringUtils.countOccurrences(line, '\t') == 1)
				found = true;
			else if (hasNext())
				line = nextLine();
			else
				line = null;
		}
		if (line == null)
			return -1;


		int matchId = 0; // used to distinguish between matches when sorting
		matches.clear();

		final String[] lines = StringUtils.split(line, '\t');
		final String queryName = lines[0];
		final String[] tokens = StringUtils.split(lines[1], ';');

		StringBuilder path = new StringBuilder();
		// add one match block for each percentage given:
		try {
			int whichToken = 0;
			while (whichToken < tokens.length) {
				String name = tokens[whichToken++];
				if (name.equals("Root"))
					name = "root";
				path.append(name).append(";");
				String scoreString = tokens[whichToken++];
				if (!scoreString.endsWith(")")) {
					System.err.println("Expected (number) in: " + line);
					break;
				}
				float bitScore = NumberUtils.parseFloat(scoreString);

				if (matches.size() < getMaxNumberOfMatchesPerRead() || bitScore > matches.last().bitScore) {
					Match match = new Match();
					match.bitScore = bitScore;
					match.id = matchId++;

					String ref = StringUtils.toString(tokens, 0, whichToken, ";") + ";";
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
