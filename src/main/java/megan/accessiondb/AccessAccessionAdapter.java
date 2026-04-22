/*
 * AccessAccessionAdapter.java Copyright (C) 2024 Daniel H. Huson
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

import megan.classification.data.IString2IntegerMap;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * adapts database accession mapping
 * Daniel Huson, 9.2019
 */
public class AccessAccessionAdapter implements IString2IntegerMap {
	public static IntUnaryOperator ACCESSION_FILTER = x -> (x > -1000 ? x : 0);
	public static Function<String, Boolean> FILE_FILTER = x -> !x.contains("-ue-") && !x.contains("_UE");

	private final AccessionMappingDB accessionDB;
	private final String classificationName;
	private final int size;

	private final String mappingDBFile;

	/**
	 * constructor
	 */
	public AccessAccessionAdapter(final String mappingDBFile, final String classificationName) throws IOException {
		this.mappingDBFile = mappingDBFile;
		accessionDB = AccessionMappingDBFactory.open(mappingDBFile);
		this.classificationName = classificationName;
		size = accessionDB.getSize(classificationName);
	}

	@Override
	public int get(String accession) {
		try {
			return accessionDB.getValue(classificationName, accession);
		} catch (Exception e) {
			return 0;
		}
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public void close() {
		try {
			accessionDB.close();
		} catch (IOException ignored) {
		}
	}

	public String getMappingDBFile() {
		return mappingDBFile;
	}

	public AccessionMappingDB getAccessAccessionMappingDatabase() {
		return accessionDB;
	}
}
