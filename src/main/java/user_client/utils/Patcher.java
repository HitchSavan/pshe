package user_client.utils;

import javax.swing.JLabel;

public class Patcher {

    public static void generatePatch(String oldFile, String newFile, String patchFile, JLabel updatingComponent) {
        RunCourgette courgetteInstance = new RunCourgette();
        String[] args = {"-gen", oldFile, newFile, patchFile};
        for (int k = 0; k < args.length; ++k) {
            System.out.print(args[k]);
            System.out.print("\t");
        }
        System.out.println();
        courgetteInstance.run(args, false, updatingComponent);
    }

    public static void applyPatch(String oldFile, String newFile, String patchFile, boolean replaceFiles, JLabel updatingComponent) {
        RunCourgette courgetteInstance = new RunCourgette();
        String[] args = {"-apply", oldFile, patchFile, newFile};
        for (int i = 0; i < args.length; ++i) {
            System.out.print(args[i]);
            System.out.print("\t");
        }
        System.out.println();
        courgetteInstance.run(args, replaceFiles, updatingComponent);
    }
}
