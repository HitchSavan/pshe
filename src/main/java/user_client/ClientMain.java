package user_client;
import java.io.IOException;

import javax.swing.JFrame;

import user_client.gui.PatcherWindow;
import user_client.utils.RunCourgette;

public class ClientMain {
    public static void main(String[] args) throws IOException {
        JFrame.setDefaultLookAndFeelDecorated(true);
        RunCourgette.unpackCourgette();
        new PatcherWindow();
    }
}
