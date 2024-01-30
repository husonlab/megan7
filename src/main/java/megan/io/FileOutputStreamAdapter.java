/*
 * FileOutputStreamAdapter.java Copyright (C) 2024 Daniel H. Huson
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
package megan.io;

import java.io.*;

/**
 * file output wrapper
 * Daniel Huson, 6.2009
 */
public class FileOutputStreamAdapter implements IOutput {
	private static final int BUFFER_SIZE = 8192;
	private final BufferedOutputStream outs;
	private long position;

	/**
	 * constructor
	 */
	public FileOutputStreamAdapter(File file) throws FileNotFoundException {
		outs = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
		position = 0;
	}

	/**
	 * constructor
	 */
	public FileOutputStreamAdapter(File file, boolean append) throws FileNotFoundException {
		outs = new BufferedOutputStream(new FileOutputStream(file, append), BUFFER_SIZE);
		if (append)
			position = file.length();
	}

	/**
	 * get position in file
	 *
	 * @return position
	 */
	public long getPosition() {
		return position;
	}

	/**
	 * get current length of file
	 *
	 * @return length
	 */
	public long length() {
		return position;
	}

	/**
	 * seek, not supported
	 */
	public void seek(long pos) {
	}

	/**
	 * seek is not supported
	 *
	 * @return false
	 */
	public boolean supportsSeek() {
		return false;
	}

	/**
	 * write a byte
	 */
	public void write(int a) throws IOException {
		outs.write(a);
		position++;
	}

	/**
	 * write bytes
	 */
	public void write(byte[] bytes, int offset, int length) throws IOException {
		outs.write(bytes, offset, length);
		position += length;
	}

	/**
	 * close this stream
	 */
	public void close() throws IOException {
		outs.close();
	}

	/**
	 * flush the current stream
	 */
	public void flush() throws IOException {
		outs.flush();
	}
}
