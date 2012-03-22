/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import com.google.gson.JsonSyntaxException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
public class AVRaceSwitchVariants {

    static String header = "AV_RaceSwitch";

    static ArrayList<BSA> BSAs;
    
    static File avPackages = new File("AV Packages/");
    static File avTextures = new File(SPGlobal.pathToData + "textures/AV Packages/");
    static File avMeshes = new File(SPGlobal.pathToData + "meshes/AV Packages/");
    
    static String alreadySwitched = "AlreadySwitched";
    static String changeRaceFormList = "RaceOptions";
    static String changeRaceBoundWeapons = "BoundWeapons";
    
    static int numSupportedTextures = 8;
    
    /*
     * Variant storage lists/maps
     */
    // AV_Nif name is key
    static Map<String, AV_Nif> nifs = new HashMap<String, AV_Nif>();
    // ArmaSrc is key
    static Map<FormID, String> armaToNif = new HashMap<FormID, String>();
    static LMergeMap<FormID, ARMA_spec> armatures = new LMergeMap<FormID, ARMA_spec>(false);
    // ArmoSrc is key
    static LMergeMap<FormID, ARMO_spec> armors = new LMergeMap<FormID, ARMO_spec>(false);
    static LMergeMap<FormID, RACE_spec> races = new LMergeMap<FormID, RACE_spec>(false);
    static Map<FormID, FLST> formLists = new HashMap<FormID, FLST>();
    static Map<FormID, SPEL> switcherSpells = new HashMap<FormID, SPEL>();
    // RaceSrc is key
    static Map<FormID, RACE> switcherRaces = new HashMap<FormID, RACE>();

    static FLST boundList;
    static FLST alreadySwitchedList;
    
    static void setUpRaceSwitchVariants (Mod source, Mod patch) throws IOException, Uninitialized {
	
	    BSAs = BSA.loadInBSAs(BSA.FileType.NIF, BSA.FileType.DDS);

	    SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Importing AV Packages");
	    AV.gatherFiles();
	    ArrayList<VariantSet> variantRead = importVariants(patch);
	    SPGUI.progress.incrementBar();

	    alreadySwitchedList = new FLST(patch, "AV_" + alreadySwitched);

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

	    // Generate Spells that use Race Switcher Magic Effects
	    generateSPELvariants();

	    attachSPELs();

	    // Sub In script attachment races on target NPCs
	    subInSwitcherRaces(source);

	    printModifiedNPCs();
    }
    
