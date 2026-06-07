/*
 * ShowCheckForUpdateCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands.show;


import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.main.Version;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;

/**
 * show the message window
 * Daniel Huson, 6.2010
 */
public class ShowCheckForUpdateCommand extends CommandBase implements ICommand {
	private final static String NAME = "Check For Updates...";


	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return NAME;
	}

	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Check for updates";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Refresh16.gif");
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
	 * parses the given command and executes it
	 */
	@Override
	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase(getSyntax());
		if (ProgramProperties.isUseGUI())
			show(getViewer().getFrame());
		else {
			System.err.printf("""
					%s updates have moved to GitHub.
					Please download the latest release from:
					%s/releases/latest%n""", Version.NAME, Version.HOME_URL);
		}
	}

	/**
	 * action to be performed
	 */
	public void actionPerformed(ActionEvent ev) {
		execute(getSyntax());
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
		for (IDirector dir : ProjectManager.getProjects()) {
			if (dir.getDirty())
				return false;
		}
		return true;
	}

	/**
	 * get command-line usage description
	 *
	 * @return usage
	 */
	@Override
	public String getSyntax() {
		return "checkForUpdate;";
	}

	/**
	 * gets the command needed to undo this command
	 *
	 * @return undo command
	 */
	public String getUndo() {
		return null;
	}

	private static void show(Component parent) {
		var panel = new JPanel(new BorderLayout(0, 10));
		var text = """
				%s updates have moved to GitHub.
				Please download the latest release from:
				""".formatted(Version.NAME);
		var textArea = new JTextArea(text);
		textArea.setEditable(false);
		textArea.setOpaque(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);

		System.err.println(Version.NAME);
		System.err.println(Version.HOME_URL);

		var url = Version.HOME_URL + "/releases/latest";

		var urlField = new JTextField(url);
		urlField.setEditable(false);
		urlField.setBorder(BorderFactory.createEmptyBorder());
		panel.add(textArea, BorderLayout.NORTH);
		panel.add(new JLabel("  "), BorderLayout.WEST);
		panel.add(new JLabel("  "), BorderLayout.EAST);
		panel.add(urlField, BorderLayout.CENTER);
		var openButton = new JButton("Open Release Page");

		openButton.addActionListener(e -> {
			try {
				var uri = new URI(url);
				System.err.println(uri);
				Desktop.getDesktop().browse(uri);
			} catch (Exception ignored) {
			}
		});

		JOptionPane.showOptionDialog(parent, panel, "%s Updates".formatted(Version.NAME),
				JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
				new Object[]{openButton, "Close"}, openButton);
	}
}
