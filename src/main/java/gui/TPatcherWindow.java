package gui;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import gui.utils.ButtonColumn;

public class TPatcherWindow extends JFrame {

    String windowName;

    JPanel mainTab;
    JPanel historyTab;

    JLabel patchPathLabel;
    JTextField patchPathField;
    Button choosePatchButton;

    JLabel projectPathLabel;
    JTextField projectPathField;
    Button chooseProjectButton;

    JFileChooser fileChooser;

    Button patchButton;

    public TPatcherWindow() {
        windowName = "PSHE patcher";
        setupUi();
        setupEvents();
    }

    private void setupUi() {
        setupMainTabUi();
        setupHistoryTabUi();
        
        JTabbedPane tabsWindow = new JTabbedPane();
        tabsWindow.addTab("Patching", mainTab);
        tabsWindow.addTab("History", historyTab);

        this.add(tabsWindow);

        this.setMinimumSize(new Dimension(500, 200));
        this.setTitle(windowName);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void setupMainTabUi() {
        projectPathLabel = new JLabel("Path to project:");
        projectPathLabel.setPreferredSize(new Dimension(90, 0));
        projectPathField = new JTextField();
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
        patchPathField = new JTextField();
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

        patchButton = new Button("Patch");
        patchButton.setMaximumSize(new Dimension(50, 20));

        mainTab = new JPanel();
        mainTab.setLayout(new BoxLayout(mainTab, BoxLayout.Y_AXIS));
        mainTab.add(projectPathPanel);
        mainTab.add(Box.createRigidArea(new Dimension(5, 5)));
        mainTab.add(patchPathPanel);
        mainTab.add(Box.createVerticalGlue());
        mainTab.add(patchButton);
    }

    private void setupHistoryTabUi() {
        historyTab = new JPanel();
        historyTab.setLayout(new BoxLayout(historyTab, BoxLayout.PAGE_AXIS));
        String[] columnNames = {"Patch date", "Version", "Message", ""};
        Object[][] data = {
            {"01-12-2023", "31541", "last patch", "checkout"},
            {"30-10-2023", "26451", "second patch", "checkout"},
            {"05-02-2023", "5655", "first patch", "checkout"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        JTable table = new JTable(model);

        Action checkout = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JTable table = (JTable)e.getSource();
                int modelRow = Integer.valueOf( e.getActionCommand() );
            }
        };
        
        ButtonColumn buttonColumn = new ButtonColumn(table, checkout, 3);
        buttonColumn.setMnemonic(KeyEvent.VK_ENTER);
        historyTab.add(table);
    }

    private void setupEvents() {
        patchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        });
    }
}
