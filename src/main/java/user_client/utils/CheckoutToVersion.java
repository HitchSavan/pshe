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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

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
import patcher.remote_api.endpoints.FilesEndpoint;
import patcher.remote_api.endpoints.PatchesEndpoint;
import patcher.remote_api.endpoints.VersionsEndpoint;
import patcher.remote_api.entities.VersionEntity;
import patcher.remote_api.entities.VersionFileEntity;
import patcher.utils.data_utils.DataEncoder;
import patcher.utils.data_utils.IntegrityChecker;
import patcher.utils.files_utils.Directories;
import patcher.utils.files_utils.FileVisitor;
import patcher.utils.patching_utils.RunCourgette;
import user_client.gui.AuthWindow;

public class CheckoutToVersion {
    public static void checkoutToVersion(Path projectPath, boolean replaceFiles, String toVersion,
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
        }

        Map<String, String> params = Map.of("v_from", currentVersion, "v_to", toVersion);

        Task<Void> task = new Task<>() {
            @Override public Void call() throws InterruptedException, IOException, NoSuchAlgorithmException, JSONException {

                Instant start = Instant.now();
                AtomicLong counter = new AtomicLong(0);
                JSONObject response = null;
                try {
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: getting patch sequence from " + params.get("v_from") + " to " + params.get("v_to"));
                    });
                    // TODO: need to sort patch array
                    response = VersionsEndpoint.getSwitch(params);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                Set<Path> subfolderSequence = new TreeSet<>();

                Map<Path, List<Map<String, String>>> patchParams = new HashMap<>();

                SaveResponse.save(response);

                counter.set(0);
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
                byte[] emptyData = {0};

                List<CourgetteHandler> threads = new ArrayList<>();

                CourgetteHandler.setMAX_THREADS_AMOUNT(20);
                CourgetteHandler.setMAX_ACTIVE_COURGETTES_AMOUNT(20);

                for (Path folder: subfolderSequence) {
                    counter.addAndGet(patchParams.get(folder).size());
                }

                final long patchesAmount = counter.get();
                CourgetteHandler.setRemainingFilesAmount((int)patchesAmount);

                counter.set(0);
                for (Path folder: subfolderSequence) {
                    for (Map<String, String> patchParam: patchParams.get(folder)) {
                        relativePatchPath = Paths.get(patchParam.get("file_location"));
                        Path patchFile = folder.resolve(relativePatchPath.toString());
                        newPath = tmpProjectPath.resolve(relativePatchPath.toString().equals("") ?
                                Paths.get("..", "..", "..", tmpProjectPath.getFileName().toString()) :
                                relativePatchPath).normalize();
                        oldPath = projectPath.resolve(relativePatchPath).normalize();
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
                        if (!oldFiles.contains(oldPath)) {
                            try {
                                oldPath.getParent().toFile().mkdirs();
                                Files.createFile(oldPath);
                                Files.write(oldPath, emptyData);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
            
                        try {
                            Files.createDirectories(newPath.getParent());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        checkoutDump.append("\tpatching ").append(patchFile).append(System.lineSeparator());
                        CourgetteHandler thread = new CourgetteHandler();
                        thread.applyPatch(oldPath, newPath, patchFile, projectPath.getParent(), false, courgettesAmountLabel, true);
                        threads.add(thread);
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: patching " + folder.relativize(patchFile).toString());
                            progressBar.setProgress(progressBar.getProgress() + 1/patchesAmount);
                        });
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
                    patchedProjectPath = tmpProjectPath;

                    Files.copy(projectPath.resolve(".psheignore"),
                            tmpProjectPath.resolve(".psheignore"), StandardCopyOption.REPLACE_EXISTING);
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

                Map<String, List<Path>> integrityResult = checkProjectIntegrity(patchedFiles, projectPath, toVersion, progressBar);
                progressIndicator.setProgress(progressIndicator.getProgress() + 0.2);
                counter.set(0);
                checkoutDump.append("failed integrity files amount ").append(integrityResult.get("failed").size()).append(System.lineSeparator());
                for (Path file: integrityResult.get("failed")) {
                    checkoutDump.append("\tre-download ").append(file).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + file.toString());
                        // TODO: split progress onto ~8 parts
                        progressBar.setProgress(progressBar.getProgress() + 1/integrityResult.get("failed").size());
                    });
                    
                    FilesEndpoint.getRoot(file, Map.of("location", patchedProjectPath.relativize(file).toString()));
                    if (!toVersion.equals(rootVersion)) {
                        // TODO: checkout from root file to toVersion, needs testing
                        checkoutFromRoot(response.getJSONArray("files"), projectPath, tmpPatchPath, patchedProjectPath,
                                file, threads, checkoutDump, courgettesAmountLabel, statusLabel);
                    }
                }
                progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);
                if (!replaceFiles) {
                    counter.set(0);
                    checkoutDump.append("removed files amount ").append(integrityResult.get("deleted").size()).append(System.lineSeparator());
                    for (Path file: integrityResult.get("deleted")) {
                        checkoutDump.append("deleted ").append(file).append(System.lineSeparator());
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: deleting " + file.toString());
                            progressBar.setProgress(progressBar.getProgress() + 1/integrityResult.get("deleted").size());
                        });
                        Files.deleteIfExists(file);
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);

                    counter.set(0);
                    String statusStr = patchedProjectPath.toString();
                    checkoutDump.append("unchanged files amount ").append(integrityResult.get("unchanged").size()).append(System.lineSeparator());
                    for (Path oldFile: integrityResult.get("unchanged")) {
                        checkoutDump.append("\tmoving unchanged files ").append(oldFile).append(System.lineSeparator());
                        System.out.println("moving " + oldFile.toString() + " to "
                                + patchedProjectPath.resolve(projectPath.relativize(oldFile)));
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: moving " + projectPath.relativize(oldFile).toString() + " to " + statusStr);
                            progressBar.setProgress(progressBar.getProgress() + 1/integrityResult.get("unchanged").size());
                        });

                        patchedProjectPath.resolve(projectPath.relativize(oldFile)).getParent().toFile().mkdirs();
                        Files.copy(oldFile, patchedProjectPath.resolve(projectPath.relativize(oldFile)), StandardCopyOption.REPLACE_EXISTING);
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);
                } else {
                    counter.set(0);
                    checkoutDump.append("removed files amount ").append(integrityResult.get("deleted").size()).append(System.lineSeparator());
                    for (Path file: integrityResult.get("deleted")) {
                        Path deletingFile = projectPath.resolve(patchedProjectPath.relativize(file));
                        checkoutDump.append("deleted ").append(deletingFile).append(System.lineSeparator());
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: deleting " + deletingFile.toString());
                            progressBar.setProgress(progressBar.getProgress() + 1/integrityResult.get("deleted").size());
                        });
                        Files.deleteIfExists(deletingFile);
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.2);
                }

                counter.set(0);
                checkoutDump.append("missing files amount ").append(integrityResult.get("missing").size()).append(System.lineSeparator());
                for (Path remoteFile: integrityResult.get("missing")) {
                    checkoutDump.append("\tre-download ").append(remoteFile).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + remoteFile.toString());
                        progressBar.setProgress(progressBar.getProgress() + 1/integrityResult.get("missing").size());
                    });
                    
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
                    counter.set(0);
                    List<Path> totalPatchedFiles = fileVisitor.walkFileTree(patchedProjectPath);
                    String pathString = patchedProjectPath.toString();
                    for (Path file: totalPatchedFiles) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: updating " + Paths.get(pathString).relativize(file).toString());
                            progressBar.setProgress(progressBar.getProgress() + 1/totalPatchedFiles.size());
                        });
                        try {
                            projectPath.resolve(patchedProjectPath.relativize(file)).getParent().toFile().mkdirs();
                            Files.copy(file, projectPath.resolve(patchedProjectPath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    progressIndicator.setProgress(progressIndicator.getProgress() + 0.1);

                    counter.set(0);
                    for (Path file: integrityResult.get("deleted")) {
                        Path targetFile = projectPath.resolve(Paths.get(pathString).relativize(file));
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: deleting " + file.toString());
                            progressBar.setProgress(progressBar.getProgress() + 1/integrityResult.get("deleted").size());
                        });
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
        new Thread(task).start();
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
                            absPatchPath, oldProjectPath.getParent(), true, courgettesAmountLabel, true);
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

        patchedFiles.forEach((relativeFile, file) -> {
            IntegrityChecker.checkLocalIntegrity(file, relativeFile, failedFiles, deletedFiles, versionFiles);
            Platform.runLater(() -> {
                progressBar.setProgress(progressBar.getProgress() + 1/(patchedFiles.size() + versionFiles.size()));
            });
        });

        versionFiles.keySet().forEach(remoteFile -> {
            Path file = oldProjectPath.resolve(remoteFile.toString());
            IntegrityChecker.checkRemoteIntegrity(file, remoteFile, patchedFiles, unchangedFiles, missingFiles, versionFiles);
            Platform.runLater(() -> {
                progressBar.setProgress(progressBar.getProgress() + 1/(patchedFiles.size() + versionFiles.size()));
            });
        });

        Map<String, List<Path>> result = new HashMap<>(
            Map.of("failed", failedFiles,
                    "missing", missingFiles,
                    "deleted", deletedFiles,
                    "unchanged", unchangedFiles));

        return result;
    }
}
