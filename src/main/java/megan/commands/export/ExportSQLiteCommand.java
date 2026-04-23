package megan.commands.export;

import jloda.swing.commands.CommandManager;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.IOExceptionWithLineNumber;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.dialogs.datadb.DataDBView;
import megan.dialogs.datadb.ExportToSQLite;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * export to SQLite
 * Daniel Huson,5/2022
 */
public class ExportSQLiteCommand extends CommandBase implements ICommand {

	public ExportSQLiteCommand() {
	}

	/**
	 * constructor
	 */
	public ExportSQLiteCommand(CommandManager commandManager) {
		super(commandManager);
	}

	public String getSyntax() {
		return "export sqlite outputFile=<file-name> small={true|false} [smallSelected={false|true}] medium={false|true} large={false|true} xlarge={false|true}  [selected={false|true}] [classifications={ALL|names...}]";
	}

	/**
	 * apply the command
	 */
	public void apply(NexusStreamParser np) throws IOExceptionWithLineNumber {
		np.matchIgnoreCase("export sqlite outputFile=");
		var outputFile = np.getWordFileNamePunctuation();

		var overwrite = false;
		if (np.peekMatchIgnoreCase("overwrite")) {
			np.matchIgnoreCase("overwrite=");
			overwrite = np.getBoolean();
		}

		var small = true;
		if (np.peekMatchIgnoreCase("small")) {
			np.matchIgnoreCase("small=");
			small = np.getBoolean();
		}

		var smallSelectedOnly = false;
		if (np.peekMatchIgnoreCase("smallSelected")) {
			np.matchIgnoreCase("smallSelected=");
			smallSelectedOnly = np.getBoolean();
		}

		var medium = false;
		if (np.peekMatchIgnoreCase("medium")) {
			np.matchIgnoreCase("medium=");
			medium = np.getBoolean();
		}
		var large = false;
		if (np.peekMatchIgnoreCase("large")) {
			np.matchIgnoreCase("large=");
			large = np.getBoolean();
		}
		var xLarge = false;
		if (np.peekMatchIgnoreCase("xLarge")) {
			np.matchIgnoreCase("xLarge=");
			xLarge = np.getBoolean();
		}

		var selected = false;
		if (np.peekMatchIgnoreCase("selected")) {
			np.matchIgnoreCase("selected=");
			selected = np.getBoolean();
		}

		var classifications = new ArrayList<String>();
		if (np.peekMatchIgnoreCase("classifications")) {
			np.matchIgnoreCase("classifications=");
			while (!np.peekMatchIgnoreCase(";")) {
				classifications.add(np.getWordRespectCase().toUpperCase());
			}
		} else
			classifications.add("ALL");

		if (!classifications.isEmpty() && (small || medium || large || xLarge)) {
			var exportSQL = new ExportToSQLite();
			exportSQL.setAllowOverwrite(overwrite);
			exportSQL.setSmall(small);
			exportSQL.setSmallSelectedOnly(smallSelectedOnly);
			exportSQL.setMedium(medium);
			exportSQL.setLarge(large);
			exportSQL.setxLarge(xLarge);
			exportSQL.setSelectedOnly(selected);

			exportSQL.getClassifications().addAll(classifications);

			if (exportSQL.isSelectedOnly() || exportSQL.isSmallSelectedOnly()) {
				if (getViewer() instanceof ClassificationViewer) {
					try {
						exportSQL.loadAllSelectedReads((ClassificationViewer) getViewer());
					} catch (IOException ignored) {
					}
				}
			}

			try {
				exportSQL.save(getDoc(), outputFile, getDoc().getProgressListener());
			} catch (IOException | SQLException e) {
				NotificationsInSwing.showError("Export failed: " + e.getMessage());
			}
		} else
			NotificationsInSwing.showWarning("Nothing to export");
	}

	public void actionPerformed(ActionEvent event) {
		var dialog = new DataDBView((ClassificationViewer) getViewer());
		dialog.setVisible(true);
	}

	public boolean isApplicable() {
		return getDoc().getNumberOfReads() > 0;
	}

	public static final String NAME = "Export to SQLITE...";

	public String getName() {
		return NAME;
	}

	public String getDescription() {
		return "Export a file to SQLITE";
	}

	public ImageIcon getIcon() {
		return ResourceManager.getIcon("sun/Export16.gif");
	}

	public boolean isCritical() {
		return true;
	}

	@Override
	public KeyStroke getAcceleratorKey() {
		return null;
	}
}
