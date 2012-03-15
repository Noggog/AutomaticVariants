/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package avpackageanalyzer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import lev.LMergeMap;
import lev.LShrinkArray;
import lev.Ln;
import lev.debug.LDebug;
import skyproc.NIF.Node;
import skyproc.NIF.NodeType;
import skyproc.*;
import skyproc.ARMA.AltTexture;
import skyproc.exceptions.BadParameter;

/**
 *
 * @author Justin Swanson
 */
public class AVPackageAnalyzer {

    static ArrayList<File> srcs;
    static LMergeMap<File, File> NIFtoSRC = new LMergeMap<>(false);
    static Map<FormID, File> ARMAtoNIF = new HashMap<>();
    static Map<FormID, FormID> ARMOtoARMA = new HashMap<>();
    static Map<FormID, FormID> RACEtoARMO = new HashMap<>();
    static Map<FormID, FormID> NPCtoARMO = new HashMap<>();
    static ArrayList<File> meshes;
    static ArrayList<BSA> bsas;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
	
	SPDefaultGUI gui = new SPDefaultGUI("AV Package Analyzer", "Does not actually make a patch.  Reads in .dds files that you dumped into the source folder, then plugs and chugs fancy calculations into list of suggested seeds in an output text file.  Use these suggestions to help decide which NPCs to use as seeds in your AV packages.\n\n"
		+ "This is a very CPU heavy process, so it may very well take several minutes.");
	
	SPGlobal.createGlobalLog();
	SPGlobal.redirectSystemOutStream();
	SPGlobal.debugNIFimport = false;
	SPGlobal.debugBSAimport = false;
	SPGlobal.pathToData = "../../../../../";

	srcs = Ln.generateFileList(new File("Source Data/"), false);
	bsas = BSA.loadInBSAs(BSA.FileType.NIF);
	meshes = Ln.generateFileList(new File(SPGlobal.pathToData + "meshes/"), false);
	checkMeshes();
	checkBSAs();
	SPGlobal.log("SrcToNifs", "Printing Sources to their Nifs:");
	NIFtoSRC.print();
	NIFtoSRC = NIFtoSRC.flip();

	// Got Nifs that use textures, find NPCs
	SPImporter importer = new SPImporter();
	Mod merger = new Mod("tmpMod", false);
	merger.addAsOverrides(importer.importActiveMods(GRUP_TYPE.NPC_, GRUP_TYPE.ARMA,
		GRUP_TYPE.ARMO, GRUP_TYPE.RACE));


	// ARMA
	LMergeMap<File, FormID> srcToARMAs = new LMergeMap<>(false);
	for (ARMA arma : merger.getArmatures()) {
	    File model = new File("MESHES/" + arma.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).toUpperCase());
	    if (NIFtoSRC.containsKey(model)) {
		srcToARMAs.put(model, arma.getForm());
	    }
	}
	ARMAtoNIF = srcToARMAs.flip().flatten();
	SPGlobal.log("ARMAs", "Printing ARMAs to their sources:");
	for (FormID arma : ARMAtoNIF.keySet()) {
	    System.out.println(SPDatabase.getMajor(arma, GRUP_TYPE.ARMA));
	    System.out.println("  " + ARMAtoNIF.get(arma));
	}

	// ARMO
	LMergeMap<FormID, FormID> srcToARMOs = new LMergeMap<>(false);
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
	LMergeMap<FormID, FormID> ARMOToRACE = new LMergeMap<>(false);
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
	LMergeMap<FormID, FormID> ARMOToNPC = new LMergeMap<>(false);
	for (NPC_ npc : merger.getNPCs()) {
	    if (!npc.getWornArmor().equals(FormID.NULL) && ARMOtoARMA.containsKey(npc.getWornArmor())) {
		ARMOToNPC.put(npc.getWornArmor(), npc.getForm());
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
	BufferedWriter out = new BufferedWriter(new FileWriter("Suggestions.txt"));
	out.write("==============================================================\n");
	out.write("================      AV Package Analyzer     ================\n");
	out.write("==============================================================\n");
	out.write("Original files:\n");
	for (File f : srcs) {
	    out.write("  " + f.getPath() + "\n");
	}
	out.write("\n\n");
	out.write("==============================================================\n\n");
	out.write("These are groups of NPCs that share a common skin.\n");
	out.write("It is suggested you put a seed from each group into the spec file,\n"
		+ "                     --or--\n"
		+ "make a separate Variant Set for each if it makes logical sense to \n"
		+ "separate them (such as Black vs Ice wolves).\n\n");
	out.write("Remember to use NPCs as seeds.  Do not use an ARMA record.\n\n");

	LMergeMap<FormID, FormID> ARMAtoNPC = new LMergeMap<>(false);
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
				ArrayList<AltTexture> keyTmp = new ArrayList<>();
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
	    out.write("Group " + group++ + ": " + SPDatabase.getMajor(arma, GRUP_TYPE.ARMA) + "\n");
	    out.write("   Seed choices:\n");
	    for (FormID npc : ARMAtoNPC.get(arma)) {
		out.write("      " + SPDatabase.getMajor(npc, GRUP_TYPE.NPC_) + "\n");
	    }
	    out.write("   Source textures to include:\n");
	    File nif = ARMAtoNIF.get(arma);
	    for (File src : NIFtoSRC.get(nif)) {
		out.write("      " + src + "\n");
	    }
	    out.write("-------------------------------------------------------------------\n\n");
	}

	out.close();
	gui.finished();

	LDebug.wrapUp();
    }

    static void checkBSAs() {
	for (BSA b : bsas) {
	    for (String folderPath : b.getFiles().keySet()) {
		ArrayList<String> files = b.getFiles().get(folderPath);
		for (String filename : files) {
		    if (filename.endsWith(".NIF")) {
			try {
			    checkNif(new File(folderPath + filename), b.getFile(folderPath + filename));
			} catch (IOException | DataFormatException ex) {
			    SPGlobal.logException(ex);
			}
		    }
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
	    ArrayList<ArrayList<String>> textures = new ArrayList<>();

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

	} catch (BadParameter | java.nio.BufferUnderflowException ex) {
	    SPGlobal.logException(ex);
	}
    }
}