package user_client.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFileChooser;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ChoosePath {
    public static void choosePath(Button invokingButton, TextField field, int mode) {
        invokingButton.setDisable(true);
        choosePath(field, mode, Paths.get(field.getText()));
        invokingButton.setDisable(false);
    }
    public static void choosePath(TextField field, int mode, Path defaultPath) {
        JFileChooser fileChooser = new JFileChooser();
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
    public static void chooseDirectory(Button invokingButton, TextField field, Stage primaryStage) {
        CustomDirChooser fileChooser = new CustomDirChooser();
        choose(fileChooser, invokingButton, field, primaryStage);
    }
    public static void chooseFile(Button invokingButton, TextField field, Stage primaryStage) {
        CustomFileChooser fileChooser = new CustomFileChooser();
        choose(fileChooser, invokingButton, field, primaryStage);
    }

    public static void choose(FileDirectoryChooser fileChooser, Button invokingButton, TextField field, Stage primaryStage) {
        invokingButton.setDisable(true);
        fileChooser.setTitle(invokingButton.getText());
        if (!field.getText().isEmpty()) {
            fileChooser.setInitialDirectory(Paths.get(field.getText()).getParent());
        }
        File directory = fileChooser.showDialog(primaryStage);
        if (directory != null) {
            field.setText(directory.toString());
        }
        invokingButton.setDisable(false);
    }
}
