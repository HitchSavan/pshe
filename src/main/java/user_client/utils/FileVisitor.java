package user_client.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.shyiko.klob.Glob;

public class FileVisitor extends SimpleFileVisitor<Path> {

    public List<Path> allFiles = new ArrayList<>();
    private List<String> patterns = new ArrayList<>();
    private List<Path> ignoredFiles = new ArrayList<>();
    private Iterator<Path> iterator;
    
    public FileVisitor() {
        this(null);
    }

    public FileVisitor(Path folder) {
        init(folder);
    }

    private void init(Path folder) {
        if (folder != null && Files.exists(Paths.get(".psheignore"))) {
            allFiles.clear();
            patterns.clear();
            ignoredFiles.clear();

            File file = new File(".psheignore");
            try {
                patterns = Files.readAllLines(Paths.get(file.toURI()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            iterator = Glob.from(patterns.toArray(new String[0])).iterate(folder);

            while (iterator.hasNext())
                ignoredFiles.add(iterator.next());
        }
    }

    public void walkFileTree(Path folder) {
        init(folder);
        try {
            Files.walkFileTree(folder, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        file = file.toAbsolutePath();
        if (!ignoredFiles.contains(file))
            allFiles.add(file);
 
        return FileVisitResult.CONTINUE;
    }
}
