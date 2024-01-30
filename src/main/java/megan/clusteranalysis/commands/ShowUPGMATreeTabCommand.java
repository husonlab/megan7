/*
 * ShowUPGMATreeTabCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * choose nj tab
 * Daniel Huson, 7.2010
 */
public class ShowUPGMATreeTabCommand extends CommandBase implements ICheckBoxCommand {
	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return "UPGMA Tree";
	}

	public String getAltName() {
		return "UPGMA Tree Tab";
	}

	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Open the UPGMA tree tab";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return null;
	}

	public boolean isSelected() {
		return getViewer().getSelectedComponent() == getViewer().getUpgmaTab();
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
	 * parses the given command and executes it
	 */
	@Override
	public void apply(NexusStreamParser np) {
	}

	/**
	 * is this a critical command that can only be executed when no other command is running?
	 *
	 * @return true, if critical
	 */
	public boolean isCritical() {
		return false;
	}

	/**
	 * get command-line usage description
	 *
	 * @return usage
	 */
	@Override
	public String getSyntax() {
		return null;
	}

	/**
	 * is the command currently applicable? Used to set enable state of command
	 *
	 * @return true, if command can be applied
	 */
	public boolean isApplicable() {
		return true;
	}

	/**
	 * action to be performed
	 */
	@Override
	public void actionPerformed(ActionEvent ev) {
		getViewer().selectComponent(getViewer().getUpgmaTab());

	}
}
