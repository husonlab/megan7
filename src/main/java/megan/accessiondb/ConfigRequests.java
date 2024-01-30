/*
 * ConfigRequests.java Copyright (C) 2024 Daniel H. Huson
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

/**
 * configuration requests for the SQLite database connections when working with mapping DBs
 * Daniel Huson, 11.22
 */
public class ConfigRequests {
	private static boolean useTempStoreInMemory = false;
	private static int cacheSize = -10000;

	/**
	 * use temp store in memory when creating a mapping DB?
	 *
	 * @return
	 */
	public static boolean isUseTempStoreInMemory() {
		return useTempStoreInMemory;
	}

	/**
	 * determine whether to request in-memory temp storage. If not requested, default will be used
	 *
	 * @param useTempStoreInMemory
	 */
	public static void setUseTempStoreInMemory(boolean useTempStoreInMemory) {
		ConfigRequests.useTempStoreInMemory = useTempStoreInMemory;
	}

	/**
	 * cache size to use
	 *
	 * @return
	 */
	public static int getCacheSize() {
		return cacheSize;
	}

	/**
	 * set the SQLite cache size. Negative values have a special meaning, see SQLite documentation
	 *
	 * @param cacheSize
	 */
	public static void setCacheSize(int cacheSize) {
		ConfigRequests.cacheSize = cacheSize;
	}
}
