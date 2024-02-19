package user_client.gui.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

public class PatternMatcher implements PathMatcher {

    public String pattern;
    private PathMatcher matcher;

    public PatternMatcher(String _pattern) {
        pattern = _pattern;
        matcher = FileSystems.getDefault().getPathMatcher("glob:" + _pattern);
    }

    @Override
    public boolean matches(Path path) {
        return path != null && matcher.matches(path);
    }
}
