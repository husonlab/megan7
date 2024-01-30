/*
 * SelectNodesByIdsCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands.select;

import jloda.graph.Node;
import jloda.swing.commands.ICommand;
import jloda.util.NumberUtils;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * select by name
 * Daniel Huson, 2.2011
 */
public class SelectNodesByIdsCommand extends CommandBase implements ICommand {
	public String getSyntax() {
		return "select id=<number> ...;";
	}

	public void apply(NexusStreamParser np) throws Exception {
		ViewerBase viewer = ((ViewerBase) getViewer());
		np.matchIgnoreCase("select id=");
		Set<Integer> ids = new HashSet<>();
		while (!np.peekMatchAnyTokenIgnoreCase(";")) {
			ids.add(NumberUtils.parseInt(np.getWordRespectCase()));
		}
		np.matchRespectCase(";");
		Set<Node> nodes = new HashSet<>();
		for (Integer id : ids) {
			if (id != null) {
				Set<Node> add = viewer.getNodes(id);
				if (add != null && add.size() > 0)
					nodes.addAll(add);
			}
		}
		viewer.getSelectedNodes().addAll(nodes);
		viewer.repaint();
	}

	/**
	 * action to be performed
	 */
	public void actionPerformed(ActionEvent ev) {
		String result = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter Id", "Select Node by Id", JOptionPane.QUESTION_MESSAGE);
		if (result != null)
			execute("select id=" + result + ";");
	}

	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return "Select By Id...";
	}

	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Select the nodes for the given ids";
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
		ViewerBase viewer = (ViewerBase) getViewer();
		return viewer.getGraph().getNumberOfNodes() > 0;
	}
}
