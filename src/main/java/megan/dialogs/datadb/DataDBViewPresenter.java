package megan.dialogs.datadb;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jloda.swing.util.ProgramProperties;
import jloda.util.FileUtils;
import megan.classification.Classification;
import megan.main.Version;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * presenter
 * Daniel Huson, 1.2023
 */
public class DataDBViewPresenter {
	private final BooleanProperty allowClose = new SimpleBooleanProperty(true);

	public DataDBViewPresenter(DataDBView dataDBView) {
		var viewer = dataDBView.getViewer();
		var controller = dataDBView.getController();
		var numberOfClassifications = viewer.getDocument().getClassificationNames().size();

		ObservableSet<String> classifications = FXCollections.observableSet();
		classifications.addListener((InvalidationListener) e -> controller.getClassificationsLabel().setText(String.format("%d selected", classifications.size())));

		classifications.addAll(List.of(ProgramProperties.get("DataDB.classifications", new String[]{Classification.Taxonomy})));
		classifications.addListener((InvalidationListener) e -> ProgramProperties.put("DataDB.classifications", classifications.toArray(new String[0])));

		for (var classificationName : viewer.getDocument().getClassificationNames()) {
			var item = new CheckMenuItem(classificationName);
			item.setSelected(classifications.contains(classificationName));
			item.selectedProperty().addListener((v, o, n) -> {
				if (n) {
					classifications.add(classificationName);
				} else {
					classifications.remove(classificationName);
				}
			});
			controller.getClassificationsMenuButton().getItems().add(item);
		}
		controller.getAllMenuItem().setOnAction(e -> {
			for (var item : controller.getClassificationsMenuButton().getItems()) {
				if (item instanceof CheckMenuItem) {
					((CheckMenuItem) item).setSelected(true);
				}
			}
		});
		controller.getAllMenuItem().disableProperty().bind(Bindings.createBooleanBinding(() -> classifications.size() == numberOfClassifications, classifications));

		controller.getNoneMenuItem().setOnAction(e -> {
			for (var item : controller.getClassificationsMenuButton().getItems()) {
				if (item instanceof CheckMenuItem) {
					((CheckMenuItem) item).setSelected(false);
				}
			}
		});
		controller.getNoneMenuItem().disableProperty().bind(Bindings.createBooleanBinding(() -> classifications.size() == 0, classifications));

		var sizesSelected = new SimpleIntegerProperty(0);

		controller.getSmallCheckBox().setSelected(true);

		InvalidationListener listener = e -> {
			var count = 0;
			if (controller.getSmallCheckBox().isSelected())
				count++;
			if (controller.getMediumCheckBox().isSelected())
				count++;
			if (controller.getLargeCheckBox().isSelected())
				count++;
			if (controller.getxLargeCheckBox().isSelected())
				count++;
			sizesSelected.set(count);
		};
		listener.invalidated(null);
		controller.getSmallCheckBox().selectedProperty().addListener(listener);
		controller.getSmallCheckBox().setDisable(viewer.getDocument().getNumberOfReads() == 0);

		controller.getMediumCheckBox().selectedProperty().addListener(listener);
		controller.getMediumCheckBox().setDisable(!viewer.getDocument().getMeganFile().hasDataConnector());
		controller.getLargeCheckBox().selectedProperty().addListener(listener);
		controller.getLargeCheckBox().setDisable(!viewer.getDocument().getMeganFile().hasDataConnector());
		controller.getxLargeCheckBox().selectedProperty().addListener(listener);
		controller.getxLargeCheckBox().setDisable(!viewer.getDocument().getMeganFile().hasDataConnector());

		final var lastDir = ProgramProperties.get("DataDBDir", System.getProperty("user.dir"));
		var fileName = lastDir + FileUtils.replaceFileSuffix(File.separator + viewer.getDocument().getMeganFile().getName(), ".datadb");
		controller.getOutputFileTextField().setText(fileName);

		controller.getCancelButton().setOnAction(e -> dataDBView.setVisible(false));
		controller.getCancelButton().disableProperty().bind(allowClose.not());

		controller.getApplyButton().setOnAction(e -> {
			if (okToWriteFile(controller.getOutputFileTextField().getText())) {
				viewer.getDir().execute(dataDBView.createCommand(), viewer.getCommandManager(), viewer.getFrame());
				ProgramProperties.put("DataDBDir", (new File(controller.getOutputFileTextField().getText()).getParent()));
				dataDBView.setVisible(false);
			}
		});
		controller.getApplyButton().disableProperty().bind(controller.getOutputFileTextField().textProperty().isEmpty().or(Bindings.isEmpty(classifications)).or(sizesSelected.isEqualTo(0)).or(allowClose.not()));

		controller.getBrowseButton().setOnAction(e -> {
			var file = getOutputFile(null, fileName);
			if (file != null)
				controller.getOutputFileTextField().setText(file.getPath());
		});

		controller.getSelectedNodesLabel().setText("");
		controller.getSelectedOnlyCheckBox().selectedProperty().addListener((v, o, n) -> {
			if (n)
				controller.getSelectedNodesLabel().setText(String.format("%,d selected", viewer.getSelectedNodes().size()));
			else
				controller.getSelectedNodesLabel().setText("");
		});
		controller.getSelectedOnlyCheckBox().setDisable(viewer.getSelectedNodes().size() == 0 || !viewer.getDocument().getMeganFile().hasDataConnector());

		controller.getSmallUseSelectionCheckBox().selectedProperty().addListener((v, o, n) -> {
			if (n)
				controller.getSmallSelectionLabel().setText(String.format("%,d selected (may take a long time)", viewer.getSelectedNodes().size()));
			else
				controller.getSmallSelectionLabel().setText("");
		});
		controller.getSmallUseSelectionCheckBox().setDisable(viewer.getSelectedNodes().size() == 0 || !viewer.getDocument().getMeganFile().hasDataConnector());

		allowClose.addListener((v, o, n) -> {
			if (!n)
				SwingUtilities.invokeLater(() -> dataDBView.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE));
			else
				SwingUtilities.invokeLater(() -> dataDBView.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE));
		});
	}

	private File getOutputFile(Stage owner, String defaultName) {
		allowClose.set(false);
		try {
			final var fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("MEGAN Data SQLite file", "*.datadb"));
			if (defaultName.length() > 0) {
				final var previousFile = new File(defaultName);
				if (previousFile.getParentFile() != null && previousFile.getParentFile().isDirectory())
					fileChooser.setInitialDirectory(previousFile.getParentFile());
				fileChooser.setInitialFileName(previousFile.getName());
			}
			fileChooser.setTitle("Output File for SQLite export - " + Version.NAME);

			var file = fileChooser.showSaveDialog(owner);
			if (file != null)
				ProgramProperties.put("DataDBDir", file.getParent());
			return file;
		} finally {
			allowClose.set(true);
		}
	}

	private boolean okToWriteFile(String fileName) {
		if (FileUtils.fileExistsAndIsNonEmpty(fileName)) {
			var alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Confirm Overwrite - " + Version.NAME);
			alert.setHeaderText("File already exists. Do you want to overwrite it?");
			alert.setContentText(fileName);

			var buttonTypeYes = new ButtonType("Yes");
			var buttonTypeNo = new ButtonType("No");

			alert.getButtonTypes().setAll(buttonTypeNo, buttonTypeYes);

			var result = alert.showAndWait();
			return result.isPresent() && result.get() == buttonTypeYes;
		} else
			return true;
	}
}
