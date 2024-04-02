package user_client.gui.tabs.remote;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import patcher.utils.patching_utils.RunCourgette;
import user_client.gui.AuthWindow;
import user_client.utils.AlertWindow;
import user_client.utils.CheckoutToVersion;
import user_client.utils.ChoosePath;

public class RemoteApplyTab extends Tab {
    public TextField projectPathField;
    public Button chooseProjectButton;
    public CheckBox rememberPathsCheckbox;
    public CheckBox replaceFilesCheckbox;
    public Button patchToRootButton;
    public Label activeCourgettesAmount;
    public Label applyStatus;
    public Path projectPath;
    
    public VBox setupUi(JSONObject config) {
        projectPath = Paths.get(config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getString("projectPath"));
        boolean rememberPaths = config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getBoolean("rememberPaths");

        Label projectPathLabel = new Label("Path to project:");
        projectPathLabel.setPrefSize(105, 25);
        projectPathField = new TextField(projectPath.toString());
        projectPathField.setEditable(true);
        chooseProjectButton = new Button("browse");
        chooseProjectButton.setPrefSize(70, 0);

        AnchorPane projectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(projectPathLabel, 5d);
        AnchorPane.setLeftAnchor(projectPathField, 5d + projectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(projectPathField, 5d + chooseProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseProjectButton, 5d);
        projectPathPanel.getChildren().addAll(projectPathLabel, projectPathField, chooseProjectButton);

        rememberPathsCheckbox = new CheckBox("Remember");
        rememberPathsCheckbox.setSelected(rememberPaths);

        replaceFilesCheckbox = new CheckBox("Replace files");
        replaceFilesCheckbox.setSelected(false);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberPathsCheckbox, replaceFilesCheckbox);

        patchToRootButton = new Button("Patch to root version");
        patchToRootButton.setPrefSize(150, 0);
        patchToRootButton.setDisable(true);
        
        CheckoutToVersion.addDisablingButton(patchToRootButton);
        CheckoutToVersion.addDisablingButton(chooseProjectButton);

        activeCourgettesAmount = new Label("Active Courgette instances:\t0");
        applyStatus = new Label("Status: idle");

        VBox applyPatchTabContent = new VBox();
        applyPatchTabContent.setAlignment(Pos.TOP_CENTER);
        applyPatchTabContent.setPadding(new Insets(5));
        applyPatchTabContent.getChildren().addAll(projectPathPanel, checkboxPanel,
                patchToRootButton, activeCourgettesAmount, applyStatus);
        return applyPatchTabContent;
    }

    public void setupEvents(String rootVersion, ProgressBar progressBar, JSONObject config, AuthWindow authWindow) {
        chooseProjectButton.setOnAction(e -> {
            ChoosePath.chooseDirectory(chooseProjectButton, projectPathField, (Stage)chooseProjectButton.getScene().getWindow());
        });
        patchToRootButton.setOnAction(e -> {
            try {
                CheckoutToVersion.checkoutToVersionByFiles(Paths.get(projectPathField.getText()), replaceFilesCheckbox.isSelected(),
                        rootVersion, applyStatus, progressBar, activeCourgettesAmount, config, authWindow,
                        rememberPathsCheckbox.isSelected(), rootVersion);
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                e1.printStackTrace();
            }
        });
    }
}
