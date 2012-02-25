package automaticvariations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import javax.swing.JOptionPane;
import lev.Ln;
import lev.debug.LDebug;
import skyproc.BSA.FileType;
import skyproc.MajorRecord.Mask;
import skyproc.*;
import skyproc.exceptions.BadParameter;
import skyproc.exceptions.Uninitialized;

/**
 *
 * @author Leviathan1753
 */
public class AutomaticVariations {

    static private String header = "AV";
    static File avPackages = new File(SPGlobal.pathToData + "AV Packages/");
    static File avTextures = new File(SPGlobal.pathToData + "textures/AV Packages/");
    static File avMeshes = new File(SPGlobal.pathToData + "meshes/AV Packages/");
    // Load in
    static ArrayList<BSA> BSAs = new ArrayList<BSA>();
    // Nif path key
    static Map<String, AV_Nif> nifs = new HashMap<String, AV_Nif>();

    // Dup buffers
//    static ArrayList<Target> targets = new ArrayList<Target>();
//    static Map<FormID, RACE> races = new HashMap<FormID, RACE>();
//    static Map<FormID, ARMO> armors = new HashMap<FormID, ARMO>();
//    static Map<FormID, ARMA> armatures = new HashMap<FormID, ARMA>();
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
                    "Oh mai gawd!  Experience the rainbow.";

            /*
             * Initializing Debug Log and Globals
             */
            SPGlobal.createGlobalLog();
            SPGlobal.debugModMerge = false;
            SPGlobal.debugExportSummary = false;
            SPGlobal.debugBSAimport = false;
            SPGlobal.debugNIFimport = false;
            LDebug.timeElapsed = true;
            LDebug.timeStamp = true;
            // Turn Debugging off except for errors
            SPGlobal.logging(false);

            /*
             * Creating SkyProc Default GUI
             */
            // Create the SkyProc Default GUI
            SPDefaultGUI gui = new SPDefaultGUI(myPatcherName, myPatcherDescription);

