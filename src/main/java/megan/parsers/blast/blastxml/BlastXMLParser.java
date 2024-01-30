/*
 * BlastXMLParser.java Copyright (C) 2024 Daniel H. Huson
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
package megan.parsers.blast.blastxml;

import jloda.util.CanceledException;
import jloda.util.FileUtils;
import jloda.util.NumberUtils;
import megan.parsers.blast.Match;
import megan.parsers.blast.Utilities;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;

/**
 * parser for BLAST XML files. Matches are posted to the given Queue
 * Daniel Huson, 2.2011, 4.2015
 */
public class BlastXMLParser extends DefaultHandler {
	static private final long AVERAGE_CHARACTERS_PER_READ = 10000L; // guess used in progress bar

	static private SAXParserFactory saxParserFactory;

	// stuff we need to access:
	private final File blastFile;
	private final BlockingQueue<MatchesText> blockQueue;
	private final int maxMatchesPerRead;

	// general info obtained from the file:
	private final InfoBlock preamble = new InfoBlock("Preamble");
	private final InfoBlock parameters = new InfoBlock("Parameters");
	private final InfoBlock stats = new InfoBlock("Stats");

	private StringBuilder elementText;

	private int numberOfReads = 0;
	private int totalMatches = 0;
	private int totalDiscardedMatches = 0;

	private Iteration iteration;
	private Hit hit;
	private HSP hsp;
	private final List<Hit> iterationHits = new LinkedList<>();

	private final TreeSet<Match> matches = new TreeSet<>(new Match()); // set of matches found for a given query

	private final long maximumProgress;

	/**
	 * constructor
	 */
	public BlastXMLParser(File blastFile, BlockingQueue<MatchesText> blockQueue, int maxMatchesPerRead) {
		this.blastFile = blastFile;
		this.blockQueue = blockQueue;
		this.maxMatchesPerRead = maxMatchesPerRead;
		maximumProgress = (FileUtils.isZIPorGZIPFile(blastFile.getPath()) ? blastFile.length() / (10 * AVERAGE_CHARACTERS_PER_READ) : blastFile.length() / AVERAGE_CHARACTERS_PER_READ);
	}

	/**
	 * apply the parser
	 */
	public void apply() throws CanceledException, IOException, ParserConfigurationException, SAXException {
		if (saxParserFactory == null)
			saxParserFactory = SAXParserFactory.newInstance();
		SAXParser saxParser = saxParserFactory.newSAXParser();
		saxParser.parse(FileUtils.getInputStreamPossiblyZIPorGZIP(blastFile.getPath()), this);
	}

