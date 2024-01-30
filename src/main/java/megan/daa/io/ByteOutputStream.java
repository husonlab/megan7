/*
 * ByteOutputStream.java Copyright (C) 2024 Daniel H. Huson
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
import megan.io.IOutput;

import java.io.OutputStream;

/**
 * byte output stream
 */
public final class ByteOutputStream extends OutputStream implements IOutput {
	private byte[] buf;
	private int count;

	public ByteOutputStream() {
		this(1024);
	}

	public ByteOutputStream(int size) {
		this.count = 0;
		this.buf = new byte[size];
	}

	public void write(int b) {
		this.ensureCapacity(1);
		this.buf[this.count] = (byte) b;
		++this.count;
	}

	private void ensureCapacity(int space) {
		int newCount = space + this.count;
		if (newCount > this.buf.length) {
			if ((long) newCount > (long) Basic.MAX_ARRAY_SIZE)
				throw new RuntimeException("ByteOutputStream overflow");
			var tmp = new byte[Math.max((int) Math.min((long) Basic.MAX_ARRAY_SIZE, ((long) this.buf.length) << 1L), newCount)];
			System.arraycopy(this.buf, 0, tmp, 0, this.count);
			this.buf = tmp;
		}
	}

	public void write(byte[] b, int off, int len) {
		this.ensureCapacity(len);
		System.arraycopy(b, off, this.buf, this.count, len);
		this.count += len;
	}

	public void write(byte[] b) {
		this.write(b, 0, b.length);
	}

	public void reset() {
		this.count = 0;
	}

	public int size() {
		return this.count;
	}

	public String toString() {
		return new String(this.buf, 0, this.count);
	}

	public void close() {
	}

	public byte[] getBytes() {
		return this.buf;
	}

	public byte[] getExactLengthCopy() {
		final byte[] bytes = new byte[size()];
		System.arraycopy(getBytes(), 0, bytes, 0, bytes.length);
		return bytes;
	}

	public int getCount() {
		return this.count;
	}

	@Override
	public long getPosition() {
		return count;
	}

	@Override
	public long length() {
		return count;
	}

	@Override
	public boolean supportsSeek() {
		return false;
	}

	@Override
	public void seek(long pos) {

	}
}
