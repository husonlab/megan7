/*
 * LabelSamplesBySamplesCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.samplesviewer.commands.samples;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.core.SampleAttributeTable;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * label by command
 * Daniel Huson, 9.2105
 */
public class LabelSamplesBySamplesCommand extends CommandBase implements ICommand {
	public String getSyntax() {
		return null;
	}

	/**
	 * parses the given command and executes it
	 */
	public void apply(NexusStreamParser np) {
	}

	public void actionPerformed(ActionEvent event) {
		execute("labelBy attribute='" + SampleAttributeTable.SAMPLE_ID + "'  samples=all;");
	}

	public boolean isApplicable() {
		return true;
	}

	public String getName() {
		return "Label by Samples";
	}

	public String getDescription() {
		return "Label samples by there names";
	}

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("Labels16.gif");
	}

	public boolean isCritical() {
		return true;
	}

	public KeyStroke getAcceleratorKey() {
		return null;
	}

}
