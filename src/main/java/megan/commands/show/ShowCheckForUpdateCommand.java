/*
 * ShowCheckForUpdateCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands.show;


import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.main.CheckForUpdate;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * show the message window
 * Daniel Huson, 6.2010
 */
public class ShowCheckForUpdateCommand extends CommandBase implements ICommand {
	private final static String NAME = "Check For Updates...";


	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return NAME;
	}

	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Check for updates";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Refresh16.gif");
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
	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase(getSyntax());
		CheckForUpdate.apply();
	}

	/**
	 * action to be performed
	 */
	public void actionPerformed(ActionEvent ev) {
		execute(getSyntax());
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
		for (IDirector dir : ProjectManager.getProjects()) {
			if (dir.getDirty())
				return false;
		}
		return true;
	}

	/**
	 * get command-line usage description
	 *
	 * @return usage
	 */
	@Override
	public String getSyntax() {
		return "checkForUpdate;";
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
