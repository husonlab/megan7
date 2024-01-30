/*
 * SetScaleBySqrtCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.chart.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.chart.gui.ChartViewer;
import megan.util.ScalingType;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SetScaleBySqrtCommand extends CommandBase implements ICheckBoxCommand {
	public boolean isSelected() {
		ChartViewer chartViewer = (ChartViewer) getViewer();
		return chartViewer.getChartDrawer() != null && chartViewer.getScalingType() == ScalingType.SQRT;
	}

	public String getSyntax() {
		return null;
	}

	public void apply(NexusStreamParser np) {
	}

	public void actionPerformed(ActionEvent event) {
		execute("set chartScale=sqrt;");
	}

	public boolean isApplicable() {
		ChartViewer chartViewer = (ChartViewer) getViewer();
		return chartViewer.getChartData() != null && chartViewer.getChartDrawer() != null && chartViewer.getChartDrawer().isSupportedScalingType(ScalingType.SQRT);
	}

	public String getName() {
		return "Sqrt Scale";
	}

	public String getDescription() {
		return "Show values on square-root scale";
	}

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("SqrtScale16.gif");
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

