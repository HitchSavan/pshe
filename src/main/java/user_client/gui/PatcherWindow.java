package user_client.gui;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
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

    FileChooser fileChooser;

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
        // setupAdminTabUi();
        
        tabsWindow = new TabPane();
        addTab(tabsWindow, "Patching", mainTab);
        // addTab(tabsWindow, "History", historyTab);
        // addTab(tabsWindow, "Admin", adminTabEmpty);

        this.primaryStage.setMinWidth(300);
        this.primaryStage.setMinHeight(210);

        this.primaryStage.setWidth(600);
        this.primaryStage.setHeight(210);
        this.primaryStage.setTitle(windowName);

        primaryScene = new Scene(tabsWindow);
        this.primaryStage.setScene(primaryScene);
        this.primaryStage.show();
    }

    private void addTab(TabPane tabbedPane, String tabName, Tab newTab) {
        tabbedPane.getTabs().add(newTab);
        tabsNames.put(tabName, tabbedPane.getTabs().size()-1);
    }

    private void setupMainTabUi() {
        String configFilename = "config.json";
        authWindow.config = new JSONObject();
        boolean rememberPaths = false;
        boolean replaceFiles = false;

        if (Files.exists(Paths.get(configFilename))) {
            File file = new File(configFilename);
            String content;
            try {
                content = new String(Files.readAllBytes(Paths.get(file.toURI())));
                authWindow.config = new JSONObject(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
            projectPath = Paths.get(authWindow.config.getJSONObject("patchingInfo").getString("projectPath"));
            patchPath = Paths.get(authWindow.config.getJSONObject("patchingInfo").getString("patchPath"));
            rememberPaths = authWindow.config.getJSONObject("patchingInfo").getBoolean("rememberPaths");
            replaceFiles = authWindow.config.getJSONObject("patchingInfo").getBoolean("replaceFiles");

            oldProjectPath = Paths.get(authWindow.config.getJSONObject("patchCreationInfo").getString("oldProjectPath"));
            newProjectPath = Paths.get(authWindow.config.getJSONObject("patchCreationInfo").getString("newProjectPath"));
            patchFolderPath = Paths.get(authWindow.config.getJSONObject("patchCreationInfo").getString("patchPath"));
        }

        projectPathLabel = new Label("Path to project:");
        // projectPathLabel.setPreferredSize(new Dimension(90, 0));
        projectPathField = new TextField(projectPath.toString());
        // projectPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        projectPathField.setEditable(true);
        chooseProjectButton = new Button("browse");
        // chooseProjectButton.setMaximumSize(new Dimension(0, 20));
        HBox projectPathPanel = new HBox();
        // projectPathPanel.setLayout(new BoxLayout(projectPathPanel, BoxLayout.X_AXIS));
        projectPathPanel.getChildren().addAll(projectPathLabel, projectPathField, chooseProjectButton);
        // projectPathPanel.add(Box.createHorizontalGlue());
        projectPathPanel.setPadding(new Insets(5));

        patchPathLabel = new Label("Path to patch:");
        // patchPathLabel.setPreferredSize(new Dimension(90, 0));
        patchPathField = new TextField(patchPath.toString());
        // patchPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        patchPathField.setEditable(true);
        choosePatchButton = new Button("browse");
        // choosePatchButton.setMaximumSize(new Dimension(0, 20));
        HBox patchPathPanel = new HBox();
        // patchPathPanel.setLayout(new BoxLayout(patchPathPanel, BoxLayout.X_AXIS));
        patchPathPanel.getChildren().addAll(patchPathLabel, patchPathField, choosePatchButton);
        // patchPathPanel.add(Box.createHorizontalGlue());
        patchPathPanel.setPadding(new Insets(5));

        rememberPathsCheckbox = new CheckBox("Remember");
        rememberPathsCheckbox.setSelected(rememberPaths);
        replaceFilesCheckbox = new CheckBox("Replace old files");
        replaceFilesCheckbox.setSelected(replaceFiles);

        applyPatchButton = new Button("Patch");
        // applyPatchButton.setMaximumSize(new Dimension(50, 20));

        activeCourgetesAmount = new Label("Active Courgette instances:\t" + RunCourgette.currentThreadsAmount());
        // activeCourgetesAmount.setAlignmentX(JFrame.CENTER_ALIGNMENT);

        VBox tabContent = new VBox();
        tabContent.getChildren().addAll(projectPathPanel, patchPathPanel, rememberPathsCheckbox, replaceFilesCheckbox, applyPatchButton, activeCourgetesAmount);

        mainTab = new Tab();
        mainTab.setContent(tabContent);
        // mainTab.setLayout(new BoxLayout(mainTab, BoxLayout.Y_AXIS));
        // mainTab.add(projectPathPanel);
        // mainTab.add(Box.createRigidArea(new Dimension(5, 5)));
        // mainTab.add(patchPathPanel);
        // mainTab.add(Box.createVerticalGlue());
        // mainTab.add(rememberPathsCheckbox);
        // mainTab.add(replaceFilesCheckbox);
        // mainTab.add(applyPatchButton);
        // mainTab.add(activeCourgetesAmount);
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

    // private void setupAdminTabUi() {
    //     adminTabEmpty = new JPanel();
    //     adminTabEmpty.setLayout(new BoxLayout(adminTabEmpty, BoxLayout.X_AXIS));
    //     JPanel loginpanel = new JPanel();
    //     loginpanel.setLayout(new BoxLayout(loginpanel, BoxLayout.Y_AXIS));

    //     loginMessage = new JLabel("You are not logged in");
    //     loginMessage.setPreferredSize(new Dimension(150, 0));
    //     loginMessage.setAlignmentX(JFrame.CENTER_ALIGNMENT);

    //     loginButton = new Button("Login");
    //     loginButton.setMaximumSize(new Dimension(50, 20));

    //     loginpanel.add(Box.createRigidArea(new Dimension(20, 20)));
    //     loginpanel.add(loginMessage);
    //     loginpanel.add(Box.createRigidArea(new Dimension(20, 20)));
    //     loginpanel.add(loginButton);

    //     adminTabEmpty.add(Box.createHorizontalGlue());
    //     adminTabEmpty.add(loginpanel);
    //     adminTabEmpty.add(Box.createHorizontalGlue());

    //     oldProjectPathLabel = new JLabel("Path to old version:");
    //     oldProjectPathLabel.setPreferredSize(new Dimension(120, 0));
    //     oldProjectPathField = new JTextField(oldProjectPath.toString());
    //     oldProjectPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
    //     oldProjectPathField.setEditable(true);
    //     chooseOldProjectButton = new Button("browse");
    //     chooseOldProjectButton.setMaximumSize(new Dimension(0, 20));
    //     JPanel oldProjectPathPanel = new JPanel();
    //     oldProjectPathPanel.setLayout(new BoxLayout(oldProjectPathPanel, BoxLayout.X_AXIS));
    //     oldProjectPathPanel.add(oldProjectPathLabel);
    //     oldProjectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
    //     oldProjectPathPanel.add(oldProjectPathField);
    //     oldProjectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
    //     oldProjectPathPanel.add(chooseOldProjectButton);
    //     oldProjectPathPanel.add(Box.createHorizontalGlue());

    //     newProjectPathLabel = new JLabel("Path to new version:");
    //     newProjectPathLabel.setPreferredSize(new Dimension(120, 0));
    //     newProjectPathField = new JTextField(newProjectPath.toString());
    //     newProjectPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
    //     newProjectPathField.setEditable(true);
    //     chooseNewProjectButton = new Button("browse");
    //     chooseNewProjectButton.setMaximumSize(new Dimension(0, 20));
    //     JPanel newProjectPathPanel = new JPanel();
    //     newProjectPathPanel.setLayout(new BoxLayout(newProjectPathPanel, BoxLayout.X_AXIS));
    //     newProjectPathPanel.add(newProjectPathLabel);
    //     newProjectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
    //     newProjectPathPanel.add(newProjectPathField);
    //     newProjectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
    //     newProjectPathPanel.add(chooseNewProjectButton);
    //     newProjectPathPanel.add(Box.createHorizontalGlue());

    //     adminPatchPathLabel = new JLabel("Path to patch folder:");
    //     adminPatchPathLabel.setPreferredSize(new Dimension(120, 0));
    //     adminPatchPathField = new JTextField(patchFolderPath.toString());
    //     adminPatchPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
    //     adminPatchPathField.setEditable(true);
    //     adminChoosePatchButton = new Button("browse");
    //     adminChoosePatchButton.setMaximumSize(new Dimension(0, 20));
    //     JPanel patchPathPanel = new JPanel();
    //     patchPathPanel.setLayout(new BoxLayout(patchPathPanel, BoxLayout.X_AXIS));
    //     patchPathPanel.add(adminPatchPathLabel);
    //     patchPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
    //     patchPathPanel.add(adminPatchPathField);
    //     patchPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
    //     patchPathPanel.add(adminChoosePatchButton);
    //     patchPathPanel.add(Box.createHorizontalGlue());

    //     rememberPathsCheckbox = new Checkbox("Remember",
    //             authWindow.config.getJSONObject("patchingInfo").getBoolean("rememberPaths"));

    //     createPatchButton = new Button("Create patch");
    //     createPatchButton.setMaximumSize(new Dimension(100, 20));

    //     activeCourgetesAdminAmount = new JLabel("Active Courgette instances:\t" + RunCourgette.currentThreadsAmount());
    //     activeCourgetesAdminAmount.setAlignmentX(JFrame.CENTER_ALIGNMENT);

    //     adminTab = new JPanel();
    //     adminTab.setLayout(new BoxLayout(adminTab, BoxLayout.Y_AXIS));
    //     adminTab.add(oldProjectPathPanel);
    //     adminTab.add(Box.createRigidArea(new Dimension(5, 5)));
    //     adminTab.add(newProjectPathPanel);
    //     adminTab.add(Box.createRigidArea(new Dimension(5, 5)));
    //     adminTab.add(patchPathPanel);
    //     adminTab.add(Box.createVerticalGlue());
    //     adminTab.add(rememberPathsCheckbox);
    //     adminTab.add(createPatchButton);
    //     adminTab.add(activeCourgetesAdminAmount);
    // }

    private void setupEvents() {
        this.primaryStage.setOnCloseRequest(e -> {
                UnpackResources.deleteDirectory("tmp");
                System.exit(0);
        });
        // choosePatchButton.addActionListener(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         choosePath(patchPathField, JFileChooser.FILES_AND_DIRECTORIES);
        //     }
        // });
        // adminChoosePatchButton.addActionListener(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         choosePath(adminPatchPathField, JFileChooser.DIRECTORIES_ONLY);
        //     }
        // });
        // chooseProjectButton.addActionListener(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         choosePath(projectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        //     }
        // });
        // chooseNewProjectButton.addActionListener(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         choosePath(newProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        //     }
        // });
        // chooseOldProjectButton.addActionListener(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         choosePath(oldProjectPathField, JFileChooser.FILES_AND_DIRECTORIES);
        //     }
        // });
        // applyPatchButton.addActionListener(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         projectPath = Paths.get(projectPathField.getText());
        //         patchPath = Paths.get(patchPathField.getText());
        //         Path tmpProjectPath = Paths.get(projectPath.getParent().toString(), "patched_tmp", projectPath.getFileName().toString());

        //         if (!authWindow.config.has("patchingInfo")) {
        //             authWindow.config.put("patchingInfo", new JSONObject());
        //         }
        //         authWindow.config.getJSONObject("patchingInfo").put("projectPath", projectPath.toString());
        //         authWindow.config.getJSONObject("patchingInfo").put("patchPath", patchPath.toString());
        //         authWindow.config.getJSONObject("patchingInfo").put("rememberPaths", rememberPathsCheckbox.getState());
        //         authWindow.config.getJSONObject("patchingInfo").put("replaceFiles", replaceFilesCheckbox.getState());

        //         try {
        //             FileOutputStream jsonOutputStream;
        //             jsonOutputStream = new FileOutputStream("config.json");
        //             jsonOutputStream.write(authWindow.config.toString(4).getBytes());
        //             jsonOutputStream.close();
        //         } catch (JSONException | IOException e1) {
        //             e1.printStackTrace();
        //         }

        //         FileVisitor fileVisitor = new FileVisitor();

        //         ArrayList<Path> oldFiles = new ArrayList<>();
        //         ArrayList<Path> patchFiles = new ArrayList<>();

        //         try {
        //             Files.walkFileTree(projectPath, fileVisitor);
        //             oldFiles = new ArrayList<>(fileVisitor.allFiles);
        //             fileVisitor.allFiles.clear();

        //             Files.walkFileTree(patchPath, fileVisitor);
        //             patchFiles = new ArrayList<>(fileVisitor.allFiles);
        //             fileVisitor.allFiles.clear();
        //         } catch (IOException e1) {
        //             e1.printStackTrace();
        //         }

        //         Path relativePatchPath;
        //         Path newPath;
        //         Path oldPath;
        //         byte[] emptyData = {0};
        
        //         for (Path patchFile: patchFiles) {
        
        //             relativePatchPath = patchPath.relativize(patchFile);
        //             newPath = Paths.get(tmpProjectPath.toString(), relativePatchPath.toString().equals("") ?
        //                     Paths.get("..", "..", "..", tmpProjectPath.getParent().getFileName().toString(),
        //                             tmpProjectPath.getFileName().toString()).toString() :
        //                     relativePatchPath.toString().substring(0, relativePatchPath.toString().length() - "_patch".length())).normalize();
        //             oldPath = Paths.get(projectPath.toString(), relativePatchPath.toString().equals("") ? "" :
        //                     relativePatchPath.toString().substring(0, relativePatchPath.toString().length() - "_patch".length())).normalize();

        //             if (!oldFiles.contains(oldPath)) {
        //                 try {
        //                     Files.createFile(oldPath);
        //                     Files.write(oldPath, emptyData);
        //                 } catch (IOException e1) {
        //                     e1.printStackTrace();
        //                 }
        //             }
        
        //             try {
        //                 Files.createDirectories(newPath.getParent());
        //             } catch (IOException e1) {
        //                 e1.printStackTrace();
        //             }
        
        //             Patcher.applyPatch(oldPath.toString(), newPath.toString(), patchFile.toString(), replaceFilesCheckbox.getState(), activeCourgetesAmount);
        //         }
        //     }
        // });
        // createPatchButton.addActionListener(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         oldProjectPath = Paths.get(oldProjectPathField.getText());
        //         newProjectPath = Paths.get(newProjectPathField.getText());
        //         patchFolderPath = Paths.get(adminPatchPathField.getText());

        //         if (!authWindow.config.has("patchCreationInfo")) {
        //             authWindow.config.put("patchCreationInfo", new JSONObject());
        //         }
        //         authWindow.config.getJSONObject("patchCreationInfo").put("patchPath", patchFolderPath.toString());
        //         authWindow.config.getJSONObject("patchCreationInfo").put("oldProjectPath", oldProjectPath.toString());
        //         authWindow.config.getJSONObject("patchCreationInfo").put("newProjectPath", newProjectPath.toString());
        //         authWindow.config.getJSONObject("patchCreationInfo").put("rememberPaths", rememberPathsCheckbox.getState());

        //         try {
        //             FileOutputStream jsonOutputStream;
        //             jsonOutputStream = new FileOutputStream("config.json");
        //             jsonOutputStream.write(authWindow.config.toString(4).getBytes());
        //             jsonOutputStream.close();
        //         } catch (JSONException | IOException e1) {
        //             e1.printStackTrace();
        //         }

        //         FileVisitor fileVisitor = new FileVisitor(newProjectPath);

        //         ArrayList<Path> oldFiles = new ArrayList<>();
        //         ArrayList<Path> newFiles = new ArrayList<>();

        //         fileVisitor.walkFileTree(oldProjectPath);
        //         oldFiles = new ArrayList<>(fileVisitor.allFiles);

        //         fileVisitor.walkFileTree(newProjectPath);
        //         newFiles = new ArrayList<>(fileVisitor.allFiles);
                
        //         generatePatch(oldProjectPath, newProjectPath, oldFiles, newFiles, "forward", activeCourgetesAdminAmount);
        //         generatePatch(newProjectPath, oldProjectPath, newFiles, oldFiles, "backward", activeCourgetesAdminAmount);
        //     }
        // });
        // loginButton.addActionListener(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         authWindow.setVisible(!authWindow.isVisible());
        //     }
        // });

        // authWindow.btnConnect.addActionListener(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         if (authWindow.curAccess == AuthWindow.ACCESS.ADMIN) {
        //             tabsWindow.setComponentAt(tabsNames.get("Admin"), adminTab);
        //         }
        //     }
        // });
    }

    // private void generatePatch(Path oldProjectPath, Path newProjectPath, ArrayList<Path> oldFiles,
    //         ArrayList<Path> newFiles, String patchSubfolder, JLabel updatingComponent) {
    //     Path relativeOldPath;
    //     Path newPath;
    //     Path patchFile;
    //     byte[] emptyData = {0};

    //     for (Path oldFile: oldFiles) {

    //         relativeOldPath = oldProjectPath.relativize(oldFile);
    //         newPath = Paths.get(newProjectPath.toString(), relativeOldPath.toString()).normalize();
    //         patchFile = Paths.get(patchFolderPath.toString(), patchSubfolder,
    //                 (relativeOldPath.toString().equals("") ? oldFile.getFileName() : relativeOldPath.toString()) + "_patch").normalize();

    //         if (oldFile.toFile().length() <= 1 || newPath.toFile().length() <= 1) {
    //             continue;
    //         }

    //         try {
    //             Files.createDirectories(patchFile.getParent());
    //         } catch (IOException e1) {
    //             e1.printStackTrace();
    //         }

    //         Patcher.generatePatch(oldFile.toString(), newPath.toString(), patchFile.toString(), updatingComponent);
    //     }

    //     Path relativeNewPath;
    //     Path oldPath;
    //     for (Path newFile: newFiles) {
            
    //         relativeNewPath = newProjectPath.relativize(newFile);
    //         oldPath = Paths.get(oldProjectPath.toString(), relativeNewPath.toString()).normalize();
    //         patchFile = Paths.get(patchFolderPath.toString(), patchSubfolder, relativeNewPath.toString() + "_patch").normalize();

    //         if (!oldFiles.contains(oldPath)) {
    //             try {
    //                 Files.createFile(oldPath);
    //                 Files.write(oldPath, emptyData);
    //                 Files.createDirectories(patchFile.getParent());
    //             } catch (IOException e1) {
    //                 e1.printStackTrace();
    //             }

    //             Patcher.generatePatch(oldPath.toString(), newFile.toString(), patchFile.toString(), updatingComponent);
    //         }
    //     }
    // }

    // private void choosePath(JTextField field, int mode) {
    //     choosePath(field, mode, Paths.get(field.getText()));
    // }

    // private void choosePath(JTextField field, int mode, Path defaultPath) {
    //     fileChooser = new JFileChooser();
    //     if (defaultPath.getParent() != null) {
    //         fileChooser.setCurrentDirectory(defaultPath.getParent().toFile());
    //     }
    //     fileChooser.setFileSelectionMode(mode);
    //     int option = fileChooser.showOpenDialog(adminTab);
    //     if(option == JFileChooser.APPROVE_OPTION){
    //        File file = fileChooser.getSelectedFile();
    //        field.setText(file.getAbsolutePath());
    //     }
    // }
}
