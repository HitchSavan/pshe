package user_client.utils;

import java.io.IOException;

import javafx.application.Platform;
import javafx.scene.control.Label;
import lombok.Getter;
import lombok.Setter;
import patcher.utils.patching_utils.Patcher;

public class CourgetteHandler extends Thread {
    private Label updatingComponent;
    private String oldFile;
    private String newPath;
    private String patchFile;
    private boolean replaceFiles;
    private boolean generate;
    private boolean redirectOutput;
    
    @Getter @Setter
    private static int MAX_THREADS_AMOUNT = 30;
    @Getter @Setter
    private static int MAX_ACTIVE_COURGETTES_AMOUNT = 20;
    private static int currentThreadsAmount = 0;
    private static int totalThreadsAmount = 0;

    private synchronized static int currentThreadsAmount() {
        return currentThreadsAmount;
    }
    private synchronized static void increaseThreadsAmount() {
        ++currentThreadsAmount;
    }
    private synchronized static void decreaseThreadsAmount() {
        --currentThreadsAmount;
    }
    
    public synchronized static int totalThreadsAmount() {
        return totalThreadsAmount;
    }
    public synchronized static void setTotalThreadsAmount(int _totalThreadsAmount) {
        totalThreadsAmount = _totalThreadsAmount;
    }
    public synchronized static void increaseTotalThreadsAmount() {
        ++totalThreadsAmount;
    }
    public synchronized static void decreaseTotalThreadsAmount() {
        --totalThreadsAmount;
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
                updatingComponent.setText("Active Courgette instances:\t" + currentThreadsAmount()
                        + System.lineSeparator() + "Files remains:\t" + totalThreadsAmount());
            });
        }
    }
    
    public void generatePatch(String oldFile, String newPath, String patchFile,
            Label updatingComponent, boolean redirectOutput) {
        init(oldFile, newPath, patchFile, false, updatingComponent, true, redirectOutput);
    }
    
    public void applyPatch(String oldFile, String newPath, String patchFile, boolean replaceFiles,
            Label updatingComponent, boolean redirectOutput) {
        init(oldFile, newPath, patchFile, replaceFiles, updatingComponent, false, redirectOutput);
    }

    @Override
    public void run() {
        // increaseTotalThreadsAmount();
        while (currentThreadsAmount() >= MAX_ACTIVE_COURGETTES_AMOUNT) {
            try {
                sleep(10);
            } catch (InterruptedException e) {
                AlertWindow.showErrorWindow("Cannot handle max courgette threads amount");
                e.printStackTrace();
                return;
            }
        }
        increaseThreadsAmount();
        updateComponent(updatingComponent);

        try {
            if (generate) {
                Patcher.generatePatch(oldFile, newPath, patchFile, redirectOutput);
            } else {
                Patcher.applyPatch(oldFile, newPath, patchFile, replaceFiles, redirectOutput);
            }
        } catch (IOException | InterruptedException e) {
            AlertWindow.showErrorWindow("Cannot run courgette instance");
            e.printStackTrace();
        }
        decreaseThreadsAmount();
        // decreaseTotalThreadsAmount();
        updateComponent(updatingComponent);
    }
}
