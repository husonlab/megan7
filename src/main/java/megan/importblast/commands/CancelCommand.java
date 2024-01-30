/*
 * CancelCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.util.parse.NexusStreamParser;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * apply
 * Daniel Huson, 11.2010
 */
public class CancelCommand extends CommandBase implements ICommand {
	public void apply(NexusStreamParser np) {
	}

	public String getSyntax() {
		return null;
	}

	public void actionPerformed(ActionEvent event) {
		ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();
		importBlastDialog.setResult(null);
		importBlastDialog.destroyView();
	}

	public String getName() {
		return "Cancel";
	}

	final public static String ALTNAME = "Cancel Import Blast";

	public String getAltName() {
		return ALTNAME;
	}


	public String getDescription() {
		return "Cancel this dialog";
	}


	public ImageIcon getIcon() {
		return null;
	}


	public boolean isCritical() {
		return true;
	}

	public boolean isApplicable() {
		ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();

		return importBlastDialog != null;
	}
}
