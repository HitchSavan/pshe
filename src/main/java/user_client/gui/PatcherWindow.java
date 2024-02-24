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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import user_client.utils.FileVisitor;
import user_client.utils.Patcher;
import user_client.utils.RunCourgette;
import user_client.utils.UnpackResources;

public class PatcherWindow extends Application {

    String windowName;
    Stage primaryStage;
    Scene primaryScene;
    AuthWindow authWindow;

    TabPane tabsWindow;
    Tab mainTab;
    Tab historyTab;
    Tab adminTabEmpty;
    Tab adminTab;
    HashMap<String, Integer> tabsNames = new HashMap<>();
    VBox adminTabContent;

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

    Label loginMessage;
    Button loginButton;

    int buttonColumnIndex = 0;
    Path projectPath;
    Path patchPath;
    Path oldProjectPath;
    Path newProjectPath;
    Path patchFolderPath;

    Label activeCourgetesAmount;
    Label activeCourgetesAdminAmount;
    
    public static void main(String[] args) {
        Application.launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        windowName = "PSHE patcher";
        authWindow = new AuthWindow();

        setupUi();
        setupEvents();
    }

    private void setupUi() {
        setupMainTabUi();
        // setupHistoryTabUi();
        setupAdminTabUi();
        
        tabsWindow = new TabPane();
        addTab(tabsWindow, "Patching", mainTab);
        // addTab(tabsWindow, "History", historyTab);
        addTab(tabsWindow, "Admin", adminTabEmpty);

        this.primaryStage.setMinWidth(300);
        this.primaryStage.setMinHeight(230);
        this.primaryStage.setMaxHeight(230);

        this.primaryStage.setWidth(600);
        this.primaryStage.setTitle(windowName);

        primaryScene = new Scene(tabsWindow);
        this.primaryStage.setScene(primaryScene);
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

        projectPath = Paths.get(authWindow.config.getJSONObject(AuthWindow.os)
                .getJSONObject("patchingInfo").getString("projectPath"));
        patchPath = Paths.get(authWindow.config.getJSONObject(AuthWindow.os)
                .getJSONObject("patchingInfo").getString("patchPath"));
        rememberPaths = authWindow.config.getJSONObject(AuthWindow.os)
                .getJSONObject("patchingInfo").getBoolean("rememberPaths");
        replaceFiles = authWindow.config.getJSONObject(AuthWindow.os)
                .getJSONObject("patchingInfo").getBoolean("replaceFiles");

        oldProjectPath = Paths.get(authWindow.config.getJSONObject(AuthWindow.os)
                .getJSONObject("patchCreationInfo").getString("oldProjectPath"));
        newProjectPath = Paths.get(authWindow.config.getJSONObject(AuthWindow.os)
                .getJSONObject("patchCreationInfo").getString("newProjectPath"));
        patchFolderPath = Paths.get(authWindow.config.getJSONObject(AuthWindow.os)
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

        activeCourgetesAmount = new Label("Active Courgette instances:\t" + RunCourgette.currentThreadsAmount());

        VBox tabContent = new VBox();
        tabContent.setAlignment(Pos.TOP_CENTER);
        tabContent.setPadding(new Insets(5));
        tabContent.getChildren().addAll(projectPathPanel, patchPathPanel,
                checkboxPanel, applyPatchButton, activeCourgetesAmount);

        mainTab = new Tab();
        mainTab.setContent(tabContent);
    }

    // private void setupHistoryTabUi() {
    //     historyTab = new JPanel();
    //     historyTab.setLayout(new BoxLayout(historyTab, BoxLayout.PAGE_AXIS));

    //     // TODO: PATCH TABLE HISTORY PLACEHOLDER
    //     String[] columnNames = {"Patch date", "Version", "Message", ""};
    //     Object[][] data = {
    //         {"01-12-2023", "31541", "last patch", "checkout"},
    //         {"30-10-2023", "26451", "second patch", "checkout"},
    //         {"05-02-2023", "5655", "first patch", "checkout"}
    //     };

    //     buttonColumnIndex = data[0].length - 1;

    //     DefaultTableModel model = new DefaultTableModel(data, columnNames) {
    //         @Override
    //         public boolean isCellEditable(int row, int column) {
    //            // set all cells non-editable, except buttons
    //            return column == buttonColumnIndex;
    //         }
    //     };

    //     JTable table = new JTable(model){         
    //         public String getToolTipText(MouseEvent e) {
    //             String tip = null;
    //             java.awt.Point p = e.getPoint();
    //             int rowIndex = rowAtPoint(p);
    //             int colIndex = columnAtPoint(p);

    //             if (colIndex == buttonColumnIndex)
    //                 return null;

    //             try {
    //                 tip = getValueAt(rowIndex, colIndex).toString();
    //             } catch (RuntimeException e1) {}

    //             return tip;
    //         }
    //     };

    //     TableColumnModel columnModel =  table.getColumnModel();
    //     columnModel.getColumn(0).setMinWidth(70);
    //     columnModel.getColumn(0).setMaxWidth(70);
    //     columnModel.getColumn(1).setMinWidth(60);
    //     columnModel.getColumn(1).setMaxWidth(60);
    //     columnModel.getColumn(columnModel.getColumnCount()-1).setMinWidth(90);
    //     columnModel.getColumn(columnModel.getColumnCount()-1).setMaxWidth(90);

    //     Action checkout = new AbstractAction() {
    //         public void actionPerformed(ActionEvent e) {
    //             JTable table = (JTable)e.getSource();
    //         }
    //     };
        
    //     ButtonColumn buttonColumn = new ButtonColumn(table, checkout, 3);
    //     buttonColumn.setMnemonic(KeyEvent.VK_ENTER);
    //     historyTab.add(new JScrollPane(table));
    // }

    private void setupAdminTabUi() {
        VBox loginpanel = new VBox();
        loginpanel.setAlignment(Pos.CENTER);

        loginMessage = new Label("You are not logged in");
        loginButton = new Button("Login");

        loginpanel.getChildren().addAll(loginMessage, loginButton);

        adminTabEmpty = new Tab();
        adminTabEmpty.setContent(loginpanel);

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
        rememberPathsCheckbox.setSelected(authWindow.config.getJSONObject(AuthWindow.os)
                .getJSONObject("patchingInfo").getBoolean("rememberPaths"));

        VBox checkboxPanel = new VBox();
        checkboxPanel.setPadding(new Insets(5));
        checkboxPanel.getChildren().addAll(rememberPathsCheckbox);

        createPatchButton = new Button("Create patch");
        createPatchButton.setPrefSize(110, 0);

        activeCourgetesAdminAmount = new Label("Active Courgette instances:\t" + RunCourgette.currentThreadsAmount());

        adminTabContent = new VBox();
        adminTabContent.setAlignment(Pos.TOP_CENTER);
        adminTabContent.setPadding(new Insets(5));
        adminTabContent.getChildren().addAll(oldProjectPathPanel, newProjectPathPanel,
                patchPathPanel, checkboxPanel, createPatchButton, activeCourgetesAdminAmount);

        adminTab = new Tab();
        adminTab.setContent(adminTabContent);
    }

    private void setupEvents() {
        this.primaryStage.setOnCloseRequest(e -> {
            UnpackResources.deleteDirectory("tmp");
            System.exit(0);
        });
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

            if (!authWindow.config.getJSONObject(AuthWindow.os).has("patchingInfo")) {
                authWindow.config.getJSONObject(AuthWindow.os).put("patchingInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(AuthWindow.os)
                    .getJSONObject("patchingInfo").put("projectPath", projectPath.toString());
            authWindow.config.getJSONObject(AuthWindow.os)
                    .getJSONObject("patchingInfo").put("patchPath", patchPath.toString());
            authWindow.config.getJSONObject(AuthWindow.os)
                    .getJSONObject("patchingInfo").put("rememberPaths", rememberPathsCheckbox.isSelected());
            authWindow.config.getJSONObject(AuthWindow.os)
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
    
                Patcher.applyPatch(oldPath.toString(), newPath.toString(), patchFile.toString(), replaceFilesCheckbox.isSelected(), activeCourgetesAmount);
            }
        });
        createPatchButton.setOnAction(e -> {
            oldProjectPath = Paths.get(oldProjectPathField.getText());
            newProjectPath = Paths.get(newProjectPathField.getText());
            patchFolderPath = Paths.get(adminPatchPathField.getText());

            if (!authWindow.config.getJSONObject(AuthWindow.os).has("patchCreationInfo")) {
                authWindow.config.getJSONObject(AuthWindow.os).put("patchCreationInfo", new JSONObject());
            }
            authWindow.config.getJSONObject(AuthWindow.os)
                    .getJSONObject("patchCreationInfo").put("patchPath", patchFolderPath.toString());
            authWindow.config.getJSONObject(AuthWindow.os)
                    .getJSONObject("patchCreationInfo").put("oldProjectPath", oldProjectPath.toString());
            authWindow.config.getJSONObject(AuthWindow.os)
                    .getJSONObject("patchCreationInfo").put("newProjectPath", newProjectPath.toString());
            authWindow.config.getJSONObject(AuthWindow.os)
                    .getJSONObject("patchCreationInfo").put("rememberPaths", rememberPathsCheckbox.isSelected());

            authWindow.saveConfig();

            FileVisitor fileVisitor = new FileVisitor(newProjectPath);

            ArrayList<Path> oldFiles = new ArrayList<>();
            ArrayList<Path> newFiles = new ArrayList<>();

            fileVisitor.walkFileTree(oldProjectPath);
            oldFiles = new ArrayList<>(fileVisitor.allFiles);

            fileVisitor.walkFileTree(newProjectPath);
            newFiles = new ArrayList<>(fileVisitor.allFiles);
            
            generatePatch(oldProjectPath, newProjectPath, oldFiles, newFiles, "forward", activeCourgetesAdminAmount);
            generatePatch(newProjectPath, oldProjectPath, newFiles, oldFiles, "backward", activeCourgetesAdminAmount);
        });
        loginButton.setOnAction(e -> {
            if (authWindow.isShowing())
                authWindow.hide();
            else
                authWindow.show();
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
