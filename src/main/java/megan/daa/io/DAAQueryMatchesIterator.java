/*
 * DAAQueryMatchesIterator.java Copyright (C) 2024 Daniel H. Huson
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

package megan.daa.io;

import jloda.util.Basic;
import jloda.util.ICloseableIterator;
import jloda.util.Pair;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * iterator over queries and their matches
 * Daniel Huson, 8.2105
 */
public class DAAQueryMatchesIterator implements ICloseableIterator<Pair<DAAQueryRecord, DAAMatchRecord[]>> {
	private final DAAParser daaParser;
	private final BlockingQueue<Pair<DAAQueryRecord, DAAMatchRecord[]>> queue;
	private final ExecutorService executorService;

	private long count = 0;
	private Pair<DAAQueryRecord, DAAMatchRecord[]> next = null;

	/**
	 * constructor
	 */
	public DAAQueryMatchesIterator(String daaFile, final boolean wantMatches, final int maxMatchesPerRead, final boolean longReads) throws IOException {
		this.daaParser = new DAAParser(daaFile);
		daaParser.getHeader().loadReferences(true);

		queue = new ArrayBlockingQueue<>(1000);

		// start a thread that loads queue:
		executorService = Executors.newSingleThreadExecutor();
		executorService.submit(() -> {
			try {
				daaParser.getAllQueriesAndMatches(wantMatches, maxMatchesPerRead, queue, longReads);
			} catch (IOException e) {
				Basic.caught(e);
			}
		});
	}

	@Override
	public void close() {
		executorService.shutdownNow();
	}

	@Override
	public long getMaximumProgress() {
		return daaParser.getHeader().getQueryRecords();
	}

	@Override
	public long getProgress() {
		return count;
	}

	@Override
	public boolean hasNext() {
		synchronized (DAAParser.SENTINEL_QUERY_MATCH_BLOCKS) {
			if (next == null) {
				try {
					next = queue.take();
				} catch (InterruptedException e) {
					Basic.caught(e);
				}
			}
			return next != DAAParser.SENTINEL_QUERY_MATCH_BLOCKS;
		}
	}

	@Override
	public Pair<DAAQueryRecord, DAAMatchRecord[]> next() {
		synchronized (DAAParser.SENTINEL_QUERY_MATCH_BLOCKS) {
			if (next == null || next == DAAParser.SENTINEL_QUERY_MATCH_BLOCKS)
				return null;
			count++;
			Pair<DAAQueryRecord, DAAMatchRecord[]> result;
			result = next;
			try {
				next = queue.take();
			} catch (InterruptedException e) {
				Basic.caught(e);
			}
			return result;
		}
	}

	@Override
	public void remove() {

	}
}
