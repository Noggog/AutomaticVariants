package automaticvariants;

import skyproc.gui.SPProgressBarPlug;
import skyproc.gui.SUM;
import automaticvariants.AVSaveFile.Settings;
import automaticvariants.gui.SettingsHeightPanel;
import automaticvariants.gui.SettingsOther;
import automaticvariants.gui.SettingsPackagesManager;
import automaticvariants.gui.SettingsPackagesOther;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;
import lev.LMergeMap;
import lev.Ln;
import lev.debug.LDebug;
import lev.gui.LSaveFile;
import skyproc.*;
import skyproc.gui.SPComplexGUI;
import skyproc.gui.SPMainMenuConfig;
import skyproc.gui.SPMainMenuPanel;

/**
 * ToDo: - Make compress work for disabled files
 *
 * @author Leviathan1753
 */
public class AV implements SUM {

    // Version
    public static String version = "1.3.1 Alpha";

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
    public static Thread parser;
    static boolean heightOnF = false;
    static String extraPath = "";
    static int numSteps = 10;
    static int step = 0;
    static int initDebugLevel = -1;
    static boolean imported = false;
    static boolean exported = false;
    //GUI
    static public SPComplexGUI GUI = new SPComplexGUI();
    static public SPMainMenuPanel settingsMenu;
    static public SPMainMenuConfig packageManagerConfig;
    static public SettingsPackagesManager packagesManagerPanel;
    static public SettingsPackagesOther packagesOtherPanel;
    static public SettingsOther otherPanel;
    static public SettingsHeightPanel heightPanel;
    static public Font settingsFont = new Font("Serif", Font.BOLD, 16);
    static public Color green = new Color(67, 162, 10);
    static public Color darkGreen = new Color(61, 128, 21);
    static public Color orange = new Color(247, 163, 52);
    static public Color blue = new Color(0, 147, 196);
    static public Color yellow = new Color(255, 204, 26);
    static public Color lightGray = new Color(190, 190, 190);
    static public Color darkGray = new Color(110, 110, 110);

    public static void main(String[] args) {
	ArrayList<String> arguments = new ArrayList<String>(Arrays.asList(args));
	try {
	    save.init();

//	    AV.save.getBool(AVSaveFile.Settings.IMPORT_AT_START)
//	    versionNum = new LLabel("v" + AV.version, new Font("Serif", Font.PLAIN, 10), AVGUI.darkGray);
//	    versionNum.setLocation(80, 82);
//	    backgroundPanel.add(versionNum);
//	    settingsMenu = new SettingsMainMenu(getSize());
//	    backgroundPanel.add(settingsMenu);
//	    settingsMenu.open();

	    if (handleArgs(arguments)) {
		SPGlobal.closeDebug();
		return;
	    }
	    cleanUp();
	    setGlobals();
	    setDebugLevel();
	    AVFileVars.gatherFiles();

	    openGUI();

	} catch (Exception e) {
	    // If a major error happens, print it everywhere and display a message box.
	    System.err.println(e.toString());
	    SPGlobal.logException(e);
	    JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution: '" + e + "'  Check the debug logs.");
	    SPGlobal.closeDebug();
	}

    }

    static void openGUI() {
	SUM AV = new AV();

	settingsMenu = new SPMainMenuPanel(green);
	settingsMenu.addLogo(AV.getLogo());
	settingsMenu.setVersion(version, new Point(80, 88));

	packagesManagerPanel = new SettingsPackagesManager(settingsMenu);
	packagesOtherPanel = new SettingsPackagesOther(settingsMenu);
	settingsMenu.addMenu(packagesManagerPanel, true, save, Settings.PACKAGES_ON);

	otherPanel = new SettingsOther(settingsMenu);
	settingsMenu.addMenu(otherPanel, false, save, Settings.AV_SETTINGS);

	SPComplexGUI.open(AV);
    }

    static void setDebugLevel() {
	if (initDebugLevel != -1) {
	    save.saveSettings.get(Settings.DEBUG_LEVEL).setTo(initDebugLevel);
	    save.curSettings.get(Settings.DEBUG_LEVEL).setTo(initDebugLevel);
	}
	if (save.getInt(Settings.DEBUG_LEVEL) < 2) {
	    SPGlobal.loggingSync(false);
	} else if (save.getInt(Settings.DEBUG_LEVEL) < 1) {
	    SPGlobal.logging(false);
	}
    }

