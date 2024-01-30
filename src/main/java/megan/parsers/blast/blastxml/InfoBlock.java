/*
 * InfoBlock.java Copyright (C) 2024 Daniel H. Huson
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

import jloda.util.NumberUtils;
import jloda.util.Pair;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * a block information
 * Daniel Huson, 2.11
 */
public class InfoBlock {
	private final String name;
	private final List<Pair<String, Object>> list = new LinkedList<>();

	public InfoBlock(String name) {
		this.name = name;
	}

	public void add(String name, String value) {
		list.add(new Pair<>(name, value));
	}

	public void addInt(String name, String value) {
		list.add(new Pair<>(name, NumberUtils.parseInt(value)));
	}

	public void addLong(String name, String value) {
		list.add(new Pair<>(name, NumberUtils.parseLong(value)));
	}

	public void addFloat(String name, String value) {
		list.add(new Pair<>(name, NumberUtils.parseFloat(value)));
	}

	public void addDouble(String name, String value) {
		list.add(new Pair<>(name, NumberUtils.parseDouble(value)));
	}

	public String toString() {
		StringWriter w = new StringWriter();
		w.write(name + ":\n");
		for (Pair<String, Object> pair : list) {
			w.write(pair.getFirst() + ": " + pair.getSecond() + "\n");
		}
		return w.toString();
	}

	public Object getValue(String name) {
		for (Pair<String, Object> pair : list) {
			if (pair.getFirst().equals(name))
				return pair.getSecond();
		}
		return null;
	}
}
