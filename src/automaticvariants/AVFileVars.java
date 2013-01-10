/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVSaveFile.Settings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public static String AVTexturesDir = SPGlobal.pathToData + "textures\\AV Packages\\";
    public static String AVMeshesDir = SPGlobal.pathToData + "meshes\\AV Packages\\";
    static String debugFolder = "File Variants/";
    static int debugNumber = 1;
    static int numSupportedTextures = 8;
    public static PackageNode AVPackages = new PackageNode(new File(AVPackagesDir), PackageNode.Type.ROOT);
    // List of unused records
    static public HashSet<FormID> unusedRaces;
    static public HashSet<FormID> unusedSkins;
    static public LMergeMap<FormID, FormID> unusedPieces;
    // Variant storage lists/maps
    static Set<FormID> taggedNPCs = new HashSet<>();
    static Map<FormID, LMergeMap<FormID, ARMO_spec>> armors = new HashMap<>();
    static Map<FormID, AV_Race> AVraces = new HashMap<>();
    static public ArrayList<VariantProfile> profiles;

    static void setUpFileVariants(Mod source) throws IOException, Uninitialized, BadParameter {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Import Packages.txt");
	    File f = new File(SPGlobal.pathToDebug() + "Asynchronous log.txt");
	    if (f.isFile()) {
		f.delete();
	    }
	}

	importVariants(true);
	AVPackages.prune();

	prepProfiles();

	dropVariantSetsInProfiles();
	SPProgressBarPlug.incrementBar();

	clearUnusedProfiles();
	SPProgressBarPlug.incrementBar();

	generateArmorRecords();

	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Finishing Up");
	SPProgressBarPlug.reset();
	SPProgressBarPlug.setMax(1);
	implementOrigAsVar();

	createAVRaceObjects();

	setUpExclusiveCellList();

	// Generate FormLists of RACE variants
	generateFormLists(source);

	// Generate Spells that use Race Switcher Magic Effects
	generateSPELvariants(source);

	// Add AV keywords to NPCs that have alt skins
	tagNPCs(source);
	SPProgressBarPlug.done();
    }

    /*
     * Shared methods
     */
    public static void importVariants(boolean progressBar) throws IOException {
	String header = "Import Variants";
	File AVPackagesDirFile = new File(AVPackagesDir);
	// wipe
	AVPackages = new PackageNode(AVPackagesDirFile, PackageNode.Type.ROOT);
	RerouteFile.reroutes.clear();
	if (AVPackagesDirFile.isDirectory()) {
	    File[] files = AVPackagesDirFile.listFiles();
	    if (progressBar) {
		SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Importing AV Packages");
		SPProgressBarPlug.reset();
		SPProgressBarPlug.setMax(files.length);
	    }
	    for (File packageFolder : files) {
		if (packageFolder.isDirectory()) {
		    AVPackage avPackage = new AVPackage(packageFolder);
		    AVPackages.add(avPackage);
		    if (progressBar) {
			SPProgressBarPlug.incrementBar();
		    }
		}
	    }
	    if (progressBar) {
		SPProgressBarPlug.done();
	    }
	} else {
	    SPGlobal.logError("Package Location", "There was no AV Packages folder.");
	}
    }

    public static void prepProfiles() {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Loading AV Profiles");
	SPProgressBarPlug.reset();
	SPProgressBarPlug.setMax(8);

	BSA.loadInBSAs(BSA.FileType.NIF, BSA.FileType.DDS);
	SPProgressBarPlug.incrementBar();
	locateUnused();
	SPProgressBarPlug.incrementBar();
	loadProfiles();
	SPProgressBarPlug.incrementBar();
    }

    public static void locateUnused() {

	if (unusedRaces != null) {
	    return;
	}

	Mod source = AV.getMerger();

	// Load all races, skins, pieces into containers
	unusedRaces = new HashSet<>(source.getRaces().numRecords());
	for (RACE race : source.getRaces()) {
	    unusedRaces.add(race.getForm());
	}
	unusedSkins = new HashSet<>(source.getArmors().numRecords());
	unusedPieces = new LMergeMap<>(false);
	LMergeMap<FormID, ARMA> unusedPiecesTmp = new LMergeMap<>(false);
	for (ARMO armor : source.getArmors()) {
	    if (!unusedSkins.contains(armor.getForm())) {
		unusedSkins.add(armor.getForm());
		for (FormID piece : armor.getArmatures()) {
		    ARMA arma = (ARMA) SPDatabase.getMajor(piece, GRUP_TYPE.ARMA);
		    if (arma != null) {
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
		ArrayList<ARMA> tmpPieces = new ArrayList<>(unusedPiecesTmp.get(skin));
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
	SPProgressBarPlug.incrementBar();
	loadUsedNIFs();
	SPProgressBarPlug.incrementBar();
	loadProfileRecords();
	SPProgressBarPlug.incrementBar();
	VariantProfile.printProfiles();
    }

    public static void locateUsedNIFs() {
	SPGlobal.log(header, "===========================================================");
	SPGlobal.log(header, "===================      Loading NIFs     =================");
	SPGlobal.log(header, "===========================================================");
	for (ARMO armo : AV.getMerger().getArmors()) {
	    if (!AVFileVars.unusedSkins.contains(armo.getForm()) && !AV.block.contains(armo.getForm())) {
		for (FormID piece : armo.getArmatures()) {
		    if (!AVFileVars.unusedPieces.get(armo.getForm()).contains(piece)) {
			ARMA arma = (ARMA) SPDatabase.getMajor(piece, GRUP_TYPE.ARMA);
			if (arma == null) {
			    if (SPGlobal.logging()) {
				SPGlobal.log(header, "Skipping " + piece + " because it didn't exist.");
			    }
			    continue;
			} else if (SPGlobal.logging()) {
			    SPGlobal.log(header, "Loading " + arma);
			}
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
		    Map<Integer, LPair<String, ArrayList<String>>> nifTextures = loadNif(profile.nifPath, nifData);
		    for (Integer index : nifTextures.keySet()) {
			LPair<String, ArrayList<String>> pair = nifTextures.get(index);
			profile.textures.put(pair.a, pair.b);
			profile.nifNodeNames.put(index, pair.a);
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
	    if ((!unusedPieces.containsKey(armo.getForm())
		    || !unusedPieces.get(armo.getForm()).contains(armaForm))
		    && !AV.block.contains(armo.getForm())) {

		ARMA arma = (ARMA) SPDatabase.getMajor(armaForm, GRUP_TYPE.ARMA);

		// Make sure it has a race
		if (arma == null) {
		    SPGlobal.logError(header, "FormID " + armaForm + " skipped because it couldn't be found in mods.");
		    continue;
		}
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

    public static Map<Integer, LPair<String, ArrayList<String>>> loadNif(String nifPath, LShrinkArray in) {
	Map<Integer, LPair<String, ArrayList<String>>> nifTextures = new HashMap<>();
	try {
	    NIF nif = new NIF(nifPath, in);
	    nifTextures = nif.extractTextures();

	    // To uppercase
	    for (Integer index : nifTextures.keySet()) {
		LPair<String, ArrayList<String>> pair = nifTextures.get(index);
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

		ArrayList<SeedProfile> seeds = varSet.getSeeds();

		if (SPGlobal.logging()) {
		    for (SeedProfile s : seeds) {
			s.print();
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

    static void generateArmorRecords() {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Generating variant records.");
	SPProgressBarPlug.reset();
	SPProgressBarPlug.setMax(VariantProfile.profiles.size());

	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Generate Variants.txt");
	}
	for (VariantProfile profile : VariantProfile.profiles) {
	    profile.generateARMOs();
	    SPProgressBarPlug.incrementBar();
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

    static void createAVRaceObjects() {
	for (FormID armoSrcForm : armors.keySet()) {
	    for (FormID race : armors.get(armoSrcForm).keySet()) {
		AVraces.put(race, new AV_Race(race));
	    }
	}
	for (FormID armoSrcForm : armors.keySet()) {
	    for (FormID race : armors.get(armoSrcForm).keySet()) {
		ArrayList<ARMO_spec> armoVars = armors.get(armoSrcForm).get(race);
		LMergeMap<FormID, ARMO_spec> cellToARMO = new LMergeMap<>(false);
		for (ARMO_spec armorSpec : armoVars) {
		    Set<FormID> cells = armorSpec.spec.getRegions();
		    if (cells.isEmpty() || AV.save.getInt(Settings.PACKAGES_ALLOW_EXCLUSIVE_REGION) < 1) {
			cellToARMO.put(FormID.NULL, armorSpec);
		    } else {
			for (FormID cell : armorSpec.spec.getRegions()) {
			    cellToARMO.put(cell, armorSpec);
			}
		    }
		}

		AVraces.get(race).variantMap.put(armoSrcForm, cellToARMO);
	    }
	}
    }

    static void setUpExclusiveCellList() {
	// If not allowed, leave it empty and return
	if (AV.save.getInt(Settings.PACKAGES_ALLOW_EXCLUSIVE_REGION) < 2) {
	    return;
	}
	ArrayList<Variant> vars = AVPackages.getVariants();
	Set<FormID> eCells = new HashSet<>();
	for (Variant var : vars) {
	    if (var.spec.Exclusive_Region) {
		for (String[] formID : var.spec.Region_Include) {
		    FormID id = FormID.parseString(formID);
		    if (!id.isNull()) {
			if (SPGlobal.logging() && !eCells.contains(id)) {
			    SPGlobal.log("Exclusive Cells", "Adding exclusive cell " + id + " from var " + var + " with source " + var.src);
			}
			eCells.add(id);
		    }
		}
	    }
	}
	AV.quest.getScriptPackage().getScript("AVQuestScript").setProperty("ExclusiveCellList", eCells.toArray(new FormID[0]));
    }

    static void generateFormLists(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + debugNumber++ + " - Generate Form Lists.txt");
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating FormLists for each ARMO variant");
	    SPGlobal.log(header, "====================================================================");
	}
	// For each race with variants
	for (FormID raceID : AVraces.keySet()) {
	    AV_Race avr = AVraces.get(raceID);
	    RACE raceSrc = avr.race;
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "  Generating for race " + raceSrc);
	    }

	    // Generate Cell Index FLST
	    avr.Cells = new FLST(SPGlobal.getGlobalPatch(), "AV_" + avr.race.getEDID() + "_cells_flst");
	    for (FormID cell : avr.getCells()) {
		if (!cell.isNull()) {
		    avr.Cells.addFormEntryAtIndex(cell, 0);
		}
	    }

	    // For each skin with variants applied to that race
	    for (FormID skinID : avr.variantMap.keySet()) {
		ARMO skinSrc = (ARMO) SPDatabase.getMajor(skinID, GRUP_TYPE.ARMO);
		FLST flstSkin = new FLST(SPGlobal.getGlobalPatch(), "AV_" + skinSrc.getEDID() + "_" + raceSrc.getEDID() + "_flst");
		if (raceSrc.getWornArmor().equals(skinID)) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "    Generating for normal skin " + skinSrc);
		    }
		    avr.AltOptions.addFormEntryAtIndex(flstSkin.getForm(), 0);
		    avr.skinKey.add(0, skinID);
		} else {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "    Generating for alt skin " + skinSrc);
		    }
		    avr.AltOptions.addFormEntry(flstSkin.getForm());
		    avr.skinKey.add(skinID);
		}

		// Calculate the lowest common mult between variant probablity dividers
		ArrayList<ARMO_spec> armoVars = avr.variantMap.get(skinID).valuesFlat();
		int[] divs = new int[armoVars.size()];
		for (int i = 0; i < divs.length; i++) {
		    divs[i] = armoVars.get(i).spec.Probability_Divider;
		}
		int lowestCommMult = Ln.lcmm(divs);

		// For each cell
		for (FormID cell : avr.getCells()) {
		    FLST flstCell = new FLST(SPGlobal.getGlobalPatch(), "AV_" + skinSrc.getEDID() + "_" + raceSrc.getEDID() + "_cell_ " + cell.getFormStr() + "_flst");
		    if (!cell.isNull()) {
			flstSkin.addFormEntryAtIndex(flstCell.getForm(), 0);
		    } else {
			flstSkin.addFormEntry(flstCell.getForm());
		    }

		    if (avr.variantMap.get(skinID).containsKey(cell)) {
			if (SPGlobal.logging()) {
			    SPGlobal.log(header, "      Generating for cell " + cell);
			}

			// For each variant
			for (ARMO_spec armorSpec : avr.variantMap.get(skinID).get(cell)) {
			    if (SPGlobal.logging()) {
				SPGlobal.log(header, "        Generating " + (lowestCommMult / armorSpec.spec.Probability_Divider) + " entries for " + armorSpec.armo);
			    }
			    // Generate correct number of entries to get probability
			    for (int i = 0; i < lowestCommMult / armorSpec.spec.Probability_Divider; i++) {
				flstCell.addFormEntry(armorSpec.armo.getForm());
			    }
			}
		    }
		}
	    }
	}
    }

    static void generateSPELvariants(Mod source) {
	for (AV_Race avr : AVraces.values()) {
	    ScriptRef script = AV.generateAttachScript();

	    script.setProperty("AltOptions", avr.AltOptions.getForm());
	    ArrayList<FormID> cells = avr.Cells.getFormIDEntries();
	    if (!cells.isEmpty()) {
		script.setProperty("CellIndexing", cells.toArray(new FormID[0]));
	    }
	    script.setProperty("RaceHeightOffset", avr.race.getHeight(Gender.MALE));

	    // Loop through all variants for this race
	    // and load up non-standard spec file info
	    ArrayList<Integer> heights = new ArrayList<>();
	    ArrayList<Integer> healths = new ArrayList<>();
	    ArrayList<Integer> magickas = new ArrayList<>();
	    ArrayList<Integer> staminas = new ArrayList<>();
	    ArrayList<Integer> speeds = new ArrayList<>();
	    ArrayList<Integer> prefixKey = new ArrayList<>();
	    ArrayList<String> prefix = new ArrayList<>();
	    ArrayList<Integer> affixKey = new ArrayList<>();
	    ArrayList<String> affix = new ArrayList<>();
	    int index = 0;
	    for (ARMO_spec variant : avr.getVariants()) {
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
	    SPEL spell = NiftyFunc.genScriptAttachingSpel(SPGlobal.getGlobalPatch(), script, avr.race.getEDID());
	    avr.race.addSpell(spell.getForm());
	    SPGlobal.getGlobalPatch().addRecord(avr.race);
	}
	SPProgressBarPlug.incrementBar();
    }

    static void standardizeNPCtag(NPC_ n) {
	float weight = n.getWeight() * 100;
	int tmp = (int) Math.round(weight);
	if (tmp != weight) {
	    n.setWeight(tmp / 100);
	    SPGlobal.getGlobalPatch().addRecord(n);
	}
    }

    static void tagNPCs(Mod source) {
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
		    && AVraces.containsKey(n.getRace())) {  // If we have variants for it
		// If fox race but does not have FOX in the name
		// We skip it as it's most likely a lazy modder
		// using the default race: FoxRace
		standardizeNPCtag(n);
		if (n.getRace().equals(foxRace.getForm())
			&& !n.getEDID().toUpperCase().contains("FOX")
			&& !n.getName().toUpperCase().contains("FOX")) {
		    tagNPC(n, 99);
		}
		ArrayList<FormID> skins = AVraces.get(n.getRace()).skinKey;
		int index = skins.indexOf(skin);
		if (index != -1) {
		    tagNPC(n, index);
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "Tagged " + n + " for skin " + SPDatabase.getMajor(skins.get(index), GRUP_TYPE.ARMO));
		    }
		} else {
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
	for (LeveledEntry entry : llist.getEntries()) {
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
	ArrayList<File> files = Ln.generateFileList(new File(AVPackagesDir), false);
	boolean pass = true;
	for (File src : files) {
	    if (isDDS(src)) {
		pass = move(src, AVFileVars.AVTexturesDir);
	    } else if (isNIF(src)) {
		pass = move(src, AVFileVars.AVMeshesDir);
	    }
	}
	if (!pass) {
	    SPGlobal.logError("Move Out", "Failed to move some files out to their texture locations.");
	}
    }

    public static boolean move(File src, String dest) {
	File destFile = new File(dest + src.getPath().substring(src.getPath().indexOf("\\") + 1));
	boolean pass = true;
	if (destFile.isFile()) {
	    pass = pass && destFile.delete();
	}
	if (pass) {
	    try {
		Ln.makeDirs(destFile);
		Files.createLink(destFile.toPath(), src.toPath());
	    } catch (IOException | UnsupportedOperationException ex) {
		SPGlobal.logException(ex);
		try {
		    Files.copy(destFile.toPath(), src.toPath());
		} catch (IOException ex1) {
		    SPGlobal.logException(ex1);
		    pass = false;
		}
	    }
	}
	return pass;
    }

    public static void saveAVPackagesListing() throws IOException {
	Set<String> packageListing = AV.save.getStrings(Settings.PACKAGE_LISTING);
	packageListing.clear();
	ArrayList<File> files = Ln.generateFileList(new File(AVPackagesDir), false);
	for (File f : files) {
	    packageListing.add(f.getPath());
	}
    }

    public static Set<String> getAVPackagesListing() throws IOException {
	return AV.save.getStrings(Settings.PACKAGE_LISTING);
    }

    /*
     * Other Methods
     */
    static public FormID getUsedSkin(NPC_ npcSrc) {
	if (!npcSrc.getSkin().equals(FormID.NULL)) {
	    return npcSrc.getSkin();
	} else {
	    RACE race = (RACE) SPDatabase.getMajor(npcSrc.getRace(), GRUP_TYPE.RACE);
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
	ArrayList<File> files = Ln.generateFileList(new File(folder), 0, 4, false);
	boolean fail = false;
	for (File file : files) {
	    File dest = new File(AVPackagesDir + file.getPath().substring(folder.length()));
	    if (!dest.exists()) {
		if (!Ln.moveFile(file, dest, true)) {
		    fail = true;
		}
	    }
	}
	if (fail) {
	    JOptionPane.showMessageDialog(null,
		    "<html>Error gathering files back to AV Package folder.</html>");
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

    static class AV_Race {

	RACE race;
	Map<FormID, LMergeMap<FormID, ARMO_spec>> variantMap = new HashMap<>();
	FLST AltOptions;
	FLST Cells;
	ArrayList<FormID> skinKey = new ArrayList<>();

	public AV_Race(FormID id) {
	    race = (RACE) SPDatabase.getMajor(id, GRUP_TYPE.RACE);
	    AltOptions = new FLST(SPGlobal.getGlobalPatch(), "AV_" + race.getEDID() + "_flst");
	}

	final public Set<FormID> getCells() {
	    HashSet<FormID> out = new HashSet<>();
	    for (LMergeMap<FormID, ARMO_spec> skin : variantMap.values()) {
		out.addAll(skin.keySet());
	    }
	    return out;
	}

	public ArrayList<ARMO_spec> getVariants() {
	    ArrayList<ARMO_spec> out = new ArrayList<>();
	    for (LMergeMap<FormID, ARMO_spec> skin : variantMap.values()) {
		out.addAll(skin.valuesFlat());
	    }
	    return out;
	}
    }

    public enum VariantType {

	NPC_;
    }
}
