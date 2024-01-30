/*
 * ExtractSamplesCommand.java Copyright (C) 2024 Daniel H. Huson
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

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ProgramProperties;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.FileUtils;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.main.MeganProperties;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * * extract samples command
 * * Daniel Huson, 6.2015
 */
public class ExtractSamplesCommand extends CommandBase implements ICommand {
	public String getSyntax() {
		return "extract samples=<name1 name2 ...> [toFile=<name>];";
	}

	/**
	 * parses the given command and executes it
	 */
	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase("extract");

		final List<String> toExtract = new ArrayList<>();

		np.matchIgnoreCase("samples=");
		while (!np.peekMatchIgnoreCase(";") && !np.peekMatchIgnoreCase("file=")) {
			String name = np.getWordRespectCase();
			toExtract.add(name);
		}

		String fileName = null;
		if (np.peekMatchIgnoreCase("file")) {
			np.matchIgnoreCase("file=");
			fileName = np.getWordFileNamePunctuation();
		}
		np.matchIgnoreCase(";");

		if (fileName == null) {
			final String sourceFileName = ((Director) getDir()).getDocument().getMeganFile().getFileName();
			if (toExtract.size() == 1)
				fileName = FileUtils.getFileWithNewUniqueName(FileUtils.replaceFileSuffix(sourceFileName, "-" + StringUtils.toCleanName(toExtract.get(0)) + ".megan")).toString();
			else
				fileName = FileUtils.getFileWithNewUniqueName(FileUtils.replaceFileSuffix(sourceFileName, "-extract.megan")).toString();
		}

		final Director newDir = Director.newProject();
		newDir.getMainViewer().setDoReInduce(true);
		newDir.getMainViewer().setDoReset(true);
		final Document newDocument = newDir.getDocument();

		if (toExtract.size() > 0) {
			newDir.notifyLockInput();
			try {
				newDocument.getMeganFile().setFile(fileName, MeganFile.Type.MEGAN_SUMMARY_FILE);
				newDocument.extractSamples(toExtract, ((Director) getDir()).getDocument());
				newDocument.setNumberReads(newDocument.getDataTable().getTotalReads());
				newDir.getMainViewer().getFrame().setVisible(true);
				System.err.println("Number of reads: " + newDocument.getNumberOfReads());
				newDocument.processReadHits();
				newDocument.setTopPercent(100);
				newDocument.setMinScore(0);
				newDocument.setMaxExpected(10000);
				newDocument.setMinSupport(1);
				newDocument.setDirty(true);
				newDocument.getActiveViewers().addAll(newDocument.getDataTable().getClassification2Class2Counts().keySet());

				if (newDocument.getNumberOfSamples() > 1) {
					newDir.getMainViewer().getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.PieChart);
				}
				NotificationsInSwing.showInformation(String.format("Extracted %,d reads to file '%s'", +newDocument.getNumberOfReads(), fileName));

			} finally {
				newDir.notifyUnlockInput();
			}
			newDir.execute("update reprocess=true reInduce=true;", newDir.getMainViewer().getCommandManager());
		}
	}

	public void actionPerformed(ActionEvent event) {
	}

	public boolean isApplicable() {
		return true;
	}

	public String getName() {
		return "Extract Samples...";
	}


	public String getDescription() {
		return "Extract samples to a new document";
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
