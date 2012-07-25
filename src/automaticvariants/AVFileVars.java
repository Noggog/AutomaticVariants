/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVSaveFile.Settings;
import automaticvariants.gui.PackageTree;
import automaticvariants.gui.PackagesManager;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.swing.JOptionPane;
import lev.LMergeMap;
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
    static ArrayList<BSA> BSAs;
    final public static String AVPackagesDir = "AV Packages\\";
    public static String inactiveAVPackagesDir = "Inactive AV Packages\\";
    public static String AVPackageListing = SPGlobal.pathToInternalFiles + "Last AV Package Listing.txt";
    public static String AVTexturesDir = SPGlobal.pathToData + "textures\\AV Packages\\";
    public static String AVMeshesDir = SPGlobal.pathToData + "meshes\\AV Packages\\";
    static String debugFolder = "File Variants/";
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
    static Set<FormID> taggedNPCs = new HashSet<FormID>();
    ///////////////////
    // AV_Nif name is key
    ///////////////////
    static Map<String, AV_Nif> nifs = new HashMap<String, AV_Nif>();
    static LMergeMap<String, ARMA> nifToARMA = new LMergeMap<String, ARMA>(false);
    // Used to detect alt race variants
    static LMergeMap<ARMO, FormID> ARMOToRace = new LMergeMap<ARMO, FormID>(false);
    //////////////////
    // ArmaSrc is key
    //////////////////
    static Map<FormID, String> armaToNif = new HashMap<FormID, String>();
    static LMergeMap<FormID, ARMA_spec> armatures = new LMergeMap<FormID, ARMA_spec>(false);
    //////////////////
    // ArmoSrc is key for outer, race of arma piece is inner key
    //////////////////
    static Map<FormID, LMergeMap<FormID, ARMO_spec>> armors = new HashMap<FormID, LMergeMap<FormID, ARMO_spec>>();
    //////////////////
    // RaceSrc of piece is key for outer, armo is inner key
    //////////////////
    static Map<FormID, Map<FormID, FLST>> formLists = new HashMap<FormID, Map<FormID, FLST>>();
    static LMergeMap<FormID, ARMO_spec> compiledVariants = new LMergeMap<FormID, ARMO_spec>(false);
    //////////////////
    // RaceSrc is key
    //////////////////
    static Map<FormID, AV_SPEL> switcherSpells = new HashMap<FormID, AV_SPEL>();
    static public ArrayList<VariantProfile> profiles;

    static void setUpFileVariants(Mod source) throws IOException, Uninitialized, BadParameter {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "1 - Import Packages.txt");
	    File f = new File(SPGlobal.pathToDebug() + "Asynchronous log.txt");
	    if (f.isFile()) {
		f.delete();
	    }
	}

	BSAs = BSA.loadInBSAs(BSA.FileType.NIF, BSA.FileType.DDS);

	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Importing AV Packages");
	if (!AVPackages.isLeaf()) {
	    // Change packages to enabled/disabled based on GUI requests
	    shufflePackages();
	}
	importVariants();
	SPProgressBarPlug.incrementBar();

	// Locate unused Races/Skins
	locateUnused();

	loadProfiles();

	// Locate and load NIFs, and assign their variants
	linkToNifs();

	// Generate TXSTs
	generateTXSTvariants();

	// Generate ARMA dups that use TXSTs
	generateARMAvariants(source);

	// Generate ARMO dups that use ARMAs
	generateARMOvariants(source);

	implementOrigAsVar();

	printVariants();

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
	    SPGlobal.newLog(debugFolder + "2 - Locate Unused.txt");
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
	profiles = VariantProfile.profiles;
	SPGlobal.newLog("Load Variant Profiles.txt");
	locateUsedNIFs();
	loadUsedNIFs();
	loadUsedARMOs();
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
			if (VariantProfile.find(null, null, null, nifPath, null) == null) {
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
		    profile.textures = loadNif(profile.nifPath, nifData);
		    if (profile.textures.isEmpty()) {
			VariantProfile.profiles.remove(profile);
			SPGlobal.log(profile.toString(), "Removing profile with nif because it had no textures: " + profile.nifPath);
		    }
		} else {
		    SPGlobal.logError(header, "Error locating nif file: " + profile.nifPath);
		}
	    } catch (IOException | DataFormatException ex) {
		SPGlobal.logException(ex);
	    }
	}
    }

    public static void loadUsedARMOs() {
	SPGlobal.log(header, "===========================================================");
	SPGlobal.log(header, "================      Loading Records     =================");
	SPGlobal.log(header, "===========================================================");
	for (ARMO armo : AV.getMerger().getArmors()) {
	    if (!unusedSkins.contains(armo.getForm())) {
		if (!AV.block.contains(armo.getForm())) {
		    loadUsedARMAs(armo);
		} else {
		    SPGlobal.log(header, "Blocked because it was on the blocklist: " + armo);
		}
	    }
	}
    }

    public static void loadUsedARMAs(ARMO armo) {
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
		VariantProfile profile = VariantProfile.find(null, null, null, nifPath, null);


		if (profile != null) {
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
			profile.race = r;
			profile.skin = armo;
			profile.piece = arma;
		    }
		} else {
		    SPGlobal.log(header, "Skipped " + arma + ", could not find a profile matching nif: " + nifPath);
		}
	    }
	}
    }

    public static ArrayList<String> loadNif(String nifPath, LShrinkArray in) {
	NIF nif;
	ArrayList<String> out = new ArrayList<>();
	try {
	    nif = new NIF(nifPath, in);
	    ArrayList<ArrayList<String>> nifTextures = new ArrayList<>();

	    ArrayList<ArrayList<NIF.Node>> NiTriShapes = nif.getNiTriShapePackages();
	    for (ArrayList<NIF.Node> nodes : NiTriShapes) {
		for (NIF.Node n : nodes) {
		    if (n.type == NIF.NodeType.BSSHADERTEXTURESET) {
			nifTextures.add(NIF.extractBSTextures(n));
		    }
		}
	    }

	    for (ArrayList<String> list : nifTextures) {
		for (String texPath : list) {
		    if (!texPath.equals("")) {
			out.add(texPath.toUpperCase());
		    }
		}
	    }

	} catch (BadParameter | java.nio.BufferUnderflowException ex) {
	    SPGlobal.logException(ex);
	}
	return out;
    }

    static void linkToNifs() {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Linking packages to .nif files.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "3 - Link to NIFs.txt");
	}
	for (PackageNode avPackageC : AVPackages.getAll(PackageNode.Type.PACKAGE)) {
	    AVPackage avPackage = (AVPackage) avPackageC;
	    for (VariantSet varSet : avPackage.sets) {
		if (varSet.spec == null || varSet.isEmpty()) {
		    SPGlobal.logError(header, "Skipping " + varSet.src + " because it was empty or missing a spec file.");
		    continue;
		}
		ArrayList<FormID> uniqueArmas = new ArrayList<FormID>();
		for (String[] s : varSet.spec.Target_FormIDs) {
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
			    SPGlobal.logError(header, "Could not locate NPC with FormID: " + id);
			    continue;
			} else if (SPGlobal.logging()) {
			    SPGlobal.log(header, "  " + record);
			}

			// Get Skin
			FormID skinID = getUsedSkin(record);
			if (skinID == null) {
			    SPGlobal.logError(header, "NPC did not have a skin: " + record);
			}
			ARMO skin = (ARMO) SPDatabase.getMajor(skinID, GRUP_TYPE.ARMO);
			if (skin == null) {
			    SPGlobal.logError(header, "Could not locate ARMO with FormID: " + skinID);
			    continue;
			} else if (SPGlobal.logging()) {
			    SPGlobal.log(header, "  " + skin);
			}

			// Didn't have a skin
			if (skin.getArmatures().isEmpty()) {
			    SPGlobal.logError(header, skin + " did not have any armatures.");
			    continue;
			}

			// Locate armature that matches armor's race
			ARMA piece = null;
			boolean found = false;
			for (FormID arma : skin.getArmatures()) {
			    piece = (ARMA) SPDatabase.getMajor(arma);
			    if (record.getRace().equals(piece.getRace())) {
				found = true;
				break;
			    }
			    for (FormID additionalRace : piece.getAdditionalRaces()) {
				if (record.getRace().equals(additionalRace)) {
				    found = true;
				    break;
				}
			    }
			}
			if (!found || piece == null) {
			    SPGlobal.logError(header, "Could not locate ARMA matching " + record + "'s race");
			    continue;
			} else if (uniqueArmas.contains(piece.getForm())) {
			    SPGlobal.log(header, "  Already logged " + piece + " for this variant set.");
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
			if (!armaToNif.containsKey(piece.getForm()) && shouldSplit(nifPath, piece, skin)) {
			    // Has alt texture, separate
			    if (!splitVariant(nifPath, piece)) {
				continue;
			    }
			} else if (!nifs.containsKey(nifPath)) {
			    AVFileVars.AV_Nif nif = new AVFileVars.AV_Nif(nifPath);
			    if (!nif.load()) {
				continue;
			    }
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

			String nif = armaToNif.get(piece.getForm());
			nifToARMA.put(nif, piece);
			ARMOToRace.put(skin, piece.getRace());

			uniqueArmas.add(piece.getForm());
			for (Variant v : varSet.multiplyAndFlatten()) {
			    nifs.get(nif).variants.add((Variant) Ln.deepCopy(v));
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

	}
	SPProgressBarPlug.incrementBar();
    }

    static boolean shouldSplit(String nifPath, ARMA piece, ARMO skin) {

	// If has alt texture set and they aren't logged
	if (!piece.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()) {
	    if (nifToARMA.containsKey(nifPath)) {
		for (ARMA rhs : nifToARMA.get(nifPath)) {
		    if (piece.equalAltTextures(rhs, Gender.MALE, Perspective.THIRD_PERSON)) {
			return false;
		    }
		}
	    }
	    if (SPGlobal.logging()) {
		SPGlobal.log("SplitVar", "  Record warrents split due to alt textures in ARMA.");
	    }
	    return true;
	}

	// If different race
	if (!skin.getRace().equals(piece.getRace())) {
	    if (!ARMOToRace.containsKey(skin)
		    || !ARMOToRace.get(skin).contains(piece.getRace())) {
		if (SPGlobal.logging()) {
		    SPGlobal.log("SplitVar", "  Record warrents split due to alt race.");
		}
		return true;
	    }
	}

	return false;
    }

    static boolean splitVariant(String nifPath, ARMA piece) throws IOException, BadParameter, DataFormatException {
	AVFileVars.AV_Nif nif = new AVFileVars.AV_Nif(nifPath);
	if (!nif.load()) {
	    return false;
	}
	SPGlobal.log(header, "  Nif path: " + nifPath);
	nif.print();

	// Need to change old filenames to alt texture filenames
	for (ARMA.AltTexture t : piece.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON)) {
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
	return true;
    }

    static void generateTXSTvariants() throws IOException {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Generating TXST variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "4 - Generate TXST.txt");
	}
	for (AVFileVars.AV_Nif n : nifs.values()) {

	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "====================================================================");
		SPGlobal.log(header, "Generating TXST records for Nif: " + n.name);
		SPGlobal.log(header, "====================================================================");
	    }
	    for (Variant v : n.variants) {

		// Find out which TXSTs need to be generated
		String[][] replacements = new String[n.textureFields.size()][numSupportedTextures];
		boolean[] needed = new boolean[n.textureFields.size()];
		for (PackageNode f : v.textures) {
		    int i = 0;
		    for (AVFileVars.AV_Nif.TextureField textureSet : n.textureFields) {
			int j = 0;
			for (String texture : textureSet.maps) {
			    if (!texture.equals("") && texture.lastIndexOf('\\') != -1) {
				String textureName = texture.substring(texture.lastIndexOf('\\') + 1);
				if (textureName.equalsIgnoreCase(f.src.getName())) {
				    replacements[i][j] = f.src.getPath();
				    needed[i] = true;
				}
			    }
			    if (j == numSupportedTextures - 1) {
				break;
			    } else {
				j++;
			    }
			}
			i++;
		    }
		}

		// Make new TXSTs
		v.TXSTs = new TextureVariant[n.textureFields.size()];
		int i = 0;
		TXST last = null;
		for (AVFileVars.AV_Nif.TextureField textureSet : n.textureFields) {
		    if (needed[i]) {
			if (textureSet.unique) {
			    // New TXST
			    TXST tmpTXST = new TXST(SPGlobal.getGlobalPatch(), v.name + "_" + n.uniqueName() + "_" + textureSet.title + "_txst");
			    tmpTXST.set(TXST.TXSTflag.FACEGEN_TEXTURES, true);

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
				if (j == numSupportedTextures - 1) {
				    break;
				} else {
				    j++;
				}
			    }
			    last = tmpTXST;
			}
			v.TXSTs[i] = new TextureVariant(last, textureSet.title);
		    }
		    i++;
		}
		if (SPGlobal.logging()) {
		    SPGlobal.log("Variant", "  --------------------------------------------------------------------------------");
		}
	    }
	    n.textureFields = null; // Not needed anymore
	}
	SPProgressBarPlug.incrementBar();
    }

    static void generateARMAvariants(Mod source) {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Generating ARMA variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "5 - Generate ARMA + ARMO.txt");
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating ARMA duplicates for each NIF");
	    SPGlobal.log(header, "====================================================================");
	}
	for (ARMA armaSrc : source.getArmatures()) {
	    if (AV.save.getBool(Settings.MINIMIZE_PATCH) && unusedSkins.contains(armaSrc.getForm())) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "  Skipping " + armaSrc + " because it was unused.");
		}
		continue;
	    }

	    // If we have variants for it
	    if (armaToNif.containsKey(armaSrc.getForm())) {
		AVFileVars.AV_Nif malenif = nifs.get(armaToNif.get(armaSrc.getForm()));
		if (malenif != null) { // we have variants for that nif
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Duplicating " + armaSrc + ", for nif: " + armaToNif.get(armaSrc.getForm()));
		    }
		    ArrayList<AVFileVars.ARMA_spec> dups = new ArrayList<AVFileVars.ARMA_spec>();
		    for (Variant v : malenif.variants) {
			ARMA dup = (ARMA) SPGlobal.getGlobalPatch().makeCopy(armaSrc, v.name + "_ID_" + armaSrc.getEDID() + "_arma");

			ArrayList<ARMA.AltTexture> alts = dup.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON);
			alts.clear();
			int i = 0;
			for (TextureVariant texVar : v.TXSTs) {
			    if (texVar != null) {
				alts.add(new ARMA.AltTexture(texVar.nifFieldName, texVar.textureRecord.getForm(), i));
			    }
			    i++;
			}

//			ArrayList<ARMA.AltTexture> femalealts = dup.getAltTextures(Gender.FEMALE, Perspective.THIRD_PERSON);
//			femalealts.clear();
//			femalealts.addAll(alts);

			dups.add(new AVFileVars.ARMA_spec(dup, v.spec));
		    }
		    armatures.put(armaSrc.getForm(), dups);
		}
	    }
	}
	SPProgressBarPlug.incrementBar();
    }

    static void generateARMOvariants(Mod source) {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Generating ARMO variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating ARMO skin duplicates for each ARMA");
	    SPGlobal.log(header, "====================================================================");
	}
	for (ARMO armoSrc : source.getArmors()) {

	    LMergeMap<FormID, AVFileVars.ARMA_spec> targets = new LMergeMap<FormID, AVFileVars.ARMA_spec>(false);
	    for (FormID armaForm : armoSrc.getArmatures()) {
		ARMA arma = (ARMA) SPDatabase.getMajor(armaForm);
		if (arma != null) {
		    if (armatures.containsKey(armaForm)) {
			targets.put(armaForm, armatures.get(armaForm));
		    }
		}
	    }

	    if (AV.save.getBool(Settings.MINIMIZE_PATCH)
		    && !targets.isEmpty() && unusedSkins.contains(armoSrc.getForm())) {
		continue;
	    }

	    for (FormID arma : targets.keySet()) {
		// See if piece is unused
		if (AV.save.getBool(Settings.MINIMIZE_PATCH)
			&& unusedPieces.get(armoSrc.getForm()).contains(arma)) {
		    continue;
		}

		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "  Duplicating " + armoSrc + ", for " + SPDatabase.getMajor(arma, GRUP_TYPE.ARMA));
		}
		if (!armors.containsKey(armoSrc.getForm())) {
		    armors.put(armoSrc.getForm(), new LMergeMap<FormID, AVFileVars.ARMO_spec>(false));
		}
		for (AVFileVars.ARMA_spec variant : targets.get(arma)) {
		    String edid = variant.arma.getEDID().substring(0, variant.arma.getEDID().lastIndexOf("_arma"));
		    edid = edid.substring(0, edid.lastIndexOf("_")) + "_" + armoSrc.getEDID() + "_armo";  // replace ARMA string with ARMO name
		    ARMO dup = (ARMO) SPGlobal.getGlobalPatch().makeCopy(armoSrc, edid);

		    dup.removeArmature(arma);
		    dup.addArmature(variant.arma.getForm());
		    armors.get(armoSrc.getForm()).put(variant.arma.getRace(), new AVFileVars.ARMO_spec(dup, variant, variant.arma.getRace()));
		}
	    }
	}
	SPProgressBarPlug.incrementBar();
    }

    static void implementOrigAsVar() {
	if (AV.save.getBool(Settings.PACKAGES_ORIG_AS_VAR)) {
	    for (FormID armoSrc : armors.keySet()) {
		ARMO src = (ARMO) SPDatabase.getMajor(armoSrc, GRUP_TYPE.ARMO);
		for (FormID race : armors.get(armoSrc).keySet()) {
		    armors.get(armoSrc).get(race).add(new ARMO_spec(src, race));
		}
	    }
	}
    }

    static void printVariants() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "Variants loaded: ");
	    for (FormID srcArmor : armors.keySet()) {
		SPGlobal.log(header, "  Armor " + SPDatabase.getMajor(srcArmor) + " has " + armors.get(srcArmor).size() + " variants.");
		for (FormID race : armors.get(srcArmor).keySet()) {
		    SPGlobal.log(header, "    For race: " + SPDatabase.getMajor(race, GRUP_TYPE.RACE));
		    for (AVFileVars.ARMO_spec variant : armors.get(srcArmor).get(race)) {
			SPGlobal.log(header, "      " + variant.armo);
			for (String spec : variant.spec.print()) {
			    SPGlobal.log(header, "         " + spec);
			}
		    }
		}
	    }
	}
    }

    static void generateFormLists(Mod source) {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Generating Form Lists.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "6 - Generate Form Lists.txt");
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
	    SPGlobal.newLog(debugFolder + "7 - Generate Switcher Spells.txt");
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

    static void standardizeNPC(NPC_ n) {
	float weight = n.getWeight() * 100;
	int tmp = (int) Math.round(weight);
	if (tmp != weight) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "Standardized " + n);
	    }
	    n.setWeight(tmp / 100);
	    SPGlobal.getGlobalPatch().addRecord(n);
	}
    }

    static void tagNPCs(Mod source) {
	SPProgressBarPlug.setStatus(AV.step++, AV.numSteps, "Tagging NPCs.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "8 - Tagging NPCs.txt");
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Tagging NPCs that have alt races");
	    SPGlobal.log(header, "====================================================================");
	}
	RACE foxRace = (RACE) SPDatabase.getMajor(new FormID("109C7CSkyrim.esm"), GRUP_TYPE.RACE);
	for (NPC_ n : source.getNPCs()) {
	    FormID skin = getUsedSkin(n);
	    if (skin != null
		    && n.getTemplate().isNull() // Not templated
		    && !skin.isNull() // If has alt skin
		    && switcherSpells.containsKey(n.getRace())) {  // If we have variants for it
		// If fox race but does not have FOX in the name
		// We skip it as it's most likely a lazy modder
		// using the default race: FoxRace
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

    static public void shufflePackages() {
	PackageTree tree = PackagesManager.tree;
	if (tree != null) {
	    PackageNode root = (PackageNode) tree.getRoot();
	    boolean fail;
	    try {
		fail = !root.moveNode();
	    } catch (IOException ex) {
		SPGlobal.logException(ex);
		fail = true;
	    }

	    if (fail) {
		JOptionPane.showMessageDialog(null,
			"<html>Error moving one of the selected files.  This is probably due to AV being run<br>"
			+ "inside a 'windows protected' folder where windows is not allowing the moves.  Either<br>"
			+ "move your Skyrim to an unprotected folder location (outside Program Files), or manually<br>"
			+ "install/uninstall packages by moving them in/out of the AV Packages folder yourself.</html>");
	    }

	    root.pruneDisabled();
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
    static class AV_Nif {

	String name;
	String path;
	ArrayList<AVFileVars.AV_Nif.TextureField> textureFields = new ArrayList<AVFileVars.AV_Nif.TextureField>();
	ArrayList<Variant> variants = new ArrayList<Variant>();

	AV_Nif(String path) {
	    name = path;
	    this.path = "meshes\\" + path;
	}

	final boolean load() throws BadParameter, IOException, DataFormatException {
	    LShrinkArray usedFileData = BSA.getUsedFile(path);
	    if (usedFileData == null) {
		SPGlobal.logError(header, "NIF file " + path + " did not exist.");
		return false;
	    }
	    NIF nif = new NIF(path, usedFileData);

	    Map<Integer, NIF.Node> BiLightingShaderProperties = nif.getNodes(NIF.NodeType.BSLIGHTINGSHADERPROPERTY);
	    Map<Integer, NIF.Node> BiShaderTextureNodes = nif.getNodes(NIF.NodeType.BSSHADERTEXTURESET);
	    Map<Integer, ArrayList<String>> BiShaderTextureSets = new HashMap<Integer, ArrayList<String>>();
	    Map<Integer, AVFileVars.AV_Nif.TextureField> fields = new HashMap<Integer, AVFileVars.AV_Nif.TextureField>();
	    ArrayList<ArrayList<NIF.Node>> NiTriShapes = nif.getNiTriShapePackages();

	    for (Integer i : BiShaderTextureNodes.keySet()) {
		BiShaderTextureSets.put(i, NIF.extractBSTextures(BiShaderTextureNodes.get(i)));
	    }

	    int i = 0;
	    for (Integer key : BiLightingShaderProperties.keySet()) {
		int textureLink = BiLightingShaderProperties.get(key).data.extractInt(40, 4);
		AVFileVars.AV_Nif.TextureField next = new AVFileVars.AV_Nif.TextureField();
		if (fields.containsKey(textureLink)) {
		    next.maps = fields.get(textureLink).maps;
		    next.unique = false;
		} else {
		    next.maps = BiShaderTextureSets.get(textureLink);
		    next.unique = true;
		    fields.put(textureLink, next);
		}

		next.title = NiTriShapes.get(i).get(0).title;
		if (SPGlobal.logging()) {
		    SPGlobal.log("AV_Nif", "  Loaded NiTriShapes: " + next.title + " (" + BiLightingShaderProperties.get(key).number + ") linked to texture index " + textureLink);
		}
		this.textureFields.add(next);
		i++;
	    }

	    return true;
	}

	public void print() {
	    if (SPGlobal.logging()) {
		int i = 0;
		for (AVFileVars.AV_Nif.TextureField set : textureFields) {
		    SPGlobal.log("AV_Nif", "  Texture index " + i++ + ": " + set.title);
		    int j = 0;
		    for (String s : set.maps) {
			if (!s.equals("")) {
			    SPGlobal.log("AV_Nif", "    " + j++ + ": " + s);
			}
		    }
		}
	    }
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    final AVFileVars.AV_Nif other = (AVFileVars.AV_Nif) obj;
	    if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
		return false;
	    }
	    return true;
	}

	@Override
	public int hashCode() {
	    int hash = 7;
	    hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
	    return hash;
	}

	public String uniqueName() {
	    return path.substring(path.lastIndexOf("\\") + 1) + hashCode();
	}

	class TextureField {

	    String title = "No Title";
	    ArrayList<String> maps = new ArrayList<String>(0);
	    boolean unique = false;

	    TextureField() {
	    }

	    TextureField(AVFileVars.AV_Nif.TextureField in) {
		this.title = in.title;
		this.maps = in.maps;
	    }

	    @Override
	    public boolean equals(Object obj) {
		if (obj == null) {
		    return false;
		}
		if (getClass() != obj.getClass()) {
		    return false;
		}
		final AVFileVars.AV_Nif.TextureField other = (AVFileVars.AV_Nif.TextureField) obj;
		if (this.maps != other.maps && (this.maps == null || !this.maps.equals(other.maps))) {
		    return false;
		}
		return true;
	    }

	    @Override
	    public int hashCode() {
		int hash = 7;
		hash = 71 * hash + (this.maps != null ? this.maps.hashCode() : 0);
		return hash;
	    }
	}
    }

    static class ARMA_spec {

	ARMA arma;
	SpecVariant spec;

	ARMA_spec(ARMA arma, SpecVariant spec) {
	    this.arma = arma;
	    this.spec = spec;
	}
    }

    static class ARMO_spec {

	ARMO armo;
	SpecVariant spec;
	ARMA targetArma;
	FormID targetRace;

	ARMO_spec(ARMO armoSrc, FormID targetRace) {
	    this.armo = armoSrc;
	    this.targetRace = targetRace;
	    spec = new SpecVariant();
	}

	ARMO_spec(ARMO armo, ARMA_spec spec, FormID targetRace) {
	    this.armo = armo;
	    targetArma = spec.arma;
	    this.targetRace = targetRace;
	    this.spec = spec.spec;
	}
    }

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

    public enum VariantType {

	NPC_;
    }
}
