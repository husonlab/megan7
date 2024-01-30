/*
 * ByteFileGetterRandomAccess.java Copyright (C) 2024 Daniel H. Huson
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
package megan.io.experimental;

import jloda.util.Basic;
import megan.io.ByteFileGetterMappedMemory;
import megan.io.IByteGetter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

/**
 * byte file getter using paged memory
 * Daniel Huson, 5.2015
 */
public class ByteFileGetterRandomAccess implements IByteGetter {
	private final File file;
	private final RandomAccessFile raf;

	private final long limit;

	/**
	 * constructor
	 */
	public ByteFileGetterRandomAccess(File file) throws IOException {
		this.file = file;
		limit = file.length();

		System.err.println("Opening file: " + file);
		raf = new RandomAccessFile(file, "r");
	}

	/**
	 * bulk get
	 */
	@Override
	public int get(long index, byte[] bytes, int offset, int len) throws IOException {
		synchronized (raf) {
			raf.seek(index);
			len = raf.read(bytes, offset, len);
		}
		return len;
	}

	/**
	 * gets value for given index
	 *
	 * @return value or 0
	 */
	@Override
	public int get(long index) throws IOException {
		synchronized (raf) {
			raf.seek(index);
			return raf.read();
		}
	}

	/**
	 * gets next four bytes as a single integer
	 *
	 * @return integer
	 */
	@Override
	public int getInt(long index) throws IOException {
		synchronized (raf) {
			raf.seek(index);
			return ((raf.read() & 0xFF) << 24) + ((raf.read() & 0xFF) << 16) + ((raf.read() & 0xFF) << 8) + (raf.read() & 0xFF);
		}
	}

	/**
	 * length of array
	 *
	 * @return array length
	 */
	@Override
	public long limit() {
		return limit;
	}

	/**
	 * close the array
	 */
	@Override
	public void close() {
		try {
			raf.close();
			System.err.println("Closing file: " + file.getName());
		} catch (IOException e) {
			Basic.caught(e);
		}
	}


	public static void main(String[] args) throws IOException {
		File file = new File("/Users/huson/data/ma/protein/index-new/table0.idx");

		final IByteGetter oldGetter = new ByteFileGetterMappedMemory(file);
		final IByteGetter newGetter = new ByteFileGetterRandomAccess(file);

		final Random random = new Random();
		System.err.println("Limit: " + oldGetter.limit());
		for (int i = 0; i < 100; i++) {
			int r = random.nextInt((int) oldGetter.limit());

			int oldValue = oldGetter.get(r);
			int newValue = newGetter.get(r);

			System.err.println(r + ": " + oldValue + (oldValue == newValue ? " == " : " != ") + newValue);
		}
		oldGetter.close();
		newGetter.close();
	}
}
