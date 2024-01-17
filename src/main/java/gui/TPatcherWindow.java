package gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class TPatcherWindow extends JFrame {

    String windowName;

    JPanel mainControPanel;

    Button patchButton;

    public TPatcherWindow() {
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
        patchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        });
    }
}
