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
package megan.inspector.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.clipboard.ClipboardBase;
import megan.inspector.InspectorWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class CutCommand extends ClipboardBase implements ICommand {
	public String getSyntax() {
		return null;
	}

	public void apply(NexusStreamParser np) {
	}

	public void actionPerformed(ActionEvent event) {
		final String selected = ((InspectorWindow) getViewer()).getSelection();
		if (selected.length() > 0) {
			StringSelection selection = new StringSelection(selected);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
			((InspectorWindow) getViewer()).deleteSelectedNodes();
		}
	}

	public boolean isApplicable() {
		InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
		return inspectorWindow != null && inspectorWindow.hasSelectedNodes();
	}

	private static final String ALT_NAME = "Inspector Cut";

	public String getAltName() {
		return ALT_NAME;
	}

	public String getName() {
		return "Cut";
	}

	public String getDescription() {
		return "Cut from inspector";
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