    static void cleanUp() {
	File delete = new File(SPGlobal.pathToInternalFiles + "Automatic Variants More Memory.bat");
	if (delete.isFile()) {
	    delete.delete();
	}
	delete = new File(SPGlobal.pathToInternalFiles + "AV Debug Mode.bat");
	if (delete.isFile()) {
	    delete.delete();
	}
	delete = new File(SPGlobal.pathToInternalFiles + "Gather Package Files.bat");
	if (delete.isFile()) {
	    delete.delete();
	}

	delete = new File("AV Starter.bat");
	if (delete.isFile()) {
	    delete.delete();
	}
    }

    static void setUpInGameScriptBasedVariants(Mod source) {
	SPEL addScriptSpell = NiftyFunc.genScriptAttachingSpel(SPGlobal.getGlobalPatch(), generateAttachScript(), "AVGenericScriptAttach");
	for (RACE race : source.getRaces()) {
	    if (!AVFileVars.switcherRaces.containsKey(race.getForm())) {
		race.addSpell(addScriptSpell.getForm());
		SPGlobal.getGlobalPatch().addRecord(race);
	    }
	}
    }

    static boolean checkNPCskip(NPC_ npcSrc, boolean print, boolean last) {
	if (npcSrc.get(NPC_.NPCFlag.Unique)) {
	    if (print && SPGlobal.logging()) {
		if (last) {
		    SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		}
		SPGlobal.log(header, "    Skipping " + npcSrc + " : Unique actor");
	    }
	    return true;
	}
	if (block.contains(AVFileVars.getUsedSkin(npcSrc))) {
	    if (print && SPGlobal.logging()) {
		if (last) {
		    SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		}
		SPGlobal.log(header, "    Skipping " + npcSrc + " : Blocked skin");
	    }
	    return true;
	}
	if (!npcSrc.getTemplate().equals(FormID.NULL)) {
	    if (npcSrc.get(NPC_.TemplateFlag.USE_TRAITS)) {
		if (print && SPGlobal.logging()) {
		    if (last) {
			SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		    }
		    SPGlobal.log(header, "    Skipping " + npcSrc + " : Template with traits flag");
		}
		return true;
	    } else if (NiftyFunc.isTemplatedToLList(npcSrc) != null) {
		if (print && SPGlobal.logging()) {
		    if (last) {
			SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		    }
		    SPGlobal.log(header, "    Skipping " + npcSrc + " : Template w/o traits flag but templated to a LList.");
		    SPGlobal.logBlocked(header, "Templated w/o traits flag but templated to a LList", npcSrc);
		}
		return true;
	    }
	}
	String edid = npcSrc.getEDID().toUpperCase();
	for (String exclude : edidExclude) {
	    if (edid.contains(exclude)) {
		if (print && SPGlobal.logging()) {
		    if (last) {
			SPGlobal.log(header, "---------------------------------------------------------------------------------------------------------");
		    }
		    SPGlobal.log(header, "    Skipping " + npcSrc + " : edid exclude '" + exclude + "'");
		    SPGlobal.logBlocked(header, "edid exclude " + exclude, npcSrc);
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

    private static void setGlobals() {
	/*
	 * Initializing Debug Log and Globals
	 */
	SPGlobal.createGlobalLog(extraPath);
	SPGlobal.debugModMerge = false;
	SPGlobal.debugExportSummary = false;
	SPGlobal.debugBSAimport = false;
	SPGlobal.debugNIFimport = false;
	LDebug.timeElapsed = true;
	LDebug.timeStamp = true;

	SPGlobal.logMain(header, "AV version: " + version);
	SPGlobal.logMain(header, "Available Memory: " + Ln.toMB(Runtime.getRuntime().totalMemory()) + "MB");
	SPGlobal.logMain(header, "Max Memory: " + Ln.toMB(Runtime.getRuntime().maxMemory()) + "MB");

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

    static boolean handleArgs(ArrayList<String> arguments) throws IOException, InterruptedException {
	String debug = "-debug";
	String nonew = "-nonew";

	for (String s : arguments) {
	    if (s.contains(debug)) {
		s = s.substring(s.indexOf(debug) + debug.length()).trim();
		try {
		    initDebugLevel = Integer.valueOf(s);
		} catch (NumberFormatException e) {
		}
	    }
	}

	if (!arguments.contains(nonew)) {
	    // Less than 1GB max memory, spawn new process with more memory
	    if (Runtime.getRuntime().maxMemory() < Math.pow(1024, 3)) {
		ProcessBuilder proc = new ProcessBuilder("java", "-jar", "-Xms100m", "-Xmx" + AV.save.getInt(Settings.MAX_MEM) + "m", "Automatic Variants.jar", "-nonew");
		Process start = proc.start();
		InputStream shellIn = start.getInputStream();
		int exitStatus = start.waitFor();
		String response = convertStreamToStr(shellIn);
		if (exitStatus != 0) {
		    JOptionPane.showMessageDialog(null, "Error allocating " + AV.save.getInt(Settings.MAX_MEM) + "MB memory:\n"
			    + response
			    + "\nMemory defaulted to lowest levels.  Please lower your\n"
			    + "allocated memory in Other Settings and start the program again.");
		} else {
		    System.exit(0);
		}
	    }
	}

	if (arguments.contains("-gather")) {
	    AVFileVars.gatherFiles();
	    return true;
	}

	return false;
    }

    public static String convertStreamToStr(InputStream is) throws IOException {

	if (is != null) {
	    Writer writer = new StringWriter();

	    char[] buffer = new char[1024];
	    try {
		Reader reader = new BufferedReader(new InputStreamReader(is,
			"UTF-8"));
		int n;
		while ((n = reader.read(buffer)) != -1) {
		    writer.write(buffer, 0, n);
		}
	    } finally {
		is.close();
	    }
	    return writer.toString();
	} else {
	    return "";
	}
    }

    public static void exitProgram() {
	SPGlobal.log(header, "Exit requested.");
	save.saveToFile();
	LDebug.wrapUpAndExit();
    }

    @Override
    public String getName() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GRUP_TYPE[] duplicateOriginalsReport() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GRUP_TYPE[] importRequests() {
	return new GRUP_TYPE[]{GRUP_TYPE.NPC_, GRUP_TYPE.RACE,
		    GRUP_TYPE.ARMO, GRUP_TYPE.ARMA,
		    GRUP_TYPE.LVLN, GRUP_TYPE.TXST,
		    GRUP_TYPE.WEAP};
    }

    @Override
    public boolean hasStandardMenu() {
	return true;
    }

    @Override
    public SPMainMenuPanel getStandardMenu() {
	return settingsMenu;
    }

    @Override
    public boolean hasLogo() {
	return true;
    }

    @Override
    public URL getLogo() {
	return SettingsOther.class.getResource("AutoVarGUITitle.png");
    }

    @Override
    public boolean hasSave() {
	return true;
    }

    @Override
    public LSaveFile getSave() {
	return save;
    }

    @Override
    public void runChangesToPatch() throws Exception {

	Mod patch = SPGlobal.getGlobalPatch();

	readInExceptions();

	SPGlobal.loggingSync(true);
	SPGlobal.logging(true);

	SPProgressBarPlug.progress.setMax(numSteps);
	SPProgressBarPlug.progress.setStatus(step++, numSteps, "Initializing AV");
	Mod source = new Mod("Temporary", false);
	source.addAsOverrides(SPGlobal.getDB());


	if (AVFileVars.raceSwitchMethod) {
	    alreadySwitchedList = new FLST(patch, "AV_" + alreadySwitched);
	}

	// For all race SWITCHING variants
	// (such as texture variants)
	AVFileVars.setUpFileVariants(source, patch);

	// For all non-race SWITCHING variants
	// (such as height variant scripting)
//	setUpInGameScriptBasedVariants(source);

	/*
	 * Close up shop.
	 */
	try {
	    // Export your custom patch.
	    patch.export();
	} catch (Exception ex) {
	    // If something goes wrong, show an error message.
	    SPGlobal.logException(ex);
	    JOptionPane.showMessageDialog(null, "There was an error exporting the custom patch.\n(" + ex.getMessage() + ")\n\nPlease contact Leviathan1753.");
	    exitProgram();
	}

	exported = true;
	SPProgressBarPlug.progress.done();
    }

    @Override
    public boolean hasCustomMenu() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void openCustomMenu() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean importAtStart() {
	return getSave().getBool(Settings.IMPORT_AT_START);
    }

    @Override
    public String getVersion() {
	return version;
    }

    @Override
    public Mod getExportPatch() {
	Mod patch = new Mod("Automatic Variants", false);
	patch.setFlag(Mod.Mod_Flags.STRING_TABLED, false);
	patch.setAuthor("Leviathan1753");
	return patch;
    }

    @Override
    public Color getHeaderColor() {
	return green;
    }
}
