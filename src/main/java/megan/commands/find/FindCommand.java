/*
 * FindCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands.find;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.director.IViewerWithFindToolBar;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Open the Find dialog
 * Daniel Huson, 7 2010
 */
public class FindCommand extends CommandBase implements ICheckBoxCommand {

	public boolean isSelected() {
		return getViewer() instanceof IViewerWithFindToolBar && ((IViewerWithFindToolBar) getViewer()).isShowFindToolBar();
	}

	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return "Find...";
	}

	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Open the find toolbar";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Find16.gif");
	}

	/**
	 * gets the accelerator key  to be used in menu
	 *
	 * @return accelerator key
	 */
	public KeyStroke getAcceleratorKey() {
		return KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
	}

	/**
	 * parses the given command and executes it
	 */
	@Override
	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase("show findToolbar=");
		boolean show = np.getBoolean();
		np.matchIgnoreCase(";");

		if (getViewer() instanceof IViewerWithFindToolBar) {
			IViewerWithFindToolBar viewer = (IViewerWithFindToolBar) getViewer();
			if (show) {
				if (!viewer.getSearchManager().getFindDialogAsToolBar().isEnabled())
					viewer.setShowFindToolBar(true);
				else
					viewer.getSearchManager().getFindDialogAsToolBar().setEnabled(true); // refocus

			} else
				viewer.setShowFindToolBar(false);

		} else {
			NotificationsInSwing.showWarning(getViewer().getFrame(), "Find not implemented for this type of window");
		}
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
	 * get command-line usage description
	 *
	 * @return usage
	 */
	@Override
	public String getSyntax() {
		return "show findToolbar={true|false};";
	}

	/**
	 * is the command currently applicable? Used to set enable state of command
	 *
	 * @return true, if command can be applied
	 */
	public boolean isApplicable() {
		return getViewer() instanceof IViewerWithFindToolBar;
	}

	/**
	 * action to be performed
	 */
	@Override
	public void actionPerformed(ActionEvent ev) {
		execute("show findToolbar=true;");
	}
}
