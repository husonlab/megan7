/*
 * FlipVCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * flip vertically
 * Daniel Huson, 3.2013
 */
public class FlipVCommand extends CommandBase implements ICheckBoxCommand {
	/**
	 * this is currently selected?
	 *
	 * @return selected
	 */
	public boolean isSelected() {
		return getViewer() != null && getViewer().getPcoaTab() != null && getViewer().getPcoaTab().isFlipV();
	}

	/**
	 * parses the given command and executes it
	 */
	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase("set flipV=");
		boolean flip = np.getBoolean();
		np.matchIgnoreCase(";");
		getViewer().getPcoaTab().setFlipV(flip);
		try {
			getViewer().getGraphView().getGraph().clear();
			getViewer().updateGraph();
		} catch (Exception ex) {
			Basic.caught(ex);
		}
	}

	/**
	 * get command-line usage description
	 *
	 * @return usage
	 */
	@Override
	public String getSyntax() {
		return "set flipV={false|true};";
	}

	/**
	 * is this a critical command that can only be executed when no other command is running?
	 *
	 * @return true, if critical
	 */
	public boolean isCritical() {
		return true;
	}

	/**
	 * is the command currently applicable? Used to set enable state of command
	 *
	 * @return true, if command can be applied
	 */
	public boolean isApplicable() {
		return getViewer().getSelectedComponent() == getViewer().getPcoaTab();
	}

	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return "Flip Vertically";
	}

	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Flip vertically";
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
		execute("set flipV=" + (!isSelected()) + ";");
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
