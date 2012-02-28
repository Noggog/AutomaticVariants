package automaticvariations;

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
    static File avPackages = new File("AV Packages/");
    static File avTextures = new File(SPGlobal.pathToData + "textures/AV Packages/");
    static File avMeshes = new File(SPGlobal.pathToData + "meshes/AV Packages/");
    // Nif path is key
    static Map<String, AV_Nif> nifs = new HashMap<String, AV_Nif>();
    //srcNPC is key
    static Map<FormID, LVLN> llists = new HashMap<FormID, LVLN>();
    static Map<FormID, ArrayList<NPC_spec>> npcs = new HashMap<FormID, ArrayList<NPC_spec>>();
    static Map<FormID, ArrayList<ARMO_spec>> armors = new HashMap<FormID, ArrayList<ARMO_spec>>();
    static Map<FormID, ArrayList<ARMA_spec>> armatures = new HashMap<FormID, ArrayList<ARMA_spec>>();
    static final String[] EDIDexcludeArray = {"AUDIOTEMPLATE"};
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
		    "Oh mai gawd!";

	    setGlobals();

	    /*
	     * Creating SkyProc Default GUI
	     */
	    // Create the SkyProc Default GUI
	    SPDefaultGUI gui = new SPDefaultGUI(myPatcherName, myPatcherDescription);


	    Mod patch = new Mod(myPatchName, false);
	    patch.setFlag(Mod.Mod_Flags.STRING_TABLED, false);
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
	    generateTXSTvariants();

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

	    // Apply template routing from original NPCs to new LLists
	    generateTemplating(source);

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

    private static void setGlobals() {
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
    }

    static void generateTemplating(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Setting source NPCs to template to LVLN loaded with variants");
	    SPGlobal.log(header, "====================================================================");
	}
	for (NPC_ npcSrc : source.getNPCs()) {
	    FormID npcForm = npcSrc.getForm();
	    if (llists.containsKey(npcForm)) {
		npcSrc.setTemplate(llists.get(npcForm).getForm());
		npcSrc.setTemplateFlag(NPC_.TemplateFlag.USE_TRAITS, true);
		SPGlobal.getGlobalPatch().addRecord(npcSrc);
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Templating " + npcSrc + " with " + llists.get(npcForm));
		}
	    }
	}
    }

    static boolean checkNPCexclude(NPC_ npcSrc) {
	String edid = npcSrc.getEDID().toUpperCase();
	for (String exclude : EDIDexclude) {
	    if (edid.contains(exclude)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "    Skipping " + npcSrc + " on edid exclude : " + exclude);
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
		for (ARMO_spec variant : armors.get(srcArmor)) {
		    SPGlobal.log(header, "    " + variant.armo + ", prob divider: 1/" + variant.probDiv);
		}
	    }
	}
    }

    static void generateLVLNs(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating LVLNs loaded with NPC variants.");
	    SPGlobal.log(header, "====================================================================");
	}
	for (FormID srcNpc : npcs.keySet()) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "Generating for " + SPDatabase.getMajor(srcNpc));
	    }

	    LVLN llist = new LVLN(SPGlobal.getGlobalPatch(), "AV_" + source.getNPCs().get(srcNpc).getEDID() + "_llist");
	    ArrayList<NPC_spec> npcVars = npcs.get(srcNpc);
	    int[] divs = new int[npcVars.size()];
	    for (int i = 0; i < divs.length; i++) {
		divs[i] = npcVars.get(i).probDiv;
	    }
//	    int lowestCommMult = Ln.lcmm(divs);
	    int lowestCommMult = 1;

	    for (NPC_spec n : npcVars) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "  Generating " + (lowestCommMult / n.probDiv) + " entries for " + n.npc);
		}
//		for (int i = 0; i < lowestCommMult / n.probDiv; i++) {
		llist.addEntry(new LVLO(n.npc.getForm(), 1, 1));
