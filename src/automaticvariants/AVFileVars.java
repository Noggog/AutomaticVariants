/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVSaveFile.Settings;
import java.io.*;
import java.util.*;
import java.util.zip.DataFormatException;
import javax.swing.JOptionPane;
import lev.LMergeMap;
import lev.LPair;
import lev.LShrinkArray;
import lev.Ln;
import skyproc.*;
import skyproc.exceptions.BadParameter;
import skyproc.exceptions.Uninitialized;
import skyproc.gui.SPProgressBarPlug;

/**
 * All the functionality concerning setting up variants associated with race
 * switching:
 *
 * Texture variants
 *
 * @Author Justin Swanson
 */
public class AVFileVars {

    static String header = "AV_FileVar";
    final public static String AVPackagesDir = "AV Packages\\";
    public static String AVPackageListing = SPGlobal.pathToInternalFiles + "Last AV Package Listing.txt";
    public static String AVTexturesDir = SPGlobal.pathToData + "textures\\AV Packages\\";
    public static String AVMeshesDir = SPGlobal.pathToData + "meshes\\AV Packages\\";
    static String debugFolder = "File Variants/";
    static int debugNumber = 1;
    static int numSupportedTextures = 8;
    public static PackageNode AVPackages = new PackageNode(new File(AVPackagesDir), PackageNode.Type.ROOT);
    /*
     * Variant storage lists/maps
     */
    // List of unused things to skip
    static public HashSet<FormID> unusedRaces;
    static public HashSet<FormID> unusedSkins;
    static public LMergeMap<FormID, FormID> unusedPieces;
    // List of what races the armor "supports"
    static LMergeMap<FormID, FormID> armoRaces;
    static Set<FormID> taggedNPCs = new HashSet<>();
    static Map<FormID, LMergeMap<FormID, ARMO_spec>> armors = new HashMap<>();
    //////////////////
    // RaceSrc of piece is key for outer, armo is inner key
    //////////////////
    static Map<FormID, Map<FormID, FLST>> formLists = new HashMap<>();
    static LMergeMap<FormID, ARMO_spec> compiledVariants = new LMergeMap<FormID, ARMO_spec>(false);
    //////////////////
    // RaceSrc is key
    //////////////////
    static Map<FormID, AV_SPEL> switcherSpells = new HashMap<>();
    static public ArrayList<VariantProfile> profiles;

    static void setUpFileVariants(Mod source) throws IOException, Uninitialized, BadParameter {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Import Packages.txt");
	    File f = new File(SPGlobal.pathToDebug() + "Asynchronous log.txt");
	    if (f.isFile()) {
		f.delete();
	    }
	}

	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Importing AV Packages");
	importVariants();
	AVPackages.prune();
	SPProgressBarPlug.incrementBar();

	prepProfiles();

	dropVariantSetsInProfiles();

	clearUnusedProfiles();

	generateRecords();

	implementOrigAsVar();

