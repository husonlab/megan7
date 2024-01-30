/*
 * ListContaminantsCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.importblast.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.ContaminantManager;
import megan.dialogs.parameters.ParametersDialog;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * list contaminants
 * Daniel Huson, 11.2017
 */
public class ListContaminantsCommand extends CommandBase implements ICommand {
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
	 * parses and applies the command
	 */
	@Override
	public void apply(NexusStreamParser np) {
	}

	/**
	 * action to be performed
	 */
	public void actionPerformed(ActionEvent ev) {
		if (getViewer() instanceof ImportBlastDialog) {
			ContaminantManager contaminantManager = new ContaminantManager();
			try {
				contaminantManager.read(((ImportBlastDialog) getViewer()).getContaminantsFileName());
			} catch (IOException e) {
				NotificationsInSwing.showError("Failed to parse file: " + ((ImportBlastDialog) getViewer()).getContaminantsFileName()
											   + ":\n" + e.getMessage());
			}
			executeImmediately("show window=message;list taxa=" + contaminantManager.getTaxonIdsString() + " title='Contaminants';");
		}
	}

	final public static String NAME = "List Contaminants";


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
		return "File provides list of contaminant taxa (ids or names)";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/About16.gif");
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
		return getParent() instanceof ParametersDialog;
	}
}
