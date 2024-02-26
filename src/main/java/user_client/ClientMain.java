package user_client;
import java.io.IOException;

import patcher.patching_utils.RunCourgette;
import user_client.gui.PatcherWindow;

public class ClientMain {
    public static void main(String[] args) throws IOException {
        RunCourgette.unpackCourgette();
        PatcherWindow.main(args);
    }
}
