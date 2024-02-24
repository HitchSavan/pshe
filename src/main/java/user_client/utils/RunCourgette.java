package user_client.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.application.Platform;
import javafx.scene.control.Label;
import user_client.gui.AuthWindow;

public class RunCourgette extends Thread {

    private static volatile int currentThreadsAmount = 0;
    public static int MAX_THREADS_AMOUNT = 10;

    public static int currentThreadsAmount() {
        return currentThreadsAmount;
    }

    String[] courgetteArgs = null;
    boolean replaceFiles;
    Label updatingComponent;

    public static void unpackCourgette() {
        UnpackResources.deleteDirectory("tmp");

        try {
            if (AuthWindow.os.contains("windows")) {
                UnpackResources.unpackResources("/win", "tmp");
            } else if (AuthWindow.os.contains("linux")) {
                UnpackResources.unpackResources("/linux", "tmp");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Process runExec(String[] args) throws IOException, InterruptedException {
        System.out.println("OS name\t -> " + System.getProperty("os.name"));
        System.out.println("OS version\t -> " + System.getProperty("os.version"));
        System.out.println("OS Architecture\t -> " + System.getProperty("os.arch"));
        System.out.println();

        Process courgette = null;
        if (AuthWindow.os.contains("windows")) {
            courgette = RunExecutable.runExec("tmp/win/courgette.exe", args);
        } else if (AuthWindow.os.contains("linux")) {
            courgette = RunExecutable.runExec("tmp/linux/courgette", args);
        }

        while (currentThreadsAmount >= MAX_THREADS_AMOUNT) {
            sleep(10000);
        }

        ++currentThreadsAmount;
        if (updatingComponent != null)
            Platform.runLater(() -> {
                updatingComponent.setText("Active Courgette instances:\t" + RunCourgette.currentThreadsAmount());
            });
        courgette.waitFor();

        if (replaceFiles) {
            Files.delete(Paths.get(args[1]));
            Files.move(Paths.get(args[3]), Paths.get(args[1]));
            Files.delete(Paths.get(args[3]).getParent());
        }
        --currentThreadsAmount;
        if (updatingComponent != null)
            Platform.runLater(() -> {
                updatingComponent.setText("Active Courgette instances:\t" + RunCourgette.currentThreadsAmount());
            });
        return courgette;
    }
    
    public void run(String[] args, boolean _replaceFiles, Label _updatingComponent) {
        courgetteArgs = args;
        replaceFiles = _replaceFiles;
        updatingComponent = _updatingComponent;
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
