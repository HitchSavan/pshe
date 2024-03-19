package user_client.gui.tabs.remote;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONObject;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import patcher.utils.files_utils.Directories;
import patcher.utils.files_utils.FileVisitor;
import patcher.utils.patching_utils.RunCourgette;
import user_client.gui.AuthWindow;
import user_client.utils.AlertWindow;
import user_client.utils.ChoosePath;
import user_client.utils.CourgetteHandler;

public class RemoteGenerateTab extends Tab {
    public TextField oldProjectPathField;
    public Button chooseOldProjectButton;
    public TextField newProjectPathField;
    public Button chooseNewProjectButton;
    public CheckBox rememberPathsCheckbox;
    public Button genPatchButton;
    public Label activeCourgettesAmount;
    public Path oldProjectPath;
    public Path newProjectPath;

    public VBox setupUi(JSONObject config) {
        // TODO: fix loading
        boolean rememberPaths = false;

        oldProjectPath = Paths.get(config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("oldProjectPath"));
        newProjectPath = Paths.get(config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("newProjectPath"));

        Label oldProjectPathLabel = new Label("Path to old version:");
        oldProjectPathLabel.setPrefSize(135, 25);
        oldProjectPathField = new TextField(oldProjectPath.toString());
        oldProjectPathField.setEditable(true);
        chooseOldProjectButton = new Button("browse");
        chooseOldProjectButton.setPrefSize(70, 0);

        AnchorPane oldProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(oldProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(oldProjectPathField, 5d + oldProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(oldProjectPathField, 5d + chooseOldProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseOldProjectButton, 5d);
        oldProjectPathPanel.getChildren().addAll(oldProjectPathLabel, oldProjectPathField, chooseOldProjectButton);

        Label newProjectPathLabel = new Label("Path to new version:");
        newProjectPathLabel.setPrefSize(135, 25);
        newProjectPathField = new TextField(newProjectPath.toString());
        newProjectPathField.setEditable(true);
        chooseNewProjectButton = new Button("browse");
        chooseNewProjectButton.setPrefSize(70, 0);

        AnchorPane newProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(newProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(newProjectPathField, 5d + newProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(newProjectPathField, 5d + chooseNewProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseNewProjectButton, 5d);
        newProjectPathPanel.getChildren().addAll(newProjectPathLabel, newProjectPathField, chooseNewProjectButton);

        rememberPathsCheckbox = new CheckBox("Remember");
        rememberPathsCheckbox.setSelected(rememberPaths);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberPathsCheckbox);

        genPatchButton = new Button("Create patch");
        genPatchButton.setPrefSize(110, 0);

        activeCourgettesAmount = new Label("Active Courgette instances:\t0");

        VBox genPatchTabContent = new VBox();
        genPatchTabContent.setAlignment(Pos.TOP_CENTER);
        genPatchTabContent.setPadding(new Insets(5));
        genPatchTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                checkboxPanel, genPatchButton, activeCourgettesAmount);
        return genPatchTabContent;
    }

    public void setupEvents(String rootVersion, JSONObject config, AuthWindow authWindow) {
        chooseNewProjectButton.setOnAction(e -> {
            ChoosePath.chooseDirectory(chooseNewProjectButton, newProjectPathField, (Stage)chooseNewProjectButton.getScene().getWindow());
        });
        chooseOldProjectButton.setOnAction(e -> {
            ChoosePath.chooseDirectory(chooseOldProjectButton, oldProjectPathField, (Stage)chooseOldProjectButton.getScene().getWindow());
        });
        genPatchButton.setOnAction(e -> {
            genPatchButton.setDisable(true);
            oldProjectPath = Paths.get(oldProjectPathField.getText());
            newProjectPath = Paths.get(newProjectPathField.getText());
            Path patchFolderPath = newProjectPath.getParent().resolve("tmp_patch");

            if (!config.getJSONObject(RunCourgette.os).has("remotePatchCreationInfo")) {
                config.getJSONObject(RunCourgette.os).put("remotePatchCreationInfo", new JSONObject());
            }
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchCreationInfo").put("oldProjectPath", oldProjectPath.toString());
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchCreationInfo").put("newProjectPath", newProjectPath.toString());
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchCreationInfo").put("rememberPaths", rememberPathsCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = null;
            try {
                fileVisitor = new FileVisitor(newProjectPath);
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }

            List<Path> oldFiles = null;
            List<Path> newFiles = null;

            try {
                oldFiles = fileVisitor.walkFileTree(oldProjectPath);
                newFiles = fileVisitor.walkFileTree(newProjectPath);
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }
            
            CourgetteHandler.generatePatch(patchFolderPath, oldProjectPath, newProjectPath, oldFiles, newFiles, "forward", activeCourgettesAmount);
            CourgetteHandler.generatePatch(patchFolderPath, newProjectPath, oldProjectPath, newFiles, oldFiles, "backward", activeCourgettesAmount);

            // TODO: implement upload
            Directories.deleteDirectory(patchFolderPath);
            genPatchButton.setDisable(false);
        });
    }
}
