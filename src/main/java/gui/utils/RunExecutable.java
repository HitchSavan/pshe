package gui.utils;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;

public class RunExecutable {
    public static Process runExec(String command, String[] args) throws IOException {
        ArrayList<String> parametrizedCommand = new ArrayList<>();
        parametrizedCommand.add(command); 
        if (args != null) {
            parametrizedCommand.addAll(Arrays.asList(args));
        }
        String[] params = new String[parametrizedCommand.size()];
        params = parametrizedCommand.toArray(params);

        ProcessBuilder pb = new ProcessBuilder(params);
        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);
        return pb.start();
    }
    public static Process runExec(String command) throws IOException {
        return runExec(command, null);
    }
}
