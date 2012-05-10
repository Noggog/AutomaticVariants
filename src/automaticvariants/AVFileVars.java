/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVSaveFile.Settings;
import automaticvariants.Variant.VariantSpec;
import automaticvariants.gui.PackageTree;
import automaticvariants.gui.SettingsPackagesManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.zip.DataFormatException;
import javax.swing.JOptionPane;
import lev.LMergeMap;
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
 * @author Justin Swanson
 */
public class AVFileVars {

    static String header = "AV_FileVar";
    static ArrayList<BSA> BSAs;
    final public static String AVPackagesDir = "AV Packages\\";
    public static String inactiveAVPackagesDir = "Inactive AV Packages\\";
    public static String AVTexturesDir = SPGlobal.pathToData + "textures\\AV Packages\\";
    public static String AVMeshesDir = SPGlobal.pathToData + "meshes\\AV Packages\\";
    static String debugFolder = "File Variants/";
    static int numSupportedTextures = 8;
    public static PackageComponent AVPackages = new PackageComponent(new File(AVPackagesDir), PackageComponent.Type.ROOT);
    /*
     * Variant storage lists/maps
     */
    // List of unused things to skip
    static HashSet<FormID> unusedRaces;
    static HashSet<FormID> unusedSkins;
    static LMergeMap<FormID, FormID> unusedPieces;
    // List of what races the armor "supports"
    static LMergeMap<FormID, FormID> armoRaces;
    ///////////////////
    // AV_Nif name is key
    ///////////////////
    static Map<String, AV_Nif> nifs = new HashMap<String, AV_Nif>();
    static LMergeMap<String, ARMA> nifToARMA = new LMergeMap<String, ARMA>(false);
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
    //////////////////
    // RaceSrc is key
    //////////////////
    static Map<FormID, SPEL_setup> switcherSpells = new HashMap<FormID, SPEL_setup>();
    static Map<NPC_, Map<FormID, FormID>> npcRacesToSkins = new HashMap<NPC_, Map<FormID, FormID>>();

