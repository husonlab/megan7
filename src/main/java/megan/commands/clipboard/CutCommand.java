/*
 * CutCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands.clipboard;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class CutCommand extends ClipboardBase implements ICommand {
	public String getSyntax() {
		return null;
	}

	public void apply(NexusStreamParser np) {
	}

	public void actionPerformed(ActionEvent event) {
		Action action = findAction(DefaultEditorKit.cutAction);
		if (action != null)
			action.actionPerformed(event);
	}

	public boolean isApplicable() {
		return true;
	}

	public String getName() {
		return "Cut";
	}

	public String getDescription() {
		return "Cut";
	}

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Cut16.gif");
	}

	public boolean isCritical() {
		return true;
	}

	public KeyStroke getAcceleratorKey() {
		return KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
	}
}

