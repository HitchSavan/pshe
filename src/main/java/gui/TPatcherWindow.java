package gui;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.json.JSONException;
import org.json.JSONObject;

import gui.utils.ButtonColumn;
import gui.utils.FileVisitor;
import gui.utils.RunCourgette;
import gui.utils.UnpackResources;

public class TPatcherWindow extends JFrame {

    String windowName;
    TPatcherWindow selfPointer = this;
    AuthWindow authWindow;

    JTabbedPane tabsWindow;
    JPanel mainTab;
    JPanel historyTab;
    JPanel adminTabEmpty;
    JPanel adminTab;
    HashMap<String, Integer> tabsNames = new HashMap<>();

    JLabel patchPathLabel;
    JTextField patchPathField;
    Button choosePatchButton;

    JLabel projectPathLabel;
    JTextField projectPathField;
    Button chooseProjectButton;

    JLabel adminPatchPathLabel;
    JTextField adminPatchPathField;
    Button adminChoosePatchButton;

    JLabel oldProjectPathLabel;
    JTextField oldProjectPathField;
    Button chooseOldProjectButton;

    JLabel newProjectPathLabel;
    JTextField newProjectPathField;
    Button chooseNewProjectButton;

    JFileChooser fileChooser;

    Checkbox rememberPathsCheckbox;
    Checkbox replaceFilesCheckbox;
    Checkbox rememberAdminPathsCheckbox;

    Button patchButton;
    Button createPatchButton;

    JLabel loginMessage;
    Button loginButton;

    int buttonColumnIndex = 0;
    Path projectPath;
    Path patchPath;
    Path oldProjectPath;
    Path newProjectPath;
    Path patchFolderPath;

    public TPatcherWindow() {
        windowName = "PSHE patcher";
        authWindow = new AuthWindow();

        setupUi();
        setupEvents();
    }

