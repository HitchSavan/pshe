package gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainWindow extends JFrame {

    String windowName;

    JPanel mainControPanel;

    Button patchButton;

    public MainWindow() {
        windowName = "PSHE patcher";
        setupUi();
        setupEvents();
    }

    private void setupUi() {
        patchButton = new Button("Patch");

        mainControPanel = new JPanel(new BorderLayout());
        mainControPanel.add(patchButton);

        this.add(mainControPanel);

        this.setMinimumSize(new Dimension(200, 200));
        this.setTitle(windowName);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void setupEvents() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}
