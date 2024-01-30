/*
 * CopyReferenceCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.alignment.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.commands.clipboard.ClipboardBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CopyReferenceCommand extends ClipboardBase implements ICommand {
	public String getSyntax() {
		return null;
	}

	public void apply(NexusStreamParser np) {
	}

	public void actionPerformed(ActionEvent event) {
		AlignmentViewer viewer = (AlignmentViewer) getViewer();
		viewer.getAlignmentViewerPanel().copyReference();
	}

	public boolean isApplicable() {
		AlignmentViewer viewer = (AlignmentViewer) getViewer();
		return viewer.getSelectedBlock().isSelected();
	}

	public String getName() {
		return "Copy Reference";
	}

	public String getDescription() {
		return "Copy selected reference sequence to clipboard";
	}

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Copy16.gif");
	}

	public boolean isCritical() {
		return true;
	}

	public KeyStroke getAcceleratorKey() {
		return null;
	}
}

