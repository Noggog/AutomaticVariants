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
    static String changeRaceFormList = "RaceOptions";
    static String changeRaceBoundWeapons = "BoundWeapons";
    static int numSupportedTextures = 8;

    /*
     * Variant storage lists/maps
     */
    public static PackageComponent AVPackages = new PackageComponent(new File(AVPackagesDir), PackageComponent.Type.ROOT);
    static HashSet<FormID> unusedRaces;
    static HashSet<FormID> unusedSkins;
    // AV_Nif name is key
    static Map<String, AV_Nif> nifs = new HashMap<String, AV_Nif>();
    static LMergeMap<String, ARMA> nifToARMA = new LMergeMap<String, ARMA>(false);
    static LMergeMap<ARMO, FormID> ARMOToRace = new LMergeMap<ARMO, FormID>(false);
    // ArmaSrc is key
    static Map<FormID, String> armaToNif = new HashMap<FormID, String>();
    static LMergeMap<FormID, ARMA_spec> armatures = new LMergeMap<FormID, ARMA_spec>(false);
    // ArmoSrc is key
    static LMergeMap<FormID, ARMO_spec> armors = new LMergeMap<FormID, ARMO_spec>(false);
    static LMergeMap<FormID, RACE_spec> races = new LMergeMap<FormID, RACE_spec>(false);
    static Map<FormID, FLST> formLists = new HashMap<FormID, FLST>();
    static Map<FormID, SPEL> switcherSpells = new HashMap<FormID, SPEL>();
    // NpcSrc is Key
    static Map<FormID, LVLN> llists = new HashMap<FormID, LVLN>();
    static LMergeMap<FormID, NPC_spec> npcs = new LMergeMap<FormID, NPC_spec>(false);
    // RaceSrc is key
    static Map<FormID, RACE> switcherRaces = new HashMap<FormID, RACE>();
    static FLST boundList;
    static boolean raceSwitchMethod = false;

    static void setUpFileVariants(Mod source, Mod patch) throws IOException, Uninitialized, BadParameter {

	BSAs = BSA.loadInBSAs(BSA.FileType.NIF, BSA.FileType.DDS);

	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Importing AV Packages");
	if (!AVPackages.isLeaf()) {
	    // Change packages to enabled/disabled based on GUI requests
	    shufflePackages();
	}
	importVariants();
	SPGUI.progress.incrementBar();

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

	// Split between two methods of achiving variants in-game.
	// Currently using NPC dup method
	if (raceSwitchMethod) {
	    raceSwitchMethod(source);
	} else {
	    npcDupMethod(source);
	}

	for (PackageComponent p : AVPackages.getAll()) {
	    p.moveOut();
	}
    }

    static void npcDupMethod(Mod source) {
	// Flatten unique NPC templates
	handleUniqueNPCTemplates(source);

	// Generate NPC_ dups that use ARMO skins
	generateNPCvariants(source);

	prepAndAddOriginals(source);

	// Load NPC_ dups into LVLNs
	generateLVLNs(source);

	// Apply template routing from original NPCs to new LLists
	generateTemplating(source);

	// Replace original NPCs in orig LVLNs, as CK throws warning/error for it
	subInOldLVLNs(source);

	// Bethesda doesn't like two LLists being on the same template chain
	// Searches for LList entries that template to new AV LLists and
	// Flattens their templating and sets up variants of their own to
	// Route around the issue.
	dupTemplatedLVLNentries(source);

//	handleUniqueNPCLListTemplates(source);

	printVariantList();
    }

    static void raceSwitchMethod(Mod source) {

	// Generate RACE dups that use ARMOs
	generateRACEvariants(source);

	// Generate FormLists of RACE variants
	generateFormLists(source);

	// Generate FormList of bound weapons, for race switch script
	locateBoundWeapons(source);

	// Generate Spells that use Race Switcher Magic Effects
	generateSPELvariants();

	attachSPELs();

	// Sub In script attachment races on target NPCs
	subInSwitcherRaces(source);
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
	if (!AV.save.getBool(Settings.MINIMIZE_PATCH)) {
	    return;
	}

	unusedRaces = new HashSet<FormID>(source.getRaces().numRecords());
	for (RACE race : source.getRaces()) {
	    unusedRaces.add(race.getForm());
	}
	unusedSkins = new HashSet<FormID>(source.getArmors().numRecords());
	for (ARMO armor : source.getArmors()) {
	    unusedSkins.add(armor.getForm());
	}

	for (NPC_ n : source.getNPCs()) {
	    unusedRaces.remove(n.getRace());
	    unusedSkins.remove(n.getSkin());
	}

	for (RACE r : source.getRaces()) {
	    if (!unusedRaces.contains(r.getForm())) {
		unusedSkins.remove(r.getWornArmor());
	    }
	}
    }

    static void linkToNifs() {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Linking packages to .nif files.");
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
	SPGUI.progress.incrementBar();
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
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Generating TXST variants.");
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
	SPGUI.progress.incrementBar();
    }

    static void generateARMAvariants(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Generating ARMA variants.");
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
	SPGUI.progress.incrementBar();
    }

    static void generateARMOvariants(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Generating ARMO variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating ARMO skin duplicates for each ARMA");
	    SPGlobal.log(header, "====================================================================");
	}
	for (ARMO armoSrc : source.getArmors()) {

	    LMergeMap<FormID, ARMA_spec> targets = new LMergeMap<FormID, ARMA_spec>(false);
	    int largest = 0;
	    for (FormID armaForm : armoSrc.getArmatures()) {
		ARMA arma = (ARMA) SPDatabase.getMajor(armaForm);
		if (arma != null) {
		    if (armatures.containsKey(armaForm)) {
			if (largest < armatures.get(armaForm).size()) {
			    largest = armatures.get(armaForm).size();
			}
			targets.put(armaForm, armatures.get(armaForm));
		    }
		}
	    }

	    if (AV.save.getBool(Settings.MINIMIZE_PATCH)
		    && !targets.isEmpty() && unusedSkins.contains(armoSrc.getForm())) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Skipping " + armoSrc + " because it is unused.");
		}
		continue;
	    }

	    for (FormID arma : targets.keySet()) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Duplicating " + armoSrc + ", for " + SPDatabase.getMajor(arma, GRUP_TYPE.ARMA));
		}
		ArrayList<ARMO_spec> dups = new ArrayList<ARMO_spec>(targets.numVals());
		for (ARMA_spec variant : targets.get(arma)) {
		    String edid = variant.arma.getEDID().substring(0, variant.arma.getEDID().lastIndexOf("_arma"));
		    edid = edid.substring(0, edid.lastIndexOf("_")) + "_" + armoSrc.getEDID() + "_armo";  // replace ARMA string with ARMO name
		    ARMO dup = (ARMO) SPGlobal.getGlobalPatch().makeCopy(armoSrc, edid);

		    dup.removeArmature(arma);
		    dup.addArmature(variant.arma.getForm());
		    dups.add(new ARMO_spec(dup, variant, variant.arma.getRace()));
		}
		armors.put(armoSrc.getForm(), dups);
	    }
	}
	printVariants();
	SPGUI.progress.incrementBar();
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

    /*
     * Race Switch Methods
     */
    static void generateRACEvariants(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Generating RACE variants.");
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
	SPGUI.progress.incrementBar();
    }

    static void generateFormLists(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Generating Form Lists.");
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
	SPGUI.progress.incrementBar();
    }

    static void locateBoundWeapons(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Locating bound weapons.");
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
	SPGUI.progress.incrementBar();
    }

    static void generateSPELvariants() {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Generating script attachment races.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Generating Spells which attach specialized scripts");
	    SPGlobal.log(header, "====================================================================");
	}
	for (FormID armoSrc : formLists.keySet()) {
	    FLST flst = formLists.get(armoSrc);
	    String name = flst.getEDID().substring(flst.getEDID().indexOf("AV_") + 3, flst.getEDID().lastIndexOf("_flst"));
	    ScriptRef script = AV.generateAttachScript();
	    script.setProperty(changeRaceBoundWeapons, boundList.getForm());
	    script.setProperty(changeRaceFormList, flst.getForm());
	    script.setProperty(AV.changeRaceOn, true);
	    switcherSpells.put(armoSrc, NiftyFunc.genScriptAttachingSpel(SPGlobal.getGlobalPatch(), script, name));
	}
	SPGUI.progress.incrementBar();
    }

    static void attachSPELs() {
	for (FormID armoSrcForm : switcherSpells.keySet()) {
	    FormID switcherSpell = switcherSpells.get(armoSrcForm).getForm();
	    for (RACE_spec r : races.get(armoSrcForm)) {
		r.race.addSpell(switcherSpell);
	    }
	}
    }

    static void subInSwitcherRaces(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Switching in scripted race versions.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Switching target NPC's races to scripted versions");
	    SPGlobal.log(header, "====================================================================");
	}
	boolean last = false;
	for (NPC_ npcSrc : source.getNPCs()) {

	    // Locate if any variants are available
	    FormID armorForm = npcSrc.getSkin();
	    if (npcSrc.getSkin().equals(FormID.NULL)) {
		RACE race = (RACE) SPDatabase.getMajor(npcSrc.getRace());
		if (race == null) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "Skipping " + npcSrc + " : did not have a worn armor or race.");
		    }
		    continue;
		}
		armorForm = race.getWornArmor();
	    }
	    if (formLists.containsKey(armorForm)) {

		if (AV.checkNPCskip(npcSrc, true, last)) {
		    last = false;
		    continue;
		}

		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		    SPGlobal.log(header, "| Scripting " + npcSrc + ", for " + SPDatabase.getMajor(armorForm, GRUP_TYPE.ARMO));
		    last = true;
		}

		RACE switchRace = races.get(armorForm).get(0).race;

		npcSrc.setRace(switchRace.getForm());

		// If has special skin, remove it
		if (!npcSrc.getSkin().equals(FormID.NULL)) {
		    ARMO specialSkin = (ARMO) SPDatabase.getMajor(npcSrc.getSkin(), GRUP_TYPE.ARMO);
		    npcSrc.setSkin(FormID.NULL);
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "| Had special skin " + specialSkin + ".  Removed.");
		    }
		}

		SPGlobal.getGlobalPatch().addRecord(npcSrc);
		AV.modifiedNPCs.put(armorForm, npcSrc);
	    }
	}
	SPGUI.progress.incrementBar();
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
     * NPC dup methods
     */
    static void generateNPCvariants(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Generating NPC variants.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================================================");
	    SPGlobal.log(header, "Generating NPC duplicates for each ARMO skin.  Only NPCs that have skin directly without templating.");
	    SPGlobal.log(header, "====================================================================================================");
	}
	boolean last = false;
	for (NPC_ npcSrc : source.getNPCs()) {

	    // Locate if any variants are available
	    FormID armorForm = getUsedSkin(npcSrc);
	    if (armorForm == null) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Skipping " + npcSrc + " : did not have a worn armor or race.");
		}
		continue;
	    }

	    ArrayList<ARMO_spec> skinVariants = armors.get(armorForm);

	    // If npc has skin with variants
	    if (skinVariants != null) {

		// See if any variants races match npc's
		boolean match = false;
		for (ARMO_spec variant : skinVariants) {
		    if (variant.targetRace.equals(npcSrc.getRace())) {
			match = true;
			break;
		    }
		}
		if (!match) {
		    continue;
		}

		// Check if this is an NPC to skip
		if (AV.checkNPCskip(npcSrc, true, last)) {
		    last = false;
		    continue;
		}

		// Find if NPC has a template
		NPC_ template = null;
		if (!npcSrc.getTemplate().equals(FormID.NULL)) {
		    template = (NPC_) SPDatabase.getMajor(npcSrc.getTemplate(), GRUP_TYPE.NPC_);
		}

		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		    SPGlobal.log(header, "| Duplicating " + npcSrc + ", for " + SPDatabase.getMajor(armorForm, GRUP_TYPE.ARMO));
		    if (template != null) {
			SPGlobal.log(header, "| Flattening template to " + template);
		    }
		    last = true;
		}

		ArrayList<NPC_spec> dups = new ArrayList<NPC_spec>(skinVariants.size());
		for (ARMO_spec variant : skinVariants) {
		    // If skin's target race does not match npc's, skip.
		    if (!npcSrc.getRace().equals(variant.targetRace)) {
			continue;
		    }

		    // Duplicate the source NPC
		    String newEDID;
		    newEDID = variant.armo.getEDID().substring(0, variant.armo.getEDID().lastIndexOf("_ID_")) + "_" + npcSrc.getEDID();
		    NPC_ dup = (NPC_) SPGlobal.getGlobalPatch().makeCopy(npcSrc, newEDID);

		    // Flatten the dup's template chain so it has no template.
		    if (template != null) {
			dup.templateTo(template);
		    }

		    // Set skin to AV's variant
		    dup.setSkin(variant.armo.getForm());

		    dups.add(new NPC_spec(dup, variant));
		}
		npcs.put(npcSrc.getForm(), dups);
	    }
	}

	SPGUI.progress.incrementBar();
    }

    static void prepAndAddOriginals(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================================================");
	    SPGlobal.log(header, "AV Prepping and Adding Originals as Variants");
	    SPGlobal.log(header, "====================================================================================================");
	}
	for (NPC_ npcSrc : source.getNPCs()) {
	    if ((npcs.containsKey(npcSrc.getForm()) && AV.save.getBool(Settings.PACKAGES_ORIG_AS_VAR))
		    || (!npcs.containsKey(npcSrc.getForm()) && AV.save.getBool(Settings.PACKAGES_PREP) && !AV.checkNPCskip(npcSrc, false, false))) {
		if (SPGlobal.logging()) {
		    if (npcs.containsKey(npcSrc.getForm())) {
			SPGlobal.log(header, "  Added as variant: " + npcSrc);
		    } else {
			SPGlobal.log(header, "  Prepped: " + npcSrc);
		    }
		}
		NPC_ dup = (NPC_) SPGlobal.getGlobalPatch().makeCopy(npcSrc, "AV_OrigVar_" + npcSrc.getEDID());
		if (!npcSrc.getTemplate().equals(FormID.NULL)) {
		    dup.templateTo(npcSrc.getTemplate());
		}
		npcs.put(npcSrc.getForm(), new NPC_spec(dup));
	    }
	}
    }

    static void generateLVLNs(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Loading NPC variants into Leveled Lists.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "===================================================");
	    SPGlobal.log(header, "Generating LVLNs loaded with NPC variants.");
	    SPGlobal.log(header, "===================================================");
	}
	for (FormID srcNpc : npcs.keySet()) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "Generating for " + SPDatabase.getMajor(srcNpc));
	    }

	    LVLN llist = new LVLN(SPGlobal.getGlobalPatch(), "AV_" + source.getNPCs().get(srcNpc).getEDID() + "_llist");
	    ArrayList<NPC_spec> npcVars = npcs.get(srcNpc);
	    if (npcVars.isEmpty()) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "  Skipping because variant list was empty");
		}
		continue;
	    }
	    int[] divs = new int[npcVars.size()];
	    for (int i = 0; i < divs.length; i++) {
		divs[i] = npcVars.get(i).probDiv;
	    }
	    int lowestCommMult = Ln.lcmm(divs);

	    for (NPC_spec n : npcVars) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "  Generating " + (lowestCommMult / n.probDiv) + " entries for " + n.npc);
		}
		for (int i = 0; i < lowestCommMult / n.probDiv; i++) {
		    llist.addEntry(new LVLO(n.npc.getForm(), 1, 1));
		}
	    }
	    llists.put(srcNpc, llist);

	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "--------------------------------------------------");
	    }
	}
	SPGUI.progress.incrementBar();
    }

    static void generateTemplating(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Templating original NPCs to variant LLists.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Setting source NPCs to template to LVLN loaded with variants");
	    SPGlobal.log(header, "====================================================================");
	}
	for (NPC_ npcSrc : source.getNPCs()) {
	    FormID npcForm = npcSrc.getForm();
	    if (llists.containsKey(npcForm)) {
		npcSrc.setTemplate(llists.get(npcForm).getForm());
		npcSrc.set(NPC_.TemplateFlag.USE_TRAITS, true);
		SPGlobal.getGlobalPatch().addRecord(npcSrc);
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Templating " + npcSrc + " with " + llists.get(npcForm));
		}
	    }
	}
	SPGUI.progress.incrementBar();
    }

    static void subInOldLVLNs(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Replacing original NPC entries in your LVLN records.");
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Replacing old NPC entries in your mod's LVLNs");
	    SPGlobal.log(header, "====================================================================");
	}
	for (LVLN llistSrc : source.getLeveledCreatures()) {
	    boolean add = false;
	    for (LVLO entry : llistSrc) {
		if (llists.containsKey(entry.getForm())) {
		    entry.setForm(llists.get(entry.getForm()).getForm());
		    add = true;
		}
	    }
	    if (add) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Modified " + llistSrc);
		}
		SPGlobal.getGlobalPatch().addRecord(llistSrc);
	    }
	}
	SPGUI.progress.incrementBar();
    }

    static void dupTemplatedLVLNentries(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "============================================================================================");
	    SPGlobal.log(header, "Checking each LVLN entry for traits templating to new variants.  Duplicating and replacing");
	    SPGlobal.log(header, "============================================================================================");
	}
	for (LVLN llist : source.getLeveledCreatures()) {

	    boolean add = false;
	    for (LVLO entry : llist) {
		LVLN template = NiftyFunc.isTemplatedToLList(entry.getForm());
		if (template != null && template.getFormMaster().equals(SPGlobal.getGlobalPatch().getInfo())) { // If entry is NPC and templated to an AV LList
		    NPC_ npcEntry = (NPC_) SPDatabase.getMajor(entry.getForm(), GRUP_TYPE.NPC_);
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  " + npcEntry + " replaced.");
		    }

		    LVLN sub = llists.get(entry.getForm());
		    if (sub == null) { // If variant LList does not already exist for entry NPC, create it
			if (SPGlobal.logging()) {
			    SPGlobal.log(header, "    Creating variants.");
			}
			sub = createLVLNvariant(npcEntry, template);
		    }

		    entry.setForm(sub.getForm());
		    add = true;
		}
	    }
	    if (add) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Modified " + llist);
		    SPGlobal.log(header, "----------------------------------------------------------------");
		}
		SPGlobal.getGlobalPatch().addRecord(llist);
	    }
	}
    }

    static LVLN createLVLNvariant(NPC_ npcEntry, LVLN template) {
	LVLN out = new LVLN(SPGlobal.getGlobalPatch(), "AV_" + npcEntry.getEDID() + "_llist");

	// Map to store NPC dups just in case there are multiple entries of the same skin
	Map<FormID, NPC_> skinToNPCdup = new HashMap<FormID, NPC_>();

	// Make NPC duplicates for each in the template LList.
	for (LVLO templateEntry : template) {
	    NPC_ templateNPC = (NPC_) SPGlobal.getGlobalPatch().getNPCs().get(templateEntry.getForm());
	    String[] edidSplit = templateNPC.getEDID().split("_");
	    String edidBase;
	    if (edidSplit.length > 4) {
		// If normal AV variant
		edidBase = edidSplit[0] + "_" + edidSplit[1] + "_" + edidSplit[2] + "_" + edidSplit[3] + "_" + edidSplit[4];
	    } else {
		// If orig as var variant
		edidBase = templateNPC.getEDID().substring(0, templateNPC.getEDID().lastIndexOf("_")) + "_TEST";
	    }

	    NPC_ dupNPC = skinToNPCdup.get(templateNPC.getSkin());
	    if (dupNPC == null) {
		dupNPC = (NPC_) SPGlobal.getGlobalPatch().makeCopy(npcEntry, edidBase + "_" + npcEntry.getEDID());
		dupNPC.templateTo(templateNPC);
		skinToNPCdup.put(templateNPC.getSkin(), dupNPC);
	    }
	    out.addEntry(new LVLO(dupNPC.getForm(), 1, 1));
	}
	llists.put(npcEntry.getForm(), out);
	return out;
    }

    static void handleUniqueNPCTemplates(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Flattening unique templates");
	    SPGlobal.log(header, "====================================================================");
	}
	for (NPC_ srcNPC : source.getNPCs()) {
	    if (srcNPC.get(NPC_.NPCFlag.Unique) && !srcNPC.getTemplate().equals(FormID.NULL)) {
		srcNPC.templateTo(srcNPC.getTemplate());
		SPGlobal.getGlobalPatch().addRecord(srcNPC);
	    }
	}
    }

    static void handleUniqueNPCLListTemplates(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "====================================================================");
	    SPGlobal.log(header, "Handling unique NPCs not liking LList templates");
	    SPGlobal.log(header, "====================================================================");
	}
	for (NPC_ srcNpc : source.getNPCs()) {
	    if (srcNpc.get(NPC_.NPCFlag.Unique)) {
		LVLN llistTemplate = srcNpc.isTemplatedToLList();
		while (llistTemplate != null && llistTemplate.getFormMaster().equals(SPGlobal.getGlobalPatch().getInfo())) {
		    srcNpc.templateTo(srcNpc.getTemplate());
		    // Best we can do is replace its template with one from alt LList.
		    // Put the first to minimize unique actor changing when people rerun av patch
		    if (!llistTemplate.isEmpty()) {
			if (SPGlobal.logging()) {
			    SPGlobal.log(header, "  Replacing unique actor " + srcNpc + "'s template "
				    + SPDatabase.getMajor(srcNpc.getTemplate(), GRUP_TYPE.NPC_, GRUP_TYPE.LVLN)
				    + " with " + SPDatabase.getMajor(llistTemplate.getEntry(0).getForm(), GRUP_TYPE.NPC_));
			}
			srcNpc.setTemplate(llistTemplate.getEntry(0).getForm());
			srcNpc.set(NPC_.TemplateFlag.USE_TRAITS, true);
			SPGlobal.getGlobalPatch().addRecord(srcNpc);
		    }
		    llistTemplate = srcNpc.isTemplatedToLList();
		}
	    }
	}
    }

    static void printVariantList() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(header, "===================================================");
	    SPGlobal.log(header, "Printing all NPCs that have a matching variant LList.");
	    SPGlobal.log(header, "===================================================");
	    Map<String, NPC_> sorter = new TreeMap<String, NPC_>();
	    for (FormID form : llists.keySet()) {
		NPC_ tmp = (NPC_) SPDatabase.getMajor(form, GRUP_TYPE.NPC_);
		sorter.put(tmp.getEDID(), tmp);
	    }

	    for (NPC_ n : sorter.values()) {
		SPGlobal.log(header, "  " + n);
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

    static class NPC_spec {

	NPC_ npc;
	int probDiv;

	NPC_spec(NPC_ npc, ARMO_spec spec) {
	    this.npc = npc;
	    probDiv = spec.probDiv;
	}

	NPC_spec(NPC_ npc) {
	    this.npc = npc;
	    probDiv = 1;
	}
    }

    static class RACE_spec {

	RACE race;
	int probDiv;

	RACE_spec(RACE race, ARMO_spec spec) {
	    this.race = race;
	    probDiv = spec.probDiv;
	}
    }
}
