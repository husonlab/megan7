/*
 * ShowWebsiteCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.swing.util.BasicSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.URL;

/**
 * go to program website
 * Daniel Huson, 6.2010
 */
public class ShowWebsiteCommand extends CommandBase implements ICommand {
	public String getSyntax() {
		return null;
	}

	public void apply(NexusStreamParser np) {
	}

	public void actionPerformed(ActionEvent event) {
		try {
			BasicSwing.openWebPage(new URL("http://megan.informatik.uni-tuebingen.de"));
		} catch (Exception e1) {
			Basic.caught(e1);
		}

	}

	public boolean isApplicable() {
		return true;
	}

	public String getName() {
		return "Community Website...";
	}

	public String getDescription() {
		return "Open the community website in your web browser";
	}

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/WebComponent16.gif");
	}

	public boolean isCritical() {
		return false;
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
