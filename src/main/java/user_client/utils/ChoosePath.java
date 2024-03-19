package user_client.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFileChooser;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
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
        invokingButton.setDisable(true);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(invokingButton.getText());
        if (!field.getText().isEmpty()) {
            directoryChooser.setInitialDirectory(Paths.get(field.getText()).getParent().toFile());
        }
        File directory = directoryChooser.showDialog(primaryStage);
        if (directory != null) {
            field.setText(directory.toString());
        }
        invokingButton.setDisable(false);
    }
    public static void chooseFile(Button invokingButton, TextField field, Stage primaryStage) {
        invokingButton.setDisable(true);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(invokingButton.getText());
        if (!field.getText().isEmpty()) {
            fileChooser.setInitialDirectory(Paths.get(field.getText()).getParent().toFile());
        }
        File directory = fileChooser.showOpenDialog(primaryStage);
        if (directory != null) {
            field.setText(directory.toString());
        }
        invokingButton.setDisable(false);
    }
}
