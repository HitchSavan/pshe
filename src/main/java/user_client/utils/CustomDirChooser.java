package user_client.utils;

import java.io.File;
import java.nio.file.Path;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class CustomDirChooser extends FileDirectoryChooser {
    public CustomDirChooser() {
        directoryChooser = new DirectoryChooser();
    }
    @Override
    public File showDialog(Stage primaryStage) {
        return directoryChooser.showDialog(primaryStage);
    }
    @Override
    public void setTitle(String str) {
        directoryChooser.setTitle(str);
    }
    @Override
    public void setInitialDirectory(Path dir) {
        directoryChooser.setInitialDirectory(dir.toFile());
    }
}
