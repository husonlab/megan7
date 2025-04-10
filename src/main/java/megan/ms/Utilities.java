/*
 * Utilities.java Copyright (C) 2024 Daniel H. Huson
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

package megan.ms;


import de.mkammerer.argon2.Argon2Factory;
import megan.daa.connector.ClassificationBlockDAA;
import megan.daa.io.ByteInputStream;
import megan.daa.io.ByteOutputStream;
import megan.daa.io.InputReaderLittleEndian;
import megan.daa.io.OutputWriterLittleEndian;
import megan.data.IClassificationBlock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * megan server utilities
 * Daniel Huson, 8.2020
 */
public class Utilities {
	public static final String SERVER_ERROR = "401 Error:";

	/**
	 * construct classification block from bytes
	 */
	public static IClassificationBlock getClassificationBlockFromBytes(byte[] bytes) throws IOException {
		try (var ins = new InputReaderLittleEndian(new ByteInputStream(bytes, 0, bytes.length))) {
			final var classificationName = ins.readNullTerminatedBytes();
			final var classificationBlock = new ClassificationBlockDAA(classificationName);
			final var numberOfClasses = ins.readInt();

			for (var c = 0; c < numberOfClasses; c++) {
				int classId = ins.readInt();
				classificationBlock.setWeightedSum(classId, ins.readInt());
				classificationBlock.setSum(classId, ins.readInt());
			}
			return classificationBlock;
		}
	}

	/**
	 * save classification block to bytes
	 */
	public static byte[] writeClassificationBlockToBytes(IClassificationBlock classificationBlock) throws IOException {
		try (var stream = new ByteOutputStream(); var outs = new OutputWriterLittleEndian(stream)) {
			outs.writeNullTerminatedString(classificationBlock.getName().getBytes());
			final var numberOfClasses = classificationBlock.getKeySet().size();
			outs.writeInt(numberOfClasses);
			for (var classId : classificationBlock.getKeySet()) {
				outs.writeInt(classId);
				outs.writeInt((int) classificationBlock.getWeightedSum(classId));
				outs.writeInt(classificationBlock.getSum(classId));
			}
			return stream.getExactLengthCopy();
		}
	}

	public static Map<String, byte[]> getAuxiliaryDataFromBytes(byte[] bytes) throws IOException {
		final var label2data = new TreeMap<String, byte[]>();

		try (var ins = new InputReaderLittleEndian(new ByteInputStream(bytes, 0, bytes.length))) {
			final var numberOfLabels = ins.readInt();
			for (var i = 0; i < numberOfLabels; i++) {
				final var label = ins.readNullTerminatedBytes();
				final var size = ins.readInt();
				final var data = new byte[size];
				final var length = ins.read_available(data, 0, size);
				if (length < size) {
					final var tmp = new byte[length];
					System.arraycopy(data, 0, tmp, 0, length);
					label2data.put(label, tmp);
					throw new IOException("buffer underflow");
				}
				label2data.put(label, data);
			}
		}
		return label2data;
	}

	public static byte[] writeAuxiliaryDataToBytes(Map<String, byte[]> label2data) throws IOException {
		try (var stream = new ByteOutputStream(); var outs = new OutputWriterLittleEndian(stream)) {
			outs.writeInt(label2data.size());
			for (var label : label2data.keySet()) {
				outs.writeNullTerminatedString(label.getBytes());
				final var data = label2data.get(label);
				outs.writeInt(data.length);
				outs.write(data, 0, data.length);
			}
			return stream.getExactLengthCopy();
		}
	}

	public static byte[] getBytesLittleEndian(int a) {
		return new byte[]{(byte) a, (byte) (a >> 8), (byte) (a >> 16), (byte) (a >> 24)};
	}

	public static byte[] getBytesLittleEndian(long a) {
		return new byte[]{(byte) a, (byte) (a >> 8), (byte) (a >> 16), (byte) (a >> 24), (byte) (a >> 32), (byte) (a >> 40), (byte) (a >> 48), (byte) (a >> 56)};
	}

	private static final byte[] SALT = "megan7server".getBytes(StandardCharsets.UTF_8);

	public static String computeHash(String password) {
		var argon2 = Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2id);
		return argon2.hash(3, 65536, 1, password.toCharArray(), StandardCharsets.UTF_8, SALT);
	}

	public static boolean verify(String password, String providedHash) {
		return computeHash(password).equals(providedHash);
	}
}
