package user_client.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import patcher.remote_api.endpoints.FilesEndpoint;
import patcher.remote_api.endpoints.PatchesEndpoint;
import patcher.remote_api.endpoints.VersionsEndpoint;
import patcher.remote_api.entities.VersionEntity;
import patcher.remote_api.entities.VersionFileEntity;
import patcher.utils.data_utils.DataEncoder;
import patcher.utils.data_utils.IntegrityChecker;
import patcher.utils.data_utils.Pair;
import patcher.utils.files_utils.Directories;
import patcher.utils.files_utils.FileVisitor;
import patcher.utils.patching_utils.RunCourgette;
import user_client.gui.AuthWindow;

public class CheckoutToVersion {
    private static Set<Button> disablingButtons = new HashSet<>();
    // Map<relativeFilePath, downloadPatchRequestParams>
    private static Map<Path, List<Map<String, String>>> patchFiles = new HashMap<>();
    // Map<relativeFilePath, Map<versionTo, patchedFileBytesizeAndChecksum>>
    private static Map<Path, Map<String, Pair<Long, String>>> patchedFilesIntegrity = new HashMap<>();
    private static Map<Path, Map<String, Thread>> patchesThreadsDownloadPerFile = new HashMap<>();
    @Getter @Setter
    private static int MAX_DOWNLOADS_AMOUNT = 20;
    private static int currentDownloadsAmount = 0;
    @Getter @Setter
    private static int MAX_PATCHING_FILES_AMOUNT = 20;
    private static int currentPatchingFilesAmount = 0;

    private static synchronized int getCurrentDownloadsAmount() {
        return currentDownloadsAmount;
    }
    private static synchronized void increaseCurrentDownloadsAmount() {
        ++currentDownloadsAmount;
    }
    private static synchronized void decreaseCurrentDownloadsAmount() {
        --currentDownloadsAmount;
    }

    private static synchronized int getCurrentPatchingFilesAmount() {
        return currentPatchingFilesAmount;
    }
    private static synchronized void increaseCurrentPatchingFilesAmount() {
        ++currentPatchingFilesAmount;
    }
    private static synchronized void decreaseCurrentPatchingFilesAmount() {
        --currentPatchingFilesAmount;
    }

    private static synchronized Map<Path, Map<String, Thread>> getPatchesThreadsDownloadPerFile() {
        return patchesThreadsDownloadPerFile;
    }
    private static synchronized void patchesThreadsDownloadPerFile_addFile(Path newFile) {
        patchesThreadsDownloadPerFile.put(newFile, new HashMap<>());
    }
    private static synchronized Thread patchesThreadsDownloadPerFile_addThread(Path file, String versionTo, Thread thread) {
        patchesThreadsDownloadPerFile.get(file).put(versionTo, thread);
        return thread;
    }

    public static void addDisablingButton(Button button) {
        disablingButtons.add(button);
    }
    public static void disableButtons() {
        disablingButtons.forEach(button -> {
            button.setDisable(true);
        });
    }
    public static void enableButtons() {
        disablingButtons.forEach(button -> {
            button.setDisable(false);
        });
    }

    // "legend":
    //     | - done,
    //     \ - done in some way, not fully

