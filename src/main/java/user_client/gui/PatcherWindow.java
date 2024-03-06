package user_client.gui;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFileChooser;

import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import patcher.files_utils.FileVisitor;
import patcher.files_utils.UnpackResources;
import patcher.patching_utils.RunCourgette;
import patcher.remote_api.endpoints.Versions;
import patcher.remote_api.entities.Version;
import patcher.remote_api.utils.Connector;
import user_client.utils.CourgetteHandler;
import user_client.utils.TableItemVersion;

public class PatcherWindow extends Application {

    String windowName = "PSHE patcher";
    int defaultWindowWidth = 600;
    int defaultWindowHeight = 260;

    Stage primaryStage;
    Scene primaryScene;
    AuthWindow authWindow;

    TabPane fileTabs;
    TabPane remoteTabs;

    Tab applyTab;
    Tab genTab;
    Tab applyRemoteTab;
    Tab historyTab;
    Tab adminTab;

    Button modeSwitchButton;
    boolean isFileMode = true;

    HashMap<TabPane, HashMap<String, Integer>> tabsNames = new HashMap<>();
    VBox applyPatchTabContent;
    VBox adminTabContent;
    HBox historyTabContent;
    
    Version checkoutVersion = null;
    Button checkoutButton;

    TextField patchPathField;
    Button choosePatchButton;

    TextField projectPathField;
    Button chooseProjectButton;

    TextField genPatchPathField;
    Button genChoosePatchButton;

    TextField oldProjectPathField;
    Button chooseOldProjectButton;

    TextField newProjectPathField;
    Button chooseNewProjectButton;

    TextField projectRemotePathField;
    Button chooseRemoteProjectButton;

    TextField oldProjectRemotePathField;
    Button chooseOldRemoteProjectButton;

    TextField newProjectRemotePathField;
    Button chooseNewRemoteProjectButton;

    JFileChooser fileChooser;

    CheckBox rememberPathsCheckbox;
    CheckBox replaceFilesCheckbox;
    CheckBox rememberAdminPathsCheckbox;
    CheckBox remoteRememberPathsCheckbox;
    CheckBox remoteReplaceFilesCheckbox;
    CheckBox remoteRememberAdminPathsCheckbox;

    Button applyPatchButton;
    Button createPatchButton;
    Button remoteApplyPatchButton;
    Button remoteCreatePatchButton;

    Button adminLoginButton;
    Button applyPatchLoginButton;
    Button historyLoginButton;

    Path projectPath;
    Path patchPath;
    Path oldProjectPath;
    Path newProjectPath;
    Path patchFolderPath;

    Label activeCourgetesApplyAmount;
    Label activeCourgetesGenAmount;
    Label activeRemoteCourgetesApplyAmount;
    Label activeRemoteCourgetesGenAmount;
    
