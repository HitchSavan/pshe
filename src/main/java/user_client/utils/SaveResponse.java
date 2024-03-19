package user_client.utils;

import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONObject;

import javafx.application.Platform;

public class SaveResponse {
    public static void save(JSONObject response) {
        try {
            FileOutputStream jsonOutputStream;
            jsonOutputStream = new FileOutputStream("response.json");
            jsonOutputStream.write(response.toString(4).getBytes());
            jsonOutputStream.close();
        } catch (IOException e) {
            Platform.runLater(() -> {
                AlertWindow.showErrorWindow("Cannot save response to file");
            });
            e.printStackTrace();
        }
    }
}
