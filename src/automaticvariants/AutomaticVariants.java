package automaticvariants;

import java.io.*;
import java.util.*;
import java.util.zip.DataFormatException;
import javax.swing.JOptionPane;
import lev.LMergeMap;
import lev.Ln;
import lev.debug.LDebug;
import skyproc.ARMA.AltTexture;
import skyproc.BSA.FileType;
import skyproc.*;
import skyproc.exceptions.BadParameter;
import skyproc.exceptions.Uninitialized;

/**
 *
 * @author Leviathan1753
 */
public class AutomaticVariants {

    /*
     * Static Strings
     */
    static private String header = "AV";
    static File avPackages = new File("AV Packages/");
    static File avTextures = new File(SPGlobal.pathToData + "textures/AV Packages/");
    static File avMeshes = new File(SPGlobal.pathToData + "meshes/AV Packages/");
    static String changeRaceScript = "AVchangeRace";
    static String changeRaceFormList = "RaceOptions";
    static String changeRaceBoundWeapons = "BoundWeapons";
    static String changeRaceBoundWeaponBuffer = "BoundBuffer";
    /*
     * Variant storage lists/maps
     */
    static ArrayList<BSA> BSAs;
    // AV_Nif name is key
    static Map<String, AV_Nif> nifs = new HashMap<String, AV_Nif>();
    // ArmoSrc is key
    static Map<FormID, MGEF> magicEffects = new HashMap<FormID, MGEF>();
    static Map<FormID, FLST> formLists = new HashMap<FormID, FLST>();
    static LMergeMap<FormID, RACE_spec> races = new LMergeMap<FormID, RACE_spec>(false);
    static LMergeMap<FormID, ARMO_spec> armors = new LMergeMap<FormID, ARMO_spec>(false);
    // ArmaSrc is key
    static LMergeMap<FormID, ARMA_spec> armatures = new LMergeMap<FormID, ARMA_spec>(false);
    static Map<FormID, String> armaToNif = new HashMap<FormID, String>();
    /*
     * Exception lists
     */
    static Set<FormID> block = new HashSet<FormID>();
    static Set<String> edidExclude = new HashSet<String>();
    /*
     * Other
     */
    static FLST boundList;
    static FLST boundBuffer;
    static String extraPath = "";
    static int numSteps = 9;
    static int step = 0;
    static int debugLevel = 1;

    public static void main(String[] args) {
	ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(args));
	try {
	    if (handleArgs(arguments)) {
		SPGlobal.closeDebug();
		return;
	    }
	    setGlobals();
	    SPDefaultGUI gui = createGUI();

	    Mod patch = new Mod("Automatic Variants", false);
	    patch.setFlag(Mod.Mod_Flags.STRING_TABLED, false);
	    SPGlobal.setGlobalPatch(patch);

	    readInExceptions();

	    importMods();

	    SPGUI.progress.setMax(numSteps);
	    SPGUI.progress.setStatus(step++, numSteps, "Initializing AV");
	    Mod source = new Mod("Temporary", false);
	    source.addAsOverrides(SPGlobal.getDB());
	    if (debugLevel >= 1) {
		SPGlobal.logging(true);
	    }
	    BSAs = BSA.loadInBSAs(FileType.NIF, FileType.DDS);

	    SPGUI.progress.setStatus(step++, numSteps, "Importing AV Packages");
	    gatherFiles();
	    ArrayList<VariantSet> variantRead = importVariants(patch);
	    SPGUI.progress.incrementBar();

	    // Locate and load NIFs, and assign their variants
	    linkToNifs(variantRead);

	    // Generate TXSTs
	    generateTXSTvariants();

	    // Generate ARMA dups that use TXSTs
	    generateARMAvariants(source);

	    // Generate ARMO dups that use ARMAs
	    generateARMOvariants(source);

	    // Generate RACE dups that use ARMOs
	    generateRACEvariants(source);

	    // Generate FormLists of RACE variants
	    generateFormLists(source);

	    // Generate FormList of bound weapons, for race switch script
	    locateBoundWeapons(source);

	    // Generate Magic Effect with script pointing to FormList variants
	    generateMGEFvariants();

	    // Generate Spells that use Race Switcher Magic Effects
	    generateSPELvariants();

	    // Add variant scripts to NPCs
	    addScripts(source);

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
	    System.err.println(e.toString());
	    SPGlobal.logException(e);
	    JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution: '" + e + "'  Check the debug logs.");
	}

