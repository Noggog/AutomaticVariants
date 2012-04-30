/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package avpackageanalyzer;

import java.io.*;
import java.util.*;
import java.util.zip.DataFormatException;
import lev.LMergeMap;
import lev.LShrinkArray;
import lev.Ln;
import lev.debug.LDebug;
import skyproc.ARMA.AltTexture;
import skyproc.NIF.Node;
import skyproc.NIF.NodeType;
import skyproc.*;
import skyproc.exceptions.BadParameter;
import skyproc.gui.SPDefaultGUI;

/**
 *
 * @author Justin Swanson
 */
public class AVPackageAnalyzer {

    static ArrayList<File> srcs;
    static LMergeMap<File, File> NIFtoSRC = new LMergeMap<File, File>(false);
    static Map<FormID, File> ARMAtoNIF = new HashMap<FormID, File>();
    static Map<FormID, FormID> ARMOtoARMA = new HashMap<FormID, FormID>();
    static Map<FormID, FormID> RACEtoARMO = new HashMap<FormID, FormID>();
    static Map<FormID, FormID> NPCtoARMO = new HashMap<FormID, FormID>();
    static ArrayList<File> meshes;
    static ArrayList<BSA> bsas;
    static BufferedWriter suggestions;
    static int numSteps = 5;
    static int step = 1;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

	SPDefaultGUI gui = init();

	suggestions = new BufferedWriter(new FileWriter("Suggestions.txt"));
	loadSources();
	try {
	    bsas = BSA.loadInBSAs(BSA.FileType.NIF);
	} catch (BadParameter ex) {
	    System.exit(0);
	}
	meshes = Ln.generateFileList(new File(SPGlobal.pathToData + "meshes/"), false);

	checkMeshes();
	checkBSAs();
	SPGlobal.log("SrcToNifs", "Printing Sources to their Nifs:");
	NIFtoSRC.print();
	NIFtoSRC = NIFtoSRC.flip();

	// Got Nifs that use textures, find NPCs
	SPImporter importer = new SPImporter();
	SPGlobal.addModToSkip(new ModListing("Automatic Variants.esp"));
	Mod merger = new Mod("tmpMod", false);
	merger.addAsOverrides(importer.importActiveMods(GRUP_TYPE.NPC_, GRUP_TYPE.ARMA,
		GRUP_TYPE.ARMO, GRUP_TYPE.RACE, GRUP_TYPE.TXST));


	// ARMA
	LMergeMap<File, FormID> NIFToARMAs = new LMergeMap<File, FormID>(false);
	for (ARMA arma : merger.getArmatures()) {
	    File model = new File("MESHES/" + arma.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).toUpperCase());

	    if (NIFtoSRC.containsKey(model)) {

		// Check for alt textures
		ArrayList<AltTexture> altTextures = arma.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON);
		Set<File> altSrcs = new HashSet<File>();
		if (!altTextures.isEmpty()) {
		    for (AltTexture t : altTextures) {
			TXST txst = (TXST) SPDatabase.getMajor(t.getTexture(), GRUP_TYPE.TXST);
			for (String path : txst) {
			    if (path != null) {
				File pathFile = new File(path);
				for (File src : srcs) {
				    if (pathFile.getName().equalsIgnoreCase(src.getName())) {
					altSrcs.add(src);
				    }
				}
			    }
			}
		    }
		}

