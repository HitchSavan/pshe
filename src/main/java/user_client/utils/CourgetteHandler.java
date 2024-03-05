package user_client.utils;

import javafx.application.Platform;
import javafx.scene.control.Label;
import patcher.patching_utils.Patcher;

public class CourgetteHandler extends Thread {
    private Label updatingComponent;
    private String oldFile;
    private String newPath;
    private String patchFile;
    private boolean replaceFiles;
    private boolean generate;
    
    public static int MAX_THREADS_AMOUNT = 10;
    private static int currentThreadsAmount = 0;

    public synchronized static int currentThreadsAmount() {
        return currentThreadsAmount;
    }
    public synchronized static void increaseThreadsAmount() {
        ++currentThreadsAmount;
    }
    public synchronized static void decreaseThreadsAmount() {
        --currentThreadsAmount;
    }

    private void init(String oldFile, String newPath, String patchFile, boolean replaceFiles, Label updatingComponent) {
        this.updatingComponent = updatingComponent;
        this.oldFile = oldFile;
        this.newPath = newPath;
        this.patchFile = patchFile;
        this.replaceFiles = replaceFiles;
    }

    public static void updateComponent(Label updatingComponent) {
        if (updatingComponent != null) {
            Platform.runLater(() -> {
                updatingComponent.setText("Active Courgette instances:\t" + currentThreadsAmount());
            });
        }
    }
    
    public void generatePatch(String oldFile, String newPath, String patchFile, Label updatingComponent) {
        init(oldFile, newPath, patchFile, false, updatingComponent);
        generate = true;
        start();
    }
    
    public void applyPatch(String oldFile, String newPath, String patchFile, boolean replaceFiles, Label updatingComponent) {
        init(oldFile, newPath, patchFile, replaceFiles, updatingComponent);
        generate = false;
        start();
    }

    @Override
    public void run() {
        while (currentThreadsAmount() >= MAX_THREADS_AMOUNT) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        increaseThreadsAmount();
        updateComponent(updatingComponent);
        if (generate) {
            Patcher.generatePatch(oldFile, newPath, patchFile);
        } else {
            Patcher.applyPatch(oldFile, newPath, patchFile, replaceFiles);
        }
        decreaseThreadsAmount();
        updateComponent(updatingComponent);
    }
}