            /*
             * Importing all Active Plugins
             */
            try {
                SPImporter importer = new SPImporter();
                Mask m = MajorRecord.getMask(Type.RACE);
                m.allow(Type.WNAM);
                importer.addMask(m);
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
             * Create your patch to export. (false means it is NOT an .esm file)
             */
            Mod source = new Mod("Temporary", false);
            source.addAsOverrides(SPGlobal.getDB());
            Mod patch = new Mod(myPatchName, false);
            SPGlobal.setGlobalPatch(patch);


            /*
             * ======================================= Your custom code begins
             * here. =======================================
             */



            SPGlobal.logging(true);

            BSAs = BSA.loadInBSAs(FileType.NIF);

            gatherFiles();

            ArrayList<VariantSet> variantRead = importVariants(patch);

            for (VariantSet v : variantRead) {
                linkToRecords(v);
            }

            for (AV_Nif n : nifs.values()) {
                n.generateVariants();
            }

//            distributeFiles();

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

    static void linkToRecords(VariantSet vars) {
        for (String s : vars.Target_FormIDs) {
            FormID id = new FormID(s);
            String header = id.toString();
            SPGlobal.log(header, "Linking: " + id.toString());
            try {

                NPC_ record = (NPC_) SPDatabase.getMajor(id, GRUP_TYPE.NPC_);
                if (record == null) {
                    SPGlobal.logError(header, "Could not locate NPC with FormID: " + s);
                    continue;
                } else if (SPGlobal.logging()) {
                    SPGlobal.log(header, "  " + record);
                }

                RACE race = (RACE) SPDatabase.getMajor(record.getRace(), GRUP_TYPE.RACE);
                if (race == null) {
                    SPGlobal.logError(header, "Could not locate RACE with FormID: " + record.getRace());
                    continue;
                } else if (SPGlobal.logging()) {
                    SPGlobal.log(header, "  " + race);
                }

                ARMO skin = (ARMO) SPDatabase.getMajor(race.getWornArmor(), GRUP_TYPE.ARMO);
                if (skin == null) {
                    SPGlobal.logError(header, "Could not locate ARMO with FormID: " + race.getWornArmor());
                    continue;
                } else if (SPGlobal.logging()) {
                    SPGlobal.log(header, "  " + skin);
                }

                if (skin.getArmatures().isEmpty()) {
                    SPGlobal.logError(header, skin + " did not have any armatures.");
                    continue;
                }
                ARMA piece = (ARMA) SPDatabase.getMajor(skin.getArmatures().get(0).getForm());
                if (piece == null) {
                    SPGlobal.logError(header, "Could not locate ARMA with FormID: " + skin.getArmatures().get(0).getForm());
                    continue;
                } else if (SPGlobal.logging()) {
                    SPGlobal.log(header, "  " + piece);
                }

                String nifPath = piece.getModelPath(Gender.MALE, Perspective.THIRD_PERSON);
                if (nifPath.equals("")) {
                    SPGlobal.logError(header, piece + " did not have a male third person model.");
                    continue;
                }
                if (!nifs.containsKey(nifPath)) {
                    AV_Nif textureFields = new AV_Nif();
                    textureFields.load(nifPath);

                    SPGlobal.log(header, "  Nif path: " + nifPath);
                    textureFields.print();

                    nifs.put(nifPath, textureFields);
                }


                nifs.get(nifPath).variants.addAll(vars.variants);

            } catch (BadParameter ex) {
                SPGlobal.logError(id.toString(), "Bad parameter passed to nif texture parser.");
                SPGlobal.logException(ex);
            } catch (FileNotFoundException ex) {
                SPGlobal.logError(id.toString(), "Could not find nif file.");
                SPGlobal.logException(ex);
            } catch (IOException ex) {
                SPGlobal.logError(id.toString(), "File IO error getting nif file.");
                SPGlobal.logException(ex);
            } catch (DataFormatException ex) {
                SPGlobal.logError(id.toString(), "BSA had a bad zipped file.");
                SPGlobal.logException(ex);
            }
        }
    }

    static ArrayList<VariantSet> importVariants(Mod patch) throws Uninitialized, FileNotFoundException {
        String header = "Import Variants";
        ArrayList<VariantSet> out = new ArrayList<VariantSet>();
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
        SPGlobal.log(header, "Importing variant set: " + variantFolder.getPath());
        ArrayList<Variant> variants = new ArrayList<Variant>();
        VariantSet varSet = new VariantSet();

        for (File variantFile : variantFolder.listFiles()) {  // Texture folders ("Grey Horker")
            if (variantFile.isFile() && variantFile.getName().toUpperCase().endsWith(".JSON")) {
                varSet = AVGlobal.parser.fromJson(new FileReader(variantFile), VariantSet.class);
                if (SPGlobal.logging()) {
                    SPGlobal.log(variantFile.getName(), "  Specifications loaded: ");
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
                            file = Ln.moveFile(file, new File(avTextures + file.getPath().substring(avPackages.getPath().length())), false);
                            variant.textures.add(file);
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

    static void gatherFiles() {
        ArrayList<File> files = Ln.generateFileList(avTextures, 3, 3, false);
        for (File file : files) {
            Ln.moveFile(file, new File(avPackages + file.getPath().substring(avTextures.getPath().length())), false);
        }
        files = Ln.generateFileList(avMeshes, 3, 3, false);
        for (File file : files) {
            Ln.moveFile(file, new File(avPackages + file.getPath().substring(avMeshes.getPath().length())), false);
        }
    }

    // Not used
    static void distributeFiles() {
        ArrayList<File> files = Ln.generateFileList(avPackages, 3, 3, false);
        for (File file : files) {
            if (file.getPath().endsWith(".dds")) {
                Ln.moveFile(file, new File(avTextures + file.getPath().substring(avPackages.getPath().length())), false);
            } else if (file.getPath().endsWith(".nif")) {
                Ln.moveFile(file, new File(avMeshes + file.getPath().substring(avPackages.getPath().length())), false);
            }
        }
    }
}
