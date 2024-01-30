/*
 * SetChartDrawerCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.chart.commandtemplates;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.chart.data.IPlot2DData;
import megan.chart.drawers.Plot2DDrawer;
import megan.chart.gui.ChartViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * set the chart drawer to use
 * Daniel Huson, 4.2015
 */
public class SetChartDrawerCommand extends CommandBase implements ICommand {
	public String getSyntax() {
		Collection<String> names;
		if (getViewer() != null)
			names = ((ChartViewer) getViewer()).getChartDrawerNames();
		else names = null;

		if (names == null || names.size() == 0)
			return "set chartDrawer=<name>;";
		else
			return "set chartDrawer={" + StringUtils.toString(names, "|") + "};";
	}

	public void apply(NexusStreamParser np) throws Exception {
		final ChartViewer chartViewer = (ChartViewer) getViewer();
		np.matchIgnoreCase("set chartDrawer=");
		String chartDrawerName = np.getWordMatchesRespectingCase(StringUtils.toString(chartViewer.getChartDrawerNames(), " "));
		np.matchIgnoreCase(";");

		chartViewer.chooseDrawer(chartDrawerName);
	}

	public void actionPerformed(ActionEvent event) {
	}

	public boolean isApplicable() {
		ChartViewer chartViewer = (ChartViewer) getViewer();
		return chartViewer != null && chartViewer.getChartData() != null && ((chartViewer.getChartDrawer() instanceof Plot2DDrawer) == (chartViewer.getChartData() instanceof IPlot2DData));
	}

	public String getName() {
		return null;
	}

	public String getDescription() {
		return "Set the chart drawer to use";
	}

	public ImageIcon getIcon() {
		return null;
	}

	public boolean isCritical() {
		return true;
	}

	public KeyStroke getAcceleratorKey() {
		return null;
	}
}
