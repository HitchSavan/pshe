package user_client.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.control.Label;
import lombok.Getter;
import lombok.Setter;
import patcher.utils.patching_utils.Patcher;

public class CourgetteHandler extends Thread {
    private Label updatingComponent;
    private Path oldFile;
    private Path newPath;
    private Path patchFile;
    private Path courgetteWorkingDirectory;
    private boolean replaceFiles;
    private boolean generate;
    private boolean redirectOutput;
    
    @Getter @Setter
    private static int MAX_THREADS_AMOUNT = 1;
    @Getter @Setter
    private static int MAX_ACTIVE_COURGETTES_AMOUNT = 1;
    private static int currentThreadsAmount = 0;
    private static int totalThreadsAmount = 0;
    private static int remainingFilesAmount = 0;

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
    
    public synchronized static int remainingFilesAmount() {
        return remainingFilesAmount;
    }
    public synchronized static void setRemainingFilesAmount(int _remainingFilesAmount) {
        remainingFilesAmount = _remainingFilesAmount;
    }
    public synchronized static void increaseRemainingFilesAmount() {
        ++remainingFilesAmount;
    }
    public synchronized static void decreaseRemainingFilesAmount() {
        --remainingFilesAmount;
    }

    // TODO: disable exec button
    private void init(Path oldFile, Path newPath, Path patchFile, Path courgetteWorkingDirectory, boolean replaceFiles,
            Label updatingComponent, boolean generate, boolean redirectOutput) {
        this.updatingComponent = updatingComponent;
        this.oldFile = oldFile;
        this.newPath = newPath;
        this.patchFile = patchFile;
        this.courgetteWorkingDirectory = courgetteWorkingDirectory;
        this.replaceFiles = replaceFiles;
        this.redirectOutput = redirectOutput;
        this.generate = generate;
        while (totalThreadsAmount() >= getMAX_THREADS_AMOUNT()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    AlertWindow.showErrorWindow("Cannot handle max courgette threads amount");
                });
                e.printStackTrace();
            }
        }
        increaseTotalThreadsAmount();
        start();
    }

    private static void updateComponent(Label updatingComponent) {
        if (updatingComponent != null) {
            Platform.runLater(() -> {
                updatingComponent.setText("Active Courgette instances:\t" + currentThreadsAmount()
                        + System.lineSeparator() + "Files remains:\t" + remainingFilesAmount());
            });
        }
    }
    
    public void generatePatch(Path oldFile, Path newPath, Path patchFile, Path courgetteWorkingDirectory,
            Label updatingComponent, boolean redirectOutput) {
        init(oldFile, newPath, patchFile, courgetteWorkingDirectory, false, updatingComponent, true, redirectOutput);
    }
    
    public void applyPatch(Path oldFile, Path newPath, Path patchFile, Path courgetteWorkingDirectory,
            boolean replaceFiles, Label updatingComponent, boolean redirectOutput) {
        init(oldFile, newPath, patchFile, courgetteWorkingDirectory, replaceFiles, updatingComponent, false, redirectOutput);
    }

    @Override
    public void run() {
        while (currentThreadsAmount() > MAX_ACTIVE_COURGETTES_AMOUNT) {
            try {
                sleep(10);
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    AlertWindow.showErrorWindow("Cannot handle max courgette threads amount");
                });
                e.printStackTrace();
                return;
            }
        }
        increaseThreadsAmount();
        updateComponent(updatingComponent);

        try {
            if (generate) {
                Patcher.generatePatch(oldFile, newPath, patchFile, courgetteWorkingDirectory, redirectOutput);
            } else {
                Patcher.applyPatch(oldFile, newPath, patchFile, courgetteWorkingDirectory, replaceFiles, redirectOutput);
            }
        } catch (IOException | InterruptedException e) {
            Platform.runLater(() -> {
                AlertWindow.showErrorWindow("Cannot run courgette instance");
            });
            e.printStackTrace();
        } finally {
            decreaseThreadsAmount();
            decreaseTotalThreadsAmount();
            decreaseRemainingFilesAmount();
            updateComponent(updatingComponent);
        }
    }

    public static void generatePatch(Path patchFolderPath, Path oldProjectPath, Path newProjectPath, List<Path> oldFiles,
            List<Path> newFiles, String patchSubfolder, Label updatingComponent) {
        Path relativeOldPath;
        Path newPath;
        Path patchFile;
        byte[] emptyData = {0};
        for (Path oldFile: oldFiles) {
            relativeOldPath = oldProjectPath.relativize(oldFile);
            newPath = newProjectPath.resolve(relativeOldPath).normalize();
            patchFile = patchFolderPath.resolve(patchSubfolder)
                    .resolve((relativeOldPath.toString().equals("") ? oldFile.getFileName() : relativeOldPath.toString()) + "_patch").normalize();

            if (oldFile.toFile().length() <= 1 || newPath.toFile().length() <= 1) {
                continue;
            }

            try {
                Files.createDirectories(patchFile.getParent());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            new CourgetteHandler().generatePatch(oldFile, newPath, patchFile, oldFile.getParent(), updatingComponent, false);
        }

        Path relativeNewPath;
        Path oldPath;
        for (Path newFile: newFiles) {
            relativeNewPath = newProjectPath.relativize(newFile);
            oldPath = oldProjectPath.resolve(relativeNewPath).normalize();
            patchFile = patchFolderPath.resolve(patchSubfolder).resolve(relativeNewPath.toString() + "_patch").normalize();

            if (!oldFiles.contains(oldPath)) {
                try {
                    oldPath.getParent().toFile().mkdirs();
                    Files.createFile(oldPath);
                    Files.write(oldPath, emptyData);
                    Files.createDirectories(patchFile.getParent());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                new CourgetteHandler().generatePatch(oldPath, newFile, patchFile, oldPath.getParent(), updatingComponent, false);
            }
        }
    }
}
