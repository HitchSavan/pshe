package user_client.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFileChooser;

import javafx.scene.control.TextField;

public class ChoosePath {
    public static void choosePath(TextField field, int mode) {
        choosePath(field, mode, Paths.get(field.getText()));
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
}