    //| get switch response
    // update progressbar based on summary files size
    //| in different threads (with max amount restriction):
    //| for each file:   set "active file version" (current project file version) to earlest;
    //|                      folders: "old" - first "from_version", "new" - first "to_version" version in patch sequence
    //\                  download patches simultaneously (with global max amount restriction)
    //|                  wait for next version sequence patch download to complete:
    //\                          check downloaded patch integrity (on failure - ???)
    //\                          run courgette patching thread on completed patch file (with global max amount restriction)
    //\                           check patched file integrity (based on switch response "new_file" info)
    //                                  (on failure - re-download root file, attempt to patch to needed version and pray)
    //|                          set "active file version" to next earliest "from_version" in patch sequence
    //|                          folders: "old" - newely patched file, "new" - next earliest "to_version" version in patch sequence
    // upon completion: check if all "version files" on remote is present in final patched files;
    //                      if something missing or integrity check failed == re-download root, patch to needen version
    //                          (needs determine if files not changed since start project version)
    //                  final integrity check, cry
    public static void checkoutToVersionByFiles(Path projectPath, boolean replaceFiles, String toVersion,
            Label statusLabel, ProgressBar progressBar, Label courgettesAmountLabel, JSONObject config, AuthWindow authWindow,
            boolean rememberPaths, String rootVersion) throws IOException {
        patchFiles.clear();
        patchedFilesIntegrity.clear();
        disableButtons();
        ProgressIndicator progressIndicator = new ProgressIndicator();
        ((VBox) courgettesAmountLabel.getScene().getRoot()).getChildren().add(progressIndicator);
        progressIndicator.setProgress(0);
        StringBuffer checkoutDump = new StringBuffer();

        Path projectParentFolder = projectPath.getParent();

        if (!config.getJSONObject(RunCourgette.os).has("remotePatchingInfo")) {
            config.getJSONObject(RunCourgette.os).put("remotePatchingInfo", new JSONObject());
        }
        config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").put("projectPath", projectPath.toString());
        config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").put("rememberPaths", rememberPaths);
        authWindow.saveConfig();

        String currentVersion = null;
        if (Files.exists(projectPath.resolve("config.json"))) {
            File file = new File(projectPath.resolve("config.json").toString());
            String content;
            try {
                content = new String(Files.readAllBytes(Paths.get(file.toURI())));
                currentVersion = new JSONObject(content).getString("currentVersion");
            } catch (IOException ee) {
                Platform.runLater(() -> {
                    AlertWindow.showErrorWindow("Cannot open project config file");
                });
                ee.printStackTrace();
                enableButtons();
                return;
            }
        }
        if (currentVersion == null) {
            Platform.runLater(() -> {
                AlertWindow.showErrorWindow("Cannot open project config file");
            });
            enableButtons();
            return;
        }

        Map<String, String> params = Map.of("v_from", currentVersion, "v_to", toVersion);
        FileVisitor fileVisitor = new FileVisitor();
        
        Task<Void> checkoutTask = new Task<>() {
            @Override public Void call() throws InterruptedException, NoSuchAlgorithmException, JSONException, IOException {
                Instant start = Instant.now();
                JSONObject response = null;
                try {
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: getting patch sequence from " + params.get("v_from") + " to " + params.get("v_to"));
                    });
                    response = VersionsEndpoint.getSwitch(params);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                SaveResponse.save(response);

                response.getJSONArray("files").forEach(fileItem -> {
                    JSONObject file = (JSONObject)fileItem;
                    Path location = Paths.get(file.getString("location"));
                    patchFiles.put(location, new ArrayList<>());
                    patchedFilesIntegrity.put(location, new HashMap<>());
                    file.getJSONArray("patches").forEach(patchItem -> {
                        JSONObject patch = (JSONObject)patchItem;
                        try {
                            patchFiles.get(location).add(
                                Map.of(
                                    "v_from", patch.getString("version_from"),
                                    "v_to", patch.getString("version_to"),
                                    "file_location", location.toString()
                                ));
                            patchedFilesIntegrity.get(location).put(patch.getString("version_to"),
                                    new Pair<>(
                                        patch.getJSONObject("new_file").getLong("size"),
                                        patch.getJSONObject("new_file").getString("checksum")
                                        ));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                });


                List<Path> oldFiles = null;
                oldFiles = fileVisitor.walkFileTree(projectPath);
                checkoutDump.append("old files amount ").append(oldFiles.size()).append(System.lineSeparator());

                CourgetteHandler.setMAX_THREADS_AMOUNT(20);
                CourgetteHandler.setMAX_ACTIVE_COURGETTES_AMOUNT(20);

                long counter = 0;
                for (Path relativeFileLocation: patchFiles.keySet()) {
                    counter += patchFiles.get(relativeFileLocation).size();
                }

                final long patchesAmount = counter;
                CourgetteHandler.setRemainingFilesAmount((int)patchesAmount);

                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                });

                List<Thread> filesThreads = new ArrayList<>();
                Map<Path, Path> finalPatchedFiles = new HashMap<>();

                downloadThread(projectParentFolder, statusLabel).start();

                for (Path relativeFileLocation: patchFiles.keySet()) {
                    Path _relativeFileLocation = Paths.get(relativeFileLocation.toString());

                    while (getCurrentPatchingFilesAmount() >= MAX_PATCHING_FILES_AMOUNT ||
                            !getPatchesThreadsDownloadPerFile().containsKey(_relativeFileLocation)) {
                        Thread.sleep(100);
                    }

                    filesThreads.add(patchingFileThread(projectPath, _relativeFileLocation, patchesAmount,
                            courgettesAmountLabel, progressBar, replaceFiles, finalPatchedFiles));
                    filesThreads.get(filesThreads.size()-1).start();
                }

                for (Thread thread: filesThreads) {
                    thread.join();
                }
                
                Path finalPatchedProject = null;
                if (!replaceFiles) {
                    finalPatchedProject = projectParentFolder.resolve("final_patched");

                    Files.copy(projectPath.resolve(".psheignore"),
                            finalPatchedProject.resolve(".psheignore"), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    finalPatchedProject = projectPath;
                }

                JSONObject updatedConfig = new JSONObject().put("currentVersion", toVersion);
                try {
                    Directories.saveJSONFile(finalPatchedProject.resolve("config.json"), updatedConfig);
                } catch (JSONException | IOException e1) {
                    Platform.runLater(() -> {
                        AlertWindow.showErrorWindow("Cannot update project config file");
                    });
                    e1.printStackTrace();
                }

                Platform.runLater(() -> {
                    statusLabel.setText("Status: checking project integrity, this can take awhile");
                });

                if (!toVersion.equals(rootVersion)) {
                    response = VersionsEndpoint.getSwitch(Map.of("v_from", rootVersion, "v_to", toVersion));
                }

                Map<String, List<Path>> integrityResult = checkProjectIntegrity(finalPatchedFiles, projectPath, toVersion, progressBar);
                progressIndicator.setProgress(progressIndicator.getProgress() + 0.2);

                checkoutDump.append("failed integrity files amount ").append(integrityResult.get("failed").size()).append(System.lineSeparator());

                for (Path file: integrityResult.get("failed")) {
                    checkoutDump.append("\tre-download ").append(file).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + file.toString());
                    });
                    addToProgressBar(progressBar, 1d/integrityResult.get("failed").size());
                    
                    FilesEndpoint.getRoot(finalPatchedFiles.get(file), Map.of("location", file.toString()));
                    if (!toVersion.equals(rootVersion)) {
                        // TODO: checkout from root file to toVersion, needs rewrite to use "perfile style"
                        // checkoutFromRoot(response.getJSONArray("files"), projectPath, tmpPatchPath, finalPatchedProject,
                        //         file, threads, checkoutDump, courgettesAmountLabel, statusLabel);
                    }
                }
                progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);
                
