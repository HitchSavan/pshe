package user_client.gui.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RunCourgette extends Thread {

    String[] courgetteArgs = null;
    boolean replaceFiles;

    public static void unpackCourgette() {
        String os = System.getProperty("os.name").toLowerCase();
        UnpackResources.deleteDirectory("tmp");

        try {
            if (os.contains("windows")) {
                UnpackResources.unpackResources("/win", "tmp");
            } else if (os.contains("linux")) {
                UnpackResources.unpackResources("/linux", "tmp");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Process runExec(String[] args) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        
        System.out.println("OS name\t -> " + System.getProperty("os.name"));
        System.out.println("OS version\t -> " + System.getProperty("os.version"));
        System.out.println("OS Architecture\t -> " + System.getProperty("os.arch"));
        System.out.println();

        Process courgette = null;
        if (os.contains("windows")) {
            courgette = RunExecutable.runExec("tmp/win/courgette.exe", args);
        } else if (os.contains("linux")) {
            courgette = RunExecutable.runExec("tmp/linux/courgette", args);
        }

        if (replaceFiles) {
            courgette.waitFor();
            
            Files.delete(Paths.get(args[1]));
            Files.move(Paths.get(args[3]), Paths.get(args[1]));
            Files.delete(Paths.get(args[3]).getParent());
        }
        
        return courgette;
    }
    
    public void run(String[] args, boolean _replaceFiles) {
        courgetteArgs = args;
        replaceFiles = _replaceFiles;
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
