/*
 * ClearCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.inspector.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.inspector.InspectorWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class ClearCommand extends CommandBase implements ICommand {
	/**
	 * parses the given command and executes it
	 */
	@Override
	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase(getSyntax());
		InspectorWindow inspectorWindow = (InspectorWindow) getViewer();

		if (inspectorWindow.getDataTree().getSelectionCount() == 0)
			inspectorWindow.clear();
		else
			inspectorWindow.deleteSelectedNodes();
	}

	/**
	 * get command-line usage description
	 *
	 * @return usage
	 */
	@Override
	public String getSyntax() {
		return "clear;";
	}

	/**
	 * action to be performed
	 */
	@Override
	public void actionPerformed(ActionEvent ev) {
		executeImmediately(getSyntax());
	}

	private static final String NAME = "Clear";

	public String getName() {
		return NAME;
	}


	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Clear the selected nodes, or all, if none selected";
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
		InspectorWindow inspectorWindow = (InspectorWindow) getViewer();

		return inspectorWindow != null && inspectorWindow.getDataTree() != null && inspectorWindow.getDataTree().getModel().getRoot() != null
			   && inspectorWindow.getDataTree().getModel().getChildCount(inspectorWindow.getDataTree().getModel().getRoot()) > 0;
	}
}
