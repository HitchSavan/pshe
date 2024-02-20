package user_client;
import java.io.IOException;

import user_client.gui.UserWindow;
import user_client.utils.RunCourgette;

public class ClientMain {
    public static void main(String[] args) throws IOException {
        RunCourgette.unpackCourgette();
        new UserWindow();
    }
}
