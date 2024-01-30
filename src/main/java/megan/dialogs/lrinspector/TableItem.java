/*
 * TableItem.java Copyright (C) 2024 Daniel H. Huson
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

package megan.dialogs.lrinspector;

import javafx.beans.property.*;

/**
 * table item
 * Daniel Huson, 2/2017
 */
public class TableItem {
	private StringProperty readName;
	private StringProperty readSequence;
	private IntegerProperty readLength;
	private IntegerProperty classId;
	private StringProperty className;
	private IntegerProperty hits;
	private IntegerProperty percentCoverage;
	private ObjectProperty<ReadLayoutPane> pane;

	/**
	 * constructor
	 */
	public TableItem(String readName, int readLength, String readSequence, String className, int classId, int hits, int percentCoverage, ReadLayoutPane pane) {
		setReadName(readName);
		setReadSequence(readSequence);
		setReadLength(readLength);
		setClassId(classId);
		if (className == null)
			className = "Unknown";
		setClassName(className);
		setHits(hits);
		setPane(pane);
		setPercentCoverage(percentCoverage);
	}

	public void setReadName(String value) {
		readNameProperty().set(value);
	}

	public String getReadName() {
		return readNameProperty().get();
	}

	public StringProperty readNameProperty() {
		if (readName == null) readName = new SimpleStringProperty(this, "readName");
		return readName;
	}

	public void setReadSequence(String value) {
		readSequenceProperty().set(value);
	}

	public String getReadSequence() {
		return readSequenceProperty().get();
	}

	public StringProperty readSequenceProperty() {
		if (readSequence == null) readSequence = new SimpleStringProperty(this, "readSequence");
		return readSequence;
	}

	public void setReadLength(Integer value) {
		readLengthProperty().set(value);
	}

	public Integer getReadLength() {
		return readLengthProperty().get();
	}

	public IntegerProperty readLengthProperty() {
		if (readLength == null) readLength = new SimpleIntegerProperty(this, "readLength");
		return readLength;
	}


	public void setClassId(Integer value) {
		classIdProperty().set(value);
	}

	public Integer getClassId() {
		return classIdProperty().get();
	}

	public IntegerProperty classIdProperty() {
		if (classId == null) classId = new SimpleIntegerProperty(this, "classId");
		return classId;
	}

	public void setClassName(String value) {
		classNameProperty().set(value);
	}

	public String getClassName() {
		return classNameProperty().get();
	}

	public StringProperty classNameProperty() {
		if (className == null) className = new SimpleStringProperty(this, "className");
		return className;
	}

	public void setHits(Integer value) {
		hitsProperty().set(value);
	}

	public Integer getHits() {
		return hitsProperty().get();
	}

	public IntegerProperty hitsProperty() {
		if (hits == null) hits = new SimpleIntegerProperty(this, "hits");
		return hits;
	}

	public void setPercentCoverage(Integer value) {
		percentCoverageProperty().set(value);
	}

	private Integer getPercentCoverage() {
		return percentCoverageProperty().get();
	}

	public IntegerProperty percentCoverageProperty() {
		if (percentCoverage == null) percentCoverage = new SimpleIntegerProperty(this, "percentCoverage");
		return percentCoverage;
	}

	public ReadLayoutPane getPane() {
		return paneProperty().get();
	}

	public ObjectProperty<ReadLayoutPane> paneProperty() {
		if (pane == null) pane = new SimpleObjectProperty<>(this, "pane");
		return pane;
	}

	public void setPane(ReadLayoutPane pane) {
		paneProperty().set(pane);
	}

	public String toString() {
		return getReadName() + "\tlength=" + getReadLength() + "\tassignment=" + getClassName() + "\talignments=" + getHits() + "\tpercentCovered=" + getPercentCoverage();
	}
}