	/**
	 * start the document
	 */
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
	}

	/**
	 * end the document
	 */
	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
	}

	/**
	 * start an element
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		elementText = new StringBuilder();

		switch (qName) {
			case "Iteration":
				iteration = new Iteration();
				iterationHits.clear();
				break;
			case "Iteration_stat":
				break;
			case "Iteration_hits":
				iterationHits.clear();
				break;
			case "Hit":
				hit = new Hit();
				break;
			case "Hsp":
				hsp = new HSP();
				break;
		}
	}

	/**
	 * end an element
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch (qName) {
			case "BlastOutput_program":
				preamble.add(qName, getElementText());
				break;
			// else if (qName.equals("BlastOutput_query-ID"))
			//     preamble.add(qName, getElementText());
			case "BlastOutput_query-def":
				preamble.add(qName, getElementText());
				break;
			case "BlastOutput_query-len":
				preamble.add(qName, getElementText());
				break;
			case "BlastOutput_db":
				preamble.add(qName, getElementText());
				break;
			case "Parameters_matrix":
				parameters.add(qName, getElementText());
				break;
			case "Parameters_expect":
				parameters.addDouble(qName, getElementText());
				break;
			case "Parameters_gap-open":
				parameters.addInt(qName, getElementText());
				break;
			case "Parameters_gap-extend":
				parameters.addInt(qName, getElementText());
				break;
			case "Parameters_filter":
				parameters.add(qName, getElementText());
				break;
			case "Iteration":
// ending an iteration, write it out

				numberOfReads++;

				MatchesText matchesText = new MatchesText();

				if (iterationHits.size() == 0) {
					matchesText.setNumberOfMatches(0);
					matchesText.setText(iteration.queryDef.getBytes());
					matchesText.setLengthOfText(matchesText.getText().length);

				} else {
					int numberOfMatches = 0;
					matches.clear();
					for (Hit hit : iterationHits) {
						if (matches.size() < getMaxMatchesPerRead() || hit.hsp.bitScore > matches.last().getBitScore()) {
							final Match match = new Match();
							final HSP hsp = hit.hsp;
							match.setBitScore(hsp.bitScore);
							match.setId(numberOfMatches++);
							int queryStart = (int) (hsp.queryFrame >= 0 ? hsp.queryFrom : hsp.queryTo);
							int queryEnd = (int) (hsp.queryFrame >= 0 ? hsp.queryTo : hsp.queryFrom);

							final StringBuilder buf = new StringBuilder();
							if (hit.def != null)
								buf.append(hit.def);
							if (hit.id != null) {
								if (buf.length() > 0)
									buf.append(" ");
								buf.append(hit.id).append(" ");
							}
							if (hit.accession != null) {
								if (buf.length() > 0)
									buf.append(" ");
								buf.append("acc|").append(hit.accession);
							}

							if (buf.length() > 0)
								buf.append(" ");

							match.setSamLine(makeSAM(iteration.queryDef, buf.toString().replaceAll("\\s+", " "), hit.len, hsp.bitScore, (float) hsp.eValue,
									(int) hsp.score, hsp.identity, hsp.queryFrame, queryStart, queryEnd, (int) hsp.hitFrom, (int) hsp.hitTo, hsp.qSeq, hsp.hSeq));
							matches.add(match);
							if (matches.size() > maxMatchesPerRead)
								matches.remove(matches.last());
						}
					}
					StringBuilder buf = new StringBuilder();
					for (Match match : matches) {
						buf.append(match.getSamLine()).append("\n");
					}
					matchesText.setText(buf.toString().getBytes());
					matchesText.setLengthOfText(matchesText.getText().length);
					matchesText.setNumberOfMatches(matches.size());
					totalMatches += iterationHits.size();
					totalDiscardedMatches += (iterationHits.size() - matchesText.getNumberOfMatches());
				}
				try {
					blockQueue.put(matchesText);
				} catch (InterruptedException e) {
					throw new SAXException("Interrupted");
				}
				break;
			case "Iteration_iter-num":
				iteration.iterNum = NumberUtils.parseLong(getElementText());
				break;
			//  else if (qName.equals("Iteration_query-ID"))
			//              iteration.queryID = getElementText();
			case "Iteration_query-def":
				iteration.queryDef = getElementText();
				break;
			case "Iteration_query-len":
				iteration.queryLen = NumberUtils.parseInt(getElementText());
				break;
			case "Hit_def":
				hit.def = getElementText();
				break;
			case "Hit_accession":
				hit.accession = getElementText();
				break;
			case "Hit_id":
				hit.id = getElementText();
				break;
			case "Hit_len":
				hit.len = NumberUtils.parseInt(getElementText());
				break;
			case "Hsp":
				// todo: a hit can have more than one HSP but we only keep the first one, for now
				if (hit.hsp == null || hsp.bitScore > hit.hsp.bitScore)
					hit.hsp = hsp;
				break;
			case "Hit":
				iterationHits.add(hit);
				break;
			case "Hsp_bit-score":
				hsp.bitScore = NumberUtils.parseFloat(getElementText());
				break;
			case "Hsp_score":
				hsp.score = NumberUtils.parseFloat(getElementText());
				break;
			case "Hsp_evalue":
				hsp.eValue = NumberUtils.parseDouble(getElementText());
				break;
			case "Hsp_query-from":
				hsp.queryFrom = Long.parseLong(getElementText());
				break;
			case "Hsp_query-to":
				hsp.queryTo = NumberUtils.parseLong(getElementText());
				break;
			case "Hsp_hit-from":
				hsp.hitFrom = NumberUtils.parseLong(getElementText());
				break;
			case "Hsp_hit-to":
				hsp.hitTo = NumberUtils.parseLong(getElementText());
				break;
			case "Hsp_hit-frame":
				hsp.hitFrame = NumberUtils.parseInt(getElementText());
				break;
			case "Hsp_query-frame":
				hsp.queryFrame = NumberUtils.parseInt(getElementText());
				break;
			case "Hsp_identity":
				hsp.identity = NumberUtils.parseInt(getElementText());
				break;
			case "Hsp_positive":
				hsp.positive = NumberUtils.parseInt(getElementText());
				break;
			case "Hsp_gaps":
				hsp.gaps = NumberUtils.parseInt(getElementText());
				break;
			case "Hsp_align-len":
				hsp.alignLength = NumberUtils.parseInt(getElementText());
				break;
			case "Hsp_density":
				hsp.density = NumberUtils.parseInt(getElementText());
				break;
			case "Hsp_qseq":
				hsp.qSeq = getElementText();
				break;
			case "Hsp_hseq":
				hsp.hSeq = getElementText();
				break;
			case "Hsp_midline":
				hsp.midLine = getElementText();
				break;
			case "Statistics_db-num":
				stats.addLong(qName, getElementText());
				break;
			case "Statistics_db-len":
				stats.addLong(qName, getElementText());
				break;
			case "Statistics_hsp-len":
				stats.addLong(qName, getElementText());
				break;
			case "Statistics_eff-space":
				stats.addDouble(qName, getElementText());
				break;
			case "Statistics_kappa":
				stats.addFloat(qName, getElementText());
				break;
			case "Statistics_lambda":
				stats.addFloat(qName, getElementText());
				break;
			case "Statistics_entropy":
				stats.addFloat(qName, getElementText());
				break;
		}
	}

	/**
	 * resolve an entity
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId) {
		return new InputSource(new StringReader(""));
	}

	/**
	 * build current element text
	 */
	@Override
	public void characters(char[] chars, int start, int length) {
		elementText.append(chars, start, length);
	}

	/**
	 * get the text of the current element
	 *
	 * @return text
	 */
	private String getElementText() {
		return elementText.toString();
	}

	public int getNumberOfReads() {
		return numberOfReads;
	}

	public int getTotalMatches() {
		return totalMatches;
	}

	public int getTotalDiscardedMatches() {
		return totalDiscardedMatches;
	}

	private int getMaxMatchesPerRead() {
		return maxMatchesPerRead;
	}

	public long getMaximumProgress() {
		return maximumProgress;
	}

	public long getProgress() {
		return numberOfReads * AVERAGE_CHARACTERS_PER_READ;
	}

	/**
	 * make a SAM line
	 */
	private String makeSAM(String queryName, String refName, int referenceLength, float bitScore, float expect, int rawScore, float percentIdentity, int frame, int queryStart, int queryEnd, int referenceStart, int referenceEnd, String alignedQuery, String alignedReference) {
		final StringBuilder buffer = new StringBuilder();
		buffer.append(queryName).append("\t");
		buffer.append(0);
		buffer.append("\t");
		buffer.append(refName).append("\t");
		buffer.append(referenceStart).append("\t");
		buffer.append("255\t");
		Utilities.appendCigar(alignedQuery, alignedReference, buffer);

		buffer.append("\t");
		buffer.append("*\t");
		buffer.append("0\t");
		buffer.append("0\t");
		buffer.append(alignedQuery.replaceAll("-", "")).append("\t");
		buffer.append("*\t");

		buffer.append(String.format("AS:i:%d\t", Math.round(bitScore)));
		buffer.append(String.format("NM:i:%d\t", Utilities.computeEditDistance(alignedQuery, alignedReference)));
		buffer.append(String.format("ZL:i:%d\t", referenceLength));
		buffer.append(String.format("ZR:i:%d\t", rawScore));
		buffer.append(String.format("ZE:f:%g\t", expect));
		buffer.append(String.format("ZI:i:%d\t", Math.round(percentIdentity)));
		if (frame != 0)
			buffer.append(String.format("ZF:i:%d\t", frame));
		buffer.append(String.format("ZS:i:%s\t", queryStart));

		Utilities.appendMDString(alignedQuery, alignedReference, buffer);

		return buffer.toString();
	}

	/**
	 * blast iteration
	 */
	static class Iteration {
		long iterNum;
		// public String queryID;
		String queryDef;
		int queryLen;
	}

	/**
	 * a hit as retrieved in XML file
	 */
	static class Hit {
		String def;
		String accession;
		String id;
		int len;
		HSP hsp;
	}
}
