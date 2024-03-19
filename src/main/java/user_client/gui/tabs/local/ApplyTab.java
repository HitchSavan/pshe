package user_client.gui.tabs.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class ApplyTab extends Tab {
    public TextField projectPathField;
    public Button chooseProjectButton;
    public TextField patchPathField;
    public Button choosePatchButton;
    public CheckBox rememberPathsCheckbox;
    public CheckBox replaceFilesCheckbox;
    public Button applyPatchButton;
    public Label activeCourgettesAmount;
    public Path projectPath;
    public Path patchPath;

    public ApplyTab(JSONObject config) {
        boolean rememberPaths = false;
        boolean replaceFiles = false;

        projectPath = Paths.get(config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getString("projectPath"));
        patchPath = Paths.get(config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getString("patchPath"));
        rememberPaths = config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getBoolean("rememberPaths");
        replaceFiles = config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getBoolean("replaceFiles");

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

        Label patchPathLabel = new Label("Path to patch:");
        patchPathLabel.setPrefSize(105, 25);
        patchPathField = new TextField(patchPath.toString());
        patchPathField.setEditable(true);
        choosePatchButton = new Button("browse");
        choosePatchButton.setPrefSize(70, 0);

        AnchorPane patchPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(patchPathLabel, 5d);
        AnchorPane.setLeftAnchor(patchPathField, 5d + patchPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(patchPathField, 5d + choosePatchButton.getPrefWidth());
        AnchorPane.setRightAnchor(choosePatchButton, 5d);
        patchPathPanel.getChildren().addAll(patchPathLabel, patchPathField, choosePatchButton);

        rememberPathsCheckbox = new CheckBox("Remember");
        rememberPathsCheckbox.setSelected(rememberPaths);
        replaceFilesCheckbox = new CheckBox("Replace old files");
        replaceFilesCheckbox.setSelected(replaceFiles);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberPathsCheckbox, replaceFilesCheckbox);

        applyPatchButton = new Button("Patch");
        applyPatchButton.setPrefSize(60, 0);

        activeCourgettesAmount = new Label("Active Courgette instances:\t0");

        VBox tabContent = new VBox();
        tabContent.setAlignment(Pos.TOP_CENTER);
        tabContent.setPadding(new Insets(5));
        tabContent.getChildren().addAll(projectPathPanel, patchPathPanel,
                checkboxPanel, applyPatchButton, activeCourgettesAmount);

        this.setContent(tabContent);
    }

    public void setupEvents(JSONObject config, AuthWindow authWindow) {
        choosePatchButton.setOnAction(e -> {
            ChoosePath.choosePath(patchPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseProjectButton.setOnAction(e -> {
            ChoosePath.choosePath(projectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        applyPatchButton.setOnAction(e -> {
            applyPatchButton.setDisable(true);

            projectPath = Paths.get(projectPathField.getText());
            patchPath = Paths.get(patchPathField.getText());

            Path tmpProjectPath = projectPath.getParent().resolve("patched_tmp").resolve(projectPath.getFileName());

            if (!config.getJSONObject(RunCourgette.os).has("localPatchingInfo")) {
                config.getJSONObject(RunCourgette.os).put("localPatchingInfo", new JSONObject());
            }
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("projectPath", projectPath.toString());
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("patchPath", patchPath.toString());
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("rememberPaths", rememberPathsCheckbox.isSelected());
            config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("replaceFiles", replaceFilesCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = null;
            try {
                fileVisitor = new FileVisitor();
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }

            List<Path> oldFiles = null;
            List<Path> patchFiles = null;

            try {
                oldFiles = fileVisitor.walkFileTree(projectPath);
                patchFiles = fileVisitor.walkFileTree(patchPath);
            } catch (IOException e1) {
                AlertWindow.showErrorWindow("Cannot walk project file tree");
                e1.printStackTrace();
            }

            Path relativePatchPath;
            Path newPath;
            Path oldPath;
            byte[] emptyData = {0};
    
            for (Path patchFile: patchFiles) {
                relativePatchPath = patchPath.relativize(patchFile);
                newPath = tmpProjectPath.resolve(relativePatchPath.toString().equals("") ?
                        Paths.get("..", "..", "..", tmpProjectPath.getParent().getFileName().toString(),
                                tmpProjectPath.getFileName().toString()).toString() :
                        relativePatchPath.toString().substring(0, relativePatchPath.toString().length() - "_patch".length())).normalize();
                oldPath = projectPath.resolve(relativePatchPath.toString().equals("") ? "" :
                        relativePatchPath.toString().substring(0, relativePatchPath.toString().length() - "_patch".length())).normalize();

                if (!oldFiles.contains(oldPath)) {
                    try {
                        oldPath.getParent().toFile().mkdirs();
                        Files.createFile(oldPath);
                        Files.write(oldPath, emptyData);
                    } catch (IOException e1) {
                        AlertWindow.showErrorWindow("Cannot create patch file");
                        e1.printStackTrace();
                        return;
                    }
                }
    
                try {
                    Files.createDirectories(newPath.getParent());
                } catch (IOException e1) {
                    AlertWindow.showErrorWindow("Cannot create patch files directory");
                    e1.printStackTrace();
                    return;
                }
                new CourgetteHandler().applyPatch(oldPath, newPath, patchFile,
                        projectPath, replaceFilesCheckbox.isSelected(), activeCourgettesAmount, false);
            }
            applyPatchButton.setDisable(false);
        });
    }
}
