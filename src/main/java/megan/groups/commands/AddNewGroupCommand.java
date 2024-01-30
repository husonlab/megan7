/*
 * AddNewGroupCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.groups.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.groups.GroupsViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * * selection command
 * * Daniel Huson, 8.2014
 */
public class AddNewGroupCommand extends CommandBase implements ICommand {
	public String getSyntax() {
		return "add group";
	}

	/**
	 * parses the given command and executes it
	 */
	@Override
	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase(getSyntax());

		if (getViewer() instanceof GroupsViewer) {
			GroupsViewer viewer = (GroupsViewer) getViewer();
			viewer.getGroupsPanel().addNewGroup(-1);
		}
	}

	public void actionPerformed(ActionEvent event) {
		executeImmediately(getSyntax());
	}

	public boolean isApplicable() {
		return true;
	}

	public String getName() {
		return "Add New Group";
	}

	public String getDescription() {
		return "Add a new group";
	}

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("NewGroup16.gif");
	}

	public boolean isCritical() {
		return true;
	}

	public KeyStroke getAcceleratorKey() {
		return KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
	}
}
