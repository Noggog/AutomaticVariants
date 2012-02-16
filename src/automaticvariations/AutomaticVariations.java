package automaticvariations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JOptionPane;
import skyproc.ARMA;
import skyproc.ARMO;
import skyproc.GRUP_TYPE;
import skyproc.Mod;
import skyproc.NPC_;
import skyproc.SPDatabase;
import skyproc.SPDefaultGUI;
import skyproc.SPGlobal;
import skyproc.SPImporter;
import skyproc.exceptions.NotFound;
import skyproc.exceptions.Uninitialized;

/**
 *
 * @author Leviathan1753
 */
public class AutomaticVariations {

    static Map<ARMA, ARMA> ARMAs = new TreeMap<ARMA, ARMA>();
    static Map<ARMO, ARMO> ARMOs = new TreeMap<ARMO, ARMO>();
    static Map<NPC_, NPC_> NPCs = new TreeMap<NPC_, NPC_>();

    public static void main(String[] args) {

        try {
            /*
             * Custom names and descriptions
             */
            // Used to export a patch such as "My Patch.esp"
            String myPatchName = "Automatic Variations";
            // Used in the GUI as the title
            String myPatcherName = "Automatic Variations";
            // Used in the GUI as the description of what your patcher does
            String myPatcherDescription =
                    "I did not change the description,\n"
                    + "but I'm patching your setup to do something fantastic!";

            /*
             * Initializing Debug Log and Globals
             */
            // Go up two folders:  SkyprocPatchers/YourFolderName/
            SPGlobal.pathToData = "../../";
            SPGlobal.createGlobalLog();
            SPGlobal.debugModMerge = false;
            SPGlobal.debugExportSummary = false;
            // Turn Debugging off except for errors
//            SPGlobal.logging(false);

            /*
             * Creating SkyProc Default GUI
             */
            // Create the SkyProc Default GUI
            SPDefaultGUI gui = new SPDefaultGUI(myPatcherName, myPatcherDescription);

            /*
             * Importing all Active Plugins
             */
            try {
                // Import all Mods and all the GRUPs SkyProc currently supports
                SPImporter importer = new SPImporter();
//                Mask m = (new RACE()).getMask();
//                m.allow(Type.WNAM);
//                importer.addMask(m);
                importer.importActiveMods(
                        GRUP_TYPE.NPC_, GRUP_TYPE.RACE,
                        GRUP_TYPE.ARMO, GRUP_TYPE.ARMA,
                        GRUP_TYPE.TXST);
            } catch (IOException ex) {
                // If things go wrong, create an error box.
                JOptionPane.showMessageDialog(null, "There was an error importing plugins.\n(" + ex.getMessage() + ")\n\nPlease contact Leviathan1753.");
                System.exit(0);
            }

            /*
             * Create your patch to export.  (false means it is NOT an .esm file)
             */
            Mod source = new Mod("Temporary", false);
            Mod patch = new Mod(myPatchName, false);


            /*
             * =======================================
             *      Your custom code begins here.
             * =======================================
             */

            SPGlobal.logging(true);
            ArrayList<VariantSet> variants = importVariants(patch);




            /*
             * Close up shop.
             */
            try {
                // Export your custom patch.
                patch.export();
            } catch (IOException ex) {
                // If something goes wrong, show an error message.
                JOptionPane.showMessageDialog(null, "There was an error exporting the custom patch.\n(" + ex.getMessage() + ")\n\nPlease contact Leviathan1753.");
                System.exit(0);
            }
            // Tell the GUI to display "Done Patching"
            gui.finished();

        } catch (Exception e) {
            // If a major error happens, print it everywhere and display a message box.
            System.out.println(e.toString());
            SPGlobal.logException(e);
            JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution.  Check the debug logs.");
        }

        // Close debug logs before program exits.
        SPGlobal.closeDebug();
    }

    static ArrayList<VariantSet> importVariants(Mod patch) throws Uninitialized, FileNotFoundException {
        String header = "Import Variants";
        ArrayList<VariantSet> out = new ArrayList<VariantSet>();
        File avPackages = new File("AV Packages/");
        if (avPackages.isDirectory()) {
            for (File packageFolder : avPackages.listFiles()) { // Bellyaches Animals
                if (packageFolder.isDirectory()) {
                    for (File variantSet : packageFolder.listFiles()) { // Horker
                        if (variantSet.isDirectory()) {
                            VariantSet varSet = importVariantSet(variantSet, header);
                            if (!varSet.isEmpty()) {
                                out.add(varSet);
                            }
                        }
                    }
                }
            }
        } else {
            SPGlobal.logError("Package Location", "There was no AV Packages folder.");
        }
        return out;
    }

    static VariantSet importVariantSet(File variantFolder, String header) throws FileNotFoundException {
        ArrayList<Variant> variants = new ArrayList<Variant>();
        VariantSet varSet = new VariantSet();

        for (File variantFile : variantFolder.listFiles()) {  // Texture folders ("Grey Horker")
            if (variantFile.isFile() && variantFile.getName().toUpperCase().endsWith(".JSON")) {
                varSet = AVGlobal.parser.fromJson(new FileReader(variantFile), VariantSet.class);
                if (SPGlobal.logging()) {
                    SPGlobal.log(variantFile.getName(), "  Specifications loaded: ");
                    SPGlobal.log(variantFile.getName(), "    Type: " + varSet.Type);
                    SPGlobal.log(variantFile.getName(), "    Target FormIDs: ");
                    for (String s : varSet.Target_FormIDs) {
                        SPGlobal.log(variantFile.getName(), "      " + s);
                    }
                    SPGlobal.log(variantFile.getName(), "    Apply to Similar: " + varSet.Apply_To_Similar);
                }
            } else if (variantFile.isDirectory()) {
                Variant variant = new Variant();
                for (File file : variantFile.listFiles()) {  // Files .dds, etc
                    if (file.isFile()) {
                        if (file.getName().endsWith(".dds")) {
                            variant.texture = file;
                            if (SPGlobal.logging()) {
                                SPGlobal.log(variantFile.getName(), "  Loaded texture: " + file.getPath());
                            }
                        }
                    }
                }
                if (!variant.isEmpty()) {
                    variants.add(variant);
                }
            }
        }

        varSet.variants.addAll(variants);
        return varSet;
    }
}
