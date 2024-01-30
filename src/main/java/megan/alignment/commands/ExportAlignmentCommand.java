/*
 * ExportAlignmentCommand.java Copyright (C) 2024 Daniel H. Huson
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
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.FastaFileFilter;
import jloda.swing.util.ProgramProperties;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.FileUtils;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.SelectedBlock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * save data command
 * Daniel Huson, 11.2010
 */
public class ExportAlignmentCommand extends CommandBase implements ICommand {
	public String getSyntax() {
		return "export alignment file=<filename> [what={all|selected}];";
	}

	public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase("export alignment file=");
		String fileName = np.getAbsoluteFileName();
		boolean saveAll = true;
		if (np.peekMatchIgnoreCase("what")) {
			np.matchIgnoreCase("what=");
			saveAll = np.getWordMatchesIgnoringCase("selected all").equalsIgnoreCase("all");
		}
		np.matchIgnoreCase(";");

		try {
			AlignmentViewer viewer = (AlignmentViewer) getViewer();
			if (saveAll) {
				System.err.println("Exporting complete alignment to: " + fileName);
				Alignment alignment = viewer.getAlignment();
				String string = alignment.toFastA();
				Writer w = new FileWriter(fileName);
				w.write(string);
				w.close();
			} else {
				System.err.println("Exporting selected alignment to: " + fileName);
				Writer w = new FileWriter(fileName);
				w.write(viewer.getAlignmentViewerPanel().getSelectedAlignment());
				w.close();
			}
		} catch (IOException e) {
			NotificationsInSwing.showError("Export Alignment failed: " + e.getMessage());
		}
	}

	public void actionPerformed(ActionEvent event) {
		File lastOpenFile = ProgramProperties.getFile("SaveAlignment");
		String fileName = ((AlignmentViewer) getViewer()).getAlignment().getName();
		if (fileName == null)
			fileName = "Untitled";
		else
			fileName = StringUtils.toCleanName(fileName);
		if (lastOpenFile != null) {
			fileName = new File(lastOpenFile.getParent(), fileName).getPath();
		}
		fileName = FileUtils.replaceFileSuffix(fileName, "-alignment.fasta");

		File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), new File(fileName), new FastaFileFilter(), new FastaFileFilter(), event, "Save alignment file", ".fasta");

		if (file != null) {
			if (FileUtils.getFileSuffix(file.getName()) == null)
				file = FileUtils.replaceFileSuffix(file, ".txt");
			ProgramProperties.put("SaveAlignment", file);
			SelectedBlock selectedBlock = ((AlignmentViewer) getViewer()).getSelectedBlock();

			executeImmediately("export alignment file='" + file.getPath() + "' what=" + (selectedBlock == null || !selectedBlock.isSelected() ? "all" : "selected") + ";");
		}
	}

	public boolean isApplicable() {
		return ((AlignmentViewer) getViewer()).getAlignment().getLength() > 0;
	}

	public String getName() {
		return "Alignment...";
	}

	public String getAltName() {
		return "Export Alignment...";
	}

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Export16.gif");
	}

	public String getDescription() {
		return "Save alignment to a file";
	}

	public boolean isCritical() {
		return false;
	}

	public KeyStroke getAcceleratorKey() {
		return KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
	}
}
