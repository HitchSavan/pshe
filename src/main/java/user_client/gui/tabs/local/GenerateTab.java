package user_client.gui.tabs.local;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;

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
import patcher.utils.files_utils.FileVisitor;
import patcher.utils.patching_utils.RunCourgette;
import user_client.gui.AuthWindow;
import user_client.utils.AlertWindow;
import user_client.utils.ChoosePath;
import user_client.utils.CourgetteHandler;

public class GenerateTab extends Tab {
    public TextField oldProjectPathField;
    public Button chooseOldProjectButton;
    public TextField newProjectPathField;
    public Button chooseNewProjectButton;
    public TextField patchPathField;
    public Button choosePatchButton;
    public CheckBox rememberPathsCheckbox;
    public Button genPatchButton;
    public Label activeCourgettesAmount;
    public Path oldProjectPath;
    public Path newProjectPath;
    public Path patchFolderPath;

    public GenerateTab(JSONObject config) {
        boolean rememberPaths = config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getBoolean("rememberPaths");
        oldProjectPath = Paths.get(config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("oldProjectPath"));
        newProjectPath = Paths.get(config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("newProjectPath"));
        patchFolderPath = Paths.get(config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("patchPath"));

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

        Label genPatchPathLabel = new Label("Path to patch folder:");
        genPatchPathLabel.setPrefSize(135, 25);
        patchPathField = new TextField(patchFolderPath.toString());
        patchPathField.setEditable(true);
        choosePatchButton = new Button("browse");
        choosePatchButton.setPrefSize(70, 0);

        AnchorPane patchPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(genPatchPathLabel, 5d);
        AnchorPane.setLeftAnchor(patchPathField, 5d + genPatchPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(patchPathField, 5d + choosePatchButton.getPrefWidth());
        AnchorPane.setRightAnchor(choosePatchButton, 5d);
        patchPathPanel.getChildren().addAll(genPatchPathLabel, patchPathField, choosePatchButton);

        rememberPathsCheckbox = new CheckBox("Remember");
        rememberPathsCheckbox.setSelected(rememberPaths);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberPathsCheckbox);

        genPatchButton = new Button("Create patch");
        genPatchButton.setPrefSize(110, 0);

        activeCourgettesAmount = new Label("Active Courgette instances:\t0");

        VBox genTabContent = new VBox();
        genTabContent.setAlignment(Pos.TOP_CENTER);
        genTabContent.setPadding(new Insets(5));
        genTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                patchPathPanel, checkboxPanel, genPatchButton, activeCourgettesAmount);

        this.setContent(genTabContent);
    }

    public void setupEvents(JSONObject config, AuthWindow authWindow) {
        choosePatchButton.setOnAction(e -> {
            ChoosePath.choosePath(patchPathField, JFileChooser.DIRECTORIES_ONLY);
        });
        chooseNewProjectButton.setOnAction(e -> {
            ChoosePath.choosePath(newProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseOldProjectButton.setOnAction(e -> {
            ChoosePath.choosePath(oldProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        genPatchButton.setOnAction(e -> {
            genPatchButton.setDisable(true);
            oldProjectPath = Paths.get(oldProjectPathField.getText());
            newProjectPath = Paths.get(newProjectPathField.getText());
            patchFolderPath = Paths.get(patchPathField.getText());

            if (!config.getJSONObject(RunCourgette.os).has("localPatchCreationInfo")) {
                config.getJSONObject(RunCourgette.os).put("localPatchCreationInfo", new JSONObject());
            }
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("patchPath", patchFolderPath.toString());
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("oldProjectPath", oldProjectPath.toString());
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("newProjectPath", newProjectPath.toString());
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("rememberPaths", rememberPathsCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = null;
            try {
                fileVisitor = new FileVisitor();
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }

            List<Path> oldFiles = null;
            List<Path> newFiles = null;

            try {
                oldFiles = new ArrayList<>(fileVisitor.walkFileTree(oldProjectPath));
                newFiles = new ArrayList<>(fileVisitor.walkFileTree(newProjectPath));
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }
            
            CourgetteHandler.generatePatch(patchFolderPath, oldProjectPath, newProjectPath, oldFiles, newFiles, "forward", activeCourgettesAmount);
            CourgetteHandler.generatePatch(patchFolderPath, newProjectPath, oldProjectPath, newFiles, oldFiles, "backward", activeCourgettesAmount);
            genPatchButton.setDisable(false);
        });
    }
}
