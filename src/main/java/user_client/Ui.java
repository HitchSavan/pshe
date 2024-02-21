package user_client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class Ui extends Application{
    public static void main(String[] args) {
        Application.launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Dogs application");
        primaryStage.setWidth(500);
        primaryStage.setHeight(400);

        Button button = new Button("WOF WOF ???'");

        button.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "WOF WOF WOF!!!");
            alert.showAndWait();
        });
        Scene primaryScene = new Scene(button);
        primaryStage.setScene(primaryScene);
        
        primaryStage.show();
    }
}