    public static void runApp(String[] args) {
        System.setProperty("javafx.preloader", CustomPreloader.class.getCanonicalName());
        Application.launch(PatcherWindow.class, args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        RunCourgette.unpackCourgette();
        
        Platform.runLater(() -> {
            authWindow = new AuthWindow();
            setupFileUi();
            setupEvents();
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        setupMainWindowUi();
    }

    private void setupFileUi() {
        setupApplyTabUi();
        setupGenTabUi();
        
        fileTabs = new TabPane();
        addTab(fileTabs, "Patching", applyTab);
        addTab(fileTabs, "Generate", genTab);
        
        applyRemoteTab = new Tab();
        applyPatchLoginButton = new Button();
        setupLoginUi(applyRemoteTab, applyPatchLoginButton);
        historyTab = new Tab();
        historyLoginButton = new Button();
        setupLoginUi(historyTab, historyLoginButton);
        adminTab = new Tab();
        adminLoginButton = new Button();
        setupLoginUi(adminTab, adminLoginButton);
        
        remoteTabs = new TabPane();
        addTab(remoteTabs, "Patching", applyRemoteTab);
        addTab(remoteTabs, "History", historyTab);
        addTab(remoteTabs, "Generate", adminTab);
    }

    private void setupRemoteUi() {
        setupApplyRemoteTabUi();
        setupHistoryTabUi();
        setupAdminTabUi();
    }

    private void setupMainWindowUi() {
        this.primaryStage.setMinWidth(300);
        this.primaryStage.setMinHeight(defaultWindowHeight);
        // this.primaryStage.setMaxHeight(defaultWindowHeight);

        this.primaryStage.setWidth(defaultWindowWidth);
        this.primaryStage.setHeight(defaultWindowHeight);
        this.primaryStage.setTitle(windowName + " - FILE MODE");

        VBox mainPane = new VBox();
        VBox.setVgrow(fileTabs, Priority.ALWAYS);
        VBox.setVgrow(remoteTabs, Priority.ALWAYS);

        modeSwitchButton = new Button("Change to remote mode");
        mainPane.setPadding(new Insets(0, 0, 5, 0));
        mainPane.setAlignment(Pos.CENTER);

        mainPane.getChildren().addAll(fileTabs, modeSwitchButton);

        this.primaryScene = new Scene(mainPane);
        this.primaryStage.setScene(primaryScene);

        modeSwitchButton.setOnAction(e -> {
            if (isFileMode) {
                mainPane.getChildren().set(0, remoteTabs);
                modeSwitchButton.setText("Change to file mode");
                this.primaryStage.setTitle(windowName + " - REMOTE MODE");
                isFileMode = false;
            } else {
                mainPane.getChildren().set(0, fileTabs);
                modeSwitchButton.setText("Change to remote mode");
                this.primaryStage.setTitle(windowName + " - FILE MODE");
                isFileMode = true;
            }
        });
        
        this.primaryStage.setOnCloseRequest(e -> {
            UnpackResources.deleteDirectory("tmp");
            System.exit(0);
        });

        this.primaryStage.show();
    }

    private void addTab(TabPane tabbedPane, String tabName, Tab newTab) {
        newTab.setText(tabName);
        newTab.setClosable(false);
        tabbedPane.getTabs().add(newTab);
        if (!tabsNames.containsKey(tabbedPane))
            tabsNames.put(tabbedPane, new HashMap<>());
        tabsNames.get(tabbedPane).put(tabName, tabbedPane.getTabs().size()-1);
    }

    private void setupApplyTabUi() {
        boolean rememberPaths = false;
        boolean replaceFiles = false;

        projectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getString("projectPath"));
        patchPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getString("patchPath"));
        rememberPaths = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getBoolean("rememberPaths");
        replaceFiles = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getBoolean("replaceFiles");

        oldProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchCreationInfo").getString("oldProjectPath"));
        newProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchCreationInfo").getString("newProjectPath"));
        patchFolderPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchCreationInfo").getString("patchPath"));

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

        activeCourgetesApplyAmount = new Label("Active Courgette instances:\t" + CourgetteHandler.currentThreadsAmount());

        VBox tabContent = new VBox();
        tabContent.setAlignment(Pos.TOP_CENTER);
        tabContent.setPadding(new Insets(5));
        tabContent.getChildren().addAll(projectPathPanel, patchPathPanel,
                checkboxPanel, applyPatchButton, activeCourgetesApplyAmount);

        applyTab = new Tab();
        applyTab.setContent(tabContent);
    }

    private void setupApplyRemoteTabUi() {
        boolean rememberPaths = false;
        boolean replaceFiles = false;

        projectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getString("projectPath"));
        rememberPaths = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getBoolean("rememberPaths");
        replaceFiles = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getBoolean("replaceFiles");

        oldProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchCreationInfo").getString("oldProjectPath"));
        newProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchCreationInfo").getString("newProjectPath"));

        Label projectPathLabel = new Label("Path to project:");
        projectPathLabel.setPrefSize(105, 25);
        projectRemotePathField = new TextField(projectPath.toString());
        projectRemotePathField.setEditable(true);
        chooseRemoteProjectButton = new Button("browse");
        chooseRemoteProjectButton.setPrefSize(70, 0);

        AnchorPane projectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(projectPathLabel, 5d);
        AnchorPane.setLeftAnchor(projectRemotePathField, 5d + projectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(projectRemotePathField, 5d + chooseRemoteProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseRemoteProjectButton, 5d);
        projectPathPanel.getChildren().addAll(projectPathLabel, projectRemotePathField, chooseRemoteProjectButton);

        remoteRememberPathsCheckbox = new CheckBox("Remember");
        remoteRememberPathsCheckbox.setSelected(rememberPaths);
        remoteReplaceFilesCheckbox = new CheckBox("Replace old files");
        remoteReplaceFilesCheckbox.setSelected(replaceFiles);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(remoteRememberPathsCheckbox, remoteReplaceFilesCheckbox);

        remoteApplyPatchButton = new Button("Patch to latest version");
        remoteApplyPatchButton.setPrefSize(150, 0);

        activeRemoteCourgetesApplyAmount = new Label("Active Courgette instances:\t" + CourgetteHandler.currentThreadsAmount());

        applyPatchTabContent = new VBox();
        applyPatchTabContent.setAlignment(Pos.TOP_CENTER);
        applyPatchTabContent.setPadding(new Insets(5));
        applyPatchTabContent.getChildren().addAll(projectPathPanel, checkboxPanel, remoteApplyPatchButton, activeRemoteCourgetesApplyAmount);
    }

    private void setupHistoryTabUi() {
        // TODO: PATCH TABLE HISTORY PLACEHOLDER
        String[] columnNames = {"Version", "Date", "Files amount", "Total size"};

        ObservableList<TableItemVersion> versions = FXCollections.observableArrayList();

        JSONObject versionsHistory = null;
        try {
            versionsHistory = Versions.getHistory();

            if (versionsHistory.getBoolean("success")) {
                versionsHistory.getJSONArray("versions").forEach(v -> {
                    versions.add(new TableItemVersion(new Version((JSONObject)v)));
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        TableView<TableItemVersion> table = new TableView<>(versions);

        TableColumn<TableItemVersion, String> dateColumn = new TableColumn<>(columnNames[0]);
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("versionString"));
        table.getColumns().add(dateColumn);

        TableColumn<TableItemVersion, String> versionFromColumn = new TableColumn<>(columnNames[1]);
        versionFromColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        table.getColumns().add(versionFromColumn);

        TableColumn<TableItemVersion, String> versionToColumn = new TableColumn<>(columnNames[2]);
        versionToColumn.setCellValueFactory(new PropertyValueFactory<>("filesCount"));
        table.getColumns().add(versionToColumn);

        TableColumn<TableItemVersion, String> messageColumn = new TableColumn<>(columnNames[3]);
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("totalSize"));
        table.getColumns().add(messageColumn);

        checkoutButton = new Button("Checkout");
        checkoutButton.setDisable(true);

        TableView.TableViewSelectionModel<TableItemVersion> selectionModel = table.getSelectionModel();
        selectionModel.selectedItemProperty().addListener(new ChangeListener<TableItemVersion>() {
            @Override
            public void changed(ObservableValue<? extends TableItemVersion> val, TableItemVersion oldVal, TableItemVersion newVal) {
                if (newVal != null) {
                    checkoutVersion = newVal.getVersion();
                    checkoutButton.setDisable(false);
                }
            }
        });

        historyTabContent = new HBox();
        HBox.setHgrow(table, Priority.ALWAYS);
        historyTabContent.setAlignment(Pos.CENTER_LEFT);
        historyTabContent.setPadding(new Insets(5));
        historyTabContent.getChildren().addAll(table, checkoutButton);
    }

    private void setupAdminTabUi() {
        Label oldProjectPathLabel = new Label("Path to old version:");
        oldProjectPathLabel.setPrefSize(135, 25);
        oldProjectRemotePathField = new TextField(oldProjectPath.toString());
        oldProjectRemotePathField.setEditable(true);
        chooseOldRemoteProjectButton = new Button("browse");
        chooseOldRemoteProjectButton.setPrefSize(70, 0);

        AnchorPane oldProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(oldProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(oldProjectRemotePathField, 5d + oldProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(oldProjectRemotePathField, 5d + chooseOldRemoteProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseOldRemoteProjectButton, 5d);
        oldProjectPathPanel.getChildren().addAll(oldProjectPathLabel, oldProjectRemotePathField, chooseOldRemoteProjectButton);

        Label newProjectPathLabel = new Label("Path to new version:");
        newProjectPathLabel.setPrefSize(135, 25);
        newProjectRemotePathField = new TextField(newProjectPath.toString());
        newProjectRemotePathField.setEditable(true);
        chooseNewRemoteProjectButton = new Button("browse");
        chooseNewRemoteProjectButton.setPrefSize(70, 0);

        AnchorPane newProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(newProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(newProjectRemotePathField, 5d + newProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(newProjectRemotePathField, 5d + chooseNewRemoteProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseNewRemoteProjectButton, 5d);
        newProjectPathPanel.getChildren().addAll(newProjectPathLabel, newProjectRemotePathField, chooseNewRemoteProjectButton);

        remoteRememberPathsCheckbox = new CheckBox("Remember");
        remoteRememberPathsCheckbox.setSelected(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getBoolean("rememberPaths"));

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(remoteRememberPathsCheckbox);

        remoteCreatePatchButton = new Button("Create patch");
        remoteCreatePatchButton.setPrefSize(110, 0);

        activeRemoteCourgetesGenAmount = new Label("Active Courgette instances:\t" + CourgetteHandler.currentThreadsAmount());

        adminTabContent = new VBox();
        adminTabContent.setAlignment(Pos.TOP_CENTER);
        adminTabContent.setPadding(new Insets(5));
        adminTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                checkboxPanel, remoteCreatePatchButton, activeRemoteCourgetesGenAmount);
    }

    private void setupGenTabUi() {
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
        genPatchPathField = new TextField(patchFolderPath.toString());
        genPatchPathField.setEditable(true);
        genChoosePatchButton = new Button("browse");
        genChoosePatchButton.setPrefSize(70, 0);

        AnchorPane patchPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(genPatchPathLabel, 5d);
        AnchorPane.setLeftAnchor(genPatchPathField, 5d + genPatchPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(genPatchPathField, 5d + genChoosePatchButton.getPrefWidth());
        AnchorPane.setRightAnchor(genChoosePatchButton, 5d);
        patchPathPanel.getChildren().addAll(genPatchPathLabel, genPatchPathField, genChoosePatchButton);

        rememberPathsCheckbox = new CheckBox("Remember");
        rememberPathsCheckbox.setSelected(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getBoolean("rememberPaths"));

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberPathsCheckbox);

        createPatchButton = new Button("Create patch");
        createPatchButton.setPrefSize(110, 0);

        activeCourgetesGenAmount = new Label("Active Courgette instances:\t" + CourgetteHandler.currentThreadsAmount());

        VBox genTabContent = new VBox();
        genTabContent.setAlignment(Pos.TOP_CENTER);
        genTabContent.setPadding(new Insets(5));
        genTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                patchPathPanel, checkboxPanel, createPatchButton, activeCourgetesGenAmount);

        genTab = new Tab();
        genTab.setContent(genTabContent);
    }

    private void setupLoginUi(Tab tab, Button button) {
        VBox loginpanel = new VBox();
        loginpanel.setAlignment(Pos.CENTER);

        Label loginMessage = new Label("You are not logged in");
        button.setText("Login");

        loginpanel.getChildren().addAll(loginMessage, button);

        tab.setContent(loginpanel);
    }

    private void setupEvents() {
        choosePatchButton.setOnAction(e -> {
            choosePath(patchPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        genChoosePatchButton.setOnAction(e -> {
            choosePath(genPatchPathField, JFileChooser.DIRECTORIES_ONLY);
        });
        chooseProjectButton.setOnAction(e -> {
            choosePath(projectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseNewProjectButton.setOnAction(e -> {
            choosePath(newProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseOldProjectButton.setOnAction(e -> {
            choosePath(oldProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        applyPatchButton.setOnAction(e -> {
            projectPath = Paths.get(projectPathField.getText());
            patchPath = Paths.get(patchPathField.getText());
            Path tmpProjectPath = Paths.get(projectPath.getParent().toString(), "patched_tmp", projectPath.getFileName().toString());

            if (!authWindow.config.getJSONObject(RunCourgette.os).has("patchingInfo")) {
                authWindow.config.getJSONObject(RunCourgette.os).put("patchingInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("patchingInfo").put("projectPath", projectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("patchingInfo").put("patchPath", patchPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("patchingInfo").put("rememberPaths", rememberPathsCheckbox.isSelected());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("patchingInfo").put("replaceFiles", replaceFilesCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = new FileVisitor();

            ArrayList<Path> oldFiles = new ArrayList<>();
            ArrayList<Path> patchFiles = new ArrayList<>();

            try {
                Files.walkFileTree(projectPath, fileVisitor);
                oldFiles = new ArrayList<>(fileVisitor.allFiles);
                fileVisitor.allFiles.clear();

                Files.walkFileTree(patchPath, fileVisitor);
                patchFiles = new ArrayList<>(fileVisitor.allFiles);
                fileVisitor.allFiles.clear();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            Path relativePatchPath;
            Path newPath;
            Path oldPath;
            byte[] emptyData = {0};
    
            for (Path patchFile: patchFiles) {
                relativePatchPath = patchPath.relativize(patchFile);
                newPath = Paths.get(tmpProjectPath.toString(), relativePatchPath.toString().equals("") ?
                        Paths.get("..", "..", "..", tmpProjectPath.getParent().getFileName().toString(),
                                tmpProjectPath.getFileName().toString()).toString() :
                        relativePatchPath.toString().substring(0, relativePatchPath.toString().length() - "_patch".length())).normalize();
                oldPath = Paths.get(projectPath.toString(), relativePatchPath.toString().equals("") ? "" :
                        relativePatchPath.toString().substring(0, relativePatchPath.toString().length() - "_patch".length())).normalize();

                if (!oldFiles.contains(oldPath)) {
                    try {
                        Files.createFile(oldPath);
                        Files.write(oldPath, emptyData);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
    
                try {
                    Files.createDirectories(newPath.getParent());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                new CourgetteHandler().applyPatch(oldPath.toString(), newPath.toString(), patchFile.toString(), replaceFilesCheckbox.isSelected(), activeCourgetesApplyAmount);
            }
        });
        createPatchButton.setOnAction(e -> {
            oldProjectPath = Paths.get(oldProjectPathField.getText());
            newProjectPath = Paths.get(newProjectPathField.getText());
            patchFolderPath = Paths.get(genPatchPathField.getText());

            if (!authWindow.config.getJSONObject(RunCourgette.os).has("patchCreationInfo")) {
                authWindow.config.getJSONObject(RunCourgette.os).put("patchCreationInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("patchCreationInfo").put("patchPath", patchFolderPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("patchCreationInfo").put("oldProjectPath", oldProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("patchCreationInfo").put("newProjectPath", newProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("patchCreationInfo").put("rememberPaths", rememberPathsCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = new FileVisitor(newProjectPath);

            ArrayList<Path> oldFiles = new ArrayList<>();
            ArrayList<Path> newFiles = new ArrayList<>();

            fileVisitor.walkFileTree(oldProjectPath);
            oldFiles = new ArrayList<>(fileVisitor.allFiles);

            fileVisitor.walkFileTree(newProjectPath);
            newFiles = new ArrayList<>(fileVisitor.allFiles);
            
            generatePatch(oldProjectPath, newProjectPath, oldFiles, newFiles, "forward", activeCourgetesGenAmount);
            generatePatch(newProjectPath, oldProjectPath, newFiles, oldFiles, "backward", activeCourgetesGenAmount);
        });

        applyPatchLoginButton.setOnAction(e -> {
            if (authWindow.isShowing())
                authWindow.hide();
            else
                authWindow.show();
        });
        adminLoginButton.setOnAction(e -> {
            if (authWindow.isShowing())
                authWindow.hide();
            else
                authWindow.show();
        });
        historyLoginButton.setOnAction(e -> {
            if (authWindow.isShowing())
                authWindow.hide();
            else
                authWindow.show();
        });

        authWindow.btnConnect.setOnAction(e -> {
            authWindow.userLogin = authWindow.loginField.getText();
            authWindow.userPassword = authWindow.passField.getText();
            authWindow.urlApi = authWindow.urlField.getText();

            if (!authWindow.config.has("userInfo")) {
                authWindow.config.put("userInfo", new JSONObject());
            }
            authWindow.config.getJSONObject("userInfo").put("login", authWindow.userLogin);
            authWindow.config.getJSONObject("userInfo").put("pass", authWindow.userPassword);
            authWindow.config.getJSONObject("userInfo").put("url", authWindow.urlApi);
            authWindow.saveConfig();
            authWindow.updateAccessRights();
            authWindow.hide();
            
            if (authWindow.curAccess == AuthWindow.ACCESS.ADMIN) {
                Connector.setBaseUrl(authWindow.urlApi);

                setupRemoteUi();
                setupRemoteEvents();

                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("Generate")).setContent(adminTabContent);
                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("History")).setContent(historyTabContent);
                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("Patching")).setContent(applyPatchTabContent);
            }
        });
    }

    private void setupRemoteEvents() {
        checkoutButton.setOnAction(e -> {
            if (checkoutVersion != null) {
                // TODO: CHECKOUT PLACEHOLDER
                System.out.print("Checkout to version ");
                System.out.println(checkoutVersion.getVersionString());
            } else {
                System.out.println("No version selected");
            }
        });
    }

    private void generatePatch(Path oldProjectPath, Path newProjectPath, ArrayList<Path> oldFiles,
            ArrayList<Path> newFiles, String patchSubfolder, Label updatingComponent) {
        Path relativeOldPath;
        Path newPath;
        Path patchFile;
        byte[] emptyData = {0};
        for (Path oldFile: oldFiles) {

            relativeOldPath = oldProjectPath.relativize(oldFile);
            newPath = Paths.get(newProjectPath.toString(), relativeOldPath.toString()).normalize();
            patchFile = Paths.get(patchFolderPath.toString(), patchSubfolder,
                    (relativeOldPath.toString().equals("") ? oldFile.getFileName() : relativeOldPath.toString()) + "_patch").normalize();

            if (oldFile.toFile().length() <= 1 || newPath.toFile().length() <= 1) {
                continue;
            }

            try {
                Files.createDirectories(patchFile.getParent());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            new CourgetteHandler().generatePatch(oldFile.toString(), newPath.toString(), patchFile.toString(), updatingComponent);
        }

        Path relativeNewPath;
        Path oldPath;
        for (Path newFile: newFiles) {
            relativeNewPath = newProjectPath.relativize(newFile);
            oldPath = Paths.get(oldProjectPath.toString(), relativeNewPath.toString()).normalize();
            patchFile = Paths.get(patchFolderPath.toString(), patchSubfolder, relativeNewPath.toString() + "_patch").normalize();

            if (!oldFiles.contains(oldPath)) {
                try {
                    Files.createFile(oldPath);
                    Files.write(oldPath, emptyData);
                    Files.createDirectories(patchFile.getParent());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                new CourgetteHandler().generatePatch(oldPath.toString(), newFile.toString(), patchFile.toString(), updatingComponent);
            }
        }
    }

    private void choosePath(TextField field, int mode) {
        choosePath(field, mode, Paths.get(field.getText()));
    }

    private void choosePath(TextField field, int mode, Path defaultPath) {
        fileChooser = new JFileChooser();
        if (defaultPath.getParent() != null) {
            fileChooser.setCurrentDirectory(defaultPath.getParent().toFile());
        }
        fileChooser.setFileSelectionMode(mode);
        int option = fileChooser.showOpenDialog(null);
        if(option == JFileChooser.APPROVE_OPTION){
           File file = fileChooser.getSelectedFile();
           field.setText(file.getAbsolutePath());
        }
    }
}
