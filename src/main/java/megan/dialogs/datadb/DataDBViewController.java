package megan.dialogs.datadb;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import jloda.swing.util.ProgramProperties;

public class DataDBViewController {

	@FXML
	private MenuItem allMenuItem;

	@FXML
	private MenuItem noneMenuItem;

	@FXML
	private Button applyButton;

	@FXML
	private Button browseButton;

	@FXML
	private Button cancelButton;

	@FXML
	private Label classificationsLabel;

	@FXML
	private MenuButton classificationsMenuButton;

	@FXML
	private CheckBox largeCheckBox;

	@FXML
	private CheckBox mediumCheckBox;

	@FXML
	private TextField outputFileTextField;

	@FXML
	private CheckBox smallCheckBox;

	@FXML
	private CheckBox xLargeCheckBox;

	@FXML
	private Label selectedNodesLabel;

	@FXML
	private CheckBox selectedOnlyCheckBox;

	@FXML
	private Label smallSelectionLabel;

	@FXML
	private CheckBox smallUseSelectionCheckBox;


	@FXML
	private void initialize() {
		smallCheckBox.setSelected(ProgramProperties.get("DataDB.smallCheckBox", true));
		smallCheckBox.selectedProperty().addListener((v, o, n) -> ProgramProperties.put("DataDB.smallCheckBox", n));

		smallUseSelectionCheckBox.setSelected(ProgramProperties.get("DataDB.smallUseSelectionCheckBox", false));
		smallUseSelectionCheckBox.selectedProperty().addListener((v, o, n) -> ProgramProperties.put("DataDB.smallUseSelectionCheckBox", n));

		mediumCheckBox.setSelected(ProgramProperties.get("DataDB.mediumCheckBox", false));
		mediumCheckBox.selectedProperty().addListener((v, o, n) -> ProgramProperties.put("DataDB.mediumCheckBox", n));
		largeCheckBox.setSelected(ProgramProperties.get("DataDB.largeCheckBox", false));
		largeCheckBox.selectedProperty().addListener((v, o, n) -> ProgramProperties.put("DataDB.largeCheckBox", n));
		xLargeCheckBox.setSelected(ProgramProperties.get("DataDB.xLargeCheckBox", false));
		xLargeCheckBox.selectedProperty().addListener((v, o, n) -> ProgramProperties.put("DataDB.xLargeCheckBox", n));

		selectedOnlyCheckBox.setSelected(ProgramProperties.get("DataDB.selectedOnlyCheckBox", false));
		selectedOnlyCheckBox.selectedProperty().addListener((v, o, n) -> ProgramProperties.put("DataDB.selectedOnlyCheckBox", n));
	}

	public MenuItem getAllMenuItem() {
		return allMenuItem;
	}

	public MenuItem getNoneMenuItem() {
		return noneMenuItem;
	}

	public Button getApplyButton() {
		return applyButton;
	}

	public Button getBrowseButton() {
		return browseButton;
	}

	public Button getCancelButton() {
		return cancelButton;
	}

	public Label getClassificationsLabel() {
		return classificationsLabel;
	}

	public MenuButton getClassificationsMenuButton() {
		return classificationsMenuButton;
	}

	public CheckBox getLargeCheckBox() {
		return largeCheckBox;
	}

	public CheckBox getMediumCheckBox() {
		return mediumCheckBox;
	}

	public TextField getOutputFileTextField() {
		return outputFileTextField;
	}

	public CheckBox getSmallCheckBox() {
		return smallCheckBox;
	}

	public CheckBox getxLargeCheckBox() {
		return xLargeCheckBox;
	}

	public Label getSelectedNodesLabel() {
		return selectedNodesLabel;
	}

	public CheckBox getSelectedOnlyCheckBox() {
		return selectedOnlyCheckBox;
	}

	public Label getSmallSelectionLabel() {
		return smallSelectionLabel;
	}

	public CheckBox getSmallUseSelectionCheckBox() {
		return smallUseSelectionCheckBox;
	}
}
