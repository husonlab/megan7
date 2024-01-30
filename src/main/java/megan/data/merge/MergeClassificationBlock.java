/*
 * MergeClassificationBlock.java Copyright (C) 2024 Daniel H. Huson
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

package megan.data.merge;

import megan.core.MeganFile;
import megan.data.IClassificationBlock;

import java.io.IOException;
import java.util.*;

public class MergeClassificationBlock implements IClassificationBlock {
	private final ArrayList<MeganFile> files = new ArrayList<>();
	private Map<Integer, Integer> id2sum;
	private Map<Integer, Float> id2weight;
	private String classificationName;

	public MergeClassificationBlock(String classificationName, Collection<MeganFile> files0) {
		this.classificationName = classificationName;
		files.addAll(files0);
	}

	@Override
	public int getSum(Integer key) {
		if (id2sum == null) {
			id2sum = new HashMap<>();
			for (var file : files) {
				try {
					var classificationBlock = file.getConnector().getClassificationBlock(classificationName);
					for (var a : classificationBlock.getKeySet()) {
						id2sum.put(a, id2sum.getOrDefault(a, 0) + classificationBlock.getSum(a));
					}
				} catch (IOException ignored) {
				}
			}
		}
		return id2sum.getOrDefault(key, 0);
	}

	@Override
	public void setSum(Integer key, int num) {
		throw new RuntimeException("Read only");
	}

	@Override
	public float getWeightedSum(Integer key) {
		if (id2weight == null) {
			id2weight = new HashMap<>();
			for (var file : files) {
				try {
					var classificationBlock = file.getConnector().getClassificationBlock(classificationName);
					for (var a : classificationBlock.getKeySet()) {
						id2weight.put(a, id2weight.getOrDefault(a, 0f) + classificationBlock.getWeightedSum(a));
					}
				} catch (IOException ignored) {
				}
			}
		}
		return id2weight.getOrDefault(key, 0f);
	}

	@Override
	public void setWeightedSum(Integer key, float num) {
		throw new RuntimeException("Read only");

	}

	@Override
	public String getName() {
		return classificationName;
	}

	@Override
	public void setName(String classificationName) {
		this.classificationName = classificationName;
	}

	@Override
	public Set<Integer> getKeySet() {

		if (id2sum == null) {
			id2sum = new HashMap<>();
			for (var file : files) {
				try {
					var classificationBlock = file.getConnector().getClassificationBlock(classificationName);
					for (var a : classificationBlock.getKeySet()) {
						id2sum.put(a, id2sum.getOrDefault(a, 0) + classificationBlock.getSum(a));
					}
				} catch (IOException ignored) {
				}
			}
		}
		return id2sum.keySet();
	}
}
