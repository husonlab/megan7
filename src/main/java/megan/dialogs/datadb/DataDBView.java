package megan.dialogs.datadb;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckMenuItem;
import jloda.swing.util.ProgramProperties;
import jloda.util.StringUtils;
import megan.fx.SwingPanel4FX;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * the export data to SQLite DB view
 * Daniel Huson, 1.2023
 */
public class DataDBView extends JDialog {
	private final SwingPanel4FX<DataDBViewController> swingPanel4FX;
	private final ClassificationViewer viewer;
	private DataDBViewController controller;
	private DataDBViewPresenter presenter;

	public DataDBView(ClassificationViewer viewer) {
		this.viewer = viewer;
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setModal(true);
		var fxmlLoader = new FXMLLoader();
		try (var ins = Objects.requireNonNull(DataDBViewController.class.getResource("DataDBView.fxml")).openStream()) {
			fxmlLoader.load(ins);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		setTitle();
		setLocationRelativeTo(viewer.getFrame());

		setSize(480, 460);

		swingPanel4FX = new SwingPanel4FX<>(this.getClass());

		var mainPanel = getContentPane();

		swingPanel4FX.runLaterInSwing(() -> {
			mainPanel.add(swingPanel4FX.getPanel(), BorderLayout.CENTER); // add panel once initialization complete
			mainPanel.validate();
			Platform.runLater(() -> {
				controller = swingPanel4FX.getController();
				presenter = new DataDBViewPresenter(this);
				// uptodate is set by controller
			});
		});
	}

	/**
	 * set the title of the window
	 */
	private void setTitle() {
		var newTitle = "Export to SQLite - " + viewer.getDocument().getTitle();

		if (viewer.getDir().getID() == 1)
			newTitle += " - " + ProgramProperties.getProgramVersion();
		else
			newTitle += " - [" + viewer.getDir().getID() + "] - " + ProgramProperties.getProgramVersion();

		if (!getTitle().equals(newTitle)) {
			setTitle(newTitle);
		}
	}

	public ClassificationViewer getViewer() {
		return viewer;
	}

	public DataDBViewController getController() {
		return controller;
	}

	public DataDBViewPresenter getPresenter() {
		return presenter;
	}

	public String createCommand() {
		// format: "export sqlite outputFile=<file-name> small={true|false} medium={false|true} large={false|true} xlarge={false|true} [classifications={ALL|names...}] [selected={false|true}]>";
		var buf = new StringBuilder("export sqlite");
		buf.append(String.format(" outputFile='%s' overwrite=true", controller.getOutputFileTextField().getText()));
		buf.append(" small=").append(controller.getSmallCheckBox().isSelected());
		buf.append(" smallSelected=").append(controller.getSmallUseSelectionCheckBox().isSelected());
		buf.append(" medium=").append(controller.getMediumCheckBox().isSelected());
		buf.append(" large=").append(controller.getLargeCheckBox().isSelected());
		buf.append(" xlarge=").append(controller.getxLargeCheckBox().isSelected());
		buf.append(" selected=").append(controller.getSelectedOnlyCheckBox().isSelected());
		{
			var selected = new ArrayList<String>();
			var all = true;
			for (var menuItem : controller.getClassificationsMenuButton().getItems()) {
				if (menuItem instanceof CheckMenuItem) {
					if (((CheckMenuItem) menuItem).isSelected()) {
						selected.add(menuItem.getText());
					} else
						all = false;
				}
			}
			if (all)
				buf.append(" classifications=all");
			else
				buf.append(" classifications=").append(StringUtils.toString(selected, " "));
		}
		buf.append(";");
		return buf.toString();
	}
}