    static VariantSet importVariantSet(File variantFolder, String header) throws FileNotFoundException, JsonSyntaxException {
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

    static class VariantSpec implements Serializable {

	int Probability_Divider = 1;

	void print() {
	    if (SPGlobal.logging()) {
		SPGlobal.log("VariantSpec", "  Loaded specs: ");
		SPGlobal.log("VariantSpec", "    Prob Div: 1/" + Probability_Divider);
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

    static void linkToNifs(ArrayList<VariantSet> variantRead) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Linking packages to .nif files.");
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
			for (Variant v : varSet.variants) {
			    nifs.get(armaToNif.get(piece.getForm())).variants.add((Variant)Ln.deepCopy(v));
			}
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
    
    static void splitVariant(String nifPath, ARMA piece) throws IOException, BadParameter, DataFormatException {
	if (SPGlobal.logging()) {
	    SPGlobal.log("SplitVar", "  Record warrents split due to alt textures in ARMA.");
	}
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
    
    static void generateVariantTXSTSets(Variant v, ArrayList<AV_Nif.TextureField> texturePack) throws IOException {

	// Find out which TXSTs need to be generated
	String[][] replacements = new String[texturePack.size()][numSupportedTextures];
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
	v.textureVariants = new TextureVariant[texturePack.size()];
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
			if (j == numSupportedTextures - 1) {
			    break;
			} else {
			    j++;
			}
		    }
		    last = tmpTXST;
		}
		v.textureVariants[i] = new TextureVariant(last, textureSet.title);
	    }
	    i++;
	}

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
		generateVariantTXSTSets(v, n.textureFields);
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
			ARMA dup = (ARMA) SPGlobal.getGlobalPatch().makeCopy(armaSrc, v.name + "_ID_" + malenif.uniqueName() + "_arma");

			ArrayList<ARMA.AltTexture> alts = dup.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON);
			alts.clear();
			int i = 0;
			for (TextureVariant texVar : v.textureVariants) {
			    if (texVar != null) {
				alts.add(new ARMA.AltTexture(texVar.nifFieldName, texVar.textureRecord.getForm(), i));
			    }
			    i++;
			}

			ArrayList<ARMA.AltTexture> femalealts = dup.getAltTextures(Gender.FEMALE, Perspective.THIRD_PERSON);
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

    static void generateARMOvariants(Mod source) {
	SPGUI.progress.setStatus(AV.step++, AV.numSteps, "Generating ARMO variants.");
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
		if (AV.block.contains(armoSrc.getForm())) {
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
	    script.setProperty(alreadySwitched, alreadySwitchedList.getForm());
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
	    if (formLists.containsKey(armorForm)) {

		if (AV.checkNPCskip(npcSrc, last)) {
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
		if (!npcSrc.getWornArmor().equals(FormID.NULL)) {
		    ARMO specialSkin = (ARMO) SPDatabase.getMajor(npcSrc.getWornArmor(), GRUP_TYPE.ARMO);
		    npcSrc.setWornArmor(FormID.NULL);
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

    static int readjustTXSTindices(int j) {
	// Because nif fields map 2->3 if facegen flag is on.
	int set = j;
	if (set == 2) {
	    set = 3;
	}
	return set;
    }
    
    // Internal Classes
    
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
	    ArrayList<ArrayList<NIF.Node>> NiTriShapes = nif.getNiTriShapePackages();
	    AV_Nif.TextureField last = new AV_Nif.TextureField();
	    for (int i = 0; i < NiTriShapes.size(); i++) {
		AV_Nif.TextureField next = new AV_Nif.TextureField(last);
		next.title = NiTriShapes.get(i).get(0).title;
		if (SPGlobal.logging()) {
		    SPGlobal.log("AV_Nif", "  Loaded NiTriShapes: " + next.title);
		}
		for (NIF.Node n : NiTriShapes.get(i)) {
		    if (n.type == NIF.NodeType.BSSHADERTEXTURESET) {
			if (SPGlobal.logging()) {
			    SPGlobal.log("AV_Nif", "  Loaded new texture maps");
			}
			next.maps = NIF.extractBSTextures(n);
			if (!next.equals(last)) {
			    next.unique = true;
			}
		    }
		}
		this.textureFields.add(next);
		last = next;
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

	ARMO_spec(ARMO armo, ARMA_spec spec) {
	    this.armo = armo;
	    targetArma = spec.arma;
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
    }

    static class RACE_spec {

	RACE race;
	int probDiv;

	RACE_spec(RACE race, ARMO_spec spec) {
	    this.race = race;
	    probDiv = spec.probDiv;
	}
    }

    static class Variant implements Serializable {

	String name = "";
	ArrayList<String> variantTexturePaths = new ArrayList<String>();
	TextureVariant[] textureVariants;
	VariantSpec specs = new VariantSpec();

	void setName(File file, int places) {
	    String[] tmp = file.getPath().split("\\\\");
	    for (int i = 1; i <= places; i++) {
		name = "_" + tmp[tmp.length - i].replaceAll(" ", "") + name;
	    }
	    name = "AV" + name;
	}

	boolean isEmpty() {
	    return variantTexturePaths.isEmpty();
	}
    }

    static class TextureVariant implements Serializable {

	String nifFieldName;
	TXST textureRecord;

	TextureVariant(TXST txst, String name) {
	    textureRecord = txst;
	    nifFieldName = name;
	}
    }

    static class VariantSet {

	String[][] Target_FormIDs;
	Boolean Apply_To_Similar = true;
	ArrayList<Variant> variants = new ArrayList<Variant>();

	boolean isEmpty() {
	    return variants.isEmpty();
	}
    }
}
