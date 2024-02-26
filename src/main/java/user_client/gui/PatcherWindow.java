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
import patcher.patching_utils.Patcher;
import patcher.patching_utils.RunCourgette;
import patcher.patching_utils.Patch;

public class PatcherWindow extends Application {

    String windowName;
    int defaultWindowWidth = 600;
    int defaultWindowHeight = 230;

    Stage primaryStage;
    Scene primaryScene;
    AuthWindow authWindow;

    TabPane tabsWindow;
    Tab mainTab;
    Tab historyTab;
    Tab adminTab;
    HashMap<String, Integer> tabsNames = new HashMap<>();
    VBox adminTabContent;
    HBox historyTabContent;
    
    Patch checkoutPatch = null;
    Button checkoutButton;

    Label patchPathLabel;
    TextField patchPathField;
    Button choosePatchButton;

    Label projectPathLabel;
    TextField projectPathField;
    Button chooseProjectButton;

    Label adminPatchPathLabel;
    TextField adminPatchPathField;
    Button adminChoosePatchButton;

    Label oldProjectPathLabel;
    TextField oldProjectPathField;
    Button chooseOldProjectButton;

    Label newProjectPathLabel;
    TextField newProjectPathField;
    Button chooseNewProjectButton;

    JFileChooser fileChooser;

    CheckBox rememberPathsCheckbox;
    CheckBox replaceFilesCheckbox;
    CheckBox rememberAdminPathsCheckbox;

    Button applyPatchButton;
    Button createPatchButton;

    Label adminLoginMessage;
    Button adminLoginButton;
    Label historyLoginMessage;
    Button historyLoginButton;

    Path projectPath;
    Path patchPath;
    Path oldProjectPath;
    Path newProjectPath;
    Path patchFolderPath;

    Label activeCourgetesApplyAmount;
    Label activeCourgetesGenAmount;
    
    public static void runApp(String[] args) {
        System.setProperty("javafx.preloader", CustomPreloader.class.getCanonicalName());
        Application.launch(PatcherWindow.class, args);
    }

    @Override
    public void init() throws Exception {

        super.init();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        windowName = "PSHE patcher";
        authWindow = new AuthWindow();

        setupUi();
        setupEvents();
        setupMainWindowUi();
    }

    private void setupUi() {
        setupMainTabUi();
        setupHistoryTabUi();
        setupAdminTabUi();
        
        tabsWindow = new TabPane();
        addTab(tabsWindow, "Patching", mainTab);
        addTab(tabsWindow, "History", historyTab);
        addTab(tabsWindow, "Admin", adminTab);

    }

    private void setupMainWindowUi() {
        this.primaryStage.setMinWidth(300);
        this.primaryStage.setMinHeight(defaultWindowHeight);
        // this.primaryStage.setMaxHeight(defaultWindowHeight);

        this.primaryStage.setWidth(defaultWindowWidth);
        this.primaryStage.setHeight(defaultWindowHeight);
        this.primaryStage.setTitle(windowName);

        primaryScene = new Scene(tabsWindow);
        this.primaryStage.setScene(primaryScene);
        
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
        tabsNames.put(tabName, tabbedPane.getTabs().size()-1);
    }

    private void setupMainTabUi() {
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

        projectPathLabel = new Label("Path to project:");
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

        patchPathLabel = new Label("Path to patch:");
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

        activeCourgetesApplyAmount = new Label("Active Courgette instances:\t" + RunCourgette.currentThreadsAmount());

        VBox tabContent = new VBox();
        tabContent.setAlignment(Pos.TOP_CENTER);
        tabContent.setPadding(new Insets(5));
        tabContent.getChildren().addAll(projectPathPanel, patchPathPanel,
                checkboxPanel, applyPatchButton, activeCourgetesApplyAmount);

        mainTab = new Tab();
        mainTab.setContent(tabContent);
    }

    private void setupHistoryTabUi() {
        VBox loginpanel = new VBox();
        loginpanel.setAlignment(Pos.CENTER);

        historyLoginMessage = new Label("You are not logged in");
        historyLoginButton = new Button("Login");

        loginpanel.getChildren().addAll(historyLoginMessage, historyLoginButton);

        historyTab = new Tab();
        historyTab.setContent(loginpanel);

        // TODO: PATCH TABLE HISTORY PLACEHOLDER
        String[] columnNames = {"Patch date", "Version from", "Version to", "Message"};
        String[][] data = {
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
            {"01-12-2023", "31541", "31542", "last patch"},
            {"30-10-2023", "26451", "31541", "second patch"},
            {"05-02-2022", "5655", "26451", "first patch"},
        };

        ObservableList<Patch> patches = FXCollections.observableArrayList();

        for (String[] row: data) {
            patches.add(new Patch(row[0], row[1], row[2], row[3]));
        }

        TableView<Patch> table = new TableView<>(patches);

        TableColumn<Patch, String> dateColumn = new TableColumn<>(columnNames[0]);
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("patchDate"));
        table.getColumns().add(dateColumn);

        TableColumn<Patch, String> versionFromColumn = new TableColumn<>(columnNames[1]);
        versionFromColumn.setCellValueFactory(new PropertyValueFactory<>("versionFrom"));
        table.getColumns().add(versionFromColumn);

