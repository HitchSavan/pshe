package gui.utils;

import java.io.IOException;

public class RunCourgette {
    public static void run() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        System.out.println("OS name\t -> " + System.getProperty("os.name"));
        System.out.println("OS version\t -> " + System.getProperty("os.version"));
        System.out.println("OS Architecture\t -> " + System.getProperty("os.arch"));

        UnpackResources.deleteDirectory("tmp");
        if (os.contains("windows")) {
            UnpackResources.unpackResources("windows");
            Process courgette = RunExecutable.run("tmp/windows/courgette.exe");
        }
    }
}
