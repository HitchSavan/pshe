package user_client.utils;

import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class SaveResponse {
    public static void save(JSONObject response) throws JSONException, IOException {
        FileOutputStream jsonOutputStream;
        jsonOutputStream = new FileOutputStream("response.json");
        jsonOutputStream.write(response.toString(4).getBytes());
        jsonOutputStream.close();
    }
}