    private void setupUi() {
        setupMainTabUi();
        setupHistoryTabUi();
        setupAdminTabUi();
        
        tabsWindow = new JTabbedPane();
        addTab(tabsWindow, "Patching", mainTab);
        addTab(tabsWindow, "History", historyTab);
        addTab(tabsWindow, "Admin", adminTabEmpty);

        this.add(tabsWindow);

        this.setMinimumSize(new Dimension(300, 200));
        this.setTitle(windowName);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void addTab(JTabbedPane tabbedPane, String tabName, Component component) {
        tabbedPane.addTab(tabName, component);
        tabsNames.put(tabName, tabbedPane.getTabCount()-1);
    }

    private void setupMainTabUi() {
        String configFilename = "config.json";
        authWindow.config = new JSONObject();
        boolean rememberPaths = false;
        boolean replaceFiles = false;

        if (Files.exists(Paths.get(configFilename))) {
            File file = new File("config.json");
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

        projectPathLabel = new JLabel("Path to project:");
        projectPathLabel.setPreferredSize(new Dimension(90, 0));
        projectPathField = new JTextField(projectPath.toString());
        projectPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        projectPathField.setEditable(true);
        chooseProjectButton = new Button("browse");
        chooseProjectButton.setMaximumSize(new Dimension(0, 20));
        JPanel projectPathPanel = new JPanel();
        projectPathPanel.setLayout(new BoxLayout(projectPathPanel, BoxLayout.X_AXIS));
        projectPathPanel.add(projectPathLabel);
        projectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        projectPathPanel.add(projectPathField);
        projectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        projectPathPanel.add(chooseProjectButton);
        projectPathPanel.add(Box.createHorizontalGlue());

        patchPathLabel = new JLabel("Path to patch:");
        patchPathLabel.setPreferredSize(new Dimension(90, 0));
        patchPathField = new JTextField(patchPath.toString());
        patchPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        patchPathField.setEditable(true);
        choosePatchButton = new Button("browse");
        choosePatchButton.setMaximumSize(new Dimension(0, 20));
        JPanel patchPathPanel = new JPanel();
        patchPathPanel.setLayout(new BoxLayout(patchPathPanel, BoxLayout.X_AXIS));
        patchPathPanel.add(patchPathLabel);
        patchPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        patchPathPanel.add(patchPathField);
        patchPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        patchPathPanel.add(choosePatchButton);
        patchPathPanel.add(Box.createHorizontalGlue());

        rememberPathsCheckbox = new Checkbox("Remember", rememberPaths);
        replaceFilesCheckbox = new Checkbox("Replace old files", replaceFiles);

        patchButton = new Button("Patch");
        patchButton.setMaximumSize(new Dimension(50, 20));

        mainTab = new JPanel();
        mainTab.setLayout(new BoxLayout(mainTab, BoxLayout.Y_AXIS));
        mainTab.add(projectPathPanel);
        mainTab.add(Box.createRigidArea(new Dimension(5, 5)));
        mainTab.add(patchPathPanel);
        mainTab.add(Box.createVerticalGlue());
        mainTab.add(rememberPathsCheckbox);
        mainTab.add(replaceFilesCheckbox);
        mainTab.add(patchButton);
    }

    private void setupHistoryTabUi() {
        historyTab = new JPanel();
        historyTab.setLayout(new BoxLayout(historyTab, BoxLayout.PAGE_AXIS));

        // TODO: PATCH TABLE HISTORY PLACEHOLDER
        String[] columnNames = {"Patch date", "Version", "Message", ""};
        Object[][] data = {
            {"01-12-2023", "31541", "last patch", "checkout"},
            {"30-10-2023", "26451", "second patch", "checkout"},
            {"05-02-2023", "5655", "first patch", "checkout"}
        };

        buttonColumnIndex = data[0].length - 1;

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
               // set all cells non-editable, except buttons
               return column == buttonColumnIndex;
            }
        };

        JTable table = new JTable(model){         
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);

                if (colIndex == buttonColumnIndex)
                    return null;

                try {
                    tip = getValueAt(rowIndex, colIndex).toString();
                } catch (RuntimeException e1) {}

                return tip;
            }
        };

        TableColumnModel columnModel =  table.getColumnModel();
        columnModel.getColumn(0).setMinWidth(70);
        columnModel.getColumn(0).setMaxWidth(70);
        columnModel.getColumn(1).setMinWidth(60);
        columnModel.getColumn(1).setMaxWidth(60);
        columnModel.getColumn(columnModel.getColumnCount()-1).setMinWidth(90);
        columnModel.getColumn(columnModel.getColumnCount()-1).setMaxWidth(90);

        Action checkout = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JTable table = (JTable)e.getSource();
            }
        };
        
        ButtonColumn buttonColumn = new ButtonColumn(table, checkout, 3);
        buttonColumn.setMnemonic(KeyEvent.VK_ENTER);
        historyTab.add(new JScrollPane(table));
    }

    // TODO: ADD SUCCESSFUL AUTHORIZATION INTERFACE UPDATE
    private void setupAdminTabUi() {
        adminTabEmpty = new JPanel();
        adminTabEmpty.setLayout(new BoxLayout(adminTabEmpty, BoxLayout.X_AXIS));
        JPanel loginpanel = new JPanel();
        loginpanel.setLayout(new BoxLayout(loginpanel, BoxLayout.Y_AXIS));

        loginMessage = new JLabel("You are not logged in");
        loginMessage.setPreferredSize(new Dimension(150, 0));
        loginMessage.setAlignmentX(CENTER_ALIGNMENT);

        loginButton = new Button("Login");
        loginButton.setMaximumSize(new Dimension(50, 20));

        loginpanel.add(Box.createRigidArea(new Dimension(20, 20)));
        loginpanel.add(loginMessage);
        loginpanel.add(Box.createRigidArea(new Dimension(20, 20)));
        loginpanel.add(loginButton);

        adminTabEmpty.add(Box.createHorizontalGlue());
        adminTabEmpty.add(loginpanel);
        adminTabEmpty.add(Box.createHorizontalGlue());

        oldProjectPathLabel = new JLabel("Path to old version:");
        oldProjectPathLabel.setPreferredSize(new Dimension(120, 0));
        oldProjectPathField = new JTextField(oldProjectPath.toString());
        oldProjectPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        oldProjectPathField.setEditable(true);
        chooseOldProjectButton = new Button("browse");
        chooseOldProjectButton.setMaximumSize(new Dimension(0, 20));
        JPanel oldProjectPathPanel = new JPanel();
        oldProjectPathPanel.setLayout(new BoxLayout(oldProjectPathPanel, BoxLayout.X_AXIS));
        oldProjectPathPanel.add(oldProjectPathLabel);
        oldProjectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        oldProjectPathPanel.add(oldProjectPathField);
        oldProjectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        oldProjectPathPanel.add(chooseOldProjectButton);
        oldProjectPathPanel.add(Box.createHorizontalGlue());

        newProjectPathLabel = new JLabel("Path to new version:");
        newProjectPathLabel.setPreferredSize(new Dimension(120, 0));
        newProjectPathField = new JTextField(newProjectPath.toString());
        newProjectPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        newProjectPathField.setEditable(true);
        chooseNewProjectButton = new Button("browse");
        chooseNewProjectButton.setMaximumSize(new Dimension(0, 20));
        JPanel newProjectPathPanel = new JPanel();
        newProjectPathPanel.setLayout(new BoxLayout(newProjectPathPanel, BoxLayout.X_AXIS));
        newProjectPathPanel.add(newProjectPathLabel);
        newProjectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        newProjectPathPanel.add(newProjectPathField);
        newProjectPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        newProjectPathPanel.add(chooseNewProjectButton);
        newProjectPathPanel.add(Box.createHorizontalGlue());

        adminPatchPathLabel = new JLabel("Path to patch folder:");
        adminPatchPathLabel.setPreferredSize(new Dimension(120, 0));
        adminPatchPathField = new JTextField(patchFolderPath.toString());
        adminPatchPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        adminPatchPathField.setEditable(true);
        adminChoosePatchButton = new Button("browse");
        adminChoosePatchButton.setMaximumSize(new Dimension(0, 20));
        JPanel patchPathPanel = new JPanel();
        patchPathPanel.setLayout(new BoxLayout(patchPathPanel, BoxLayout.X_AXIS));
        patchPathPanel.add(adminPatchPathLabel);
        patchPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        patchPathPanel.add(adminPatchPathField);
        patchPathPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        patchPathPanel.add(adminChoosePatchButton);
        patchPathPanel.add(Box.createHorizontalGlue());

        rememberPathsCheckbox = new Checkbox("Remember",
                authWindow.config.getJSONObject("patchingInfo").getBoolean("rememberPaths"));

        createPatchButton = new Button("Create patch");
        createPatchButton.setMaximumSize(new Dimension(100, 20));

        adminTab = new JPanel();
        adminTab.setLayout(new BoxLayout(adminTab, BoxLayout.Y_AXIS));
        adminTab.add(oldProjectPathPanel);
        adminTab.add(Box.createRigidArea(new Dimension(5, 5)));
        adminTab.add(newProjectPathPanel);
        adminTab.add(Box.createRigidArea(new Dimension(5, 5)));
        adminTab.add(patchPathPanel);
        adminTab.add(Box.createVerticalGlue());
        adminTab.add(rememberPathsCheckbox);
        adminTab.add(createPatchButton);
    }

    private void setupEvents() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                UnpackResources.deleteDirectory("tmp");
                System.exit(0);
            }
        });
        choosePatchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choosePath(patchPathField, JFileChooser.FILES_AND_DIRECTORIES, patchPath);
            }
        });
        adminChoosePatchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choosePath(adminPatchPathField, JFileChooser.DIRECTORIES_ONLY, patchFolderPath);
            }
        });
        chooseProjectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choosePath(projectPathField, JFileChooser.FILES_AND_DIRECTORIES, projectPath);
            }
        });
        chooseNewProjectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choosePath(newProjectPathField, JFileChooser.FILES_AND_DIRECTORIES, newProjectPath);
            }
        });
        chooseOldProjectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choosePath(oldProjectPathField, JFileChooser.FILES_AND_DIRECTORIES, oldProjectPath);
            }
        });
        patchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                projectPath = Paths.get(projectPathField.getText());
                patchPath = Paths.get(patchPathField.getText());
                Path tmpProjectPath = Paths.get(projectPath.getParent().getParent().toString(), "tmp", projectPath.getFileName().toString());

                if (!authWindow.config.has("patchingInfo")) {
                    authWindow.config.put("patchingInfo", new JSONObject());
                }
                authWindow.config.getJSONObject("patchingInfo").put("projectPath", projectPath.toString());
                authWindow.config.getJSONObject("patchingInfo").put("patchPath", patchPath.toString());
                authWindow.config.getJSONObject("patchingInfo").put("rememberPaths", rememberPathsCheckbox.getState());
                authWindow.config.getJSONObject("patchingInfo").put("replaceFiles", replaceFilesCheckbox.getState());

                try {
                    FileOutputStream jsonOutputStream;
                    jsonOutputStream = new FileOutputStream("config.json");
                    jsonOutputStream.write(authWindow.config.toString(4).getBytes());
                    jsonOutputStream.close();
                } catch (JSONException | IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    Files.createDirectories(tmpProjectPath.getParent());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                RunCourgette courgetteInstance = new RunCourgette();
                // TODO: USE RECURSIVE FILE ITERATION FOR PATCHING FOLDER (PROJECT)
                String[] args = {"-apply", projectPath.toString(), patchPath.toString(), tmpProjectPath.toString()};
                for (int i = 0; i < args.length; ++i) {
                    System.out.print(args[i]);
                    System.out.print("\t");
                }
                System.out.println();
                courgetteInstance.setDaemon(true);
                courgetteInstance.run(args, replaceFilesCheckbox.getState());
            }
        });
        createPatchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                oldProjectPath = Paths.get(oldProjectPathField.getText());
                newProjectPath = Paths.get(newProjectPathField.getText());
                patchFolderPath = Paths.get(adminPatchPathField.getText());

                if (!authWindow.config.has("patchCreationInfo")) {
                    authWindow.config.put("patchCreationInfo", new JSONObject());
                }
                authWindow.config.getJSONObject("patchCreationInfo").put("patchPath", patchFolderPath.toString());
                authWindow.config.getJSONObject("patchCreationInfo").put("oldProjectPath", oldProjectPath.toString());
                authWindow.config.getJSONObject("patchCreationInfo").put("newProjectPath", newProjectPath.toString());
                authWindow.config.getJSONObject("patchCreationInfo").put("rememberPaths", rememberPathsCheckbox.getState());

                try {
                    FileOutputStream jsonOutputStream;
                    jsonOutputStream = new FileOutputStream("config.json");
                    jsonOutputStream.write(authWindow.config.toString(4).getBytes());
                    jsonOutputStream.close();
                } catch (JSONException | IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    Files.createDirectories(patchFolderPath);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                FileVisitor fileVisitor = new FileVisitor();

                ArrayList<Path> oldFiles = new ArrayList<>();
                ArrayList<Path> newFiles = new ArrayList<>();

                try {
                    Files.walkFileTree(oldProjectPath, fileVisitor);
                    oldFiles = new ArrayList<>(fileVisitor.allFiles);

                    Files.walkFileTree(newProjectPath, fileVisitor);
                    newFiles = new ArrayList<>(fileVisitor.allFiles);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                
                Path relativeOldPath;
                Path newPath;
                Path patchFile;
                byte[] emptyData = {0};

                for (Path oldFile: oldFiles) {

                    relativeOldPath = oldProjectPath.relativize(oldFile);
                    newPath = Paths.get(newProjectPath.toString(), relativeOldPath.toString()).normalize();
                    patchFile = Paths.get(patchFolderPath.toString(), relativeOldPath.toString()).normalize();

                    if (!newFiles.contains(newPath)) {
                        try {
                            Files.createFile(newPath);
                            Files.write(newPath, emptyData);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }

                    try {
                        Files.createDirectories(patchFile.getParent());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    RunCourgette courgetteInstance = new RunCourgette();
                    String[] args = {"-gen", oldFile.toString(), newPath.toString(),
                            Paths.get(patchFolderPath.toString(), relativeOldPath.toString()).toString() + "_patch"};
                    for (int k = 0; k < args.length; ++k) {
                        System.out.print(args[k]);
                        System.out.print("\t");
                    }
                    System.out.println();
                    courgetteInstance.run(args, false);
                }

                Path relativeNewPath;
                Path oldPath;
                for (Path newFile: newFiles) {
                    
                    relativeNewPath = newProjectPath.relativize(newFile);
                    oldPath = Paths.get(oldProjectPath.toString(), relativeNewPath.toString()).normalize();
                    patchFile = Paths.get(patchFolderPath.toString(), relativeNewPath.toString()).normalize();

                    if (!oldFiles.contains(oldPath)) {
                        try {
                            Files.createFile(oldPath);
                            Files.write(oldPath, emptyData);
                            Files.createDirectories(patchFile.getParent());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
    
                        RunCourgette courgetteInstance = new RunCourgette();
                        String[] args = {"-gen", oldPath.toString(), newFile.toString(),
                                Paths.get(patchFolderPath.toString(), relativeNewPath.toString()).toString() + "_patch"};
                        for (int k = 0; k < args.length; ++k) {
                            System.out.print(args[k]);
                            System.out.print("\t");
                        }
                        System.out.println();
                        courgetteInstance.run(args, false);
                    }
                }
            }
        });
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                authWindow.setVisible(!authWindow.isVisible());
            }
        });

        authWindow.btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (authWindow.curAccess == AuthWindow.ACCESS.ADMIN) {
                    tabsWindow.setComponentAt(tabsNames.get("Admin"), adminTab);
                }
            }
        });
    }

    private void choosePath(JTextField field, int mode, Path defaultPath) {
        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(defaultPath.getParent().toFile());
        fileChooser.setFileSelectionMode(mode);
        int option = fileChooser.showOpenDialog(selfPointer);
        if(option == JFileChooser.APPROVE_OPTION){
           File file = fileChooser.getSelectedFile();
           field.setText(file.getAbsolutePath());
        }
    }
}