	skinSwitchMethod(source);
    }

    static void skinSwitchMethod(Mod source) {

	// Generate FormLists of RACE variants
	generateFormLists(source);

	// Generate Spells that use Race Switcher Magic Effects
	generateSPELvariants(source);

	// Add AV keywords to NPCs that have alt skins
	tagNPCs(source);
    }

    /*
     * Shared methods
     */
    public static void importVariants() throws IOException {
	String header = "Import Variants";
	File AVPackagesDirFile = new File(SPGlobal.SUMpath + AVPackagesDir);

	// wipe
	AVPackages = new PackageNode(AVPackagesDirFile, PackageNode.Type.ROOT);
	RerouteFile.reroutes.clear();
	if (AVPackagesDirFile.isDirectory()) {
	    for (File packageFolder : AVPackagesDirFile.listFiles()) {
		if (packageFolder.isDirectory()) {
		    AVPackage avPackage = new AVPackage(packageFolder);
		    AVPackages.add(avPackage);
		}
	    }
	} else {
	    SPGlobal.logError("Package Location", "There was no AV Packages folder.");
	}
    }

    public static void prepProfiles() {
	BSA.loadInBSAs(BSA.FileType.NIF, BSA.FileType.DDS);
	locateUnused();
	loadProfiles();
    }

    public static void locateUnused() {

	if (unusedRaces != null) {
	    return;
	}

	Mod source = AV.getMerger();

	// Load all races, skins, pieces into containers
	unusedRaces = new HashSet<FormID>(source.getRaces().numRecords());
	for (RACE race : source.getRaces()) {
	    unusedRaces.add(race.getForm());
	}
	unusedSkins = new HashSet<FormID>(source.getArmors().numRecords());
	unusedPieces = new LMergeMap<FormID, FormID>(false);
	armoRaces = new LMergeMap<FormID, FormID>(false);
	LMergeMap<FormID, ARMA> unusedPiecesTmp = new LMergeMap<FormID, ARMA>(false);
	for (ARMO armor : source.getArmors()) {
	    if (!unusedSkins.contains(armor.getForm())) {
		unusedSkins.add(armor.getForm());
		for (FormID piece : armor.getArmatures()) {
		    ARMA arma = (ARMA) SPDatabase.getMajor(piece, GRUP_TYPE.ARMA);
		    if (arma != null) {
			armoRaces.put(armor.getForm(), arma.getRace());
			unusedPiecesTmp.put(armor.getForm(), arma);
		    }
		}
	    }
	}


	// Removed used races/skins/pieces
	for (NPC_ n : source.getNPCs()) {
	    FormID skin = getUsedSkin(n);
	    if (AV.block.contains(skin)) {
		continue;
	    }
	    unusedRaces.remove(n.getRace());
	    unusedSkins.remove(skin);
	    if (unusedPiecesTmp.containsKey(skin)) {
		ArrayList<ARMA> tmpPieces = new ArrayList<ARMA>(unusedPiecesTmp.get(skin));
		for (ARMA piece : tmpPieces) {
		    if (piece.getRace().equals(n.getRace())) {
			unusedPiecesTmp.get(skin).remove(piece);
		    }
		}
	    }
	}

	// Load unused armor pieces into final container
	for (FormID skin : unusedPiecesTmp.keySet()) {
	    if (!unusedPiecesTmp.get(skin).isEmpty()) {
		for (ARMA piece : unusedPiecesTmp.get(skin)) {
		    unusedPieces.put(skin, piece.getForm());
		}
	    } else {
		unusedPieces.put(skin, new ArrayList<FormID>(0));
	    }
	}

	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Locate Unused.txt");
	    SPGlobal.log(header, "Unused Races:");
	    for (FormID race : unusedRaces) {
		SPGlobal.log(header, "  " + SPDatabase.getMajor(race, GRUP_TYPE.RACE));
	    }
	    SPGlobal.log(header, "Unused Skins:");
	    for (FormID skin : unusedSkins) {
		SPGlobal.log(header, "  " + SPDatabase.getMajor(skin, GRUP_TYPE.ARMO));
	    }
	    SPGlobal.log(header, "Unused Pieces:");
	    for (FormID skin : unusedPieces.keySet()) {
		SPGlobal.log(header, "  For " + SPDatabase.getMajor(skin, GRUP_TYPE.ARMO));
		for (FormID piece : unusedPieces.get(skin)) {
		    SPGlobal.log(header, "    " + SPDatabase.getMajor(piece, GRUP_TYPE.ARMA));
		}
	    }
	}
    }

    public static void loadProfiles() {
	if (profiles != null) {
	    return;
	}
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Load Variant Profiles.txt");
	}
	profiles = VariantProfile.profiles;
	locateUsedNIFs();
	loadUsedNIFs();
	loadProfileRecords();
	VariantProfile.printProfiles();
    }

    public static void locateUsedNIFs() {
	SPGlobal.log(header, "===========================================================");
	SPGlobal.log(header, "===================      Loading NIFs     =================");
	SPGlobal.log(header, "===========================================================");
	for (ARMO armo : AV.getMerger().getArmors()) {
	    if (!AVFileVars.unusedSkins.contains(armo.getForm())) {
		for (FormID piece : armo.getArmatures()) {
		    if (!AVFileVars.unusedPieces.get(armo.getForm()).contains(piece)) {
			ARMA arma = (ARMA) SPDatabase.getMajor(piece, GRUP_TYPE.ARMA);
			String nifPath = "MESHES\\" + arma.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).toUpperCase();
			if (nifPath.equals("MESHES\\")) {
			    SPGlobal.log(header, "Skipping " + arma + " because it had no nif.");
			    continue;
			}
			if (VariantProfile.find(null, null, null, nifPath) == null) {
			    VariantProfile profile = new VariantProfile();
			    profile.nifPath = nifPath;
			}
		    }
		}
	    }
	}
    }

    public static void loadUsedNIFs() {
	for (VariantProfile profile : new ArrayList<>(VariantProfile.profiles)) {
	    try {
		LShrinkArray nifData = BSA.getUsedFile(profile.nifPath);
		if (nifData != null) {
		    if (profile.nifPath.equals("MESHES\\BELLYACHES NEW DRAGON SPECIES\\KEENE'S TALON\\DRAGON.NIF")) {
			int wer = 23;
		    }
		    for (LPair<String, ArrayList<String>> pair : loadNif(profile.nifPath, nifData)) {
			profile.textures.put(pair.a, pair.b);
			profile.nifNodeNames.add(pair.a);
		    }
		    if (profile.textures.isEmpty()) {
			VariantProfile.profiles.remove(profile);
			SPGlobal.log(profile.toString(), "Removing profile with nif because it had no textures: " + profile.nifPath);
		    }
		} else {
		    VariantProfile.profiles.remove(profile);
		    SPGlobal.logError(header, "Error locating nif file: " + profile.nifPath + ", removing profile.");
		}
	    } catch (IOException | DataFormatException ex) {
		SPGlobal.logException(ex);
	    }
	}
    }

    public static void loadProfileRecords() {
	SPGlobal.log(header, "===========================================================");
	SPGlobal.log(header, "================      Loading Records     =================");
	SPGlobal.log(header, "===========================================================");
	for (ARMO armo : AV.getMerger().getArmors()) {
	    if (!unusedSkins.contains(armo.getForm())) {
		if (!AV.block.contains(armo.getForm())) {
		    loadProfileSkin(armo);
		} else {
		    SPGlobal.log(header, "Blocked because it was on the blocklist: " + armo);
		}
	    }
	}
	for (VariantProfile p : VariantProfile.profiles) {
	    p.finalizeProfile();
	}
    }

    public static void loadProfileSkin(ARMO armo) {
	for (FormID armaForm : armo.getArmatures()) {
	    // If a used piece
	    if (!unusedPieces.containsKey(armo.getForm())
		    || !unusedPieces.get(armo.getForm()).contains(armaForm)) {
		ARMA arma = (ARMA) SPDatabase.getMajor(armaForm, GRUP_TYPE.ARMA);

		// Make sure it has a race
		if (arma.getRace().isNull()) {
		    SPGlobal.logError(header, arma + " skipped because it had no race.");
		    continue;
		}

		// Find profile with that nif
		String nifPath = "MESHES\\" + arma.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).toUpperCase();
		VariantProfile profile = VariantProfile.find(null, null, null, nifPath);


		if (profile != null) {
		    try {
			Set<RACE> races = new HashSet<>();
			races.add((RACE) SPDatabase.getMajor(arma.getRace(), GRUP_TYPE.RACE));
			for (FormID raceID : arma.getAdditionalRaces()) {
			    races.add((RACE) SPDatabase.getMajor(raceID, GRUP_TYPE.RACE));
			}

			for (RACE r : races) {
			    //If profile is already filled, make duplicate
			    if (profile.race != null) {
				SPGlobal.log(header, "Duplicating for " + profile.nifPath + " || " + profile.race);
				profile = new VariantProfile(profile);
			    }
			    //Load in record setup
			    profile.race = r;
			    profile.skin = armo;
			    profile.piece = arma;

			    if (profile.ID == 254) {
				int wer = 23;
			    }

			    //Load Alt Textures
			    profile.loadAltTextures(arma.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON));
			}
		    } catch (Exception ex) {
			SPGlobal.logError(header, "Skipping profile " + profile + " because an exception occured.");
			SPGlobal.logException(ex);
			VariantProfile.profiles.remove(profile);
		    }
		} else {
		    SPGlobal.log(header, "Skipped " + arma + ", could not find a profile matching nif: " + nifPath);
		}
	    }
	}
    }

    public static ArrayList<LPair<String, ArrayList<String>>> loadNif(String nifPath, LShrinkArray in) {
	ArrayList<LPair<String, ArrayList<String>>> nifTextures = new ArrayList<>();
	try {
	    NIF nif = new NIF(nifPath, in);
	    nifTextures = nif.extractTextures();

	    // To uppercase
	    for (LPair<String, ArrayList<String>> pair : nifTextures) {
		for (int i = 0; i < pair.b.size(); i++) {
		    pair.b.set(i, pair.b.get(i).toUpperCase());
		}
	    }

	} catch (BadParameter | java.nio.BufferUnderflowException ex) {
	    SPGlobal.logException(ex);
	}
	return nifTextures;
    }

    public static void dropVariantSetsInProfiles() {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Processing Variant Seeds.txt");
	}
	for (PackageNode avPackageC : AVPackages.getAll(PackageNode.Type.PACKAGE)) {
	    AVPackage avPackage = (AVPackage) avPackageC;
	    for (PackageNode varSetP : avPackage.getAll(PackageNode.Type.VARSET)) {
		VariantSet varSet = (VariantSet) varSetP;
		if (varSet.spec == null || varSet.isEmpty()) {
		    SPGlobal.logError(header, "Skipping " + varSet.src + " because it was empty or missing a spec file.");
		    continue;
		} else if (SPGlobal.logging()) {
		    SPGlobal.log("SortVariantSets", " /====================================");
		    SPGlobal.log("SortVariantSets", "| Processing: " + varSet.src);
		    SPGlobal.log("SortVariantSets", "|==============\\");
		    SPGlobal.log("SortVariantSets", "|== Files: =====|");
		    SPGlobal.log("SortVariantSets", "|==============/");
		    for (String s : varSet.getTextures()) {
			SPGlobal.log("SortVariantSets", "|    " + s);
		    }
		    SPGlobal.log("SortVariantSets", "|==============\\");
		    SPGlobal.log("SortVariantSets", "|== Seeds: =====|");
		    SPGlobal.log("SortVariantSets", "|==============/");
		}

		ArrayList<SeedProfile> seeds = new ArrayList<>();
		for (NPC_ n : varSet.getSeedNPCs()) {
		    SeedProfile seed = new SeedProfile(n);
		    if (seed.load()) {
			seeds.add(seed);
			if (SPGlobal.logging()) {
			    seed.print();
			}
		    }
		}

		boolean absorbed = false;
		for (VariantProfile varProfile : VariantProfile.profiles) {
		    if (varProfile.absorb(varSet, seeds)) {
			SPGlobal.log("Absorb", "  /======================================");
			SPGlobal.log("Absorb", " /=== " + varSet.src + " absorbed by:");
			SPGlobal.log("Absorb", "|=======================================");
			varProfile.printShort();
			SPGlobal.log("Absorb", " \\======================================");
			absorbed = true;
		    }
		}

		if (SPGlobal.logging()) {
		    if (!absorbed) {
			SPGlobal.logError("Absorbing", "Variant Set " + varSet.src + " could not be absorbed by any profile.");
		    } else {
			SPGlobal.log(header, "");
			SPGlobal.log(header, "");
			SPGlobal.log(header, "");
			SPGlobal.log(header, "");
		    }
		}
	    }
	}
    }

    static void clearUnusedProfiles() {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Clear Unused Profiles.txt");
	}
	ArrayList<VariantProfile> tmp = new ArrayList<>(VariantProfile.profiles);
	for (VariantProfile profile : tmp) {
	    if (profile.sets.isEmpty()) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Removing profile " + profile + " because it was empty.");
		    profile.print();
		}
		VariantProfile.profiles.remove(profile);
	    }
	}
    }

    static void generateRecords() {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Generating variant records.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Generate Variants.txt");
	}
	for (VariantProfile profile : VariantProfile.profiles) {
	    profile.generateRecords();
	}
    }

    static void implementOrigAsVar() {
	if (AV.save.getBool(Settings.PACKAGES_ORIG_AS_VAR)) {
	    for (FormID armoSrc : armors.keySet()) {
		ARMO src = (ARMO) SPDatabase.getMajor(armoSrc, GRUP_TYPE.ARMO);
		for (FormID race : armors.get(armoSrc).keySet()) {
		    armors.get(armoSrc).put(race, new ARMO_spec(src));
		}
	    }
	}
    }

    static void generateFormLists(Mod source) {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Generating Form Lists.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Generate Form Lists.txt");
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating FormLists for each ARMO variant");
	    SPGlobal.log(header, "====================================================================");
	}
	for (FormID armoSrcForm : armors.keySet()) {
	    ARMO armoSrc = (ARMO) SPDatabase.getMajor(armoSrcForm, GRUP_TYPE.ARMO);
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "  Generating FLSTs for " + armoSrc);
	    }
	    for (FormID race : armors.get(armoSrcForm).keySet()) {
		RACE raceSrc = (RACE) SPDatabase.getMajor(race, GRUP_TYPE.RACE);
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "    Generating FLST for race " + raceSrc);
		}
		FLST flst = new FLST(SPGlobal.getGlobalPatch(), "AV_" + armoSrc.getEDID() + "_" + raceSrc.getEDID() + "_flst");
		ArrayList<ARMO_spec> armoVars = armors.get(armoSrcForm).get(race);
		int[] divs = new int[armoVars.size()];
		for (int i = 0; i < divs.length; i++) {
		    divs[i] = armoVars.get(i).spec.Probability_Divider;
		}
		int lowestCommMult = Ln.lcmm(divs);

		for (ARMO_spec armorSpec : armors.get(armoSrcForm).get(race)) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "      Generating " + (lowestCommMult / armorSpec.spec.Probability_Divider) + " entries for " + armorSpec.armo);
		    }
		    for (int i = 0; i < lowestCommMult / armorSpec.spec.Probability_Divider; i++) {
			flst.addFormEntry(armorSpec.armo.getForm());
		    }
		    compiledVariants.put(race, armorSpec);
		}
		if (!formLists.containsKey(race)) {
		    formLists.put(race, new HashMap<FormID, FLST>());
		}
		formLists.get(race).put(armoSrcForm, flst);
	    }

	}
	SPProgressBarPlug.incrementBar();
    }

    static void generateSPELvariants(Mod source) {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Generating script attachment races.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Generate Switcher Spells.txt");
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating Spells which attach specialized scripts");
	    SPGlobal.log(header, "====================================================================");
	}

	for (RACE raceSrc : source.getRaces()) {
	    if (formLists.containsKey(raceSrc.getForm())) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Has variants: " + raceSrc);
		}
		ScriptRef script = AV.generateAttachScript();
		FLST flstArray = new FLST(SPGlobal.getGlobalPatch(), "AV_" + raceSrc.getEDID() + "_flst_Array");
		ArrayList<FormID> skinKey = new ArrayList<FormID>();

		// Add normal worn armor to last on the array
		if (formLists.get(raceSrc.getForm()).containsKey(raceSrc.getWornArmor())) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Added normal skin " + SPDatabase.getMajor(raceSrc.getWornArmor(), GRUP_TYPE.ARMO));
		    }
		    flstArray.addFormEntry(formLists.get(raceSrc.getForm()).get(raceSrc.getWornArmor()).getForm());
		    skinKey.add(raceSrc.getWornArmor());
		}

		// Add Alt Skins to array setup
		for (FormID skinF : formLists.get(raceSrc.getForm()).keySet()) {
		    if (!skinF.equals(raceSrc.getWornArmor())) {
			if (SPGlobal.logging()) {
			    SPGlobal.log(header, "  Has alt skin " + SPDatabase.getMajor(skinF, GRUP_TYPE.ARMO));
			}
			flstArray.addFormEntry(formLists.get(raceSrc.getForm()).get(skinF).getForm());
			skinKey.add(skinF);
		    }
		}

		script.setProperty("AltOptions", flstArray.getForm());
		script.setProperty("RaceHeightOffset", raceSrc.getHeight(Gender.MALE));

		// Loop through all variants for this race
		// and load up non-standard spec file info
		ArrayList<Integer> heights = new ArrayList<Integer>();
		ArrayList<Integer> healths = new ArrayList<Integer>();
		ArrayList<Integer> magickas = new ArrayList<Integer>();
		ArrayList<Integer> staminas = new ArrayList<Integer>();
		ArrayList<Integer> speeds = new ArrayList<Integer>();
		ArrayList<Integer> prefixKey = new ArrayList<Integer>();
		ArrayList<String> prefix = new ArrayList<String>();
		ArrayList<Integer> affixKey = new ArrayList<Integer>();
		ArrayList<String> affix = new ArrayList<String>();
		int index = 0;
		for (ARMO_spec variant : compiledVariants.get(raceSrc.getForm())) {
		    if (variant.spec.Height_Mult != SpecVariant.prototype.Height_Mult) {
			heights.add(index);
			heights.add(variant.spec.Height_Mult);
		    }
		    if (variant.spec.Health_Mult != SpecVariant.prototype.Health_Mult) {
			healths.add(index);
			healths.add(variant.spec.Health_Mult);
		    }
		    if (variant.spec.Magicka_Mult != SpecVariant.prototype.Magicka_Mult) {
			magickas.add(index);
			magickas.add(variant.spec.Magicka_Mult);
		    }
		    if (variant.spec.Stamina_Mult != SpecVariant.prototype.Stamina_Mult) {
			staminas.add(index);
			staminas.add(variant.spec.Stamina_Mult);
		    }
		    if (variant.spec.Speed_Mult != SpecVariant.prototype.Speed_Mult) {
			speeds.add(index);
			speeds.add(variant.spec.Speed_Mult);
		    }
		    if (!variant.spec.Name_Prefix.equals("")) {
			prefixKey.add(index);
			prefix.add(variant.spec.Name_Prefix);
		    }
		    if (!variant.spec.Name_Affix.equals("")) {
			affixKey.add(index);
			affix.add(variant.spec.Name_Affix);
		    }
		    index++;
		}
		if (!heights.isEmpty()) {
		    script.setProperty("HeightVariants", heights.toArray(new Integer[0]));
		}
		if (!healths.isEmpty()) {
		    script.setProperty("HealthVariants", healths.toArray(new Integer[0]));
		}
		if (!magickas.isEmpty()) {
		    script.setProperty("MagickaVariants", magickas.toArray(new Integer[0]));
		}
		if (!staminas.isEmpty()) {
		    script.setProperty("StaminaVariants", staminas.toArray(new Integer[0]));
		}
		if (!speeds.isEmpty()) {
		    script.setProperty("SpeedVariants", speeds.toArray(new Integer[0]));
		}
		if (!prefixKey.isEmpty()) {
		    script.setProperty("PrefixKey", prefixKey.toArray(new Integer[0]));
		    script.setProperty("Prefix", prefix.toArray(new String[0]));
		}
		if (!affixKey.isEmpty()) {
		    script.setProperty("AffixKey", affixKey.toArray(new Integer[0]));
		    script.setProperty("Affix", affix.toArray(new String[0]));
		}

		// Generate the spell
		SPEL spell = NiftyFunc.genScriptAttachingSpel(SPGlobal.getGlobalPatch(), script, raceSrc.getEDID());
		switcherSpells.put(raceSrc.getForm(), new AV_SPEL(spell, flstArray, skinKey));
		raceSrc.addSpell(spell.getForm());
		SPGlobal.getGlobalPatch().addRecord(raceSrc);
	    }
	}
	SPProgressBarPlug.incrementBar();
    }

    static void standardizeNPCtag(NPC_ n) {
	float weight = n.getWeight() * 100;
	int tmp = (int) Math.round(weight);
	if (tmp != weight) {
//	    if (SPGlobal.logging()) {
//		SPGlobal.log(header, "Standardized " + n);
//	    }
	    n.setWeight(tmp / 100);
	    SPGlobal.getGlobalPatch().addRecord(n);
	}
    }

    static void tagNPCs(Mod source) {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Tagging NPCs.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Tagging NPCs.txt");
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Tagging NPCs that have alt skins");
	    SPGlobal.log(header, "====================================================================");
	}
	RACE foxRace = (RACE) SPDatabase.getMajor(new FormID("109C7CSkyrim.esm"), GRUP_TYPE.RACE);
	for (NPC_ n : source.getNPCs()) {
	    FormID skin = getUsedSkin(n);
	    if (skin != null
		    && (!n.isTemplated() || !n.get(NPC_.TemplateFlag.USE_TRAITS)) // Not templated with traits
		    && !skin.isNull() // If has alt skin
		    && switcherSpells.containsKey(n.getRace())) {  // If we have variants for it
		// If fox race but does not have FOX in the name
		// We skip it as it's most likely a lazy modder
		// using the default race: FoxRace
		standardizeNPCtag(n);
		if (n.getRace().equals(foxRace.getForm())
			&& !n.getEDID().toUpperCase().contains("FOX")
			&& !n.getName().toUpperCase().contains("FOX")) {
		    tagNPC(n, 99);
		}
		ArrayList<FormID> skins = switcherSpells.get(n.getRace()).key;
		boolean tagged = false;
		for (int i = 0; i < skins.size(); i++) {
		    if (skins.get(i).equals(skin)) {
			if (SPGlobal.logging()) {
			    SPGlobal.log(header, "Tagged " + n + " for skin " + SPDatabase.getMajor(skins.get(i), GRUP_TYPE.ARMO));
			}
			tagNPC(n, i);
			tagged = true;
			break;
		    }
		}
		if (!tagged) {
		    SPGlobal.log(header, "EXCLUDE BECAUSE NO VARIANTS " + n);
		    tagNPC(n, 99);
		}
		SPGlobal.getGlobalPatch().addRecord(n);
	    }
	}
    }

    static void tagNPC(NPC_ n, float i) {
	n.setHeight(n.getHeight() + i / 10000);
    }

    static FormID getTemplatedSkin(NPC_ n) {
	if (n == null) {
	    return null;
	}
	if (!n.getTemplate().isNull() && n.get(NPC_.TemplateFlag.USE_TRAITS)) {
	    return getTemplatedSkin((NPC_) SPDatabase.getMajor(n.getTemplate(), GRUP_TYPE.NPC_));
	} else {
	    return n.getSkin();
	}
    }

    static boolean hasVariant(LVLN llist) {
	for (LVLO entry : llist.getEntries()) {
	    if (taggedNPCs.contains(entry.getForm())) {
		return true;
	    }
	    LVLN testLList = (LVLN) SPDatabase.getMajor(entry.getForm(), GRUP_TYPE.LVLN);
	    if (testLList != null && hasVariant(testLList)) {
		return true;
	    }
	}
	return false;
    }

    static void printModifiedNPCs() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Printing Modified NPCs: ");
	    SPGlobal.log(header, "====================================================================");
	    for (FormID armoSrc : AV.modifiedNPCs.keySet()) {
		SPGlobal.log(header, "For " + SPDatabase.getMajor(armoSrc, GRUP_TYPE.ARMO));
		for (NPC_ n : AV.modifiedNPCs.get(armoSrc)) {
		    SPGlobal.log(header, "    " + n);
		}
		SPGlobal.log(header, "--------------------------------------");
	    }
	}
    }

    static public void moveOut() {
	for (PackageNode p : AVPackages.getAll()) {
	    p.moveOut();
	}
    }

    public static void saveAVPackagesListing() throws IOException {
	File packageDir = new File(AVPackagesDir);
	ArrayList<File> files = Ln.generateFileList(packageDir, false);
	File save = new File(AVPackageListing);
	BufferedWriter out = new BufferedWriter(new FileWriter(save));
	for (File f : files) {
	    out.write(f.getPath() + "\n");
	}
	out.close();
    }

    public static ArrayList<String> getAVPackagesListing() throws IOException {
	ArrayList<String> out = new ArrayList<>();
	BufferedReader in = new BufferedReader(new FileReader(AVPackageListing));
	String line;
	while ((line = in.readLine()) != null) {
	    out.add(line);
	}
	in.close();
	return out;
    }

    /*
     * Other Methods
     */
    static public FormID getUsedSkin(NPC_ npcSrc) {
	if (!npcSrc.getSkin().equals(FormID.NULL)) {
	    return npcSrc.getSkin();
	} else {
	    RACE race = (RACE) SPDatabase.getMajor(npcSrc.getRace());
	    if (race == null) {
		return null;
	    }
	    if (!race.getWornArmor().equals(FormID.NULL)) {
		return race.getWornArmor();
	    } else {
		return null;
	    }
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

    public static void gatherFiles() {
	gatherFolder(AVTexturesDir);
	gatherFolder(AVMeshesDir);
    }

    public static void gatherFolder(String folder) {
	ArrayList<File> files = Ln.generateFileList(new File(folder), 2, 4, false);
	for (File file : files) {
	    File dest = new File(AVPackagesDir + file.getPath().substring(folder.length()));
	    if (dest.exists()) {
		file.delete();
	    } else {
		if (!Ln.moveFile(file, dest, false)) {
		    JOptionPane.showMessageDialog(null,
			    "<html>Error gathering files back to AV Package folder.</html>");
		}
	    }
	}
    }

    static boolean isSpec(File f) {
	return Ln.isFileType(f, "JSON");
    }

    static boolean isDDS(File f) {
	return Ln.isFileType(f, "DDS");
    }

    static boolean isNIF(File f) {
	return Ln.isFileType(f, "NIF");
    }

    static boolean isReroute(File f) {
	return Ln.isFileType(f, "reroute");
    }

    /*
     * Internal Classes
     */
    static class AV_SPEL {

	SPEL spell;
	FLST skins;
	ArrayList<FormID> key;

	AV_SPEL(SPEL spell, FLST skins, ArrayList<FormID> key) {
	    this.spell = spell;
	    this.skins = skins;
	    this.key = key;
	}
    }

    static class ARMO_spec {

	ARMO armo;
	SpecVariant spec;

	ARMO_spec(ARMO armoSrc) {
	    this.armo = armoSrc;
	    spec = new SpecVariant();
	}

	ARMO_spec(ARMO armo, SpecVariant spec) {
	    this.armo = armo;
	    this.spec = spec;
	}
    }

    public enum VariantType {

	NPC_;
    }
}
