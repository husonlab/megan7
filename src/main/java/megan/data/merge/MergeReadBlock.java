/*
 * MergeReadBlock.java Copyright (C) 2024 Daniel H. Huson
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

package megan.data.merge;

import megan.data.IMatchBlock;
import megan.data.IReadBlock;

/**
 * readblock used in bundle
 * The getUid() methods contains the file number in its two most signficant bytes
 */
public class MergeReadBlock implements IReadBlock {
	private final IReadBlock readBlock;
	private final int fileNumber;

	public MergeReadBlock(int fileNumber, IReadBlock readBlock) {
		this.fileNumber = fileNumber;
		this.readBlock = readBlock;

	}

	@Override
	public long getUId() {
		return getCombinedFileNumberAndUid(fileNumber, readBlock.getUId());
	}

	@Override
	public void setUId(long uid) {
		readBlock.setUId(uid);

	}

	@Override
	public String getReadName() {
		return readBlock.getReadName();
	}

	@Override
	public String getReadHeader() {
		return readBlock.getReadHeader();
	}

	@Override
	public void setReadHeader(String readHeader) {
		readBlock.setReadHeader(readHeader);

	}

	@Override
	public String getReadSequence() {
		return readBlock.getReadSequence();
	}

	@Override
	public void setReadSequence(String readSequence) {
		readBlock.setReadSequence(readSequence);
	}

	@Override
	public long getMateUId() {
		return ((long) fileNumber << 48) | readBlock.getMateUId();
	}

	@Override
	public void setMateUId(long mateReadUId) {
		readBlock.setMateUId(mateReadUId);

	}

	@Override
	public byte getMateType() {
		return readBlock.getMateType();
	}

	@Override
	public void setMateType(byte type) {
		readBlock.setMateType(type);

	}

	@Override
	public void setReadLength(int readLength) {
		readBlock.setReadLength(readLength);
	}

	@Override
	public int getReadLength() {
		return readBlock.getReadLength();
	}

	@Override
	public void setComplexity(float complexity) {
		readBlock.setComplexity(complexity);
	}

	@Override
	public float getComplexity() {
		return readBlock.getComplexity();
	}

	@Override
	public void setReadWeight(int weight) {
		readBlock.setReadWeight(weight);
	}

	@Override
	public int getReadWeight() {
		return readBlock.getReadWeight();
	}

	@Override
	public int getNumberOfMatches() {
		return readBlock.getNumberOfMatches();
	}

	@Override
	public void setNumberOfMatches(int numberOfMatches) {
		readBlock.setNumberOfMatches(numberOfMatches);
	}

	@Override
	public int getNumberOfAvailableMatchBlocks() {
		return readBlock.getNumberOfAvailableMatchBlocks();
	}

	@Override
	public IMatchBlock[] getMatchBlocks() {
		return readBlock.getMatchBlocks();
	}

	@Override
	public void setMatchBlocks(IMatchBlock[] matchBlocks) {
		readBlock.setMatchBlocks(matchBlocks);
	}

	@Override
	public IMatchBlock getMatchBlock(int i) {
		return readBlock.getMatchBlock(i);
	}

	public static long getCombinedFileNumberAndUid(int fileNumber, long uid) {
		return ((long) fileNumber << 48) | uid;
	}

	public static long getOriginalUid(long uidWithFileNumber) {
		return uidWithFileNumber & 0x00FFFFFFFFFFFFFFFL;
	}

	public static int getFileNumber(long uidWithFileNumber) {
		return (int) (uidWithFileNumber >>> 48);
	}
}
