package user_client.gui.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public class FileVisitor extends SimpleFileVisitor<Path> {

    public ArrayList<Path> allFiles = new ArrayList<>();
    private ArrayList<PatternMatcher> patterns = new ArrayList<>();

    public FileVisitor() {
        
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {


        
        allFiles.add(file.toAbsolutePath());
 
        return FileVisitResult.CONTINUE;
    }
}
