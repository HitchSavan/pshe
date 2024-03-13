package user_client.utils;

import javafx.application.Platform;
import javafx.scene.control.Label;
import patcher.utils.patching_utils.Patcher;

public class CourgetteHandler extends Thread {
    private Label updatingComponent;
    private String oldFile;
    private String newPath;
    private String patchFile;
    private boolean replaceFiles;
    private boolean generate;
    private boolean redirectOutput;
    
    private static int MAX_THREADS_AMOUNT = 10;
    private static int currentThreadsAmount = 0;

    private synchronized static int currentThreadsAmount() {
        return currentThreadsAmount;
    }
    private synchronized static void increaseThreadsAmount() {
        ++currentThreadsAmount;
    }
    private synchronized static void decreaseThreadsAmount() {
        --currentThreadsAmount;
    }

    // TODO: disable exec button
    private void init(String oldFile, String newPath, String patchFile, boolean replaceFiles,
            Label updatingComponent, boolean generate, boolean redirectOutput) {
        this.updatingComponent = updatingComponent;
        this.oldFile = oldFile;
        this.newPath = newPath;
        this.patchFile = patchFile;
        this.replaceFiles = replaceFiles;
        this.redirectOutput = redirectOutput;
        this.generate = generate;
        start();
    }

    private static void updateComponent(Label updatingComponent) {
        if (updatingComponent != null) {
            Platform.runLater(() -> {
                updatingComponent.setText("Active Courgette instances:\t" + currentThreadsAmount());
            });
        }
    }
    
    public void generatePatch(String oldFile, String newPath, String patchFile,
            Label updatingComponent, boolean redirectOutput) {
        init(oldFile, newPath, patchFile, false, updatingComponent, true, redirectOutput);
    }
    
    public void applyPatch(String oldFile, String newPath, String patchFile, boolean replaceFiles,
            Label updatingComponent, boolean redirectOutput) {
        init(oldFile, newPath, patchFile, replaceFiles, updatingComponent, true, redirectOutput);
    }

    @Override
    public void run() {
        while (currentThreadsAmount() >= MAX_THREADS_AMOUNT) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                AlertWindow.showErrorWindow("Cannot handle max courgette threads amount");
                e.printStackTrace();
                return;
            }
        }
        increaseThreadsAmount();
        updateComponent(updatingComponent);

        if (generate) {
            Patcher.generatePatch(oldFile, newPath, patchFile, redirectOutput);
        } else {
            Patcher.applyPatch(oldFile, newPath, patchFile, replaceFiles, redirectOutput);
        }
        decreaseThreadsAmount();
        updateComponent(updatingComponent);
    }
}
