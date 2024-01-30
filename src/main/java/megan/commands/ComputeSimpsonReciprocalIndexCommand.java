/*
 * ComputeSimpsonReciprocalIndexCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.util.parse.NexusStreamParser;
import megan.util.DiversityIndex;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ComputeSimpsonReciprocalIndexCommand extends CommandBase implements ICommand {
	public void apply(NexusStreamParser np) {
	}

	public boolean isApplicable() {
		return getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getNumberSelectedNodes() > 0 && getDir().getDocument().getNumberOfSamples() > 0;
	}

	public boolean isCritical() {
		return true;
	}

	public String getSyntax() {
		return null;
	}

	public void actionPerformed(ActionEvent event) {
		execute("compute index=" + DiversityIndex.SIMPSON_RECIPROCAL + ";");
	}

	public String getName() {
		return "Simpson-Reciprocal Index...";
	}

	public ImageIcon getIcon() {
		return null;
	}

	public String getDescription() {
		return "Compute the Simpson-Reciprocal diversity index";
	}
}


