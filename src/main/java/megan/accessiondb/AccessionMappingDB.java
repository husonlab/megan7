/*
 * AccessionMappingDB.java Copyright (C) 2024 Daniel H. Huson
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

package megan.accessiondb;

import jloda.util.FileUtils;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * interface to SQL-based accession mapping
 * Daniel Huson, 2.2026
 */
public interface AccessionMappingDB extends Closeable {
	String getInfo() throws SQLException;

	int getSize() throws SQLException;

	Collection<String> getClassificationNames() throws SQLException;

	int getClassificationIndex(String classificationName) throws SQLException;

	int getSize(String classificationName);

	String getInfo(String classificationName) throws SQLException;

	int getValue(String classificationName, String accession) throws SQLException;

	HashMap<String, int[]> getValues(String[] accessions, int length) throws SQLException;

	int[][] getValues(String[] accessions, int numberOfAccessions, String[] cNames) throws SQLException;

	void getValues(Collection<String> accessions, int[] result) throws SQLException;

	int[] getValues(Collection<String> accessions, String[] cNames) throws SQLException;

	int[] setupMapClassificationId2DatabaseRank(String[] classificationNames) throws SQLException;

	static Collection<String> getContainedClassificationsIfDBExists(String fileName) {
		if (FileUtils.fileExistsAndIsNonEmpty(fileName)) {
			try (var accessionDB = AccessionMappingDBFactory.open(fileName)) {
				return accessionDB.getClassificationNames();
			} catch (Exception ex) {
				// ignore
			}
		}
		return Collections.emptySet();
	}
}