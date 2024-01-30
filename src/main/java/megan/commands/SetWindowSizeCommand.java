/*
 * SetWindowSizeCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.StringTokenizer;

public class SetWindowSizeCommand extends jloda.swing.commands.CommandBase implements ICommand {
	public String getSyntax() {
		return "set windowSize=<width> x <height>;";
	}

	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase("set windowSize=");
		int width = np.getInt(1, Integer.MAX_VALUE);
		np.matchIgnoreCase("x");
		int height = np.getInt(1, Integer.MAX_VALUE);
		np.matchIgnoreCase(";");
		getViewer().getFrame().setSize(width, height);
	}

	public void actionPerformed(ActionEvent event) {
		IDirectableViewer viewer = getViewer();
		String original = viewer.getFrame().getWidth() + " x " + viewer.getFrame().getHeight();
		String result = JOptionPane.showInputDialog(viewer.getFrame(), "Set window size (width x height):", original);
		if (result != null && !result.equals(original)) {
			int height = 0;
			int width = 0;
			StringTokenizer st = new StringTokenizer(result, "x ");
			try {
				if (st.hasMoreTokens())
					width = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
					height = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
					throw new NumberFormatException("Unexpected characters at end of string");
				execute("set windowSize=" + width + " x " + height + ";");
			} catch (NumberFormatException e) {
				NotificationsInSwing.showError("Window Size: Invalid entry: " + e.getMessage());
			}
		}
	}

	public boolean isApplicable() {
		return true;
	}

	public String getName() {
		return "Set Window Size...";
	}

	public String getDescription() {
		return "Set the window size";
	}

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Preferences16.gif");
	}

	public boolean isCritical() {
		return false;
	}


	public KeyStroke getAcceleratorKey() {
		return null;
	}
}