    static void setUpFileVariants(Mod source, Mod patch) throws IOException, Uninitialized, BadParameter {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "1 - Import Packages.txt");
	    File f = new File(SPGlobal.pathToDebug() + "Asynchronous log.txt");
	    if (f.isFile()) {
		f.delete();
	    }
	}

	BSAs = BSA.loadInBSAs(BSA.FileType.NIF, BSA.FileType.DDS);

	SPProgressBarPlug.progress.setStatus(AV.step++, AV.numSteps, "Importing AV Packages");
	if (!AVPackages.isLeaf()) {
	    // Change packages to enabled/disabled based on GUI requests
	    shufflePackages();
	}
	importVariants();
	SPProgressBarPlug.progress.incrementBar();

	// Locate unused Races/Skins
	locateUnused(source);

	// Locate and load NIFs, and assign their variants
	linkToNifs();

	// Generate TXSTs
	generateTXSTvariants();

	// Generate ARMA dups that use TXSTs
	generateARMAvariants(source);

	// Generate ARMO dups that use ARMAs
	generateARMOvariants(source);

	skinSwitchMethod(source);

	for (PackageComponent p : AVPackages.getAll()) {
	    p.moveOut();
	}
    }

    static void skinSwitchMethod(Mod source) {

	// Generate FormLists of RACE variants
	generateFormLists(source);

	// Generate Spells that use Race Switcher Magic Effects
	generateSPELvariants(source);

	// Add AV keywords to NPCs that have alt skins
	loadAltNpcs(source);
    }

    /*
     * Shared methods
     */
    public static void importVariants() throws FileNotFoundException, IOException {
	String header = "Import Variants";
	File AVPackagesDirFile = new File(AVPackagesDir);

	// wipe
	AVPackages = new PackageComponent(new File(AVPackagesDir), PackageComponent.Type.ROOT);
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

    public static void locateUnused(Mod source) {

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
		    armoRaces.put(armor.getForm(), arma.getRace());
		    unusedPiecesTmp.put(armor.getForm(), arma);
		}
	    }
	}


	// Removed used races/skins/pieces
	for (NPC_ n : source.getNPCs()) {
	    unusedRaces.remove(n.getRace());
	    FormID skin = getUsedSkin(n);
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

    static void linkToNifs() {
	SPProgressBarPlug.progress.setStatus(AV.step++, AV.numSteps, "Linking packages to .nif files.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "3 - Link to NIFs.txt");
	}
	for (PackageComponent avPackageC : AVPackages.getAll(PackageComponent.Type.PACKAGE)) {
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
			    SPGlobal.logError(header, "Could not locate NPC with FormID: " + s);
			    continue;
			} else if (SPGlobal.logging()) {
			    SPGlobal.log(header, "  " + record);
			}

			// NPC's skin field
			ARMO skin;
			skin = (ARMO) SPDatabase.getMajor(record.getSkin(), GRUP_TYPE.ARMO);

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
			    if (piece.getRace().equals(record.getRace())) {
				break;
			    }
			    piece = null;
			}
			if (piece == null) {
			    SPGlobal.logError(header, "Could not locate ARMA matching ARMO's race");
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
	SPProgressBarPlug.progress.incrementBar();
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

    static void splitVariant(String nifPath, ARMA piece) throws IOException, BadParameter, DataFormatException {
	AV_Nif nif = new AV_Nif(nifPath);
	nif.load();
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
    }

    static void generateTXSTvariants() throws IOException {
	SPProgressBarPlug.progress.setStatus(AV.step++, AV.numSteps, "Generating TXST variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "4 - Generate TXST.txt");
	}
	for (AV_Nif n : nifs.values()) {

	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "====================================================================");
		SPGlobal.log(header, "Generating TXST records for Nif: " + n.name);
		SPGlobal.log(header, "====================================================================");
	    }
	    for (Variant v : n.variants) {

		// Find out which TXSTs need to be generated
		String[][] replacements = new String[n.textureFields.size()][numSupportedTextures];
		boolean[] needed = new boolean[n.textureFields.size()];
		for (PackageComponent f : v.textures) {
		    int i = 0;
		    for (AV_Nif.TextureField textureSet : n.textureFields) {
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
		for (AV_Nif.TextureField textureSet : n.textureFields) {
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
	SPProgressBarPlug.progress.incrementBar();
    }

    static void generateARMAvariants(Mod source) {
	SPProgressBarPlug.progress.setStatus(AV.step++, AV.numSteps, "Generating ARMA variants.");
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
		AV_Nif malenif = nifs.get(armaToNif.get(armaSrc.getForm()));
		if (malenif != null) { // we have variants for that nif
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Duplicating " + armaSrc + ", for nif: " + armaToNif.get(armaSrc.getForm()));
		    }
		    ArrayList<ARMA_spec> dups = new ArrayList<ARMA_spec>();
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

			dups.add(new ARMA_spec(dup, v.spec));
		    }
		    armatures.put(armaSrc.getForm(), dups);
		}
	    }
	}
	SPProgressBarPlug.progress.incrementBar();
    }

    static void generateARMOvariants(Mod source) {
	SPProgressBarPlug.progress.setStatus(AV.step++, AV.numSteps, "Generating ARMO variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating ARMO skin duplicates for each ARMA");
	    SPGlobal.log(header, "====================================================================");
	}
	for (ARMO armoSrc : source.getArmors()) {

	    LMergeMap<FormID, ARMA_spec> targets = new LMergeMap<FormID, ARMA_spec>(false);
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
		    armors.put(armoSrc.getForm(), new LMergeMap<FormID, ARMO_spec>(false));
		}
		for (ARMA_spec variant : targets.get(arma)) {
		    String edid = variant.arma.getEDID().substring(0, variant.arma.getEDID().lastIndexOf("_arma"));
		    edid = edid.substring(0, edid.lastIndexOf("_")) + "_" + armoSrc.getEDID() + "_armo";  // replace ARMA string with ARMO name
		    ARMO dup = (ARMO) SPGlobal.getGlobalPatch().makeCopy(armoSrc, edid);

		    dup.removeArmature(arma);
		    dup.addArmature(variant.arma.getForm());
		    armors.get(armoSrc.getForm()).put(variant.arma.getRace(), new ARMO_spec(dup, variant, variant.arma.getRace()));
		}
	    }
	}
	printVariants();
	SPProgressBarPlug.progress.incrementBar();
    }

    static void printVariants() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "Variants loaded: ");
	    for (FormID srcArmor : armors.keySet()) {
		SPGlobal.log(header, "  Armor " + SPDatabase.getMajor(srcArmor) + " has " + armors.get(srcArmor).size() + " variants.");
		for (FormID armoSrc : armors.keySet()) {
		    for (FormID race : armors.get(armoSrc).keySet()) {
			SPGlobal.log(header, "    For race: " + SPDatabase.getMajor(race, GRUP_TYPE.RACE));
			for (ARMO_spec variant : armors.get(armoSrc).get(race)) {
			    SPGlobal.log(header, "      " + variant.armo + ", prob divider: 1/" + variant.probDiv);
			}
		    }
		}
	    }
	}
    }

    /*
     * Race Switch Methods
     */
    static void generateFormLists(Mod source) {
	SPProgressBarPlug.progress.setStatus(AV.step++, AV.numSteps, "Generating Form Lists.");
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
		    divs[i] = armoVars.get(i).probDiv;
		}
		int lowestCommMult = Ln.lcmm(divs);

		for (ARMO_spec armorSpec : armors.get(armoSrcForm).get(race)) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "      Generating " + (lowestCommMult / armorSpec.probDiv) + " entries for " + armorSpec.armo);
		    }
		    for (int i = 0; i < lowestCommMult / armorSpec.probDiv; i++) {
			flst.addFormEntry(armorSpec.armo.getForm());
		    }
		}
		if (!formLists.containsKey(race)) {
		    formLists.put(race, new HashMap<FormID, FLST>());
		}
		formLists.get(race).put(armoSrcForm, flst);
	    }

	}
	SPProgressBarPlug.progress.incrementBar();
    }

    static void generateSPELvariants(Mod source) {
	SPProgressBarPlug.progress.setStatus(AV.step++, AV.numSteps, "Generating script attachment races.");
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
		SPEL_setup spell = new SPEL_setup(raceSrc);

		// Add Alt Skins to key/array setup
		for (FormID skinF : formLists.get(raceSrc.getForm()).keySet()) {
		    if (!skinF.equals(raceSrc.getWornArmor())) {
			if (SPGlobal.logging()) {
			    SPGlobal.log(header, "  Has alt skin " + SPDatabase.getMajor(skinF, GRUP_TYPE.ARMO));
			}
			ARMO skin = (ARMO) SPDatabase.getMajor(skinF, GRUP_TYPE.ARMO);
			FLST npcList = new FLST(SPGlobal.getGlobalPatch(), "AV_" + raceSrc.getEDID() + "_" + skin.getEDID() + "_flst_NPCList");
			spell.key.addFormEntry(npcList.getForm());
			spell.array.addFormEntry(formLists.get(raceSrc.getForm()).get(skinF).getForm());
		    }
		}

		// Add normal worn armor to last on the array
		if (formLists.get(raceSrc.getForm()).containsKey(raceSrc.getWornArmor())) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Added normal skin " + SPDatabase.getMajor(raceSrc.getWornArmor(), GRUP_TYPE.ARMO));
		    }
		    spell.array.addFormEntry(formLists.get(raceSrc.getForm()).get(raceSrc.getWornArmor()).getForm());
		}

		switcherSpells.put(raceSrc.getForm(), spell);
		raceSrc.addSpell(spell.spell.getForm());
		SPGlobal.getGlobalPatch().addRecord(raceSrc);
	    }
	}
	SPProgressBarPlug.progress.incrementBar();
    }

    static void loadAltNpcs(Mod source) {
	SPProgressBarPlug.progress.setStatus(AV.step++, AV.numSteps, "Loading NPCs.");
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder + "8 - Loading NPCs.txt");
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Loading NPCs that have alt races");
	    SPGlobal.log(header, "====================================================================");
	}
	for (NPC_ n : source.getNPCs()) {
	    // Race is key, Skin is value
	    Map<FormID, FormID> racesAndSkins = getRacesAndSkins(n);
	    if (!racesAndSkins.isEmpty()) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, n.toString());
		    for (FormID race : racesAndSkins.keySet()) {
			SPGlobal.log(header, "  " + SPDatabase.getMajor(race, GRUP_TYPE.RACE));
			SPGlobal.log(header, "    " + SPDatabase.getMajor(racesAndSkins.get(race), GRUP_TYPE.ARMO));
		    }
		}
