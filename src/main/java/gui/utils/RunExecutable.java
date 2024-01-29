package gui.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class RunExecutable {
    public static Process run(String command, String[] args) throws IOException {
        ArrayList<String> parametrizedCommand = new ArrayList<>();
        parametrizedCommand.add(command);
        
        Runtime rt = Runtime.getRuntime();
        if (args != null) {
            parametrizedCommand.addAll(Arrays.asList(args));
        }
        String[] params = new String[parametrizedCommand.size()];
        params = parametrizedCommand.toArray(params);
        return rt.exec(params); 
    }
    public static Process run(String command) throws IOException {
        return run(command, null);
    }
}
