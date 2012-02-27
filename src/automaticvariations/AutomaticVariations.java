package automaticvariations;

import automaticvariations.Variant.TextureVariant;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import javax.swing.JOptionPane;
import lev.Ln;
import lev.debug.LDebug;
import skyproc.BSA.FileType;
import skyproc.MajorRecord.Mask;
import skyproc.*;
import skyproc.ARMA.AltTexture;
import skyproc.LVLN.LVLO;
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
    // Nif path key
    static Map<String, AV_Nif> nifs = new HashMap<String, AV_Nif>();
    // Dup buffers
    static Map<FormID, LVLN> llists = new HashMap<FormID, LVLN>();
    static Map<FormID, ArrayList<NPC_>> npcs = new HashMap<FormID, ArrayList<NPC_>>();
    static Map<FormID, ArrayList<ARMO>> armors = new HashMap<FormID, ArrayList<ARMO>>();
    static Map<FormID, ArrayList<ARMA>> armatures = new HashMap<FormID, ArrayList<ARMA>>();
    static String[] EDIDexcludeArray = {"AUDIOTEMPLATE"};
    static ArrayList<String> EDIDexclude = new ArrayList<String>(Arrays.asList(EDIDexcludeArray));

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
//            SPGlobal.logging(false);

            /*
             * Creating SkyProc Default GUI
             */
            // Create the SkyProc Default GUI
            SPDefaultGUI gui = new SPDefaultGUI(myPatcherName, myPatcherDescription);


            Mod patch = new Mod(myPatchName, false);
            SPGlobal.setGlobalPatch(patch);

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
                        GRUP_TYPE.LVLN);
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


            /*
             * ======================================= Your custom code begins
             * here. =======================================
             */



            SPGlobal.logging(true);

            BSA.BSAs = BSA.loadInBSAs(FileType.NIF, FileType.DDS);

            gatherFiles();

            ArrayList<VariantSet> variantRead = importVariants(patch);

            // Locate and load NIFs, and assign their variants
            for (VariantSet v : variantRead) {
                linkToNif(v);
            }

            // Generate TXSTs
            for (AV_Nif n : nifs.values()) {
                n.generateVariantTXSTs();
            }

            // Generate ARMA dups that use TXSTs
            generateARMAvariants(source);

            // Generate ARMO dups that use ARMAs
            generateARMOvariants(source);
            printVariants();

            // Generate NPC_ dups that use ARMO skins
            generateNPCvariants(source);
            printNPCdups();

            // Load NPC_ dups into LVLNs
            generateLVLNs(source);

            // Replace original NPCs with LVLN replacements
            swapInLVLNs(source);

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

    static boolean checkNPCexclude(NPC_ npcSrc) {
        String edid = npcSrc.getEDID().toUpperCase();
        for (String exclude : EDIDexclude) {
            if (edid.contains(exclude)) {
                if (SPGlobal.logging()) {
                    SPGlobal.log(header, "  Skipping " + npcSrc + " on edid exclude : " + exclude);
                }
                return true;
            }
        }
        return false;
    }

    static void printNPCdups() {
        if (SPGlobal.logging()) {
            SPGlobal.log(header, "NPC dup summary: ");
            for (FormID form : npcs.keySet()) {
                SPGlobal.log(header, "  " + SPDatabase.getMajor(form));
            }
        }
    }

    static void printVariants() {
        if (SPGlobal.logging()) {
            SPGlobal.log(header, "Variants loaded: ");
            for (FormID srcArmor : armors.keySet()) {
                SPGlobal.log(header, "  Armor " + SPDatabase.getMajor(srcArmor) + " has " + armors.get(srcArmor).size() + " variants.");
                for (ARMO variant : armors.get(srcArmor)) {
                    SPGlobal.log(header, "    " + variant);
                }
            }
        }
    }

    static void swapInLVLNs(Mod source) {
        for (LVLN llist : source.getLeveledLists()) {
            boolean override = false;
            for (LVLO entry : llist) {
                LVLN replacement = llists.get(entry.getForm());
                if (replacement != null) {
                    if (SPGlobal.logging()) {
                        SPGlobal.log(header, "Replacing LList entry: " + SPDatabase.getMajor(entry.getForm()) + ", for LVLN: " + replacement.getEDID() + ", in LList: " + llist);
                    }
                    entry.setForm(replacement.getForm());
                    override = true;
                }
            }
            if (override) {
                SPGlobal.getGlobalPatch().addRecord(llist);
            }
        }
    }

    static void generateLVLNs(Mod source) {
        for (FormID srcNpc : npcs.keySet()) {
            LVLN llist = new LVLN(SPGlobal.getGlobalPatch());
            llist.setEDID("AV_" + source.getNPCs().get(srcNpc).getEDID() + "_llist");
            for (NPC_ n : npcs.get(srcNpc)) {
                llist.addEntry(new LVLO(n.getForm(), 1, 1));
            }
            llists.put(srcNpc, llist);
        }
    }

    static void generateNPCvariants(Mod source) {
        for (NPC_ npcSrc : source.getNPCs()) {

            // If it's pulling from a template, it adopts its template race. No need to dup
            if (!npcSrc.getTemplate().equals(FormID.NULL)) {
                continue;
            }

            // Locate if any variants are available
            FormID armorForm = npcSrc.getWornArmor();
            if (npcSrc.getWornArmor().equals(FormID.NULL)) {
                RACE race = (RACE) SPDatabase.getMajor(npcSrc.getRace());
                armorForm = race.getWornArmor();
            }
            ArrayList<ARMO> skinVariants = armors.get(armorForm);
            if (skinVariants != null) {

                if (checkNPCexclude(npcSrc)) {
                    continue;
                }


                if (SPGlobal.logging()) {
                    SPGlobal.log(header, "Duplicating " + npcSrc + ", for " + SPDatabase.getMajor(armorForm, GRUP_TYPE.ARMO));
                }
                ArrayList<NPC_> dups = new ArrayList<NPC_>(skinVariants.size());
                for (ARMO variant : skinVariants) {
                    NPC_ dup = (NPC_) SPGlobal.getGlobalPatch().makeCopy(npcSrc);
                    dup.setEDID(variant.getEDID().substring(0, variant.getEDID().length() - 5) + "_" + npcSrc.getEDID());

                    dup.setWornArmor(variant.getForm());
                    dups.add(dup);
                }
                npcs.put(npcSrc.getForm(), dups);
            }
        }
    }

    static void generateARMOvariants(Mod source) {
        for (ARMO armoSrc : source.getArmors()) {
            ArrayList<ARMA> variants = null;
            FormID target = null;
            for (SubForm arma : armoSrc.getArmatures()) {
                target = arma.getForm();
                variants = armatures.get(target);
                if (variants != null) {
                    break;
                }
            }

            if (variants != null) {
                if (SPGlobal.logging()) {
                    SPGlobal.log(header, "Duplicating " + armoSrc + ", for " + SPDatabase.getMajor(target, GRUP_TYPE.ARMA));
                }
                ArrayList<ARMO> dups = new ArrayList<ARMO>(variants.size());
                for (ARMA variant : variants) {
                    ARMO dup = (ARMO) SPGlobal.getGlobalPatch().makeCopy(armoSrc);
                    dup.setEDID(variant.getEDID().substring(0, variant.getEDID().length() - 5) + "_armo");

                    dup.removeArmature(target);
                    dup.addArmature(variant.getForm());
                    dups.add(dup);
                }
                armors.put(armoSrc.getForm(), dups);
            }
        }
    }

    static void generateARMAvariants(Mod source) {
        for (ARMA armaSrc : source.getArmatures()) {
            String modelPath = armaSrc.getModelPath(Gender.MALE, Perspective.THIRD_PERSON);
            AV_Nif malenif = nifs.get(modelPath.toUpperCase());
            if (malenif != null) { // we have variants for that nif
                if (SPGlobal.logging()) {
                    SPGlobal.log(header, "Duplicating " + armaSrc + ", for nif: " + modelPath);
                }
                ArrayList<ARMA> dups = new ArrayList<ARMA>();
                for (Variant v : malenif.variants) {
                    ARMA dup = (ARMA) SPGlobal.getGlobalPatch().makeCopy(armaSrc);
                    dup.setEDID(v.name + "_arma");

                    ArrayList<AltTexture> alts = dup.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON);
                    alts.clear();
                    int i = 0;
                    for (TextureVariant texVar : v.textureVariants) {
                        if (texVar != null) {
                            alts.add(new AltTexture(texVar.nifFieldName, texVar.textureRecord.getForm(), i));
                        }
                        i++;
                    }

                    ArrayList<AltTexture> femalealts = dup.getAltTextures(Gender.FEMALE, Perspective.THIRD_PERSON);
                    femalealts.clear();
                    femalealts.addAll(alts);

                    dups.add(dup);
                }
                armatures.put(armaSrc.getForm(), dups);
            }
        }
    }

    static void linkToNif(VariantSet vars) {
        for (String s : vars.Target_FormIDs) {
            FormID id = new FormID(s);
            String header = id.toString();
            SPGlobal.log(header, "Linking: " + id.toString());
            String nifPath = "...";
            try {

                NPC_ record = (NPC_) SPDatabase.getMajor(id, GRUP_TYPE.NPC_);
                if (record == null) {
                    SPGlobal.logError(header, "Could not locate NPC with FormID: " + s);
                    continue;
                } else if (SPGlobal.logging()) {
                    SPGlobal.log(header, "  " + record);
                }

                // NPC's skin field
                ARMO skin;
                skin = (ARMO) SPDatabase.getMajor(record.getWornArmor(), GRUP_TYPE.ARMO);

                if (skin == null) {
                    RACE race = (RACE) SPDatabase.getMajor(record.getRace(), GRUP_TYPE.RACE);
                    if (race == null) {
                        SPGlobal.logError(header, "Could not locate RACE with FormID: " + record.getRace());
                        continue;
                    } else if (SPGlobal.logging()) {
                        SPGlobal.log(header, "  " + race);
                    }

                    skin = (ARMO) SPDatabase.getMajor(race.getWornArmor(), GRUP_TYPE.ARMO);
                    if (skin == null) {
                        SPGlobal.logError(header, "Could not locate ARMO with FormID: " + race.getWornArmor());
                        continue;
                    } else if (SPGlobal.logging()) {
                        SPGlobal.log(header, "  " + skin);
                    }
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

                nifPath = piece.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).toUpperCase();
                if (nifPath.equals("")) {
                    SPGlobal.logError(header, piece + " did not have a male third person model.");
                    continue;
                }
                if (!nifs.containsKey(nifPath)) {
                    AV_Nif nif = new AV_Nif();
                    nif.load(nifPath);

                    SPGlobal.log(header, "  Nif path: " + nifPath);
                    nif.print();

                    nifs.put(nifPath, nif);
                }


                nifs.get(nifPath).variants.addAll(vars.variants);

            } catch (BadParameter ex) {
                SPGlobal.logError(id.toString(), "Bad parameter passed to nif texture parser: " + nifPath);
                SPGlobal.logException(ex);
            } catch (FileNotFoundException ex) {
                SPGlobal.logError(id.toString(), "Could not find nif file: " + nifPath);
                SPGlobal.logException(ex);
            } catch (IOException ex) {
                SPGlobal.logError(id.toString(), "File IO error getting nif file: " + nifPath);
                SPGlobal.logException(ex);
            } catch (DataFormatException ex) {
                SPGlobal.logError(id.toString(), "BSA had a bad zipped file: " + nifPath);
                SPGlobal.logException(ex);
            } catch (Exception e) {
                SPGlobal.logError(header, "Exception occured while loading nif: " + nifPath);
                SPGlobal.logException(e);
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
        ArrayList<String> commonTexturePaths = new ArrayList<String>();

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
            } else if (variantFile.isFile() && variantFile.getName().toUpperCase().endsWith(".DDS")) {
                commonTexturePaths.add(variantFile.getPath().substring(6));
                Ln.moveFile(variantFile, new File(avTextures + variantFile.getPath().substring(avPackages.getPath().length())), false);
                if (SPGlobal.logging()) {
                    SPGlobal.log(variantFile.getName(), "  Loaded common texture: " + variantFile.getPath());
                }
            } else if (variantFile.isDirectory()) {
                Variant variant = new Variant();
                for (File file : variantFile.listFiles()) {  // Files .dds, etc
                    if (file.isFile()) {
                        if (file.getName().endsWith(".dds")) {
                            variant.variantTexturePaths.add(file.getPath().substring(6));
                            variant.setName(file);
                            Ln.moveFile(file, new File(avTextures + file.getPath().substring(avPackages.getPath().length())), false);
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

        for (Variant v : variants) {
            for (String s : commonTexturePaths) {
                v.variantTexturePaths.add(s);
            }
        }

        varSet.variants.addAll(variants);
        return varSet;
    }

    static void gatherFiles() {
        ArrayList<File> files = Ln.generateFileList(avTextures, 2, 3, false);
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
