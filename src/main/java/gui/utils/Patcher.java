package gui.utils;

public class Patcher {

    public static void generatePatch(String oldFile, String newFile, String patchFile) {
        RunCourgette courgetteInstance = new RunCourgette();
        String[] args = {"-gen", oldFile, newFile, patchFile};
        for (int k = 0; k < args.length; ++k) {
            System.out.print(args[k]);
            System.out.print("\t");
        }
        System.out.println();
        courgetteInstance.run(args, false);
    }
}
