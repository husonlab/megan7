/*
 * Accession2IdMap.java Copyright (C) 2024 Daniel H. Huson
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

package megan.classification.data;

import jloda.util.FileLineIterator;
import jloda.util.NumberUtils;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.data.IName2IdMap;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * maps accession strings to ids
 * Daniel Huson, 3.2016
 */
public class Accession2IdMap implements IString2IntegerMap, Closeable {
	private final Map<String, Integer> map;

	/**
	 * constructor
	 */
	public Accession2IdMap(final IName2IdMap label2id, final String fileName, final ProgressListener progress) throws IOException {
		map = new HashMap<>(200000000);
		try (FileLineIterator it = new FileLineIterator(fileName)) {
			progress.setSubtask("Loading file: " + fileName);
			progress.setMaximum(it.getMaximumProgress());
			while (it.hasNext()) {
				String[] tokens = StringUtils.split(it.next(), '\t');
				if (tokens.length == 2) {
					if (NumberUtils.isInteger(tokens[1])) {
						int id = NumberUtils.parseInt(tokens[1]);
						if (id != 0) {
							map.put(tokens[0], id);
						}
					} else if (label2id != null) {
						int id = label2id.get(tokens[1]);
						if (id != 0) {
							map.put(tokens[0], id);
						}
					}
				}
				progress.setProgress(it.getProgress());
			}
		}
	}

	public int size() {
		return map.size();
	}

	@Override
	public void close() {
		map.clear();
	}

	public int get(String accession) {
		final var result = map.get(accession);
		return Objects.requireNonNullElse(result, 0);
	}

	public Map<String, Integer> getMap() {
		return map;
	}

}
