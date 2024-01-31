package gui.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UnpackResources {
    public static void unpackResources(String resourceName) throws IOException {
        URL resourceURL = UnpackResources.class.getClassLoader().getResource(resourceName);
        // TODO: TEST THIS APPROACH
        System.out.print(new File(resourceURL.getPath()).getCanonicalFile());
        System.out.print("\t");
        System.out.println(new File(resourceURL.getPath()).getCanonicalFile().isDirectory());
        if (resourceURL.getProtocol().equals("file")) {
            if (new File(resourceURL.getPath()).getCanonicalFile().isDirectory()) {
                List<String> files = getResourceFiles(resourceName);
                for (String filename: files) {
                    unpackResources(resourceName + "/" + filename);
                }
            } else {
                unpackResourceFile(resourceName);
            }
        }
    }

    public static void unpackResourceFile(String resourceName) throws IOException {
        URL url = UnpackResources.class.getClassLoader().getResource(resourceName);
        try {
            Files.createDirectories(Paths.get("tmp/" + resourceName.substring(0, resourceName.lastIndexOf("/"))));
        } catch (IOException e) {
            System.err.println("Cannot create directories - " + e);
        }
        FileOutputStream output = new FileOutputStream("tmp/" + resourceName);
        InputStream input = url.openStream();
        byte [] buffer = new byte[4096];
        int bytesRead = input.read(buffer);
        while (bytesRead != -1) {
            output.write(buffer, 0, bytesRead);
            bytesRead = input.read(buffer);
        }
        output.close();
        input.close();
    }

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
    public static void deleteDirectory(String directoryToBeDeleted) {
        deleteDirectory(Paths.get(directoryToBeDeleted).toFile());
    }

    private static List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        try (InputStream in = getResourceAsStream(path); BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }
        return filenames;
    }
    
    private static InputStream getResourceAsStream(String resource) {
        final InputStream in = getContextClassLoader().getResourceAsStream(resource);
        return in == null ? UnpackResources.class.getClass().getResourceAsStream(resource) : in;
    }
    
    private static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
