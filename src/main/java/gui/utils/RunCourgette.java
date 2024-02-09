package gui.utils;

import java.io.IOException;

public class RunCourgette extends Thread {

    static String[] courgetteArgs = null;

    public static void runExec() throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();

        System.out.println("OS name\t -> " + System.getProperty("os.name"));
        System.out.println("OS version\t -> " + System.getProperty("os.version"));
        System.out.println("OS Architecture\t -> " + System.getProperty("os.arch"));
        System.out.println();

        UnpackResources.deleteDirectory("tmp");
        if (os.contains("windows")) {
            UnpackResources.unpackResources("win");
            Process courgette = RunExecutable.runExec("tmp/win/courgette.exe", courgetteArgs);
        } else if (os.contains("linux")) {
            UnpackResources.unpackResources("linux");
            Process courgette = RunExecutable.runExec("tmp/linux/courgette", courgetteArgs);
        }
    }
    
    public void run(String[] args) {
        courgetteArgs = args;
        run();
    }

    @Override
    public void run() {
        try {
            runExec();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
