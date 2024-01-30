/*
 * SetAminoAcidColorSchemeCommand.java Copyright (C) 2024 Daniel H. Huson
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

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ProgramProperties;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.gui.colors.ColorSchemeAminoAcids;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 4.2012
 */
public class SetAminoAcidColorSchemeCommand extends CommandBase implements ICommand {
	/**
	 * parses the given command and executes it
	 */
	@Override
	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase("set aminoAcidColors=");

		String value = np.getWordMatchesIgnoringCase(StringUtils.toString(ColorSchemeAminoAcids.getNames(), " "));
		np.matchIgnoreCase(";");

		AlignmentViewer viewer = (AlignmentViewer) getViewer();
		viewer.setAminoAcidColoringScheme(value);
		// the following forces re-coloring:
		viewer.setShowAminoAcids(viewer.isShowAminoAcids());
	}

	/**
	 * get command-line usage description
	 *
	 * @return usage
	 */
	@Override
	public String getSyntax() {
		return "set aminoAcidColors=[" + StringUtils.toString(ColorSchemeAminoAcids.getNames(), "|") + "];";
	}

	/**
	 * action to be performed
	 */
	@Override
	public void actionPerformed(ActionEvent ev) {
		String choice = ProgramProperties.get("AminoAcidColorScheme", ColorSchemeAminoAcids.NAMES.Default.toString());
		String result = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Choose amino acid color scheme", "Choose colors", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(),
				ColorSchemeAminoAcids.getNames(), choice);
		if (result != null) {
			result = result.trim();
			if (result.length() > 0) {
				ProgramProperties.put("AminoAcidColorScheme", result);
				execute("set aminoAcidColors='" + result + "';");
			}
		}
	}

	private static final String NAME = "Set Amino Acid Colors...";

	public String getName() {
		return NAME;
	}


	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Set the color scheme for amino acids";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return null;
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
		return true;
	}
}
