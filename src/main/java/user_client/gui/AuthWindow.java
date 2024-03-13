package user_client.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import patcher.utils.data_utils.DataEncoder;
import patcher.utils.patching_utils.RunCourgette;
import user_client.utils.AlertWindow;

public class AuthWindow extends Stage {

    public enum ACCESS {
        ADMIN,
        USER,
        GUEST,
    }
    
    Scene primaryScene;

    ACCESS curAccess = ACCESS.GUEST;
    
    String windowName;
    JSONObject config;

    String userLogin = "";
    String userPassword = "";
    String urlApi = "";

    VBox startPanel;

    Label loginLabel;
    TextField loginField;

    Label passLabel;
    TextField passField;

    Label urlLabel;
    TextField urlField;
    
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
                content = DataEncoder.toString(Files.readAllBytes(Paths.get(file.toURI())));
                config = new JSONObject(content);
            } catch (IOException e) {
                AlertWindow.showErrorWindow("Cannot open app config file");
                e.printStackTrace();
                return;
            }
        } else {
            config.put("userInfo", new JSONObject().put("login", "").put("pass", "").put("url", ""));
        }
        
        if (!config.has(RunCourgette.os)) {
            config.put(RunCourgette.os, new JSONObject());
        }
        if (!config.getJSONObject(RunCourgette.os).has("localPatchingInfo"))
            config.getJSONObject(RunCourgette.os)
                    .put("localPatchingInfo", new JSONObject()
                    .put("rememberPaths", false)
                    .put("replaceFiles", false)
                    .put("projectPath", "")
                    .put("patchPath", ""));
        if (!config.getJSONObject(RunCourgette.os).has("remotePatchingInfo"))
            config.getJSONObject(RunCourgette.os)
                    .put("remotePatchingInfo", new JSONObject()
                    .put("rememberPaths", false)
                    .put("replaceFiles", false)
                    .put("projectPath", "")
                    .put("patchPath", ""));
        if (!config.getJSONObject(RunCourgette.os).has("localPatchCreationInfo"))
            config.getJSONObject(RunCourgette.os)
                    .put("localPatchCreationInfo", new JSONObject()
                    .put("rememberPaths", false)
                    .put("patchPath", "")
                    .put("newProjectPath", "")
                    .put("oldProjectPath", ""));
        if (!config.getJSONObject(RunCourgette.os).has("remotePatchCreationInfo"))
            config.getJSONObject(RunCourgette.os)
                    .put("remotePatchCreationInfo", new JSONObject()
                    .put("rememberPaths", false)
                    .put("patchPath", "")
                    .put("newProjectPath", "")
                    .put("oldProjectPath", ""));
        saveConfig();

        userLogin = config.getJSONObject("userInfo").getString("login");
        userPassword = config.getJSONObject("userInfo").getString("pass");
        urlApi = config.getJSONObject("userInfo").getString("url");

        loginLabel = new Label("Login");
        loginLabel.setPrefSize(75, 25);
        loginField = new TextField(userLogin);
        loginField.setEditable(true);

        HBox loginPanel = new HBox();
        loginPanel.getChildren().addAll(loginLabel, loginField);

        passLabel = new Label("Password");
        passLabel.setPrefSize(75, 25);
        passField = new TextField(userPassword);
        loginField.setEditable(true);

        HBox passPanel = new HBox();
        passPanel.getChildren().addAll(passLabel, passField);

        urlLabel = new Label("URL");
        urlLabel.setPrefSize(75, 25);
        urlField = new TextField(urlApi);
        urlField.setEditable(true);

        HBox urlPanel = new HBox();
        urlPanel.getChildren().addAll(urlLabel, urlField);

        btnConnect = new Button("Connect");
        btnConnect.setPrefSize(75, 25);

        VBox startPanel = new VBox();
        startPanel.setAlignment(Pos.BOTTOM_CENTER);
        startPanel.getChildren().addAll(btnConnect);

        VBox fieldsPanel = new VBox();
        fieldsPanel.getChildren().addAll(loginPanel, passPanel, urlPanel);

        BorderPane primaryPane = new BorderPane();
        primaryPane.setPadding(new Insets(5));
        primaryPane.setTop(fieldsPanel);
        primaryPane.setBottom(startPanel);

        primaryScene = new Scene(primaryPane);

        this.setScene(primaryScene);
        this.setMinWidth(200);
        this.setMinHeight(150);
        this.setResizable(false);
        this.setTitle("client");
        this.setTitle(windowName);
    }

    private void setupEvents() {
        this.setOnCloseRequest(e -> {
            hide();
        });

        btnConnect.setOnAction(e -> {
            System.out.println("authwindow");
            userLogin = loginField.getText();
            userPassword = passField.getText();

            if (!config.has("userInfo")) {
                config.put("userInfo", new JSONObject());
            }
            config.getJSONObject("userInfo").put("login", userLogin);
            config.getJSONObject("userInfo").put("pass", userPassword);
            config.getJSONObject("userInfo").put("url", urlApi);

            saveConfig();
            updateAccessRights();
            hide();
        });
    }

    public void updateAccessRights() {
        // TODO: ADD ADMIN VERIFICATION
        curAccess = ACCESS.ADMIN;
    }

    public void saveConfig() {
        try {
            FileOutputStream jsonOutputStream;
            jsonOutputStream = new FileOutputStream("config.json");
            jsonOutputStream.write(config.toString(4).getBytes());
            jsonOutputStream.close();
        } catch (JSONException | IOException e1) {
            AlertWindow.showErrorWindow("Cannot save app config file");
            e1.printStackTrace();
            return;
        }
    }
}
