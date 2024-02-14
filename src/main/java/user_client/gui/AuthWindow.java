package user_client.gui;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthWindow extends JFrame {

    public enum ACCESS {
        ADMIN,
        USER,
        GUEST,
    }

    ACCESS curAccess = ACCESS.GUEST;
    
    String windowName;
    JSONObject config;

    String userLogin = "";
    String userPassword = "";

    JPanel startPanel;

    JLabel loginLabel;
    JTextField loginField;

    JLabel passLabel;
    JTextField passField;
    
    Button btnConnect;
    
    public AuthWindow() {
        windowName = "PSHE patcher";
        setupUi();
        setupEvents();
    }

    private void setupUi() {
        String configFilename = "config.json";
        config = new JSONObject();

        if (Files.exists(Paths.get(configFilename))) {
            File file = new File("config.json");
            String content;
            try {
                content = new String(Files.readAllBytes(Paths.get(file.toURI())));
                config = new JSONObject(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config.put("userInfo", new JSONObject().put("login", "").put("pass", ""));
            config.put("patchingInfo", new JSONObject()
                    .put("rememberPaths", false)
                    .put("replaceFiles", false)
                    .put("projectPath", "")
                    .put("patchPath", ""));
            config.put("patchCreationInfo", new JSONObject()
                    .put("rememberPaths", false)
                    .put("patchPath", "")
                    .put("newProjectPath", "")
                    .put("oldProjectPath", ""));

            try {
                FileOutputStream jsonOutputStream;
                jsonOutputStream = new FileOutputStream("config.json");
                jsonOutputStream.write(config.toString(4).getBytes());
                jsonOutputStream.close();
            } catch (JSONException | IOException e1) {
                e1.printStackTrace();
            }
        }

        userLogin = config.getJSONObject("userInfo").getString("login");
        userPassword = config.getJSONObject("userInfo").getString("pass");

        startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.Y_AXIS));

        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.X_AXIS));

        loginLabel = new JLabel("Login");
        loginLabel.setAlignmentX(JLabel.RIGHT);
        loginLabel.setPreferredSize(new Dimension(75, 5));
        loginPanel.add(loginLabel);

        loginField = new JTextField(userLogin);
        loginField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        loginField.setEditable(true);
        loginPanel.add(loginField);

        JPanel passPanel = new JPanel();
        passPanel.setLayout(new BoxLayout(passPanel, BoxLayout.X_AXIS));

        passLabel = new JLabel("Password");
        passLabel.setAlignmentX(JLabel.RIGHT);
        passLabel.setPreferredSize(new Dimension(75, 5));
        passPanel.add(passLabel);

        passField = new JTextField(userPassword);
        passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        loginField.setEditable(true);
        passPanel.add(passField);

        btnConnect = new Button("Connect");
        btnConnect.setMaximumSize(new Dimension(65, 20));
        startPanel.add(btnConnect);

        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.add(loginPanel);
        fieldsPanel.add(passPanel);

        this.setMinimumSize(new Dimension(200, 150));
        this.setTitle("client");
        this.add(fieldsPanel, "North");
        this.add(startPanel, "South");
        this.setTitle(windowName);
        this.setLocationRelativeTo(null);
        // this.setVisible(true);
    }

    private void setupEvents() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userLogin = loginField.getText();
                userPassword = passField.getText();

                if (!config.has("userInfo")) {
                    config.put("userInfo", new JSONObject());
                }
                config.getJSONObject("userInfo").put("login", userLogin);
                config.getJSONObject("userInfo").put("pass", userPassword);

                try {
                    FileOutputStream jsonOutputStream;
                    jsonOutputStream = new FileOutputStream("config.json");
                    jsonOutputStream.write(config.toString(4).getBytes());
                    jsonOutputStream.close();
                } catch (JSONException | IOException e1) {
                    e1.printStackTrace();
                }

                curAccess = ACCESS.ADMIN;
                
                setVisible(false);
                // TODO: ADD ADMIN VERIFICATION
            }
        });
    }
}