        TableColumn<Patch, String> versionToColumn = new TableColumn<>(columnNames[2]);
        versionToColumn.setCellValueFactory(new PropertyValueFactory<>("versionTo"));
        table.getColumns().add(versionToColumn);

        TableColumn<Patch, String> messageColumn = new TableColumn<>(columnNames[3]);
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        table.getColumns().add(messageColumn);

        checkoutButton = new Button("Checkout");
        checkoutButton.setDisable(true);

        TableView.TableViewSelectionModel<Patch> selectionModel = table.getSelectionModel();
        selectionModel.selectedItemProperty().addListener(new ChangeListener<Patch>() {
            @Override
            public void changed(ObservableValue<? extends Patch> val, Patch oldVal, Patch newVal) {
                if (newVal != null) {
                    checkoutPatch = newVal;
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
        VBox loginpanel = new VBox();
        loginpanel.setAlignment(Pos.CENTER);

        adminLoginMessage = new Label("You are not logged in");
        adminLoginButton = new Button("Login");

        loginpanel.getChildren().addAll(adminLoginMessage, adminLoginButton);

        adminTab = new Tab();
        adminTab.setContent(loginpanel);

        oldProjectPathLabel = new Label("Path to old version:");
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

        newProjectPathLabel = new Label("Path to new version:");
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

        adminPatchPathLabel = new Label("Path to patch folder:");
        adminPatchPathLabel.setPrefSize(135, 25);
        adminPatchPathField = new TextField(patchFolderPath.toString());
        adminPatchPathField.setEditable(true);
        adminChoosePatchButton = new Button("browse");
        adminChoosePatchButton.setPrefSize(70, 0);

        AnchorPane patchPathPanel = new AnchorPane();
        AnchorPane.setLeftAnchor(adminPatchPathLabel, 5d);
        AnchorPane.setLeftAnchor(adminPatchPathField, 5d + adminPatchPathLabel.getPrefWidth());
        AnchorPane.setRightAnchor(adminPatchPathField, 5d + adminChoosePatchButton.getPrefWidth());
        AnchorPane.setRightAnchor(adminChoosePatchButton, 5d);
        patchPathPanel.getChildren().addAll(adminPatchPathLabel, adminPatchPathField, adminChoosePatchButton);

        rememberPathsCheckbox = new CheckBox("Remember");
        rememberPathsCheckbox.setSelected(authWindow.config.getJSONObject(RunCourgette.os)
                .getJSONObject("patchingInfo").getBoolean("rememberPaths"));

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberPathsCheckbox);

        createPatchButton = new Button("Create patch");
        createPatchButton.setPrefSize(110, 0);

        activeCourgetesGenAmount = new Label("Active Courgette instances:\t" + RunCourgette.currentThreadsAmount());

        adminTabContent = new VBox();
        adminTabContent.setAlignment(Pos.TOP_CENTER);
        adminTabContent.setPadding(new Insets(5));
        adminTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                patchPathPanel, checkboxPanel, createPatchButton, activeCourgetesGenAmount);
    }

    private void setupEvents() {
        choosePatchButton.setOnAction(e -> {
            choosePath(patchPathField, JFileChooser.FILES_AND_DIRECTORIES);
        });
        adminChoosePatchButton.setOnAction(e -> {
            choosePath(adminPatchPathField, JFileChooser.DIRECTORIES_ONLY);
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
    
                Patcher.applyPatch(oldPath.toString(), newPath.toString(), patchFile.toString(), replaceFilesCheckbox.isSelected(), activeCourgetesApplyAmount);
            }
        });
        createPatchButton.setOnAction(e -> {
            oldProjectPath = Paths.get(oldProjectPathField.getText());
            newProjectPath = Paths.get(newProjectPathField.getText());
            patchFolderPath = Paths.get(adminPatchPathField.getText());

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

        checkoutButton.setOnAction(e -> {
            if (checkoutPatch != null) {
                // TODO: CHECKOUT PLACEHOLDER
                System.out.print("Checkout to version ");
                System.out.println(checkoutPatch.getVersionTo());
            } else {
                System.out.println("No version selected");
            }
        });

        authWindow.btnConnect.setOnAction(e -> {
            
            authWindow.userLogin = authWindow.loginField.getText();
            authWindow.userPassword = authWindow.passField.getText();

            if (!authWindow.config.has("userInfo")) {
                authWindow.config.put("userInfo", new JSONObject());
            }
            authWindow.config.getJSONObject("userInfo").put("login", authWindow.userLogin);
            authWindow.config.getJSONObject("userInfo").put("pass", authWindow.userPassword);

            authWindow.saveConfig();

            authWindow.curAccess = AuthWindow.ACCESS.ADMIN;
            
            authWindow.hide();
            
            if (authWindow.curAccess == AuthWindow.ACCESS.ADMIN) {
                tabsWindow.getTabs().get(tabsNames.get("Admin")).setContent(adminTabContent);
                tabsWindow.getTabs().get(tabsNames.get("History")).setContent(historyTabContent);
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

            Patcher.generatePatch(oldFile.toString(), newPath.toString(), patchFile.toString(), updatingComponent);
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

                Patcher.generatePatch(oldPath.toString(), newFile.toString(), patchFile.toString(), updatingComponent);
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
