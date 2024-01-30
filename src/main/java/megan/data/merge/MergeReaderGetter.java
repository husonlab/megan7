/*
 * MergeReaderGetter.java Copyright (C) 2024 Daniel H. Huson
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

import jloda.util.FunctionWithIOException;
import megan.data.IReadBlock;
import megan.data.IReadBlockGetter;

import java.io.IOException;
import java.util.*;

/**
 * read getter for a bundle of files
 * Daniel Huson, 5.2022
 */
public class MergeReaderGetter implements IReadBlockGetter {
	private final Set<Integer> fileNumbers = new TreeSet<>();
	private final Map<Integer, IReadBlockGetter> fileGetterMap = new HashMap<>();
	private final FunctionWithIOException<Integer, IReadBlockGetter> fileReadGetterFunction;
	private long count = 0;

	public MergeReaderGetter(Collection<Integer> fileNumbers, FunctionWithIOException<Integer, IReadBlockGetter> fileIReadBlockGetterFunction) throws IOException {
		this.fileNumbers.addAll(fileNumbers);
		this.fileReadGetterFunction = fileIReadBlockGetterFunction;
	}

	@Override
	public IReadBlock getReadBlock(long uid) throws IOException {
		var fileId = MergeReadBlock.getFileNumber(uid);
		if (fileNumbers.contains(fileId)) {
			uid = MergeReadBlock.getOriginalUid(uid);
			if (uid >= 0) {
				var readGetter = fileGetterMap.get(fileId);
				if (readGetter == null) {
					readGetter = fileReadGetterFunction.apply(fileId);
					fileGetterMap.put(fileId, readGetter);
				}
				return readGetter.getReadBlock(uid);
			}
		}
		return null;
	}

	@Override
	public void close() {
		for (var readGetter : fileGetterMap.values()) {
			readGetter.close();
		}
	}

	@Override
	public long getCount() {
		if (count == 0) {
			for (var fileId : fileNumbers) {
				var readGetter = fileGetterMap.get(fileId);
				if (readGetter == null) {
					try {
						readGetter = fileReadGetterFunction.apply(fileId);
						fileGetterMap.put(fileId, readGetter);
						count += readGetter.getCount();
					} catch (IOException ignored) {
					}
				}
			}
		}
		return count;
	}
}
