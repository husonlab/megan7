/*
 * DrawDiamondsCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands.format;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * draw selected nodes as diamonds
 * Daniel Huson, 3.2013
 */
public class DrawDiamondsCommand extends CommandBase implements ICommand {
	/**
	 * apply
	 */
	public void apply(NexusStreamParser np) {
	}

	/**
	 * get command-line usage description
	 *
	 * @return usage
	 */
	public String getSyntax() {
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
		return ((Director) getDir()).getDocument().getSampleSelection().size() > 0;
	}

	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return "Diamond";
	}

	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Diamond node shape";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return ResourceManager.getIcon("BlueDiamond16.gif");
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
		execute("set nodeShape=diamond;");
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
