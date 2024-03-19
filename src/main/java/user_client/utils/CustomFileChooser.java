package user_client.utils;

import java.io.File;
import java.nio.file.Path;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class CustomFileChooser extends FileDirectoryChooser {
    public CustomFileChooser() {
        fileChooser = new FileChooser();
    }
    @Override
    public File showDialog(Stage primaryStage) {
        return fileChooser.showOpenDialog(primaryStage);
    }
    @Override
    public void setTitle(String str) {
        fileChooser.setTitle(str);
    }
    @Override
    public void setInitialDirectory(Path dir) {
        fileChooser.setInitialDirectory(dir.toFile());
    }
}