//		if (keywords.containsKey(n.getSkin())) {
//		    if (SPGlobal.logging()) {
//			SPGlobal.log(header, "Tagged " + n);
//		    }
//		    n.keywords.addKeywordRef(keywords.get(n.getSkin()).getForm());
//		    SPGlobal.getGlobalPatch().addRecord(n);
//		} else {
//		    RACE race = (RACE) SPDatabase.getMajor(n.getRace(), GRUP_TYPE.RACE);
//		    if (!race.getWornArmor().equals(n.getSkin())) {
//			if (SPGlobal.logging()) {
//			    SPGlobal.log(header, "Tagged As Excluded Skin " + n);
//			}
//			n.keywords.addKeywordRef(excluded.getForm());
//			SPGlobal.getGlobalPatch().addRecord(n);
//		    }
//		}
	    }
	}
    }

    static Map<FormID, FormID> getRacesAndSkins(NPC_ n) {
	if (npcRacesToSkins.containsKey(n)) {
	    return npcRacesToSkins.get(n);
	}

	Map<FormID, FormID> out;
	if (!n.getTemplate().isNull() && n.get(NPC_.TemplateFlag.USE_TRAITS)) {
	    NPC_ npc = (NPC_) SPDatabase.getMajor(n.getTemplate(), GRUP_TYPE.NPC_);
	    if (npc != null) {
		out = getRacesAndSkins(npc);
	    } else {
		out = getRacesAndSkins((LVLN) SPDatabase.getMajor(n.getTemplate(), GRUP_TYPE.LVLN));
	    }
	} else {
	    if (!n.getSkin().isNull()) {
		out = new HashMap<FormID, FormID>(1);
		out.put(n.getRace(), n.getSkin());
		return out;
	    } else {
		out = new HashMap<FormID, FormID>(0);
	    }
	}

	npcRacesToSkins.put(n, out);
	return out;
    }

    static Map<FormID, FormID> getRacesAndSkins(LVLN llist) {
	Map<FormID, FormID> out = new HashMap<FormID, FormID>(3);
	for (LVLO entry : llist.getEntries()) {
	    NPC_ npc = (NPC_) SPDatabase.getMajor(entry.getForm(), GRUP_TYPE.NPC_);
	    Map<FormID, FormID> tmp;
	    if (npc != null) {
		tmp = getRacesAndSkins(npc);
	    } else {
		tmp = getRacesAndSkins((LVLN) SPDatabase.getMajor(entry.getForm(), GRUP_TYPE.LVLN));
	    }
	    for (FormID key : tmp.keySet()) {
		if (out.containsKey(key)
			&& !out.get(key).isNull()
			&& !out.get(key).equals(tmp.get(key))) {
		    SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, header, "Warning: Ambiguous LVLN setup");
		    SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, header, "  " + llist);
		    SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, header, "  Has race " + SPDatabase.getMajor(key, GRUP_TYPE.RACE));
		    SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, header, "  with two differing skins to pick from:");
		    SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, header, "    " + SPDatabase.getMajor(tmp.get(key), GRUP_TYPE.ARMO));
		    SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, header, "    " + SPDatabase.getMajor(out.get(key), GRUP_TYPE.ARMO));
		    SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, header, "---------------------------");
		}
	    }
	    out.putAll(tmp);
	}
	return out;
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

    /*
     * Other Methods
     */
    static FormID getUsedSkin(NPC_ npcSrc) {
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
	PackageTree tree = SettingsPackagesManager.tree;
	if (tree != null) {
	    PackageComponent root = (PackageComponent) tree.getRoot();
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

    static boolean isReroute(File f) {
	return Ln.isFileType(f, "reroute");
    }

    /*
     * Internal Classes
     */
    static class AV_Nif {

	String name;
	String path;
	ArrayList<AV_Nif.TextureField> textureFields = new ArrayList<AV_Nif.TextureField>();
	ArrayList<Variant> variants = new ArrayList<Variant>();

	AV_Nif(String path) {
	    name = path;
	    this.path = "meshes\\" + path;
	}

	final void load() throws BadParameter, FileNotFoundException, IOException, DataFormatException {
	    NIF nif = new NIF(path, BSA.getUsedFile(path));
	    if (nif == null) {
		throw new FileNotFoundException("NIF file did not exist for path: " + path);
	    }

	    Map<Integer, NIF.Node> BiLightingShaderProperties = nif.getNodes(NIF.NodeType.BSLIGHTINGSHADERPROPERTY);
	    Map<Integer, NIF.Node> BiShaderTextureNodes = nif.getNodes(NIF.NodeType.BSSHADERTEXTURESET);
	    Map<Integer, ArrayList<String>> BiShaderTextureSets = new HashMap<Integer, ArrayList<String>>();
	    Map<Integer, AV_Nif.TextureField> fields = new HashMap<Integer, AV_Nif.TextureField>();
	    ArrayList<ArrayList<NIF.Node>> NiTriShapes = nif.getNiTriShapePackages();

	    for (Integer i : BiShaderTextureNodes.keySet()) {
		BiShaderTextureSets.put(i, NIF.extractBSTextures(BiShaderTextureNodes.get(i)));
	    }

	    int i = 0;
	    for (Integer key : BiLightingShaderProperties.keySet()) {
		int textureLink = BiLightingShaderProperties.get(key).data.extractInt(40, 4);
		AV_Nif.TextureField next = new AV_Nif.TextureField();
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
	}

	public void print() {
	    if (SPGlobal.logging()) {
		int i = 0;
		for (AV_Nif.TextureField set : textureFields) {
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
	    final AV_Nif other = (AV_Nif) obj;
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

	    TextureField(AV_Nif.TextureField in) {
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
		final AV_Nif.TextureField other = (AV_Nif.TextureField) obj;
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
	int probDiv;

	ARMA_spec(ARMA arma, VariantSpec spec) {
	    this.arma = arma;
	    probDiv = spec.Probability_Divider;
	}
    }

    static class ARMO_spec {

	ARMO armo;
	int probDiv;
	ARMA targetArma;
	FormID targetRace;

	ARMO_spec(NPC_ src) {
	    armo = (ARMO) SPDatabase.getMajor(getUsedSkin(src), GRUP_TYPE.ARMO);
	    probDiv = 1;
	    targetRace = src.getRace();
	}

	ARMO_spec(ARMO armoSrc, FormID targetRace) {
	    this.armo = armoSrc;
	    this.targetRace = targetRace;
	    probDiv = 1;
	}

	ARMO_spec(ARMO armo, ARMA_spec spec, FormID targetRace) {
	    this.armo = armo;
	    targetArma = spec.arma;
	    this.targetRace = targetRace;
	    probDiv = spec.probDiv;
	}
    }

    static class SPEL_setup {

	SPEL spell;
	FLST key;
	FLST array;
	// Armo is key
	Map<FormID, FLST> npcKeyLists = new HashMap<FormID, FLST>();

	SPEL_setup(RACE raceSrc) {
	    ScriptRef script = AV.generateAttachScript();
	    key = new FLST(SPGlobal.getGlobalPatch(), "AV_" + raceSrc.getEDID() + "_flst_Key");
	    array = new FLST(SPGlobal.getGlobalPatch(), "AV_" + raceSrc.getEDID() + "_flst_Array");

	    script.setProperty("AltOptions", array.getForm());
	    script.setProperty("AltKey", key.getForm());

	    spell = NiftyFunc.genScriptAttachingSpel(SPGlobal.getGlobalPatch(), script, raceSrc.getEDID());
	}
    }
}
