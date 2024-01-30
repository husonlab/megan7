/*
 * ColorSchemeNucleotides.java Copyright (C) 2024 Daniel H. Huson
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
package megan.alignment.gui.colors;

import java.awt.*;
import java.util.LinkedList;

/**
 * create a new color scheme
 * Daniel Huson, 4.2012
 */
public class ColorSchemeNucleotides implements IColorScheme {
	public enum NAMES {
		Default
	}

	private final IColorScheme colorScheme;

	/**
	 * constructor:
	 */
	public ColorSchemeNucleotides(String name) {
		NAMES which = NAMES.Default;
		for (NAMES type : NAMES.values()) {
			if (type.toString().equalsIgnoreCase(name)) {
				break;
			}
		}
		colorScheme = new ColorSchemeNucleotidesDefault();
	}

	/**
	 * get the foreground color
	 *
	 * @return color
	 */
	public Color getColor(int ch) {
		return colorScheme.getColor(ch);
	}

	/**
	 * get the background color
	 *
	 * @return color
	 */
	public Color getBackground(int ch) {
		return colorScheme.getBackground(ch);
	}

	/**
	 * get list of names
	 *
	 * @return names
	 */
	public static String[] getNames() {
		LinkedList<String> names = new LinkedList<>();
		for (NAMES type : NAMES.values()) {
			names.add(type.toString());
		}
		return names.toArray(new String[0]);
	}

}