//		}
	    }
	    llists.put(srcNpc, llist);

	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "--------------------------------------------------");
	    }
	}
    }

    static void generateNPCvariants(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating NPC duplicates for each ARMO skin");
	    SPGlobal.log(header, "====================================================================");
	}
	boolean last = false;
	for (NPC_ npcSrc : source.getNPCs()) {
	    
	    // If it's pulling from a template, it adopts its template race. No need to dup
	    if (!npcSrc.getTemplate().equals(FormID.NULL) && npcSrc.getTemplateFlag(NPC_.TemplateFlag.USE_TRAITS)) {
		if (SPGlobal.logging()) {
		    if (last) {
			SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
			last = false;
		    }
		    SPGlobal.log(header, "    Skipping because of template : " + npcSrc);
		}
		continue;
	    }

	    // Locate if any variants are available
	    FormID armorForm = npcSrc.getWornArmor();
	    if (npcSrc.getWornArmor().equals(FormID.NULL)) {
		RACE race = (RACE) SPDatabase.getMajor(npcSrc.getRace());
		armorForm = race.getWornArmor();
	    }
	    ArrayList<ARMO_spec> skinVariants = armors.get(armorForm);
	    if (skinVariants != null) {

		if (checkNPCexclude(npcSrc)) {
		    continue;
		}

		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		    SPGlobal.log(header, "| Duplicating " + npcSrc + ", for " + SPDatabase.getMajor(armorForm, GRUP_TYPE.ARMO));
		    last = true;
		}
		ArrayList<NPC_spec> dups = new ArrayList<NPC_spec>(skinVariants.size());
		for (ARMO_spec variant : skinVariants) {
		    NPC_ dup = (NPC_) SPGlobal.getGlobalPatch().makeCopy(npcSrc, variant.armo.getEDID().substring(0, variant.armo.getEDID().length() - 5) + "_" + npcSrc.getEDID());

		    dup.setWornArmor(variant.armo.getForm());
		    dups.add(new NPC_spec(dup, variant));
		}
		npcs.put(npcSrc.getForm(), dups);
	    }
	}
    }

    static void generateARMOvariants(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating ARMO skin duplicates for each ARMA");
	    SPGlobal.log(header, "====================================================================");
	}
	for (ARMO armoSrc : source.getArmors()) {
	    ArrayList<ARMA_spec> variants = null;
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
		ArrayList<ARMO_spec> dups = new ArrayList<ARMO_spec>(variants.size());
		for (ARMA_spec variant : variants) {
		    ARMO dup = (ARMO) SPGlobal.getGlobalPatch().makeCopy(armoSrc, variant.arma.getEDID().substring(0, variant.arma.getEDID().length() - 5) + "_armo");

		    dup.removeArmature(target);
		    dup.addArmature(variant.arma.getForm());
		    dups.add(new ARMO_spec(dup, variant));
		}
		armors.put(armoSrc.getForm(), dups);
	    }
	}
    }

    static void generateARMAvariants(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating ARMA duplicates for each NIF");
	    SPGlobal.log(header, "====================================================================");
	}
	for (ARMA armaSrc : source.getArmatures()) {
	    String modelPath = armaSrc.getModelPath(Gender.MALE, Perspective.THIRD_PERSON);
	    AV_Nif malenif = nifs.get(modelPath.toUpperCase());
	    if (malenif != null) { // we have variants for that nif
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Duplicating " + armaSrc + ", for nif: " + modelPath);
		}
		ArrayList<ARMA_spec> dups = new ArrayList<ARMA_spec>();
		for (Variant v : malenif.variants) {
		    ARMA dup = (ARMA) SPGlobal.getGlobalPatch().makeCopy(armaSrc, v.name + "_arma");

		    ArrayList<AltTexture> alts = dup.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON);
		    alts.clear();
		    int i = 0;
		    for (Variant.TextureVariant texVar : v.textureVariants) {
			if (texVar != null) {
			    alts.add(new AltTexture(texVar.nifFieldName, texVar.textureRecord.getForm(), i));
			}
			i++;
		    }

		    ArrayList<AltTexture> femalealts = dup.getAltTextures(Gender.FEMALE, Perspective.THIRD_PERSON);
		    femalealts.clear();
		    femalealts.addAll(alts);

		    dups.add(new ARMA_spec(dup, v.specs));
		}
		armatures.put(armaSrc.getForm(), dups);
	    }
	}
    }
    
    static void generateVariant(Variant v, ArrayList<AV_Nif.TextureField> texturePack) throws IOException {

	// Find out which TXSTs need to be generated
	String[][] replacements = new String[texturePack.size()][Variant.numSupportedTextures];
	boolean[] needed = new boolean[texturePack.size()];
	for (String s : v.variantTexturePaths) {
	    String fileName = s;
	    fileName = fileName.substring(fileName.lastIndexOf('\\'));
	    int i = 0;
	    for (AV_Nif.TextureField textureSet : texturePack) {
		int j = 0;
		for (String texture : textureSet.maps) {
		    if (!texture.equals("") && texture.lastIndexOf('\\') != -1) {
			String textureName = texture.substring(texture.lastIndexOf('\\'));
			if (textureName.equalsIgnoreCase(fileName)) {
			    replacements[i][j] = s;
			    needed[i] = true;
			}
		    }
		    if (j == Variant.numSupportedTextures - 1) {
			break;
		    } else {
			j++;
		    }
		}
		i++;
	    }
	}

	// Make new TXSTs
	v.textureVariants = new Variant.TextureVariant[texturePack.size()];
	int i = 0;
	for (AV_Nif.TextureField textureSet : texturePack) {
	    if (needed[i]) {
		// New TXST
		TXST tmpTXST = new TXST(SPGlobal.getGlobalPatch(), v.name + "_txst");
		tmpTXST.setFlag(TXST.TXSTflag.FACEGEN_TEXTURES, true);

		// Set maps
		int j = 0;
		for (String texture : textureSet.maps) {

		    // Because nif fields map 2->3 if facegen flag is on.
		    int set = j;
		    if (set == 2) {
			set = 3;
		    }

		    if (replacements[i][j] != null) {
			tmpTXST.setNthMap(set, replacements[i][j]);
			if (SPGlobal.logging()) {
			    SPGlobal.log("Variant", "  Replaced set " + i + ", texture " + j + " with " + replacements[i][j] + " on variant " + v.name);
			}
		    } else if (!"".equals(texture)) {
			tmpTXST.setNthMap(set, texture.substring(texture.indexOf('\\') + 1));
		    }
		    if (j == Variant.numSupportedTextures - 1) {
			break;
		    } else {
			j++;
		    }
		}
		v.textureVariants[i] = new Variant.TextureVariant(tmpTXST, textureSet.title);
	    }
	    i++;
	}

	v.variantTexturePaths = null; // Free up space
    }

    static void generateTXSTvariants() throws IOException {
	for (AV_Nif n : nifs.values()) {

	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "====================================================================");
		SPGlobal.log(header, "Generating TXST records for Nif: " + n.path);
		SPGlobal.log(header, "====================================================================");
	    }
	    for (Variant v : n.variants) {
		generateVariant(v, n.textureFields);
	    }
	    n.textureFields = null; // Not needed anymore
	}
    }

    static void linkToNif(VariantSet vars) {
	for (String s : vars.Target_FormIDs) {
	    FormID id = new FormID(s);
	    String header = id.toString();
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "====================================================================");
		SPGlobal.log(header, "Locating " + id.toString() + "'s NIF file and analyzing it.");
		SPGlobal.log(header, "====================================================================");
	    }
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
		    AV_Nif nif = new AV_Nif(nifPath);
		    nif.load();

		    SPGlobal.log(header, "  Nif path: " + nifPath);
		    nif.print();

		    nifs.put(nifPath, nif);
		} else if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Already a nif with that path.");
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
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "======================================================================");
	    SPGlobal.log(header, "Importing variant set: " + variantFolder.getPath());
	    SPGlobal.log(header, "======================================================================");
	}
	ArrayList<Variant> variants = new ArrayList<Variant>();
	VariantSet varSet = new VariantSet();
	ArrayList<File> commonTexturePaths = new ArrayList<File>();

	for (File variantFile : variantFolder.listFiles()) {  // Texture folders ("Grey Horker")
	    if (variantFile.isFile() && variantFile.getName().toUpperCase().endsWith(".JSON")) {
		varSet = AVGlobal.parser.fromJson(new FileReader(variantFile), VariantSet.class);
		if (SPGlobal.logging()) {
		    SPGlobal.log(variantFile.getName(), "  General Specifications loaded: ");
		    SPGlobal.log(variantFile.getName(), "    Target FormIDs: ");
		    for (String s : varSet.Target_FormIDs) {
			SPGlobal.log(variantFile.getName(), "      " + s);
		    }
		    SPGlobal.log(variantFile.getName(), "    Apply to Similar: " + varSet.Apply_To_Similar);
		}
	    } else if (variantFile.isFile() && variantFile.getName().toUpperCase().endsWith(".DDS")) {
		commonTexturePaths.add(variantFile);
		Ln.moveFile(variantFile, new File(avTextures + variantFile.getPath().substring(avPackages.getPath().length())), false);
		if (SPGlobal.logging()) {
		    SPGlobal.log(variantFile.getName(), "  Loaded common texture: " + variantFile.getPath());
		}
	    } else if (variantFile.isDirectory()) {
		Variant variant = new Variant();
		variant.setName(variantFile);
		for (File file : variantFile.listFiles()) {  // Files .dds, etc
		    if (file.isFile()) {
			if (file.getName().endsWith(".dds")) {
			    variant.variantTexturePaths.add(file.getPath());
			    Ln.moveFile(file, new File(avTextures + file.getPath().substring(avPackages.getPath().length())), false);
			    if (SPGlobal.logging()) {
				SPGlobal.log(variantFile.getName(), "  Loaded texture: " + file.getPath());
			    }
			} else if (file.getName().endsWith(".json")) {
			    variant.specs = AVGlobal.parser.fromJson(new FileReader(file), VariantSpec.class);
			    variant.specs.print();
			}
		    }
		}
		if (!variant.isEmpty()) {
		    variants.add(variant);
		}
	    } else if (SPGlobal.logging()) {
		SPGlobal.log(header, "  Skipped file: " + variantFile);
	    }
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "  ----------------------------------------------------------");
	    }
	}

	for (Variant v : variants) {
	    for (File f : commonTexturePaths) {
		boolean skip = false;
		for (String s : v.variantTexturePaths) {
		    if (s.toUpperCase().contains(f.getName().toUpperCase())) {
			skip = true;
			break;
		    }
		}
		if (!skip) {
		    v.variantTexturePaths.add(f.getPath());
		}
	    }
	}

	varSet.variants.addAll(variants);
	return varSet;
    }

    static void gatherFiles() {
	ArrayList<File> files = Ln.generateFileList(avTextures, 2, 3, false);
	for (File file : files) {
	    Ln.moveFile(file, new File(avPackages + file.getPath().substring(avTextures.getPath().length())), false);
	    if (file.exists()) {
		file.delete();
	    }
	}
	files = Ln.generateFileList(avMeshes, 3, 3, false);
	for (File file : files) {
	    Ln.moveFile(file, new File(avPackages + file.getPath().substring(avMeshes.getPath().length())), false);
	    if (file.exists()) {
		file.delete();
	    }
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
