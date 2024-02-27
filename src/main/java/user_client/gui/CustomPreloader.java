package user_client.gui;

import javafx.application.Preloader;
import javafx.application.Preloader.StateChangeNotification.Type;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CustomPreloader extends Preloader {
    private Stage preloaderStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;
    
        VBox loading = new VBox(20);
        loading.setAlignment(Pos.CENTER);
        loading.getChildren().add(new ProgressBar());
        loading.getChildren().add(new Label("Launching app..."));
    
        BorderPane root = new BorderPane(loading);
        Scene scene = new Scene(root);
    
        primaryStage.setWidth(200);
        primaryStage.setHeight(150);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    @Override
    public void handleStateChangeNotification(StateChangeNotification stateChangeNotification) {
        if (stateChangeNotification.getType() == Type.BEFORE_START) {
            preloaderStage.hide();
        }
    }
}
