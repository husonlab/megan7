/*
 * IlluminaReporterFileFilter.java Copyright (C) 2024 Daniel H. Huson
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
package megan.util;

import jloda.swing.util.FileFilterBase;
import jloda.util.FileUtils;
import jloda.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * A Illumnar reporter file filter
 * Daniel Huson         1.2016
 */
public class IlluminaReporterFileFilter extends FileFilterBase implements FilenameFilter {
	static private IlluminaReporterFileFilter instance;

	public static IlluminaReporterFileFilter getInstance() {
		if (instance == null) {
			instance = new IlluminaReporterFileFilter();
			instance.setAllowGZipped(true);
			instance.setAllowZipped(true);
		}
		return instance;
	}

	private IlluminaReporterFileFilter() {
		add("txt");
		add("rdp");
	}

	/**
	 * @return description of file matching the filter
	 */
	public String getBriefDescription() {
		return "Illumina Reporter";
	}

	/**
	 * is file acceptable?
	 *
	 * @return true if acceptable
	 */
	@Override
	public boolean accept(File directory, String fileName) {
		if (!super.accept(directory, fileName))
			return false;
		String[] firstLines = FileUtils.getFirstLinesFromFile(new File(fileName), 2);
		return firstLines != null && firstLines.length == 2 && firstLines[0].startsWith(">") && StringUtils.contains(firstLines[1], ';', 2)
			   && !firstLines[1].toLowerCase().contains("root");
	}
}
