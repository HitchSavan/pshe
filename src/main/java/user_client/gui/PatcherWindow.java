package user_client.gui;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import patcher.utils.files_utils.FileVisitor;
import patcher.utils.files_utils.UnpackResources;
import patcher.utils.patching_utils.RunCourgette;
import patcher.remote_api.endpoints.PatchesEndpoint;
import patcher.remote_api.endpoints.VersionsEndpoint;
import patcher.remote_api.entities.VersionEntity;
import patcher.utils.remote_utils.Connector;
import user_client.utils.CourgetteHandler;
import user_client.utils.AlertWindow;
import user_client.utils.HistoryTableItem;

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
    Tab remoteApplyTab;
    Tab historyTab;
    Tab remoteGenTab;

    Button modeSwitchButton;
    boolean isFileMode;

    HashMap<TabPane, HashMap<String, Integer>> tabsNames = new HashMap<>();
    VBox applyPatchTabContent;
    VBox genPatchTabContent;
    HBox historyTabContent;
    
    VersionEntity checkoutVersion = null;
    VersionEntity rootVersion = null;
    Button checkoutButton;

    TextField patchPathField;
    Button choosePatchButton;

    TextField projectPathField;
    Button chooseProjectButton;

    TextField genPatchPathField;
    Button chooseGenPatchButton;

    TextField oldProjectPathField;
    Button chooseOldProjectButton;

    TextField newProjectPathField;
    Button chooseNewProjectButton;

    TextField remoteProjectPathField;
    Button chooseRemoteProjectButton;

    TextField remoteOldProjectPathField;
    Button chooseRemoteOldProjectButton;

    TextField remoteNewProjectPathField;
    Button chooseRemoteNewProjectButton;

    JFileChooser fileChooser;

    CheckBox rememberApplyPathsCheckbox;
    CheckBox replaceFilesCheckbox;
    CheckBox rememberGenPathsCheckbox;
    CheckBox remoteRememberApplyPathsCheckbox;
    CheckBox remoteReplaceFilesCheckbox;
    CheckBox remoteRememberGenPathsCheckbox;

    Button applyPatchButton;
    Button genPatchButton;
    Button remoteApplyPatchButton;
    Button remoteGenPatchButton;

    Button genPatchLoginButton;
    Button applyPatchLoginButton;
    Button historyLoginButton;

    Path projectPath;
    Path patchPath;
    Path oldProjectPath;
    Path newProjectPath;
    Path patchFolderPath;

    Path remoteProjectPath;
    Path remoteOldProjectPath;
    Path remoteNewProjectPath;

    Label activeCourgetesApplyAmount;
    Label activeCourgetesGenAmount;
    Label remoteActiveCourgetesApplyAmount;
    Label remoteActiveCourgetesGenAmount;
    Label remoteGenStatus;
    Label remoteApplyStatus;
    
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
        
        remoteApplyTab = new Tab();
        applyPatchLoginButton = new Button();
        setupLoginUi(remoteApplyTab, applyPatchLoginButton);
        historyTab = new Tab();
        historyLoginButton = new Button();
        setupLoginUi(historyTab, historyLoginButton);
        genTab = new Tab();
        genPatchLoginButton = new Button();
        setupLoginUi(genTab, genPatchLoginButton);
        
        remoteTabs = new TabPane();
        addTab(remoteTabs, "Patching", remoteApplyTab);
        addTab(remoteTabs, "History", historyTab);
        addTab(remoteTabs, "Generate", genTab);
    }

    private void setupRemoteUi() {
        setupRemoteApplyTabUi();
        setupHistoryTabUi();
        setupRemoteGenTabUi();
    }

    private void setupMainWindowUi() {
        this.primaryStage.setMinWidth(300);
        this.primaryStage.setMinHeight(defaultWindowHeight);

        this.primaryStage.setWidth(defaultWindowWidth);
        this.primaryStage.setHeight(defaultWindowHeight);

        VBox mainPane = new VBox();
        VBox.setVgrow(fileTabs, Priority.ALWAYS);
        VBox.setVgrow(remoteTabs, Priority.ALWAYS);

        modeSwitchButton = new Button("Change to remote mode");
        mainPane.setPadding(new Insets(0, 0, 5, 0));
        mainPane.setAlignment(Pos.CENTER);

        if (!authWindow.config.has("defaultFilemode")) {
            authWindow.config.put("defaultFilemode", true);
        }

        isFileMode = authWindow.config.getBoolean("defaultFilemode");
        
        mainPane.getChildren().addAll(fileTabs, modeSwitchButton);

        switchMode(mainPane, !isFileMode);

        this.primaryScene = new Scene(mainPane);
        this.primaryStage.setScene(primaryScene);

        modeSwitchButton.setOnAction(e -> {
            switchMode(mainPane, isFileMode);
        });
        
        this.primaryStage.setOnCloseRequest(e -> {
            authWindow.saveConfig();
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
                .getJSONObject("localPatchingInfo").getString("projectPath"));
        patchPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getString("patchPath"));
        rememberPaths = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchingInfo").getBoolean("rememberPaths");
        replaceFiles = authWindow.config.getJSONObject(RunCourgette.os)
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

        rememberApplyPathsCheckbox = new CheckBox("Remember");
        rememberApplyPathsCheckbox.setSelected(rememberPaths);
        replaceFilesCheckbox = new CheckBox("Replace old files");
        replaceFilesCheckbox.setSelected(replaceFiles);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberApplyPathsCheckbox, replaceFilesCheckbox);

        applyPatchButton = new Button("Patch");
        applyPatchButton.setPrefSize(60, 0);

        activeCourgetesApplyAmount = new Label("Active Courgette instances:\t0");

        VBox tabContent = new VBox();
        tabContent.setAlignment(Pos.TOP_CENTER);
        tabContent.setPadding(new Insets(5));
        tabContent.getChildren().addAll(projectPathPanel, patchPathPanel,
                checkboxPanel, applyPatchButton, activeCourgetesApplyAmount);

        applyTab = new Tab();
        applyTab.setContent(tabContent);
    }

    private void setupRemoteApplyTabUi() {
        boolean rememberPaths = false;

        remoteProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getString("projectPath"));
        rememberPaths = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").getBoolean("rememberPaths");

        Label projectPathLabel = new Label("Path to project:");
        projectPathLabel.setPrefSize(105, 25);
        remoteProjectPathField = new TextField(remoteProjectPath.toString());
        remoteProjectPathField.setEditable(true);
        chooseRemoteProjectButton = new Button("browse");
        chooseRemoteProjectButton.setPrefSize(70, 0);

        AnchorPane projectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(projectPathLabel, 5d);
        AnchorPane.setLeftAnchor(remoteProjectPathField, 5d + projectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(remoteProjectPathField, 5d + chooseRemoteProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseRemoteProjectButton, 5d);
        projectPathPanel.getChildren().addAll(projectPathLabel, remoteProjectPathField, chooseRemoteProjectButton);

        remoteRememberApplyPathsCheckbox = new CheckBox("Remember");
        remoteRememberApplyPathsCheckbox.setSelected(rememberPaths);

        remoteReplaceFilesCheckbox = new CheckBox("Replace files");
        remoteReplaceFilesCheckbox.setSelected(false);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(remoteRememberApplyPathsCheckbox, remoteReplaceFilesCheckbox);

        remoteApplyPatchButton = new Button("Patch to latest version");
        remoteApplyPatchButton.setPrefSize(150, 0);
        remoteApplyPatchButton.setDisable(true);

        remoteActiveCourgetesApplyAmount = new Label("Active Courgette instances:\t0");
        remoteApplyStatus = new Label("Status: idle");

        applyPatchTabContent = new VBox();
        applyPatchTabContent.setAlignment(Pos.TOP_CENTER);
        applyPatchTabContent.setPadding(new Insets(5));
        applyPatchTabContent.getChildren().addAll(projectPathPanel, checkboxPanel,
                remoteApplyPatchButton, remoteActiveCourgetesApplyAmount, remoteApplyStatus);
    }

    private void customiseFactory(TableColumn<HistoryTableItem, Object> columnCel) {
        columnCel.setCellFactory(column -> {
            return new TableCell<HistoryTableItem, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String strItem = item.toString();
                        setText(strItem);
                        HistoryTableItem tableItem = getTableView().getItems().get(getIndex());


                        if (tableItem.getIsRoot()) {
                            Font font = Font.font(getFont().getName(), FontWeight.BOLD, FontPosture.REGULAR, getFont().getSize());
                            setFont(font);
                        } else {
                            Font font = Font.font(getFont().getName(), FontWeight.NORMAL, FontPosture.REGULAR, getFont().getSize());
                            setFont(font);
                        }
                    }
                }
            };
        });
    }

    private void setupHistoryTabUi() {
        String[] columnNames = {"Version", "Date", "Files amount", "Total size"};

        ObservableList<HistoryTableItem> versions = FXCollections.observableArrayList();

        TableView<HistoryTableItem> table = new TableView<>(versions);

        TableColumn<HistoryTableItem, Object> versionColumn = new TableColumn<>(columnNames[0]);
        versionColumn.setCellValueFactory(new PropertyValueFactory<>("versionString"));
        customiseFactory(versionColumn);
        table.getColumns().add(versionColumn);
        versionColumn.setMinWidth(90);

        TableColumn<HistoryTableItem, Object> dateColumn = new TableColumn<>(columnNames[1]);
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        customiseFactory(dateColumn);
        table.getColumns().add(dateColumn);

        TableColumn<HistoryTableItem, Object> filesColumn = new TableColumn<>(columnNames[2]);
        filesColumn.setCellValueFactory(new PropertyValueFactory<>("filesCount"));
        customiseFactory(filesColumn);
        table.getColumns().add(filesColumn);

        TableColumn<HistoryTableItem, Object> sizeColumn = new TableColumn<>(columnNames[3]);
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("totalSize"));
        customiseFactory(sizeColumn);
        table.getColumns().add(sizeColumn);
        sizeColumn.setMinWidth(100);

        checkoutButton = new Button("Checkout");
        checkoutButton.setDisable(true);

        TableView.TableViewSelectionModel<HistoryTableItem> selectionModel = table.getSelectionModel();
        selectionModel.selectedItemProperty().addListener(new ChangeListener<HistoryTableItem>() {
            @Override
            public void changed(ObservableValue<? extends HistoryTableItem> val, HistoryTableItem oldVal, HistoryTableItem newVal) {
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

        Task<Void> task = new Task<>() {
            @Override public Void call() {
                JSONObject versionsHistory = null;
                try {
                    versionsHistory = VersionsEndpoint.getHistory();
    
                    if (versionsHistory.getBoolean("success")) {
                        versionsHistory.getJSONArray("versions").forEach(v -> {
                            if (((JSONObject)v).getBoolean("is_root")) {
                                rootVersion = new VersionEntity(((JSONObject)v).put("files", new JSONArray()));
                                versions.add(new HistoryTableItem(rootVersion));
                                remoteApplyPatchButton.setDisable(false);
                            } else {
                                versions.add(new HistoryTableItem(new VersionEntity(((JSONObject)v).put("files", new JSONArray()))));
                            }
                        });

                        table.setItems(versions);
                    }
                } catch (IOException e) {
                    AlertWindow.showErrorWindow("Cannot load history");
                    e.printStackTrace();
                }
                return null;
            }
        };

        new Thread(task).start();
    }

    private void setupRemoteGenTabUi() {

        boolean rememberPaths = false;

        remoteOldProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("oldProjectPath"));
        remoteNewProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("newProjectPath"));

        Label oldProjectPathLabel = new Label("Path to old version:");
        oldProjectPathLabel.setPrefSize(135, 25);
        remoteOldProjectPathField = new TextField(remoteOldProjectPath.toString());
        remoteOldProjectPathField.setEditable(true);
        chooseRemoteOldProjectButton = new Button("browse");
        chooseRemoteOldProjectButton.setPrefSize(70, 0);

        AnchorPane oldProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(oldProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(remoteOldProjectPathField, 5d + oldProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(remoteOldProjectPathField, 5d + chooseRemoteOldProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseRemoteOldProjectButton, 5d);
        oldProjectPathPanel.getChildren().addAll(oldProjectPathLabel, remoteOldProjectPathField, chooseRemoteOldProjectButton);

        Label newProjectPathLabel = new Label("Path to new version:");
        newProjectPathLabel.setPrefSize(135, 25);
        remoteNewProjectPathField = new TextField(remoteNewProjectPath.toString());
        remoteNewProjectPathField.setEditable(true);
        chooseRemoteNewProjectButton = new Button("browse");
        chooseRemoteNewProjectButton.setPrefSize(70, 0);

        AnchorPane newProjectPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(newProjectPathLabel, 5d);
        AnchorPane.setLeftAnchor(remoteNewProjectPathField, 5d + newProjectPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(remoteNewProjectPathField, 5d + chooseRemoteNewProjectButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseRemoteNewProjectButton, 5d);
        newProjectPathPanel.getChildren().addAll(newProjectPathLabel, remoteNewProjectPathField, chooseRemoteNewProjectButton);

        remoteRememberGenPathsCheckbox = new CheckBox("Remember");
        remoteRememberGenPathsCheckbox.setSelected(rememberPaths);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(remoteRememberGenPathsCheckbox);

        remoteGenPatchButton = new Button("Create patch");
        remoteGenPatchButton.setPrefSize(110, 0);

        remoteActiveCourgetesGenAmount = new Label("Active Courgette instances:\t0");

        genPatchTabContent = new VBox();
        genPatchTabContent.setAlignment(Pos.TOP_CENTER);
        genPatchTabContent.setPadding(new Insets(5));
        genPatchTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                checkboxPanel, remoteGenPatchButton, remoteActiveCourgetesGenAmount);
    }

    private void setupGenTabUi() {
        boolean rememberPaths = false;

        rememberPaths = authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getBoolean("rememberPaths");

        oldProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("oldProjectPath"));
        newProjectPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("localPatchCreationInfo").getString("newProjectPath"));
        patchFolderPath = Paths.get(authWindow.config.getJSONObject(RunCourgette.os)
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
        genPatchPathField = new TextField(patchFolderPath.toString());
        genPatchPathField.setEditable(true);
        chooseGenPatchButton = new Button("browse");
        chooseGenPatchButton.setPrefSize(70, 0);

        AnchorPane patchPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(genPatchPathLabel, 5d);
        AnchorPane.setLeftAnchor(genPatchPathField, 5d + genPatchPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(genPatchPathField, 5d + chooseGenPatchButton.getPrefWidth());
        AnchorPane.setRightAnchor(chooseGenPatchButton, 5d);
        patchPathPanel.getChildren().addAll(genPatchPathLabel, genPatchPathField, chooseGenPatchButton);

        rememberGenPathsCheckbox = new CheckBox("Remember");
        rememberGenPathsCheckbox.setSelected(rememberPaths);

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberGenPathsCheckbox);

        genPatchButton = new Button("Create patch");
        genPatchButton.setPrefSize(110, 0);

        activeCourgetesGenAmount = new Label("Active Courgette instances:\t0");

        VBox genTabContent = new VBox();
        genTabContent.setAlignment(Pos.TOP_CENTER);
        genTabContent.setPadding(new Insets(5));
        genTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                patchPathPanel, checkboxPanel, genPatchButton, activeCourgetesGenAmount);

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
        chooseGenPatchButton.setOnAction(e -> {
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

            if (!authWindow.config.getJSONObject(RunCourgette.os).has("localPatchingInfo")) {
                authWindow.config.getJSONObject(RunCourgette.os).put("localPatchingInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("projectPath", projectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("patchPath", patchPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("rememberPaths", rememberApplyPathsCheckbox.isSelected());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchingInfo").put("replaceFiles", replaceFilesCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = new FileVisitor();

            ArrayList<Path> oldFiles = new ArrayList<>();
            ArrayList<Path> patchFiles = new ArrayList<>();

            oldFiles = new ArrayList<>(fileVisitor.walkFileTree(projectPath));
            patchFiles = new ArrayList<>(fileVisitor.walkFileTree(patchPath));

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
                new CourgetteHandler().applyPatch(oldPath.toString(), newPath.toString(), patchFile.toString(),
                        replaceFilesCheckbox.isSelected(), activeCourgetesApplyAmount, false);
            }
        });
        genPatchButton.setOnAction(e -> {
            oldProjectPath = Paths.get(oldProjectPathField.getText());
            newProjectPath = Paths.get(newProjectPathField.getText());
            patchFolderPath = Paths.get(genPatchPathField.getText());

            if (!authWindow.config.getJSONObject(RunCourgette.os).has("localPatchCreationInfo")) {
                authWindow.config.getJSONObject(RunCourgette.os).put("localPatchCreationInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("patchPath", patchFolderPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("oldProjectPath", oldProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("newProjectPath", newProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("localPatchCreationInfo").put("rememberPaths", rememberGenPathsCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = new FileVisitor();

            ArrayList<Path> oldFiles = new ArrayList<>();
            ArrayList<Path> newFiles = new ArrayList<>();

            oldFiles = new ArrayList<>(fileVisitor.walkFileTree(oldProjectPath));
            newFiles = new ArrayList<>(fileVisitor.walkFileTree(newProjectPath));
            
            generatePatch(oldProjectPath, newProjectPath, oldFiles, newFiles, "forward", activeCourgetesGenAmount);
            generatePatch(newProjectPath, oldProjectPath, newFiles, oldFiles, "backward", activeCourgetesGenAmount);
        });

        applyPatchLoginButton.setOnAction(e -> {
            if (authWindow.isShowing())
                authWindow.hide();
            else
                authWindow.show();
        });
        genPatchLoginButton.setOnAction(e -> {
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

                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("Generate")).setContent(genPatchTabContent);
                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("History")).setContent(historyTabContent);
                remoteTabs.getTabs().get(tabsNames.get(remoteTabs).get("Patching")).setContent(applyPatchTabContent);
            }
        });
    }

    private void setupRemoteEvents() {
        chooseRemoteProjectButton.setOnAction(e -> {
            choosePath(remoteProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseRemoteNewProjectButton.setOnAction(e -> {
            choosePath(remoteNewProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        chooseRemoteOldProjectButton.setOnAction(e -> {
            choosePath(remoteOldProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        checkoutButton.setOnAction(e -> {
            if (checkoutVersion != null) {
                // TODO: CHECKOUT PLACEHOLDER
                System.out.print("Checkout to version ");
                System.out.println(checkoutVersion.getVersionString());
            } else {
                System.out.println("No version selected");
            }
        });

        remoteApplyPatchButton.setOnAction(e -> {
            remoteProjectPath = Paths.get(remoteProjectPathField.getText());
            Path tmpProjectPath = Paths.get(remoteProjectPath.getParent().toString(), "patched_tmp", remoteProjectPath.getFileName().toString());
            Path tmpPatchPath = Paths.get(remoteProjectPath.getParent().toString(), "patch_tmp", remoteProjectPath.getFileName().toString());

            if (!authWindow.config.getJSONObject(RunCourgette.os).has("remotePatchingInfo")) {
                authWindow.config.getJSONObject(RunCourgette.os).put("remotePatchingInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchingInfo").put("projectPath", remoteProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchingInfo").put("rememberPaths", remoteRememberApplyPathsCheckbox.isSelected());
            authWindow.saveConfig();

            String currentVersion = null;
            if (Files.exists(Paths.get(remoteProjectPath.toString(), "config.json"))) {
                File file = new File(Paths.get(remoteProjectPath.toString(), "config.json").toString());
                String content;
                try {
                    content = new String(Files.readAllBytes(Paths.get(file.toURI())));
                    currentVersion = new JSONObject(content).getString("currentVersion");
                } catch (IOException ee) {
                    AlertWindow.showErrorWindow("Cannot open project config file");
                    ee.printStackTrace();
                    return;
                }
            }

            Map<String, String> params = new HashMap<>();
            params.put("v_from", currentVersion);
            params.put("v_to", rootVersion.getVersionString());

            Task<Void> task = new Task<>() {
                @Override public Void call() throws InterruptedException {

                    Instant start = Instant.now();
    
                    JSONObject response = null;
                    try {
                        Platform.runLater(() -> {
                            remoteApplyStatus.setText("Status: getting patch sequence from " + params.get("v_from") + " to " + params.get("v_to"));
                        });
                        response = VersionsEndpoint.getSwitch(params);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    ArrayList<Path> subfolderSequence = new ArrayList<>();

                    response.getJSONArray("files").forEach(fileItem -> {
                        JSONObject file = (JSONObject)fileItem;
                        file.getJSONArray("patches").forEach(patchItem -> {
                            JSONObject patch = (JSONObject)patchItem;
                            Map<String, String> patchParams = new HashMap<>(
                                Map.of(
                                    "v_from", patch.getString("version_from"),
                                    "v_to", patch.getString("version_to"),
                                    "file_location", file.getString("location")
                                )
                            );
                            Path patchPath = null;
                            try {
                                Path subfolderPath = Paths.get(tmpPatchPath.toString(),
                                        patchParams.get("v_from") + patchParams.get("v_to"));
                                patchPath = Paths.get(subfolderPath.toString(), file.getString("location"));
                                if (!subfolderSequence.contains(subfolderPath))
                                    subfolderSequence.add(subfolderPath);
                                Platform.runLater(() -> {
                                    remoteApplyStatus.setText("Status: downloading patch " + patchParams.get("file_location"));
                                });
                                PatchesEndpoint.getFile(patchPath, patchParams);
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        });
                    });

                    FileVisitor fileVisitor = new FileVisitor();

                    ArrayList<Path> oldFiles = new ArrayList<>();
                    ArrayList<Path> patchFiles = new ArrayList<>();

                    oldFiles = new ArrayList<>(fileVisitor.walkFileTree(remoteProjectPath));

                    Path relativePatchPath;
                    Path newPath;
                    Path oldPath;
                    byte[] emptyData = {0};

                    ArrayList<CourgetteHandler> threads = new ArrayList<>();

                    for (Path folder: subfolderSequence) {
                        patchFiles = new ArrayList<>(fileVisitor.walkFileTree(folder));

                        for (Path patchFile: patchFiles) {
                            relativePatchPath = folder.relativize(patchFile);
                            newPath = Paths.get(tmpProjectPath.toString(), relativePatchPath.toString().equals("") ?
                                    Paths.get("..", "..", "..", tmpProjectPath.getFileName().toString()).toString() :
                                    relativePatchPath.toString()).normalize();
                            oldPath = Paths.get(remoteProjectPath.toString(), relativePatchPath.toString().equals("") ? "" :
                                    relativePatchPath.toString()).normalize();

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
                            CourgetteHandler thread = new CourgetteHandler();
                            thread.applyPatch(oldPath.toString(), newPath.toString(), patchFile.toString(),
                                    remoteReplaceFilesCheckbox.isSelected(), remoteActiveCourgetesApplyAmount, false);
                            threads.add(thread);
                        }
                        Platform.runLater(() -> {
                            remoteApplyStatus.setText("Status: patching " + folder.toString());
                        });

                        for (CourgetteHandler thread: threads) {
                            thread.join();
                        }

                        UnpackResources.deleteDirectory(folder);
                    }
                    if (tmpPatchPath.getParent().endsWith("patch_tmp"))
                        UnpackResources.deleteDirectory(tmpPatchPath.getParent());

                    Instant finish = Instant.now();
                    StringBuilder str = new StringBuilder("Status: done ");
                    str.append(ChronoUnit.MINUTES.between(start, finish));
                    str.append(ChronoUnit.SECONDS.between(start, finish) - ChronoUnit.MINUTES.between(start, finish)*60);
                    str.append(" mins");

                    Platform.runLater(() -> {
                        remoteApplyStatus.setText(str.toString());
                    });

                    return null;
                }
            };
            new Thread(task).start();
        });

        remoteGenPatchButton.setOnAction(e -> {
            remoteOldProjectPath = Paths.get(remoteOldProjectPathField.getText());
            remoteNewProjectPath = Paths.get(remoteNewProjectPathField.getText());
            Path patchFolderPath = Paths.get(remoteNewProjectPath.getParent().toString(), "/tmp_patch");

            if (!authWindow.config.getJSONObject(RunCourgette.os).has("remotePatchCreationInfo")) {
                authWindow.config.getJSONObject(RunCourgette.os).put("remotePatchCreationInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchCreationInfo").put("oldProjectPath", remoteOldProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchCreationInfo").put("newProjectPath", remoteNewProjectPath.toString());
            authWindow.config.getJSONObject(RunCourgette.os)
                    .getJSONObject("remotePatchCreationInfo").put("rememberPaths", remoteRememberGenPathsCheckbox.isSelected());
            authWindow.saveConfig();

            FileVisitor fileVisitor = new FileVisitor(remoteNewProjectPath);

            ArrayList<Path> oldFiles = new ArrayList<>();
            ArrayList<Path> newFiles = new ArrayList<>();

            oldFiles = new ArrayList<>(fileVisitor.walkFileTree(remoteOldProjectPath));
            newFiles = new ArrayList<>(fileVisitor.walkFileTree(remoteNewProjectPath));
            
            generatePatch(patchFolderPath, remoteOldProjectPath, remoteNewProjectPath, oldFiles, newFiles, "forward", activeCourgetesGenAmount);
            generatePatch(patchFolderPath, remoteNewProjectPath, remoteOldProjectPath, newFiles, oldFiles, "backward", activeCourgetesGenAmount);

            // TODO: implement upload
            UnpackResources.deleteDirectory(patchFolderPath);
        });
    }

    private void generatePatch(Path patchFolderPath, Path oldProjectPath, Path newProjectPath, ArrayList<Path> oldFiles,
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
            new CourgetteHandler().generatePatch(oldFile.toString(), newPath.toString(),
                    patchFile.toString(), updatingComponent, false);
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
                new CourgetteHandler().generatePatch(oldPath.toString(), newFile.toString(),
                        patchFile.toString(),updatingComponent, false);
            }
        }
    }
    private void generatePatch(Path oldProjectPath, Path newProjectPath, ArrayList<Path> oldFiles,
            ArrayList<Path> newFiles, String patchSubfolder, Label updatingComponent) {
        generatePatch(patchFolderPath, oldProjectPath, newProjectPath, oldFiles, newFiles, patchSubfolder, updatingComponent);
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

    private void switchMode(VBox pane, boolean isFileMode) {
        if (isFileMode) {
            pane.getChildren().set(0, remoteTabs);
            modeSwitchButton.setText("Change to file mode");
            this.primaryStage.setTitle(windowName + " - REMOTE MODE");
            this.isFileMode = false;
        } else {
            pane.getChildren().set(0, fileTabs);
            modeSwitchButton.setText("Change to remote mode");
            this.primaryStage.setTitle(windowName + " - FILE MODE");
            this.isFileMode = true;
        }
        authWindow.config.put("defaultFilemode", this.isFileMode);
    }
}