	// Close debug logs before program exits.
	SPGlobal.closeDebug();
    }

    static boolean handleArgs(ArrayList<String> arguments) {
	String debug = "-debug";
	String extraPth = "-extraPath";
	for (String s : arguments) {
	    if (s.contains(debug)) {
		s = s.substring(s.indexOf(debug) + debug.length()).trim();
		try {
		    debugLevel = Integer.valueOf(s);
		    SPGlobal.logMain(header, "Debug level set to: " + debugLevel);
		} catch (NumberFormatException e) {
		    SPGlobal.logError(header, "Error parsing the debug level: '" + s + "'");
		}
	    } else if (s.contains(extraPth)) {
		s = s.substring(s.indexOf(extraPth) + extraPth.length()).trim();
		extraPath = s;
		SPGlobal.pathToData = extraPath + SPGlobal.pathToData;
		avPackages = new File(extraPath + avPackages.getPath());
		avMeshes = new File(extraPath + avMeshes.getPath());
		avTextures = new File(extraPath + avTextures.getPath());
		if (SPGlobal.logging()) {
		    SPGlobal.logMain(header, "Extra Path set to: " + extraPath);
		    SPGlobal.logMain(header, "Path to data: " + SPGlobal.pathToData);
		}
	    }

	}

	if (arguments.contains("-gather")) {
	    gatherFiles();
	    return true;
	}

	return false;
    }

    static boolean checkNPCskip(NPC_ npcSrc, boolean last) {
	String edid = npcSrc.getEDID().toUpperCase();
	for (String exclude : edidExclude) {
//	    if (!npcSrc.getTemplate().equals(FormID.NULL) && npcSrc.get(NPC_.TemplateFlag.USE_TRAITS)) {
//		if (SPGlobal.logging()) {
//		    if (last) {
//			SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
//		    }
//		    SPGlobal.log(header, "    Skipping " + npcSrc + " : Template with traits flag");
//		}
//		return true;
//	    } else
	    if (edid.contains(exclude)) {
		if (SPGlobal.logging()) {
		    if (last) {
			SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		    }
		    SPGlobal.log(header, "    Skipping " + npcSrc + " : edid exclude '" + exclude + "'");
		}
		return true;
	    }
	}
	return false;
    }

    static void importMods() {
	try {
	    SPImporter importer = new SPImporter();
	    importer.importActiveMods(
		    GRUP_TYPE.NPC_, GRUP_TYPE.RACE,
		    GRUP_TYPE.ARMO, GRUP_TYPE.ARMA,
		    GRUP_TYPE.LVLN, GRUP_TYPE.TXST,
		    GRUP_TYPE.WEAP);
	} catch (IOException ex) {
	    // If things go wrong, create an error box.
	    JOptionPane.showMessageDialog(null, "There was an error importing plugins.\n(" + ex.getMessage() + ")\n\nPlease contact Leviathan1753.");
	    System.exit(0);
	}
    }

    static int readjustTXSTindices(int j) {
	// Because nif fields map 2->3 if facegen flag is on.
	int set = j;
	if (set == 2) {
	    set = 3;
	}
	return set;
    }

    private static void setGlobals() {
	/*
	 * Initializing Debug Log and Globals
	 */
	if (debugLevel > 0) {
	    SPGlobal.createGlobalLog(extraPath);
	    SPGlobal.debugModMerge = false;
	    SPGlobal.debugExportSummary = false;
	    SPGlobal.debugBSAimport = false;
	    SPGlobal.debugNIFimport = false;
	    LDebug.timeElapsed = true;
	    LDebug.timeStamp = true;
	    // Turn Debugging off except for errors
	    if (debugLevel < 2) {
		SPGlobal.logging(false);
	    }
	}
    }

    static void locateBoundWeapons(Mod source) {
	SPGUI.progress.setStatus(step++, numSteps, "Locating bound weapons.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Locating Bound Weapons");
	    SPGlobal.log(header, "====================================================================");
	}
	boundList = new FLST(SPGlobal.getGlobalPatch(), "AV_BoundWeaponsList");
	for (WEAP weap : source.getWeapons()) {
	    if (weap.get(WEAP.WeaponFlag.BoundWeapon)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "  Added: " + weap);
		}
		boundList.addFormEntry(weap.getForm());
	    }
	}
	boundBuffer = new FLST(SPGlobal.getGlobalPatch(), "AV_BoundWeaponsBuffer");
    }

    static void addScripts(Mod source) {
	SPGUI.progress.setStatus(step++, numSteps, "Attaching Variant scripts to NPCs.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Attaching Variant scripts to NPCs");
	    SPGlobal.log(header, "====================================================================");
	}
	boolean last = false;
	for (NPC_ npcSrc : source.getNPCs()) {

	    // Locate if any variants are available
	    FormID armorForm = npcSrc.getWornArmor();
	    if (npcSrc.getWornArmor().equals(FormID.NULL)) {
		RACE race = (RACE) SPDatabase.getMajor(npcSrc.getRace());
		if (race == null) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "Skipping " + npcSrc + " : did not have a worn armor or race.");
		    }
		    continue;
		}
		armorForm = race.getWornArmor();
	    }
	    FLST varList = formLists.get(armorForm);
	    if (varList != null) {

		if (checkNPCskip(npcSrc, last)) {
		    last = false;
		    continue;
		}

		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		    SPGlobal.log(header, "| Scripting " + npcSrc + ", for " + SPDatabase.getMajor(armorForm, GRUP_TYPE.ARMO));
		    last = true;
		}

		npcSrc.scripts.addScript(changeRaceScript);
		npcSrc.scripts.setProperty(changeRaceScript, changeRaceFormList, varList.getForm());
		npcSrc.scripts.setProperty(changeRaceScript, changeRaceBoundWeapons, boundList.getForm());
		npcSrc.scripts.setProperty(changeRaceScript, changeRaceBoundWeaponBuffer, boundBuffer.getForm());

		// If has special skin, remove it, and confirm races match
		if (!npcSrc.getWornArmor().equals(FormID.NULL)) {
		    ARMO specialSkin = (ARMO) SPDatabase.getMajor(npcSrc.getWornArmor(), GRUP_TYPE.ARMO);
		    npcSrc.setWornArmor(FormID.NULL);
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "| Had special skin " + specialSkin + ".  Removed.");
		    }
		}

		SPGlobal.getGlobalPatch().addRecord(npcSrc);
	    }
	}
	SPGUI.progress.incrementBar();
    }

    static void generateSPELvariants() {
	for (FormID armoSrc : magicEffects.keySet()) {
	    MGEF mgef = magicEffects.get(armoSrc);
	    String name = mgef.getEDID().substring(0, mgef.getEDID().lastIndexOf("_mgef")) + "_spell";
	    SPEL spel = new SPEL(SPGlobal.getGlobalPatch(), name);
	}
    }

    static void generateMGEFvariants() {
	for (FormID armoSrc : formLists.keySet()) {
	    FLST flst = formLists.get(armoSrc);
	    String name = flst.getEDID().substring(flst.getEDID().indexOf("AV_") + 3, flst.getEDID().lastIndexOf("_flst")) + "_mgef";
	    MGEF mgef = new MGEF(SPGlobal.getGlobalPatch(), "AV_" + name, "AV_SwitchRace_" + name);
	    mgef.scripts.addScript(changeRaceScript);
	    mgef.scripts.setProperty(changeRaceScript, changeRaceBoundWeaponBuffer, boundBuffer.getForm());
	    mgef.scripts.setProperty(changeRaceScript, changeRaceBoundWeapons, boundList.getForm());
	    mgef.scripts.setProperty(changeRaceScript, changeRaceFormList, flst.getForm());
	    magicEffects.put(armoSrc, mgef);
	}
    }

    static void generateFormLists(Mod source) {
	SPGUI.progress.setStatus(step++, numSteps, "Generating Form Lists.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating FormLists for each RACE variant");
	    SPGlobal.log(header, "====================================================================");
	}
	for (FormID armoSrcForm : races.keySet()) {
	    ARMO armoSrc = (ARMO) SPDatabase.getMajor(armoSrcForm, GRUP_TYPE.ARMO);
	    FLST flst = new FLST(SPGlobal.getGlobalPatch(), "AV_" + armoSrc.getEDID() + "_flst");
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "  Generating FLST " + flst);
	    }

	    ArrayList<RACE_spec> raceVars = races.get(armoSrcForm);
	    int[] divs = new int[raceVars.size()];
	    for (int i = 0; i < divs.length; i++) {
		divs[i] = raceVars.get(i).probDiv;
	    }
	    int lowestCommMult = Ln.lcmm(divs);

	    for (RACE_spec raceSpec : races.get(armoSrcForm)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "    Generating " + (lowestCommMult / raceSpec.probDiv) + " entries for " + raceSpec.race);
		}
		for (int i = 0; i < lowestCommMult / raceSpec.probDiv; i++) {
		    flst.addFormEntry(raceSpec.race.getForm());
		}
	    }
	    formLists.put(armoSrcForm, flst);
	}
    }

    static void generateRACEvariants(Mod source) {
	SPGUI.progress.setStatus(step++, numSteps, "Generating RACE variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating RACE duplicates for each ARMO variant");
	    SPGlobal.log(header, "====================================================================");
	}
	for (FormID armoSrcID : armors.keySet()) {
	    ARMO armoSrc = (ARMO) SPDatabase.getMajor(armoSrcID, GRUP_TYPE.ARMO);
	    RACE raceSrc = null;
	    if (!armoSrc.getRace().equals(FormID.NULL)) {
		raceSrc = (RACE) SPDatabase.getMajor(armoSrc.getRace(), GRUP_TYPE.RACE);
	    } else if (SPGlobal.logging()) {
		SPGlobal.log(header, "  Dup ARMO " + armoSrc + " had no race.  Skipping.");
		continue;
	    }

	    if (raceSrc == null) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "  Dup ARMO " + armoSrc + " had a formID that was not a race: " + armoSrc.getRace());
		}
		continue;
	    } else if (SPGlobal.logging()) {
		SPGlobal.log(header, "  Duplicating " + raceSrc + ", for " + armoSrc);
	    }

	    ArrayList<RACE_spec> dups = new ArrayList<RACE_spec>(armors.get(armoSrcID).size());
	    for (ARMO_spec armoDupSpec : armors.get(armoSrcID)) {
		ARMO armoDup = armoDupSpec.armo;
		ARMA armaDup = armoDupSpec.targetArma;
		RACE raceDup = (RACE) SPGlobal.getGlobalPatch().makeCopy(raceSrc, armoDup.getEDID().substring(0, armoDup.getEDID().lastIndexOf("_armo")) + "_race");
		raceDup.setWornArmor(armoDup.getForm());
		armoDup.setRace(raceDup.getForm());
		armaDup.setRace(raceDup.getForm());
		dups.add(new RACE_spec(raceDup, armoDupSpec));
	    }
	    races.put(armoSrc.getForm(), dups);
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

    static void generateARMOvariants(Mod source) {
	SPGUI.progress.setStatus(step++, numSteps, "Generating ARMO variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating ARMO skin duplicates for each ARMA");
	    SPGlobal.log(header, "====================================================================");
	}
	for (ARMO armoSrc : source.getArmors()) {
	    ArrayList<ARMA_spec> variants = null;
	    FormID target = null;
	    for (FormID armaForm : armoSrc.getArmatures()) {
		ARMA arma = (ARMA) SPDatabase.getMajor(armaForm);
		if (arma != null && arma.getRace().equals(armoSrc.getRace())) {
		    target = armaForm;
		    variants = armatures.get(target);
		    if (variants != null) {
			break;
		    }
		}
	    }

	    if (variants != null) {
		if (block.contains(armoSrc.getForm())) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "Skipping " + armoSrc + " because it is on the block list");
		    }
		    continue;
		} else if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Duplicating " + armoSrc + ", for " + SPDatabase.getMajor(target, GRUP_TYPE.ARMA));
		}
		ArrayList<ARMO_spec> dups = new ArrayList<ARMO_spec>(variants.size());
		for (ARMA_spec variant : variants) {
		    ARMO dup = (ARMO) SPGlobal.getGlobalPatch().makeCopy(armoSrc, variant.arma.getEDID().substring(0, variant.arma.getEDID().lastIndexOf("_arma")) + "_armo");

		    dup.removeArmature(target);
		    dup.addArmature(variant.arma.getForm());
		    dups.add(new ARMO_spec(dup, variant));
		}
		armors.put(armoSrc.getForm(), dups);
	    }
	}
	printVariants();
	SPGUI.progress.incrementBar();
    }

    static void generateARMAvariants(Mod source) {
	SPGUI.progress.setStatus(step++, numSteps, "Generating ARMA variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating ARMA duplicates for each NIF");
	    SPGlobal.log(header, "====================================================================");
	}
	for (ARMA armaSrc : source.getArmatures()) {
	    // If we have variants for it
	    if (armaToNif.containsKey(armaSrc.getForm())) {
		AV_Nif malenif = nifs.get(armaToNif.get(armaSrc.getForm()));
		if (malenif != null) { // we have variants for that nif
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "Duplicating " + armaSrc + ", for nif: " + armaToNif.get(armaSrc.getForm()));
		    }
		    ArrayList<ARMA_spec> dups = new ArrayList<ARMA_spec>();
		    for (Variant v : malenif.variants) {
			ARMA dup = (ARMA) SPGlobal.getGlobalPatch().makeCopy(armaSrc, v.name + "_ID_" + malenif.uniqueName() + "_arma");

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
	SPGUI.progress.incrementBar();
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
	TXST last = null;
	for (AV_Nif.TextureField textureSet : texturePack) {
	    if (needed[i]) {
		if (textureSet.unique) {
		    // New TXST
		    TXST tmpTXST = new TXST(SPGlobal.getGlobalPatch(), v.name + "_" + textureSet.title + "_txst");
		    tmpTXST.setFlag(TXST.TXSTflag.FACEGEN_TEXTURES, true);

		    // Set maps
		    int j = 0;
		    for (String texture : textureSet.maps) {
			int set = readjustTXSTindices(j);

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
		    last = tmpTXST;
		}
		v.textureVariants[i] = new Variant.TextureVariant(last, textureSet.title);
	    }
	    i++;
	}

    }

    static void generateTXSTvariants() throws IOException {
	SPGUI.progress.setStatus(step++, numSteps, "Generating TXST variants.");
	for (AV_Nif n : nifs.values()) {

	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "====================================================================");
		SPGlobal.log(header, "Generating TXST records for Nif: " + n.name);
		SPGlobal.log(header, "====================================================================");
	    }
	    for (Variant v : n.variants) {
		generateVariant(v, n.textureFields);
	    }
	    n.textureFields = null; // Not needed anymore
	}
	SPGUI.progress.incrementBar();
    }

    static void splitVariant(String nifPath, ARMA piece) throws IOException, BadParameter, DataFormatException {
	if (SPGlobal.logging()) {
	    SPGlobal.log("SplitVar", "  Record warrents split due to alt textures in ARMA.");
	}
	AV_Nif nif = new AV_Nif(nifPath);
	nif.load();
	SPGlobal.log(header, "  Nif path: " + nifPath);
	nif.print();

	// Need to change old filenames to alt texture filenames
	for (AltTexture t : piece.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON)) {
	    TXST txst = (TXST) SPDatabase.getMajor(t.getTexture(), GRUP_TYPE.TXST);
	    ArrayList<String> textureMaps = nif.textureFields.get(t.getIndex()).maps;
	    for (int i = 0; i < TXST.NUM_MAPS; i++) {
		if (i == 2) {
		    continue;
		}
		int set;
		if (i == 3) {
		    set = 2;
		} else {
		    set = i;
		}
		if (!txst.getNthMap(i).equals("")) {
		    String altMapName = "textures\\" + txst.getNthMap(i);
		    if (SPGlobal.logging() && !textureMaps.get(set).equalsIgnoreCase(altMapName)) {
			SPGlobal.log(header, "  Alt Texture index " + t.getIndex() + " texture map[" + set + "] replaced from " + textureMaps.get(set) + " to " + altMapName);
		    }
		    textureMaps.set(set, altMapName);
		} else if (!textureMaps.get(set).equals("")) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Alt Texture index " + t.getIndex() + " texture map[" + set + "] removed.  Was: " + textureMaps.get(set));
		    }
		    textureMaps.set(set, "");
		}
	    }
	}

	nif.name = nifPath + "_ALT_" + piece.getForm();
	nifs.put(nif.name, nif);
	armaToNif.put(piece.getForm(), nif.name);
    }

    static void linkToNifs(ArrayList<VariantSet> variantRead) {
	SPGUI.progress.setStatus(step++, numSteps, "Linking packages to .nif files.");
	for (VariantSet varSet : variantRead) {
	    ArrayList<FormID> uniqueArmas = new ArrayList<FormID>();
	    for (String[] s : varSet.Target_FormIDs) {
		FormID id = new FormID(s[0], s[1]);
		String header = id.toString();
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "====================================================================");
		    SPGlobal.log(header, "Locating " + SPDatabase.getMajor(id, GRUP_TYPE.NPC_) + "'s NIF file and analyzing the Skin.");
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

		    // Didn't have a skin
		    if (skin.getArmatures().isEmpty()) {
			SPGlobal.logError(header, skin + " did not have any armatures.");
			continue;
		    }

		    // Locate armature that matches armor's race
		    ARMA piece = null;
		    for (FormID arma : skin.getArmatures()) {
			piece = (ARMA) SPDatabase.getMajor(arma);
			if (piece.getRace().equals(skin.getRace())) {
			    break;
			}
			piece = null;
		    }
		    if (piece == null) {
			SPGlobal.logError(header, "Could not locate ARMA matching ARMO's race");
			continue;
		    } else if (SPGlobal.logging()) {
			SPGlobal.log(header, "  " + piece);
		    }

		    // Locate armature's nif
		    nifPath = piece.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).toUpperCase();
		    if (nifPath.equals("")) {
			SPGlobal.logError(header, piece + " did not have a male third person model.");
			continue;
		    }

		    // Load in and add to maps
		    if (!armaToNif.containsKey(piece.getForm()) && !piece.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()) {
			// Has alt texture, separate
			splitVariant(nifPath, piece);
		    } else if (!nifs.containsKey(nifPath)) {
			AV_Nif nif = new AV_Nif(nifPath);
			nif.load();

			SPGlobal.log(header, "  Nif path: " + nifPath);
			nif.print();

			nifs.put(nif.name, nif);
			armaToNif.put(piece.getForm(), nif.name);
		    } else {
			if (SPGlobal.logging()) {
			    SPGlobal.log(header, "  Already a nif with path: " + nifPath);
			}
			// Doesn't have alt texture, just route to existing nif
			armaToNif.put(piece.getForm(), nifPath);
		    }

		    if (!uniqueArmas.contains(piece.getForm())) {
			uniqueArmas.add(piece.getForm());
			nifs.get(armaToNif.get(piece.getForm())).variants.addAll(varSet.variants);
		    } else if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Already logged that arma for this variant set.");
		    }

		    //Oh nos
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
	SPGUI.progress.incrementBar();
    }

    static ArrayList<VariantSet> importVariants(Mod patch) throws Uninitialized, FileNotFoundException {
	String header = "Import Variants";
	ArrayList<VariantSet> out = new ArrayList<VariantSet>();
	if (avPackages.isDirectory()) {
	    for (File packageFolder : avPackages.listFiles()) { // Bellyaches Animals
		if (packageFolder.isDirectory()) {
		    for (File variantSet : packageFolder.listFiles()) { // Horker
			if (variantSet.isDirectory()) {
			    try {
				VariantSet varSet = importVariantSet(variantSet, header);
				if (!varSet.isEmpty()) {
				    out.add(varSet);
				}
			    } catch (com.google.gson.JsonSyntaxException ex) {
				SPGlobal.logException(ex);
				JOptionPane.showMessageDialog(null, "Variant set " + variantSet.getPath() + " had a bad specifications file.  Skipped.");
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

    static VariantSet importVariantSet(File variantFolder, String header) throws FileNotFoundException, com.google.gson.JsonSyntaxException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "======================================================================");
	    SPGlobal.log(header, "Importing variant set: " + variantFolder.getPath());
	    SPGlobal.log(header, "======================================================================");
	}
	ArrayList<Variant> variants = new ArrayList<Variant>();
	VariantSet varSet = new VariantSet();
	ArrayList<File> commonTexturePaths = new ArrayList<File>();
	boolean loadedSpecs = false;




	for (File variantFile : variantFolder.listFiles()) {  // Texture folders ("Grey Horker")
	    if (variantFile.isFile() && variantFile.getName().toUpperCase().endsWith(".JSON")) {
		varSet = AVGlobal.parser.fromJson(new FileReader(variantFile), VariantSet.class);
		if (SPGlobal.logging()) {
		    SPGlobal.log(variantFile.getName(), "  General Specifications loaded: ");
		    SPGlobal.log(variantFile.getName(), "    Target FormIDs: ");
		    for (String[] s : varSet.Target_FormIDs) {
			SPGlobal.log(variantFile.getName(), "      " + s[0] + " | " + s[1]);
		    }
		    SPGlobal.log(variantFile.getName(), "    Apply to Similar: " + varSet.Apply_To_Similar);
		}
		loadedSpecs = true;
	    } else if (variantFile.isFile() && variantFile.getName().toUpperCase().endsWith(".DDS")) {
		commonTexturePaths.add(variantFile);
		Ln.moveFile(variantFile, new File(avTextures + variantFile.getPath().substring(avPackages.getPath().length())), false);
		if (SPGlobal.logging()) {
		    SPGlobal.log(variantFile.getName(), "  Loaded common texture: " + variantFile.getPath());
		}
	    } else if (variantFile.isDirectory()) {
		Variant variant = new Variant();
		variant.setName(variantFile, 3);
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
		} else if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Variant was empty, skipping.");
		}
	    } else if (SPGlobal.logging()) {
		SPGlobal.log(header, "  Skipped file: " + variantFile);
	    }
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "  ----------------------------------------------------------");
	    }

	}
	// If no specifications file, return empty
	if (!loadedSpecs) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "Variant set " + variantFolder.getPath() + "did not have specifications file.  Skipping.");
	    }
	    return varSet;
	}

	// If no specific variants, but generic files still present (perhaps to make a single variant)
	if (!commonTexturePaths.isEmpty() && variants.isEmpty()) {
	    Variant variant = new Variant();
	    variant.setName(variantFolder, 2);
	    variants.add(variant);
	}

	for (Variant v : variants) {
	    for (File f : commonTexturePaths) {
		boolean skip = false;
		// Check to see if deeper folder has a more specialized version of the file
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

    static void readInExceptions() throws IOException {
	try {
	    BufferedReader in = new BufferedReader(new FileReader(extraPath + "Files/BlockList.txt"));
	    Set target = null;
	    String read;
	    while (in.ready()) {
		read = in.readLine();
		if (read.indexOf("//") != -1) {
		    read = read.substring(0, read.indexOf("//"));
		}
		read = read.trim().toUpperCase();
		if (read.contains("ARMO BLOCKS")) {
		    target = block;
		} else if (read.contains("EDID BLOCKS")) {
		    target = edidExclude;
		} else if (target != null && !read.equals("")) {
		    if (target == block) {
			target.add(new FormID(read));
		    } else {
			target.add(read);
		    }
		}
	    }
	} catch (FileNotFoundException ex) {
	    SPGlobal.logError("ReadInExceptions", "Failed to locate 'BlockList.txt'");
	}
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

    static SPDefaultGUI createGUI() {
	/*
	 * Custom names and descriptions
	 */
	// Used in the GUI as the title
	String myPatcherName = "Automatic Variants";
	// Used in the GUI as the description of what your patcher does
	String myPatcherDescription =
		"Loading in packages; Creating variant records; Inserting them into Skyrim.\n"
		+ "Enjoy!";

	/*
	 * Creating SkyProc Default GUI
	 */
	SPDefaultGUI gui = new SPDefaultGUI(myPatcherName, myPatcherDescription);




	try {
	    gui.replaceHeader(AutomaticVariants.class.getResource("AutoVarGUITitle.png"), - 35);
	} catch (IOException ex) {
	    SPGlobal.logException(ex);
	}
	return gui;
    }
}
