package automaticvariants;

import automaticvariants.gui.AVGUI;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;
import lev.LMergeMap;
import lev.debug.LDebug;
import lev.gui.LSaveFile;
import skyproc.*;
import skyproc.exceptions.BadParameter;
import skyproc.exceptions.Uninitialized;

/**
 *
 * @author Leviathan1753
 */
public class AV {

    /*
     * Static Strings
     */
    static private String header = "AV";
    /*
     * Storage Maps
     */
    static LMergeMap<FormID, NPC_> modifiedNPCs = new LMergeMap<FormID, NPC_>(false);
    /*
     * Exception lists
     */
    static Set<FormID> block = new HashSet<FormID>();
    static Set<String> edidExclude = new HashSet<String>();
    /*
     * Script/Property names
     */
    static String alreadySwitched = "AlreadySwitched";
    static String raceAttachScript = "AVRaceAttachment";
    static String changeRaceOn = "RaceVariantOn";
    static String heightOn = "HeightVariantOn";
    static String heightMin = "HeightVariantMin";
    static String heightMax = "HeightVariantMax";
    static String heightWidth = "HeightVariantWidth";
    static String heightIntensity = "HeightVariantIntensity";
    /*
     * Other
     */
    public static LSaveFile save = new AVSaveFile();
    static FLST alreadySwitchedList;
    static boolean heightOnF = false;
    static String extraPath = "";
    static int numSteps = 10;
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
	    save.init();
//	    AVGUI.open();
	    SPDefaultGUI gui = createGUI();

	    // AVGUI runs the program after it's finished displaying.
	    runProgram();

	} catch (Exception e) {
	    // If a major error happens, print it everywhere and display a message box.
	    System.err.println(e.toString());
	    SPGlobal.logException(e);
	    JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution: '" + e + "'  Check the debug logs.");
	}

	// Close debug logs before program exits.
	SPGlobal.closeDebug();
    }

    static void runProgram() throws IOException, Uninitialized, BadParameter {

	Mod patch = new Mod("Automatic Variants", false);
	patch.setFlag(Mod.Mod_Flags.STRING_TABLED, false);
	patch.setAuthor("Leviathan1753");
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


	alreadySwitchedList = new FLST(patch, "AV_" + alreadySwitched);

	// For all race SWITCHING variants
	// (such as texture variants)
	AVRaceSwitchVariants.setUpRaceSwitchVariants(source, patch);

	// For all non-race SWITCHING variants
	// (such as height variant scripting)
	setUpInGameScriptBasedVariants(source);

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

	AVGUI.progress.done();
    }

    static void setUpInGameScriptBasedVariants(Mod source) {
	SPEL addScriptSpell = NiftyFunc.genScriptAttachingSpel(SPGlobal.getGlobalPatch(), generateAttachScript(), "AVGenericScriptAttach");
	for (RACE race : source.getRaces()) {
	    if (!AVRaceSwitchVariants.switcherRaces.containsKey(race.getForm())) {
		race.addSpell(addScriptSpell.getForm());
		SPGlobal.getGlobalPatch().addRecord(race);
	    }
	}
    }

    static boolean checkNPCskip(NPC_ npcSrc, boolean last) {
	String edid = npcSrc.getEDID().toUpperCase();
	if (!npcSrc.getTemplate().equals(FormID.NULL) && npcSrc.get(NPC_.TemplateFlag.USE_TRAITS)) {
	    if (SPGlobal.logging()) {
		if (last) {
		    SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		}
		SPGlobal.log(header, "    Skipping " + npcSrc + " : Template with traits flag");
	    }
	    return true;
	}
	for (String exclude : edidExclude) {
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

    static ScriptRef generateAttachScript() {
	ScriptRef script = new ScriptRef(raceAttachScript);
	script.setProperty(changeRaceOn, false);
	script.setProperty(alreadySwitched, alreadySwitchedList.getForm());
	if (heightOnF) {
	    script.setProperty(heightOn, true);
	    script.setProperty(heightMin, (float) 0);
	    script.setProperty(heightMax, (float) .98);
	    script.setProperty(heightWidth, (float) 5);
	    script.setProperty(heightIntensity, (float) 9);
	} else {
	    script.setProperty(heightOn, false);
	}
	return script;
    }

    static void importMods() throws IOException {
	try {
	    SPImporter importer = new SPImporter();
	    importer.importActiveMods(
		    GRUP_TYPE.NPC_, GRUP_TYPE.RACE,
		    GRUP_TYPE.ARMO, GRUP_TYPE.ARMA,
		    GRUP_TYPE.LVLN, GRUP_TYPE.TXST,
		    GRUP_TYPE.WEAP);
	} catch (IOException ex) {
	    // If things go wrong, create an error box.
	    SPGlobal.logException(ex);
	    JOptionPane.showMessageDialog(null, "There was an error importing plugins.\n(" + ex.getMessage() + ")\n\nPlease contact Leviathan1753.");
	    LDebug.wrapUp();
	    System.exit(0);
	}
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
		AVRaceSwitchVariants.avPackages = new File(extraPath + AVRaceSwitchVariants.avPackages.getPath());
		AVRaceSwitchVariants.avMeshes = new File(extraPath + AVRaceSwitchVariants.avMeshes.getPath());
		AVRaceSwitchVariants.avTextures = new File(extraPath + AVRaceSwitchVariants.avTextures.getPath());
		if (SPGlobal.logging()) {
		    SPGlobal.logMain(header, "Extra Path set to: " + extraPath);
		    SPGlobal.logMain(header, "Path to data: " + SPGlobal.pathToData);
		}
	    }

	}

	if (arguments.contains("-gather")) {
	    AVRaceSwitchVariants.gatherFiles();
	    return true;
	}

	return false;
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
	    gui.replaceHeader(AVGUI.class.getResource("AutoVarGUITitle.png"), - 35);
	} catch (IOException ex) {
	    SPGlobal.logException(ex);
	}
	return gui;
    }

    public enum Settings {
	HEIGHT_ON,
	HEIGHT_STD
	;
    }
}
