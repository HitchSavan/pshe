package user_client.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import patcher.remote_api.endpoints.FilesEndpoint;
import patcher.remote_api.endpoints.PatchesEndpoint;
import patcher.remote_api.endpoints.VersionsEndpoint;
import patcher.utils.data_utils.IntegrityChecker;
import patcher.utils.files_utils.Directories;
import patcher.utils.files_utils.FileVisitor;
import patcher.utils.patching_utils.RunCourgette;
import user_client.gui.AuthWindow;

public class CheckoutToVersion {
    public static void checkoutToVersion(Path projectPath, boolean replaceFiles, String toVersion,
            Label statusLabel, Label courgettesAmountLabel, Button button, JSONObject config, AuthWindow authWindow,
            boolean rememberPaths, String rootVersion) {
        button.setDisable(true);
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
                AlertWindow.showErrorWindow("Cannot open project config file");
                ee.printStackTrace();
                return;
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("v_from", currentVersion);
        params.put("v_to", toVersion);

        Task<Void> task = new Task<>() {
            @Override public Void call() throws InterruptedException, IOException {

                Instant start = Instant.now();
                AtomicLong counter = new AtomicLong(0);
                JSONObject response = null;
                try {
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: getting patch sequence from " + params.get("v_from") + " to " + params.get("v_to"));
                    });
                    response = VersionsEndpoint.getSwitch(params);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                List<Path> subfolderSequence = new ArrayList<>();

                Map<Path, List<Map<String, String>>> patchParams = new HashMap<>();

                counter.set(0);
                response.getJSONArray("files").forEach(fileItem -> {
                    JSONObject file = (JSONObject)fileItem;
                    file.getJSONArray("patches").forEach(patchItem -> {
                        JSONObject patch = (JSONObject)patchItem;
                        try {
                            Path subfolderPath = tmpPatchPath.resolve(
                                    "from_" + patch.getString("version_from") + "_to_" + patch.getString("version_to"));
                            if (!subfolderSequence.contains(subfolderPath)) {
                                subfolderSequence.add(subfolderPath);
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

                CourgetteHandler.setMAX_THREADS_AMOUNT(30);
                CourgetteHandler.setMAX_ACTIVE_COURGETTES_AMOUNT(20);

                for (Path folder: subfolderSequence) {
                    counter.addAndGet(patchParams.get(folder).size());
                }

                final long patchesAmount = counter.get();
                CourgetteHandler.setTotalThreadsAmount((int)patchesAmount);

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
                        PatchesEndpoint.getFile(folder.resolve(relativePatchPath), patchParam);

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
                        thread.applyPatch(projectParentFolder.relativize(oldPath).toString(),
                                projectParentFolder.relativize(newPath).toString(), patchFile.toString(),
                                false, courgettesAmountLabel, false);
                        threads.add(thread);
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: patching " + folder.relativize(patchFile).toString());
                            courgettesAmountLabel.setText("Active Courgette instances:\t"
                                    + CourgetteHandler.activeCount() + "" +
                                    System.lineSeparator() + "Files remains:\t" +
                                    (patchesAmount - counter.getAndIncrement()));
                        });
                        while (CourgetteHandler.activeCount() >= CourgetteHandler.getMAX_THREADS_AMOUNT()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                AlertWindow.showErrorWindow("Cannot handle max courgette threads amount");
                                e.printStackTrace();
                            }
                        }
                        CourgetteHandler.decreaseTotalThreadsAmount();
                    }

                    for (CourgetteHandler thread: threads) {
                        thread.join();
                    }

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
                    AlertWindow.showErrorWindow("Cannot update project config file");
                    e1.printStackTrace();
                }

                Map<Path, Path> patchedFiles = new HashMap<>();
                for (Path filePath: fileVisitor.walkFileTree(patchedProjectPath)) {
                    patchedFiles.put(patchedProjectPath.relativize(filePath), filePath);
                }

                Platform.runLater(() -> {
                    statusLabel.setText("Status: checking project integrity, this can take awhile");
                });
                Map<String, ArrayList<Path>> integrityResult = IntegrityChecker.checkRemoteIntegrity(patchedFiles, projectPath, toVersion);
                counter.set(0);
                checkoutDump.append("removed files amount ").append(integrityResult.get("deleted").size()).append(System.lineSeparator());
                for (Path file: integrityResult.get("deleted")) {
                    checkoutDump.append("deleted ").append(file).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: deleting " + file.toString());
                        courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                System.lineSeparator() + "Files remains:\t" +
                                (integrityResult.get("deleted").size() - counter.getAndIncrement()));
                    });
                    Files.deleteIfExists(file);
                }
                counter.set(0);
                checkoutDump.append("failed integrity files amount ").append(integrityResult.get("failed").size()).append(System.lineSeparator());
                for (Path file: integrityResult.get("failed")) {
                    checkoutDump.append("\tre-download ").append(file).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + file.toString());
                        courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                System.lineSeparator() + "Files remains:\t" +
                                (integrityResult.get("failed").size() - counter.getAndIncrement()));
                    });
                    
                    FilesEndpoint.getRoot(file, Map.of("location", file.toString()));
                    if (!toVersion.equals(rootVersion)) {
                        // TODO: checkout from root file to toVersion
                    }
                }
                counter.set(0);
                checkoutDump.append("unchanged files amount ").append(integrityResult.get("unchanged").size()).append(System.lineSeparator());
                for (Path oldFile: integrityResult.get("unchanged")) {
                    checkoutDump.append("\tmoving unchanged files ").append(oldFile).append(System.lineSeparator());
                    System.out.println("moving " + oldFile.toString() + " to "
                            + patchedProjectPath.resolve(projectPath.relativize(oldFile)));
                    String statusStr = patchedProjectPath.toString();
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: moving " + oldFile.toString() + " to "
                                + Paths.get(statusStr, projectPath.relativize(oldFile).toString()));
                        courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                System.lineSeparator() + "Files remains:\t" +
                                (integrityResult.get("unchanged").size() - counter.getAndIncrement()));
                    });

                    Files.copy(oldFile, patchedProjectPath.resolve(projectPath.relativize(oldFile)), StandardCopyOption.REPLACE_EXISTING);
                }
                counter.set(0);
                checkoutDump.append("missing files amount ").append(integrityResult.get("missing").size()).append(System.lineSeparator());
                for (Path remoteFile: integrityResult.get("missing")) {
                    checkoutDump.append("\tre-download ").append(remoteFile).append(System.lineSeparator());
                    Platform.runLater(() -> {
                        statusLabel.setText("Status: downloading " + remoteFile.toString());
                        courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                System.lineSeparator() + "Files remains:\t" +
                                (integrityResult.get("missing").size() - counter.getAndIncrement()));
                    });
                    
                    FilesEndpoint.getRoot(patchedProjectPath.resolve(remoteFile), Map.of("location", remoteFile.toString()));
                    if (!toVersion.equals(rootVersion)) {
                        // TODO: checkout from root file to toVersion
                    }
                }

                CourgetteHandler.setTotalThreadsAmount(0);
                if (replaceFiles) {
                    counter.set(0);
                    List<Path> totalPatchedFiles = fileVisitor.walkFileTree(patchedProjectPath);
                    String pathString = patchedProjectPath.toString();
                    for (Path file: totalPatchedFiles) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: updating " + Paths.get(pathString).relativize(file).toString());
                            courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                    System.lineSeparator() + "Files remains:\t" +
                                    (totalPatchedFiles.size() - counter.getAndIncrement()));
                        });
                        try {
                            Files.copy(file, projectPath.resolve(patchedProjectPath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    counter.set(0);
                    for (Path file: integrityResult.get("deleted")) {
                        Path targetFile = projectPath.resolve(Paths.get(pathString).relativize(file));
                        Platform.runLater(() -> {
                            statusLabel.setText("Status: deleting " + file.toString());
                            courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                                    System.lineSeparator() + "Files remains:\t" +
                                    (integrityResult.get("deleted").size() - counter.getAndIncrement()));
                        });
                        Files.deleteIfExists(targetFile);
                    }
                }

                Platform.runLater(() -> {
                    courgettesAmountLabel.setText("Active Courgette instances:\t0" +
                            System.lineSeparator() + "Files remains:\t0");
                });

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

                return null;
            }
        };
        new Thread(task).start();
    }
}
