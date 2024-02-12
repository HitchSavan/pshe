package gui.utils;

import java.io.IOException;

public class RunCourgette extends Thread {

    static String[] courgetteArgs = null;

    public Process runExec(String[] args) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();

        System.out.println("OS name\t -> " + System.getProperty("os.name"));
        System.out.println("OS version\t -> " + System.getProperty("os.version"));
        System.out.println("OS Architecture\t -> " + System.getProperty("os.arch"));
        System.out.println();

        UnpackResources.deleteDirectory("tmp");
        Process courgette = null;
        if (os.contains("windows")) {
            UnpackResources.unpackResources("win");
            courgette = RunExecutable.runExec("tmp/win/courgette.exe", args);
        } else if (os.contains("linux")) {
            UnpackResources.unpackResources("linux");
            courgette = RunExecutable.runExec("tmp/linux/courgette", args);
        }

        return courgette;
    }
    
    public void run(String[] args) {
        courgetteArgs = args;
        start();
    }

    @Override
    public void run() {
        try {
            runExec(courgetteArgs);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