                if (!replaceFiles) {
                    String statusStr = finalPatchedProject.toString();
                    checkoutDump.append("unchanged files amount ").append(integrityResult.get("unchanged").size()).append(System.lineSeparator());
                    for (Path oldFile: integrityResult.get("unchanged")) {
                        checkoutDump.append("\tmoving unchanged files ").append(oldFile).append(System.lineSeparator());
                        System.out.println("moving " + oldFile.toString() + " to " + statusStr);
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: moving " + oldFile.toString() + " to " + statusStr);
                        });
                        addToProgressBar(progressBar, 1d/integrityResult.get("unchanged").size());

                        Files.createDirectories(finalPatchedProject.resolve(oldFile).getParent());
                        Files.copy(finalPatchedFiles.get(oldFile), finalPatchedProject.resolve(oldFile), StandardCopyOption.REPLACE_EXISTING);
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);
                } else {
                    checkoutDump.append("removed files amount ").append(integrityResult.get("deleted").size()).append(System.lineSeparator());
                    for (Path file: integrityResult.get("deleted")) {
                        checkoutDump.append("deleted ").append(file).append(System.lineSeparator());
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: deleting " + file.toString());
                        });
                        addToProgressBar(progressBar, 1d/integrityResult.get("deleted").size());
                        Files.deleteIfExists(finalPatchedProject.resolve(file));
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);
                }
                
                checkoutDump.append("missing files amount ").append(integrityResult.get("missing").size()).append(System.lineSeparator());
                for (Path remoteFile: integrityResult.get("missing")) {
                    checkoutDump.append("\tre-download ").append(remoteFile).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + remoteFile.toString());
                    });
                    addToProgressBar(progressBar, 1d/integrityResult.get("missing").size());
                    
                    FilesEndpoint.getRoot(finalPatchedProject.resolve(remoteFile), Map.of("location", remoteFile.toString()));
                    if (!toVersion.equals(rootVersion)) {
                        // TODO: checkout from root file to toVersion
                        // checkoutFromRoot(response.getJSONArray("files"), projectPath, tmpPatchPath, patchedProjectPath,
                        //         patchedProjectPath.resolve(remoteFile), threads, checkoutDump, courgettesAmountLabel, statusLabel);
                    }
                }
                progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);
                CourgetteHandler.setRemainingFilesAmount(0);

                Path targetPath = finalPatchedProject;
                finalPatchedFiles.forEach((relativeLocation, absPath) -> {
                    try {
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: updating " + relativeLocation.toString());
                        });
                        addToProgressBar(progressBar, 1d/finalPatchedFiles.size());
                        if (!integrityResult.get("deleted").contains(relativeLocation)) {
                            Files.createDirectories(targetPath.resolve(relativeLocation).getParent());
                            Files.move(absPath, targetPath.resolve(relativeLocation), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        AlertWindow.showErrorWindow("Failed to copy patched file to final location :(");
                        e.printStackTrace();
                    }
                });
                progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);

                progressIndicator.setProgress(1);

                Instant finish = Instant.now();
                StringBuilder str = new StringBuilder("Status: done ");
                str.append(ChronoUnit.MINUTES.between(start, finish));
                str.append(" mins ");
                str.append(ChronoUnit.SECONDS.between(start, finish) - ChronoUnit.MINUTES.between(start, finish)*60);
                str.append(" secs");
                Platform.runLater(() -> {
                    statusLabel.setText(str.toString());
                });

                enableButtons();
                
                BufferedWriter writer = new BufferedWriter(new FileWriter("dump.txt"));
                writer.write(checkoutDump.toString());
                writer.close();

                Platform.runLater(() -> {
                    progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                });

                return null;
            }
        };
        new Thread(checkoutTask).start();
    }

    private static Thread downloadThread(Path projectParentFolder, Label statusLabel) {
        Task<Void> downloadTask = new Task<>() {
            @Override public Void call() throws InterruptedException {
                for (Path relativeFileLocation: patchFiles.keySet()) {
                    Path _relativeFileLocation = Paths.get(relativeFileLocation.toString());
                    patchesThreadsDownloadPerFile_addFile(_relativeFileLocation);
                    for (Map<String, String> patchRequest: patchFiles.get(_relativeFileLocation)) {
                        Map<String, String> _patchRequest = new HashMap<>(patchRequest);
                        Task<Void> downloadPatchTask = new Task<>() {
                            @Override public Void call() throws IOException, NoSuchAlgorithmException, JSONException {
                                increaseCurrentDownloadsAmount();
                                String versionTo = _patchRequest.get("v_to");
                                Path patchFile = projectParentFolder.resolve("patches").resolve(versionTo).resolve(_relativeFileLocation);
                                String statusStr = _relativeFileLocation.toString();
                                Platform.runLater(() -> {
                                    statusLabel.setText("Status: downloading " + statusStr);
                                });
                                PatchesEndpoint.getFile(patchFile, _patchRequest);

                                JSONObject patchInfoResponse = PatchesEndpoint.getInfo(_patchRequest);
                                // TODO: handle failure
                                IntegrityChecker.checkFileIntegrity(patchFile,
                                        patchInfoResponse.getJSONObject("patch_file").getLong("patch_size"),
                                        patchInfoResponse.getJSONObject("patch_file").getString("patch_checksum"));
                                decreaseCurrentDownloadsAmount();
                                return null;
                            };
                        };

                        while (getCurrentDownloadsAmount() >= MAX_DOWNLOADS_AMOUNT) {
                            Thread.sleep(100);
                        }
                        
                        patchesThreadsDownloadPerFile_addThread(_relativeFileLocation,
                                patchRequest.get("v_to"), new Thread(downloadPatchTask)).start();
                    }
                }
                return null;
            }
        };

        return new Thread(downloadTask);
    }

    private static Thread patchingFileThread(Path projectPath, Path relativeFileLocation, long patchesAmount,
            Label courgettesAmountLabel, ProgressBar progressBar, boolean replaceFiles, Map<Path, Path> finalPatchedFiles) {
        Path projectParentFolder = projectPath.getParent();
        Task<Void> perfileTask = new Task<Void>() {
            @Override public Void call() throws InterruptedException, IOException, NoSuchAlgorithmException {   
                increaseCurrentPatchingFilesAmount();                         
                Path currentFile = projectPath.resolve(relativeFileLocation);
                Path newFile;
                Path patchFile;
                for (Map<String, String> patchRequest: patchFiles.get(relativeFileLocation)) {
                    String versionTo = patchRequest.get("v_to");

                    while (!getPatchesThreadsDownloadPerFile().get(relativeFileLocation).containsKey(versionTo)) {
                        Thread.sleep(100);
                    }
                    getPatchesThreadsDownloadPerFile().get(relativeFileLocation).get(versionTo).join();

                    patchFile = projectParentFolder.resolve("patches").resolve(versionTo).resolve(relativeFileLocation);
                    newFile = projectParentFolder.resolve("patched").resolve(versionTo).resolve(relativeFileLocation);
                    
                    CourgetteHandler courgetteThread = new CourgetteHandler();
                    courgetteThread.applyPatch(currentFile, newFile, patchFile,
                            currentFile.getParent(), false, courgettesAmountLabel, true);
                    courgetteThread.join();
                    addToProgressBar(progressBar, 1d/patchesAmount);
                    // TODO: handle failure
                    IntegrityChecker.checkFileIntegrity(newFile,
                            patchedFilesIntegrity.get(relativeFileLocation).get(versionTo).first,
                            patchedFilesIntegrity.get(relativeFileLocation).get(versionTo).second);
                    // Files.deleteIfExists(patchFile);
                    if (replaceFiles) {
                        Files.deleteIfExists(currentFile);
                    }
                    currentFile = newFile;
                };
                finalPatchedFiles.put(relativeFileLocation, currentFile);
                decreaseCurrentPatchingFilesAmount();
                return null;
            }
        };
        return new Thread(perfileTask);
    }

    public static void checkoutToVersionByPatches(Path projectPath, boolean replaceFiles, String toVersion,
            Label statusLabel, ProgressBar progressBar, Label courgettesAmountLabel, Button button, JSONObject config, AuthWindow authWindow,
            boolean rememberPaths, String rootVersion) {
        button.setDisable(true);
        ProgressIndicator progressIndicator = new ProgressIndicator();
        ((VBox) button.getScene().getRoot()).getChildren().add(progressIndicator);
        progressIndicator.setProgress(0);
        StringBuffer checkoutDump = new StringBuffer();

        Path projectParentFolder = projectPath.getParent();

        Path tmpProjectPath = projectParentFolder.resolve("patched_tmp").resolve(projectPath.getFileName());
        Path tmpPatchPath = projectParentFolder.resolve("patch_tmp").resolve(projectPath.getFileName());

        if (!config.getJSONObject(RunCourgette.os).has("remotePatchingInfo")) {
            config.getJSONObject(RunCourgette.os).put("remotePatchingInfo", new JSONObject());
        }
        config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").put("projectPath", projectPath.toString());
        config.getJSONObject(RunCourgette.os)
                .getJSONObject("remotePatchingInfo").put("rememberPaths", rememberPaths);
        authWindow.saveConfig();

        String currentVersion = null;
        if (Files.exists(projectPath.resolve("config.json"))) {
            File file = new File(projectPath.resolve("config.json").toString());
            String content;
            try {
                content = new String(Files.readAllBytes(Paths.get(file.toURI())));
                currentVersion = new JSONObject(content).getString("currentVersion");
            } catch (IOException ee) {
                Platform.runLater(() -> {
                    AlertWindow.showErrorWindow("Cannot open project config file");
                });
                ee.printStackTrace();
                return;
            }
        } else {
            Platform.runLater(() -> {
                AlertWindow.showErrorWindow("Cannot open project config file");
            });
            button.setDisable(false);
            return;
        }

        Map<String, String> params = Map.of("v_from", currentVersion, "v_to", toVersion);

        Task<Void> checkoutTask = new Task<>() {
            @Override public Void call() throws InterruptedException, IOException, NoSuchAlgorithmException, JSONException {
                Instant start = Instant.now();
                JSONObject response = null;
                try {
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: getting patch sequence from " + params.get("v_from") + " to " + params.get("v_to"));
                    });
                    response = VersionsEndpoint.getSwitch(params);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                Set<Path> subfolderSequence = new TreeSet<>();

                Map<Path, List<Map<String, String>>> patchParams = new HashMap<>();

                SaveResponse.save(response);

                response.getJSONArray("files").forEach(fileItem -> {
                    JSONObject file = (JSONObject)fileItem;
                    file.getJSONArray("patches").forEach(patchItem -> {
                        JSONObject patch = (JSONObject)patchItem;
                        try {
                            Path subfolderPath = tmpPatchPath.resolve("to_" + patch.getString("version_to"));
                            if (subfolderSequence.add(subfolderPath)) {
                                patchParams.put(subfolderPath, new ArrayList<>());
                            }

                            patchParams.get(subfolderPath).add(new HashMap<>(
                                Map.of(
                                    "v_from", patch.getString("version_from"),
                                    "v_to", patch.getString("version_to"),
                                    "file_location", file.getString("location")
                                )
                            ));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                });

                FileVisitor fileVisitor = new FileVisitor();

                List<Path> oldFiles = null;

                oldFiles = fileVisitor.walkFileTree(projectPath);
                checkoutDump.append("old files amount ").append(oldFiles.size()).append(System.lineSeparator());

                Path relativePatchPath;
                Path newPath;
                Path oldPath;

                List<CourgetteHandler> threads = new ArrayList<>();

                CourgetteHandler.setMAX_THREADS_AMOUNT(20);
                CourgetteHandler.setMAX_ACTIVE_COURGETTES_AMOUNT(20);

                long counter = 0;
                for (Path folder: subfolderSequence) {
                    counter += patchParams.get(folder).size();
                }

                final long patchesAmount = counter;
                CourgetteHandler.setRemainingFilesAmount((int)patchesAmount);

                Path oldActiveProjectPath = projectPath;
                Path patchedActiveProjectPath = tmpProjectPath;

                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                });
                for (Path folder: subfolderSequence) {
                    for (Map<String, String> patchParam: patchParams.get(folder)) {
                        relativePatchPath = Paths.get(patchParam.get("file_location"));
                        Path patchFile = folder.resolve(relativePatchPath.toString());
                        newPath = patchedActiveProjectPath.resolve(relativePatchPath.toString().equals("") ?
                                Paths.get("..", "..", "..", patchedActiveProjectPath.getFileName().toString()) :
                                relativePatchPath).normalize();
                        oldPath = oldActiveProjectPath.resolve(relativePatchPath).normalize();
                        String statusStr = relativePatchPath.toString();
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: downloading " + statusStr);
                        });
                        PatchesEndpoint.getFile(patchFile, patchParam);

                        JSONObject patchInfoResponse = PatchesEndpoint.getInfo(patchParam);
                        if (DataEncoder.getByteSize(patchFile) == patchInfoResponse.getJSONObject("patch_file").getLong("patch_size")) {
                            if (!IntegrityChecker.compareChecksum(patchFile, patchInfoResponse.getJSONObject("patch_file").getString("patch_checksum"))) {
                                System.out.print("FAILED CHECKSUM FOR PATCH ");
                                System.out.println(patchFile);
                                System.exit(4);
                            }
                        } else {
                            System.out.print("FAILED BYTESIZE FOR PATCH ");
                            System.out.println(patchFile);
                            System.exit(5);
                        }
            
                        try {
                            Files.createDirectories(newPath.getParent());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        checkoutDump.append("\tpatching ").append(patchFile).append(System.lineSeparator());
                        CourgetteHandler thread = new CourgetteHandler();
                        thread.applyPatch(oldPath, newPath, patchFile, oldPath.getParent(), false, courgettesAmountLabel, true);
                        threads.add(thread);
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: patching " + folder.relativize(patchFile).toString());
                        });
                        addToProgressBar(progressBar, 1d/patchesAmount);
                    }

                    for (CourgetteHandler thread: threads) {
                        thread.join();
                    }
                    threads.clear();
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1/subfolderSequence.size());

                    // Directories.deleteDirectory(folder);
                }
                // if (tmpPatchPath.getParent().endsWith("patch_tmp"))
                //     Directories.deleteDirectory(tmpPatchPath.getParent());
                
                Path patchedProjectPath = null;
                if (!replaceFiles) {
                    patchedProjectPath = patchedActiveProjectPath;

                    Files.copy(projectPath.resolve(".psheignore"),
                            patchedProjectPath.resolve(".psheignore"), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    patchedProjectPath = projectPath;
                }

                JSONObject updatedConfig = new JSONObject().put("currentVersion", toVersion);
                try {
                    Directories.saveJSONFile(patchedProjectPath.resolve("config.json"), updatedConfig);
                } catch (JSONException | IOException e1) {
                    Platform.runLater(() -> {
                        AlertWindow.showErrorWindow("Cannot update project config file");
                    });
                    e1.printStackTrace();
                }

                Map<Path, Path> patchedFiles = new HashMap<>();
                for (Path filePath: fileVisitor.walkFileTree(patchedProjectPath)) {
                    patchedFiles.put(patchedProjectPath.relativize(filePath), filePath);
                }

                Platform.runLater(() -> {
                    statusLabel.setText("Status: checking project integrity, this can take awhile");
                });

                
                if (!toVersion.equals(rootVersion)) {
                    response = VersionsEndpoint.getSwitch(Map.of("v_from", rootVersion, "v_to", toVersion));
                }

                Map<String, List<Path>> integrityResult = checkProjectIntegrity(patchedFiles, oldActiveProjectPath, toVersion, progressBar);
                progressIndicator.setProgress(progressIndicator.getProgress() + 0.2);
                checkoutDump.append("failed integrity files amount ").append(integrityResult.get("failed").size()).append(System.lineSeparator());
                for (Path file: integrityResult.get("failed")) {
                    checkoutDump.append("\tre-download ").append(file).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + file.toString());
                    });
                    addToProgressBar(progressBar, 1d/integrityResult.get("failed").size());
                    
                    FilesEndpoint.getRoot(file, Map.of("location", patchedProjectPath.relativize(file).toString()));
                    if (!toVersion.equals(rootVersion)) {
                        // TODO: checkout from root file to toVersion, needs testing
                        checkoutFromRoot(response.getJSONArray("files"), projectPath, tmpPatchPath, patchedProjectPath,
                                file, threads, checkoutDump, courgettesAmountLabel, statusLabel);
                    }
                }
                progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);
                if (!replaceFiles) {
                    checkoutDump.append("removed files amount ").append(integrityResult.get("deleted").size()).append(System.lineSeparator());
                    for (Path file: integrityResult.get("deleted")) {
                        checkoutDump.append("deleted ").append(file).append(System.lineSeparator());
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: deleting " + file.toString());
                        });
                        addToProgressBar(progressBar, 1d/integrityResult.get("deleted").size());
                        Files.deleteIfExists(file);
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);

                    String statusStr = patchedProjectPath.toString();
                    checkoutDump.append("unchanged files amount ").append(integrityResult.get("unchanged").size()).append(System.lineSeparator());
                    for (Path oldFile: integrityResult.get("unchanged")) {
                        checkoutDump.append("\tmoving unchanged files ").append(oldFile).append(System.lineSeparator());
                        System.out.println("moving " + oldFile.toString() + " to "
                                + patchedProjectPath.resolve(projectPath.relativize(oldFile)));
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: moving " + projectPath.relativize(oldFile).toString() + " to " + statusStr);
                        });
                        addToProgressBar(progressBar, 1d/integrityResult.get("unchanged").size());

                        patchedProjectPath.resolve(projectPath.relativize(oldFile)).getParent().toFile().mkdirs();
                        Files.copy(oldFile, patchedProjectPath.resolve(projectPath.relativize(oldFile)), StandardCopyOption.REPLACE_EXISTING);
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);
                } else {
                    checkoutDump.append("removed files amount ").append(integrityResult.get("deleted").size()).append(System.lineSeparator());
                    for (Path file: integrityResult.get("deleted")) {
                        Path deletingFile = projectPath.resolve(patchedProjectPath.relativize(file));
                        checkoutDump.append("deleted ").append(deletingFile).append(System.lineSeparator());
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: deleting " + deletingFile.toString());
                        });
                        addToProgressBar(progressBar, 1d/integrityResult.get("deleted").size());
                        Files.deleteIfExists(deletingFile);
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.2);
                }

                checkoutDump.append("missing files amount ").append(integrityResult.get("missing").size()).append(System.lineSeparator());
                for (Path remoteFile: integrityResult.get("missing")) {
                    checkoutDump.append("\tre-download ").append(remoteFile).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + remoteFile.toString());
                    });
                    addToProgressBar(progressBar, 1d/integrityResult.get("missing").size());
                    
                    FilesEndpoint.getRoot(patchedProjectPath.resolve(remoteFile), Map.of("location", remoteFile.toString()));
                    if (!toVersion.equals(rootVersion)) {
                        // TODO: checkout from root file to toVersion
                        checkoutFromRoot(response.getJSONArray("files"), projectPath, tmpPatchPath, patchedProjectPath,
                                patchedProjectPath.resolve(remoteFile), threads, checkoutDump, courgettesAmountLabel, statusLabel);
                    }
                }
                progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);

                CourgetteHandler.setRemainingFilesAmount(0);
                if (replaceFiles) {
                    List<Path> totalPatchedFiles = fileVisitor.walkFileTree(patchedProjectPath);
                    String pathString = patchedProjectPath.toString();
                    for (Path file: totalPatchedFiles) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: updating " + Paths.get(pathString).relativize(file).toString());
                        });
                        addToProgressBar(progressBar, 1d/totalPatchedFiles.size());
                        try {
                            projectPath.resolve(patchedProjectPath.relativize(file)).getParent().toFile().mkdirs();
                            Files.copy(file, projectPath.resolve(patchedProjectPath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);

                    for (Path file: integrityResult.get("deleted")) {
                        Path targetFile = projectPath.resolve(Paths.get(pathString).relativize(file));
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: deleting " + file.toString());
                        });
                        addToProgressBar(progressBar, 1d/integrityResult.get("deleted").size());
                        Files.deleteIfExists(targetFile);
                    }
                    Files.deleteIfExists(patchedProjectPath);
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);
                }
                progressIndicator.setProgress(1);

                Instant finish = Instant.now();
                StringBuilder str = new StringBuilder("Status: done ");
                str.append(ChronoUnit.MINUTES.between(start, finish));
                str.append(" mins ");
                str.append(ChronoUnit.SECONDS.between(start, finish) - ChronoUnit.MINUTES.between(start, finish)*60);
                str.append(" secs");
                Platform.runLater(() -> {
                    statusLabel.setText(str.toString());
                });

                button.setDisable(false);
                
                BufferedWriter writer = new BufferedWriter(new FileWriter("dump.txt"));
                writer.write(checkoutDump.toString());
                writer.close();

                Platform.runLater(() -> {
                    progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                });

                return null;
            }
        };
        new Thread(checkoutTask).start();
    }

    public static void checkoutFromRoot(JSONArray files, Path oldProjectPath, Path patchFolderPath,
            Path patchedProjectPath, Path absRootFilePath, List<CourgetteHandler> threads, StringBuffer checkoutDump, 
            Label courgettesAmountLabel, Labeled statusLabel) throws InterruptedException {
        for (Object fileItem: files) {
            JSONObject jsonFile = (JSONObject)fileItem;
            if (jsonFile.getString("location").equals(patchedProjectPath.relativize(absRootFilePath).toString())) {
                jsonFile.getJSONArray("patches").forEach(patchItem -> {
                    JSONObject patch = (JSONObject)patchItem;
                    Path subfolderPath = Paths.get("");
                    try {
                        subfolderPath = patchFolderPath.resolve("to_" + patch.getString("version_to"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Path absPatchPath = subfolderPath.resolve(jsonFile.getString("location"));

                    try {
                        checkoutDump.append("\tpatching ").append(jsonFile.getString("location")).append(System.lineSeparator());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    CourgetteHandler thread = new CourgetteHandler();
                    thread.applyPatch(absRootFilePath, Paths.get(absRootFilePath.toString() + "_patched"),
                            absPatchPath, absRootFilePath.getParent(), true, courgettesAmountLabel, true);
                    threads.add(thread);
                    String statusStr = subfolderPath.relativize(absPatchPath).toString();
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: patching " + statusStr);
                    });
                });
            }
            
        }
        for (CourgetteHandler thread: threads) {
            thread.join();
        }
    }

    public static Map<String, List<Path>> checkProjectIntegrity(
            Map<Path, Path> patchedFiles, Path oldProjectPath, String version, ProgressBar progressBar) throws IOException {

        Map<Path, VersionFileEntity> versionFiles = new VersionEntity(VersionsEndpoint.getVersions(Map.of("v", version)).getJSONObject("version")).getFiles();

        List<Path> failedFiles = new ArrayList<>();
        List<Path> missingFiles = new ArrayList<>();
        List<Path> deletedFiles = new ArrayList<>();
        List<Path> unchangedFiles = new ArrayList<>();

        // patchedFiles.forEach((relativeFile, file) -> {
        //     IntegrityChecker.checkLocalIntegrity(file, relativeFile, failedFiles, deletedFiles, versionFiles);
        //     addToProgressBar(progressBar, 1d/(patchedFiles.size() + versionFiles.size()));
        // });

        versionFiles.keySet().forEach(remoteFile -> {
            Path file = oldProjectPath.resolve(remoteFile.toString());
            IntegrityChecker.checkRemoteIntegrity(file, remoteFile, patchedFiles, unchangedFiles, missingFiles, versionFiles);
            addToProgressBar(progressBar, 1d/(versionFiles.size()));
        });

        Map<String, List<Path>> result = new HashMap<>(
            Map.of("failed", failedFiles,
                    "missing", missingFiles,
                    "deleted", deletedFiles,
                    "unchanged", unchangedFiles));

        return result;
    }

    private static void addToProgressBar(ProgressBar progressBar, double percentage) {
        Platform.runLater(() -> {
            progressBar.setProgress(progressBar.getProgress() + percentage);
        });
    }
}
