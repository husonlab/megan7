/*
 * LoadAlternativeTaxonomyFileCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands.load;

import jloda.swing.commands.ICommand;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ProgramProperties;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.FileUtils;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * load command
 * Daniel Huson, 11.2010
 */
public class LoadAlternativeTaxonomyFileCommand extends CommandBase implements ICommand {
	/**
	 * get the name to be used as a menu label
	 *
	 * @return name
	 */
	public String getName() {
		return "Use Alternative Taxonomy...";
	}

	/**
	 * get description to be used as a tooltip
	 *
	 * @return description
	 */
	public String getDescription() {
		return "Open alternative taxonomy.tre and taxonomy.map files";
	}

	/**
	 * get icon to be used in menu or button
	 *
	 * @return icon
	 */
	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Open16.gif");
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
	public void apply(NexusStreamParser np) {
	}

	/**
	 * action to be performed
	 */
	public void actionPerformed(ActionEvent ev) {
		File lastOpenFile = ProgramProperties.getFile(MeganProperties.TAXONOMYFILE);

		getDir().notifyLockInput();
		File file = ChooseFileDialog.chooseFileToOpen(getViewer().getFrame(), lastOpenFile, new TextFileFilter("tre"), new TextFileFilter("tre"), ev, "Open Tree File");
		getDir().notifyUnlockInput();

		if (file != null && file.exists() && file.canRead()) {
			ProgramProperties.put(MeganProperties.TAXONOMYFILE, file.getAbsolutePath());
			String mappingFile = FileUtils.replaceFileSuffix(file.getPath(), ".map");
			if (!(new File(mappingFile)).exists()) {
				mappingFile = null;
			}

			StringBuilder buf = new StringBuilder();
			buf.append("load taxonomyFile='").append(file.getPath()).append("'");
			if (mappingFile != null)
				buf.append(" mapFile='").append(mappingFile).append("';");
			else
				buf.append(";");
			buf.append("collapse level=2;");
			execute(buf.toString());
		}
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
		return ProjectManager.getNumberOfProjects() == 1 && ((Director) ProjectManager.getProjects().get(0)).getDocument().getNumberOfSamples() == 0;
	}

	/**
	 * get command-line usage description
	 *
	 * @return usage
	 */
	@Override
	public String getSyntax() {
		return null;
	}
}