		if (altSrcs.isEmpty()) {
		    NIFToARMAs.put(model, arma.getForm());
		} else {
		    // Split variant
		    File altModel = new File(model.getPath() + "ALT-" + arma.getFormStr());
		    for (File altSrc : altSrcs) {
			NIFtoSRC.put(altModel, altSrc);
		    }
		    NIFToARMAs.put(altModel, arma.getForm());
		}
	    }

	}
	ARMAtoNIF = NIFToARMAs.flip().flatten();
	SPGlobal.log("ARMAs", "Printing ARMAs to their sources:");
	for (FormID arma : ARMAtoNIF.keySet()) {
	    System.out.println(SPDatabase.getMajor(arma, GRUP_TYPE.ARMA));
	    System.out.println("  " + ARMAtoNIF.get(arma));
	}

	// ARMO
	LMergeMap<FormID, FormID> srcToARMOs = new LMergeMap<FormID, FormID>(false);
	for (ARMO armo : merger.getArmors()) {
	    for (FormID armaf : armo.getArmatures()) {
		if (ARMAtoNIF.containsKey(armaf)) {
		    ARMA arma = (ARMA) SPDatabase.getMajor(armaf);
		    if (armo.getRace().equals(arma.getRace())) {
			srcToARMOs.put(armaf, armo.getForm());
		    }
		}
	    }
	}
	ARMOtoARMA = srcToARMOs.flip().flatten();
	SPGlobal.log("ARMOs", "Printing ARMOs to their sources:");
	for (FormID armo : ARMOtoARMA.keySet()) {
	    System.out.println(SPDatabase.getMajor(armo, GRUP_TYPE.ARMO));
	    System.out.println("  " + SPDatabase.getMajor(ARMOtoARMA.get(armo), GRUP_TYPE.ARMA));
	}


	// RACE
	LMergeMap<FormID, FormID> ARMOToRACE = new LMergeMap<FormID, FormID>(false);
	for (RACE race : merger.getRaces()) {
	    if (ARMOtoARMA.containsKey(race.getWornArmor())) {
		ARMOToRACE.put(race.getWornArmor(), race.getForm());
	    }
	}
	RACEtoARMO = ARMOToRACE.flip().flatten();
	SPGlobal.log("RACEs", "Printing RACEs to their sources:");
	for (FormID race : RACEtoARMO.keySet()) {
	    System.out.println(SPDatabase.getMajor(race, GRUP_TYPE.RACE));
	    System.out.println("  " + SPDatabase.getMajor(RACEtoARMO.get(race), GRUP_TYPE.ARMO));
	}


	// NPCs
	LMergeMap<FormID, FormID> ARMOToNPC = new LMergeMap<FormID, FormID>(false);
	for (NPC_ npc : merger.getNPCs()) {
	    if (!npc.getSkin().equals(FormID.NULL) && ARMOtoARMA.containsKey(npc.getSkin())) {
		ARMOToNPC.put(npc.getSkin(), npc.getForm());
	    } else if (RACEtoARMO.containsKey(npc.getRace())) {
		ARMOToNPC.put(RACEtoARMO.get(npc.getRace()), npc.getForm());
	    }
	}
	NPCtoARMO = ARMOToNPC.flip().flatten();
	SPGlobal.log("NPCs", "Printing NPCs to their sources:");
	for (FormID npc : NPCtoARMO.keySet()) {
	    System.out.println(SPDatabase.getMajor(npc, GRUP_TYPE.NPC_));
	    System.out.println("  " + SPDatabase.getMajor(NPCtoARMO.get(npc), GRUP_TYPE.ARMO));
	}

	//Print suggestions
	suggestions.write("\n\n");
	suggestions.write("==============================================================\n\n");
	suggestions.write("These are groups of NPCs that share a common skin.\n");
	suggestions.write("It is suggested you put a seed from each group into your spec file,\n"
		+ "                     --or--\n"
		+ "make a separate Variant Set for each if it makes logical sense to \n"
		+ "separate them (such as Black vs Ice wolves).\n\n");
	suggestions.write("Remember to use NPCs as seeds.  Do not use an ARMA record.\n\n");

	LMergeMap<FormID, FormID> ARMAtoNPC = new LMergeMap<FormID, FormID>(false);
	for (FormID npc : NPCtoARMO.keySet()) {
	    FormID armo = NPCtoARMO.get(npc);
	    FormID arma = ARMOtoARMA.get(armo);
	    if (ARMAtoNPC.containsKey(arma)) {
		ARMAtoNPC.put(arma, npc);
	    } else {
		ARMA ARMAtest = (ARMA) SPDatabase.getMajor(arma, GRUP_TYPE.ARMA);
		boolean added = false;
		SPGlobal.log("Linking", "================================================");
		SPGlobal.log("Linking", "Arma test: " + ARMAtest);
		// Check to see if there's already a matching ARMA
		for (FormID armaK : ARMAtoNPC.keySet()) {
		    ARMA ARMAkey = (ARMA) SPDatabase.getMajor(armaK, GRUP_TYPE.ARMA);
		    SPGlobal.log("Linking", "  Arma key: " + ARMAkey);
		    //If have same model path
		    if (ARMAtest.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).equalsIgnoreCase(ARMAkey.getModelPath(Gender.MALE, Perspective.THIRD_PERSON))) {

			// if neither have alt textures
			if (ARMAtest.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()
				&& ARMAkey.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()) {
			    SPGlobal.log("Linking", "  Neither had Alt Textures, merging.");
			    ARMAtoNPC.put(armaK, npc);
			    added = true;
			    break;

			    // if both have alt textures
			} else if (!ARMAtest.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()
				&& !ARMAkey.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()) {
			    boolean exists = true;

			    // check if both alt texture lists are the same
			    for (AltTexture test : ARMAtest.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON)) {
				boolean matched = false;
				ArrayList<AltTexture> keyTmp = new ArrayList<AltTexture>();
				keyTmp.addAll(ARMAkey.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON));
				for (AltTexture key : keyTmp) {
				    if (key.equals(test)) {
					SPGlobal.log("Linking", "  Alt texture " + test + " matched with " + key);
					matched = true;
					keyTmp.remove(key);
					break;
				    }
				}
				if (!matched) {
				    SPGlobal.log("Linking", "  Alt texture " + test + " could not find a match.");
				    exists = false;
				    break;
				}
			    }
			    // If alt textures are the same, merge
			    if (exists) {
				SPGlobal.log("Linking", "  Alt textures matched, merging.");
				ARMAtoNPC.put(armaK, npc);
				added = true;
				break;
			    }
			} else {
			    SPGlobal.log("Linking", "  One had alt textures while the other did not.");
			}
		    } else {
			SPGlobal.log("Linking", "  Different model paths:");
			SPGlobal.log("Linking", "  Test: " + ARMAtest.getModelPath(Gender.MALE, Perspective.THIRD_PERSON));
			SPGlobal.log("Linking", "  Key: " + ARMAkey.getModelPath(Gender.MALE, Perspective.THIRD_PERSON));
		    }
		    SPGlobal.log("Linking", "  ---------------------------------------------------");
		}

		// If no arma exists that is equal with same alt textures, then add new
		if (!added) {
		    SPGlobal.log("Linking", "No existing ARMA matched, making new split.");
		    ARMAtoNPC.put(arma, npc);
		}
	    }
	}

	int group = 1;
	for (FormID arma : ARMAtoNPC.keySet()) {
	    suggestions.write("Group " + group++ + ": " + SPDatabase.getMajor(arma, GRUP_TYPE.ARMA) + "\n");
	    suggestions.write("   Seed choices (pick one):\n");
	    for (FormID npc : ARMAtoNPC.get(arma)) {
		NPC_ npc_ = (NPC_) SPDatabase.getMajor(npc, GRUP_TYPE.NPC_);
		suggestions.write("\t\t[\"" + npc.getFormStr().substring(0,6) + "\",\"" + npc.getFormStr().substring(6) + "\"]"
			+ "   //" + npc_.getEDID() + "\n");
	    }
	    suggestions.write("   Source textures to include (include all of them):\n");
	    File nif = ARMAtoNIF.get(arma);
	    for (File src : NIFtoSRC.get(nif)) {
		suggestions.write("      " + src + "\n");
	    }
	    suggestions.write("-------------------------------------------------------------------\n\n");
	}

	suggestions.close();
	gui.finished();

	LDebug.wrapUp();
    }

    static void loadSources() throws IOException {
	srcs = Ln.generateFileList(new File("Source Data/"), false);
	suggestions.write("==============================================================\n");
	suggestions.write("================      AV Package Analyzer     ================\n");
	suggestions.write("==============================================================\n");
	suggestions.write("Original files:\n");
	for (File f : srcs) {
	    suggestions.write("  " + f.getPath() + "\n");
	}
	Set<String> noDups = new HashSet<String>(srcs.size());
    }

    static SPDefaultGUI init() throws IOException {
	SPDefaultGUI gui = new SPDefaultGUI("AV Package Analyzer", "Does not actually make a patch.  Reads in .dds files that you dumped into the source folder, then plugs and chugs fancy calculations into list of suggested seeds in an output text file.  Use these suggestions to help decide which NPCs to use as seeds in your AV packages.\n\n"
		+ "This is a very CPU heavy process, so it may very well take several minutes.");
	SPGlobal.createGlobalLog();
	SPGlobal.redirectSystemOutStream();
	SPGlobal.debugNIFimport = false;
	SPGlobal.debugBSAimport = false;
	SPGlobal.pathToData = "../../../../";
	return gui;
    }

    static void checkBSAs() {
	//For ProgressBar
	int numFiles = 0;
	for (BSA b : bsas) {
	    numFiles += b.numFiles();
	}
	SPGUI.progress.setMax(numFiles);

	for (BSA b : bsas) {
	    for (String folderPath : b.getFiles().keySet()) {
		ArrayList<String> files = b.getFiles().get(folderPath);
		for (String fileName : files) {
		    if (fileName.endsWith(".NIF")) {
			try {
			    checkNif(new File(folderPath + fileName), b.getFile(folderPath + fileName));
			    SPGUI.progress.setStatus("Processed: " + fileName);
			} catch (IOException ex) {
			    SPGlobal.logException(ex);
			} catch (DataFormatException ex) {
			    SPGlobal.logException(ex);
			}
		    }
		    SPGUI.progress.incrementBar();
		}
	    }
	}
    }

    static void checkMeshes() {
	for (File f : meshes) {
	    if (!f.getName().toUpperCase().endsWith(".NIF")) {
		continue;
	    }
	    checkNif(f);
	}
    }

    static void checkNif(File file) {
	try {
	    checkNif(file, new LShrinkArray(file));
	} catch (FileNotFoundException ex) {
	    SPGlobal.logException(ex);
	} catch (IOException ex) {
	    SPGlobal.logException(ex);
	}
    }

    static void checkNif(File file, LShrinkArray in) {
	NIF nif;
	try {
	    nif = new NIF(file.getPath(), in);
	    ArrayList<ArrayList<String>> textures = new ArrayList<ArrayList<String>>();

	    ArrayList<ArrayList<Node>> NiTriShapes = nif.getNiTriShapePackages();
	    for (ArrayList<Node> nodes : NiTriShapes) {
		for (Node n : nodes) {
		    if (n.type == NodeType.BSSHADERTEXTURESET) {
			textures.add(NIF.extractBSTextures(n));
		    }
		}
	    }

	    for (ArrayList<String> list : textures) {
		for (String nifPath : list) {
		    for (File src : srcs) {
			if (nifPath.toUpperCase().contains(src.getName().toUpperCase())) {
			    NIFtoSRC.put(src, file);
			}
		    }
		}
	    }

	} catch (BadParameter ex) {
	    SPGlobal.logException(ex);
	} catch (java.nio.BufferUnderflowException ex) {
	    SPGlobal.logException(ex);
	}
    }
}
