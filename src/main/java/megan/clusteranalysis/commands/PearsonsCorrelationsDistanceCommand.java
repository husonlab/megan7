/*
 * PearsonsCorrelationsDistanceCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.clusteranalysis.commands;

import jloda.swing.commands.ICheckBoxCommand;
import megan.clusteranalysis.ClusterViewer;
import megan.clusteranalysis.indices.PearsonDistance;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * PearsonsCorrelationsDistanceCommand command
 * Daniel Huson, 7.2010
 */
public class PearsonsCorrelationsDistanceCommand extends EcologicalIndexCommand implements ICheckBoxCommand {
	/**
	 * this is currently selected?
	 *
	 * @return selected
	 */
	public boolean isSelected() {
		ClusterViewer viewer = getViewer();
		return viewer.getEcologicalIndex().equalsIgnoreCase(PearsonDistance.NAME);
	}

	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return "Use Pearson";
	}

	/**
	 * get description to be used as a tool-tip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Use Pearson's correlation distance";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return null;
	}

	/**
	 * gets the accelerator key  to be used in menu
	 *
	 * @return accelerator key
	 */
	public KeyStroke getAcceleratorKey() {
		return null;
	}

	/**
	 * action to be performed
	 */
	public void actionPerformed(ActionEvent ev) {
		execute("set index=" + PearsonDistance.NAME + ";");
	}

	/**
	 * gets the command needed to undo this command
	 *
	 * @return undo command
	 */
	public String getUndo() {
		return null;
	}
}
