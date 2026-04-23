package megan.dialogs.datadb;

import jloda.util.CanceledException;
import jloda.util.FileUtils;
import jloda.util.progress.ProgressListener;
import megan.accessiondb.ConfigRequests;
import megan.core.Document;
import megan.viewer.ClassificationViewer;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * export contents of a file to SQLITE
 * Daniel Huson, 1.2023
 */
public class ExportToSQLite {
	public static String ALL = "ALL";

	private boolean small = true;
	private boolean medium = false;
	private boolean large = false;
	private boolean xLarge = false;

	private boolean smallSelectedOnly = false;

	private boolean selectedOnly = false;

	private String outputFile = null;
	private final ArrayList<String> classifications = new ArrayList<>();
	private boolean allowOverwrite = false;

	private final Set<String> selectedReads = new HashSet<>();

	public ExportToSQLite() {
		classifications.add(ALL);
	}

	public void save(Document document, String outputFile, ProgressListener progress) throws IOException, SQLException {
		if (FileUtils.fileExistsAndIsNonEmpty(outputFile)) {
			if (!isAllowOverwrite())
				throw new IOException("File exists: " + outputFile + ", specify 'allow overwrite'");
			else
				FileUtils.deleteFileIfExists(outputFile);
		}
		if (!document.getMeganFile().hasDataConnector()) {
			if (isMedium())
				throw new IOException("Export 'medium' not available for summary file");
			if (isLarge())
				throw new IOException("Export 'large' not available for summary file");
			if (isxLarge())
				throw new IOException("Export 'xLarge' not available for summary file");
		}

		var config = new SQLiteConfig();
		config.setCacheSize(ConfigRequests.getCacheSize());
		config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
		config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

		if (getClassifications().size() > 1)
			getClassifications().remove(ALL);

		config.setTempStore(ConfigRequests.isUseTempStoreInMemory() ? SQLiteConfig.TempStore.MEMORY : SQLiteConfig.TempStore.DEFAULT);

		try (var connection = config.createConnection("jdbc:sqlite:" + outputFile)) {
			if (isSmall())
				ExportSmall.apply(connection, document, classifications, isSmallSelectedOnly() ? selectedReads : null, progress);
			if (isMedium())
				ExportMedium.apply(connection, document.getConnector(), classifications, isSelectedOnly() ? selectedReads : null, progress);
			if (isLarge()) {
				ExportLarge.apply(connection, document, isSelectedOnly() ? selectedReads : null, progress);
			}
			if (isxLarge()) {
				ExportXLarge.apply(connection, document, isSelectedOnly() ? selectedReads : null, progress);
			}
		} catch (CanceledException ex) {
			throw new IOException("ExportToSQLite: user CANCELED");
		}
		System.err.println("Exported SQLITE database to: " + outputFile);
	}

	public ArrayList<String> getClassifications() {
		return classifications;
	}

	public boolean isSmall() {
		return small;
	}

	public void setSmall(boolean small) {
		this.small = small;
	}

	public boolean isMedium() {
		return medium;
	}

	public void setMedium(boolean medium) {
		this.medium = medium;
	}

	public boolean isLarge() {
		return large;
	}

	public void setLarge(boolean large) {
		this.large = large;
	}

	public boolean isxLarge() {
		return xLarge;
	}

	public void setxLarge(boolean xLarge) {
		this.xLarge = xLarge;
	}

	public boolean isSmallSelectedOnly() {
		return smallSelectedOnly;
	}

	public void setSmallSelectedOnly(boolean smallSelectedOnly) {
		this.smallSelectedOnly = smallSelectedOnly;
	}

	public boolean isSelectedOnly() {
		return selectedOnly;
	}

	public void setSelectedOnly(boolean selectedOnly) {
		this.selectedOnly = selectedOnly;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public boolean isAllowOverwrite() {
		return allowOverwrite;
	}

	public void setAllowOverwrite(boolean allowOverwrite) {
		this.allowOverwrite = allowOverwrite;
	}

	public void loadAllSelectedReads(ClassificationViewer viewer) throws IOException {
		var connector = viewer.getDocument().getConnector();
		var progress = viewer.getDocument().getProgressListener();
		progress.setTasks("Preparing export", "collecting selected");
		var selected = viewer.getSelectedNodeIds();
		progress.setMaximum(selected.size());
		progress.setProgress(0);
		for (var classId : selected) {
			try (var it = connector.getReadsIterator(viewer.getClassification().getName(), classId, 0, 10, false, false)) {
				while (it.hasNext())
					selectedReads.add(it.next().getReadName());
				progress.incrementProgress();
			}
		}
		progress.reportTaskCompleted();
	}
}
