/*
 * UseProgramColorsCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands.color;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class UseProgramColorsCommand extends CommandBase implements ICheckBoxCommand {
	@Override
	public boolean isSelected() {
		return ((Director) getDir()).getDocument().getChartColorManager().isUsingProgramColors();
	}

	public String getSyntax() {
		return "set useProgramColors={false|true};";
	}

	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase("set useProgramColors=");
		boolean use = np.getBoolean();
		np.matchIgnoreCase(";");

		final Document doc = ((Director) getDir()).getDocument();
		if (doc.getChartColorManager().isUsingProgramColors() != use)
			doc.setupChartColorManager(use);

	}

	public void actionPerformed(ActionEvent event) {
		execute("set useProgramColors=" + (!isSelected()) + ";");
	}

	public boolean isApplicable() {
		return true;
	}

	public static final String NAME = "Use Program Color Table";

	public String getName() {
		return NAME;
	}

	public String getDescription() {
		return "Use program color table rather than document specific one";
	}

	public ImageIcon getIcon() {
		return null;
	}

	public boolean isCritical() {
		return true;
	}

	/**
	 * gets the accelerator key  to be used in menu
	 *
	 * @return accelerator key
	 */
	public KeyStroke getAcceleratorKey() {
		return null;
	}
}

