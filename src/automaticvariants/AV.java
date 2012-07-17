package automaticvariants;

import automaticvariants.AVSaveFile.Settings;
import automaticvariants.gui.*;
import com.google.gson.Gson;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Point;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lev.LMergeMap;
import lev.Ln;
import lev.debug.LDebug;
import lev.gui.LSaveFile;
import lev.gui.resources.LFonts;
import skyproc.*;
import skyproc.GLOB.GLOBType;
import skyproc.gui.*;

/**
 * ToDo: - Make compress work for disabled files
 *
 * @author Leviathan1753
 */
public class AV implements SUM {

    // Version
    public static String version = "1.4.3 Beta";

    /*
     * Static Strings
     */
    static private String header = "AV";
    /*
     * Storage Maps
     */
    static LMergeMap<FormID, NPC_> modifiedNPCs = new LMergeMap<>(false);
    /*
     * Exception lists
     */
    static Set<FormID> block = new HashSet<>();
    static Set<String> edidExclude = new HashSet<>();
    /*
     * Script/Property names
     */
    static String raceAttachScript = "AVRaceAttachment";
    GLOB texturesOn;
    GLOB statsOn;
    GLOB heightScale;
    GLOB healthScale;
    GLOB magickaScale;
    GLOB staminaScale;
    GLOB speedScale;
    GLOB tieStats;
    /*
     * Other
     */
    public static LSaveFile save = new AVSaveFile();
    public static QUST quest;
    public static Thread parser;
    public static Gson gson = new Gson();
    static boolean heightOnF = false;
    static String extraPath = "";
    static int numSteps = 8;
    static int step = 0;
    static int initDebugLevel = -1;
    //GUI
    static public SPMainMenuPanel settingsMenu;
    static public SPMainMenuConfig packageManagerConfig;
    static public SettingsPackagesManager packagesManagerPanel;
    static public SettingsPackagesOther packagesOtherPanel;
    static public SettingsPackagesVariant packagesVariantPanel;
    static public SettingsPackagesVariantSet packagesVariantSetPanel;
    static public SettingsOther otherPanel;
    static public SettingsStatsPanel heightPanel;
    static public Font AVFont;
    static public Font AVFontSmall;
    static public Color green = new Color(67, 162, 10);
    static public Color darkGreen = new Color(61, 128, 21);
    static public Color orange = new Color(247, 163, 52);
    static public Color blue = new Color(0, 147, 196);
    static public Color yellow = new Color(255, 204, 26);
    static public Color lightGray = new Color(190, 190, 190);
    static public Color darkGray = new Color(110, 110, 110);
    static public boolean gatheringAndExiting = false;

    public static void main(String[] args) {
	ArrayList<String> arguments = new ArrayList<>(Arrays.asList(args));
	try {
	    save.init();

	    if (handleArgs(arguments)) {
		SPGlobal.closeDebug();
		return;
	    }
	    cleanUp();
	    setSkyProcGlobals();
	    setDebugLevel();

	    SUMGUI.open(new AV());

	} catch (Exception e) {
	    // If a major error happens, print it everywhere and display a message box.
	    System.err.println(e.toString());
	    SPGlobal.logException(e);
	    JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution: '" + e + "'  Check the debug logs.");
	    SPGlobal.closeDebug();
	}

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
	    if (!AVFileVars.switcherSpells.containsKey(race.getForm())) {
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
	script.setProperty("AVQuest", quest.getForm());
	return script;
    }

    public void makeAVQuest() {
	ScriptRef questScript = new ScriptRef("AVQuestScript");
	questScript.setProperty("TexturesOn", texturesOn.getForm());
	questScript.setProperty("StatsOn", statsOn.getForm());
	questScript.setProperty("HeightScale", heightScale.getForm());
	questScript.setProperty("HealthScale", healthScale.getForm());
	questScript.setProperty("MagickaScale", magickaScale.getForm());
	questScript.setProperty("StaminaScale", staminaScale.getForm());
	questScript.setProperty("SpeedScale", speedScale.getForm());
	questScript.setProperty("TieStats", tieStats.getForm());

	// Log Table
	Float[] logTable = new Float[1000];
	for (int i = 0; i < logTable.length; i++) {
	    logTable[i] = (float) Math.log((i + 1) / 1000.0);
	}
	questScript.setProperty("LogTable", logTable);

	quest = NiftyFunc.makeScriptQuest(SPGlobal.getGlobalPatch(), questScript);
    }

    public void makeGlobals() {
	texturesOn = new GLOB(SPGlobal.getGlobalPatch(), "AVTexturesOn", GLOBType.Short);
	texturesOn.setValue(save.getBool(Settings.PACKAGES_ON));
	texturesOn.setConstant(true);

	statsOn = new GLOB(SPGlobal.getGlobalPatch(), "AVStatsOn", GLOBType.Short);
	statsOn.setValue(save.getBool(Settings.STATS_ON));
	statsOn.setConstant(true);

	double scale = 100.0 // To percent (.01) instead of ints (1)
		* 3.0; // Scaled to 3 standard deviations

	heightScale = new GLOB(SPGlobal.getGlobalPatch(), "AVHeightScale", GLOBType.Float);
	heightScale.setValue((float) (save.getInt(Settings.STATS_HEIGHT_MAX) / scale));
	heightScale.setConstant(true);

	healthScale = new GLOB(SPGlobal.getGlobalPatch(), "AVHealthScale", GLOBType.Float);
	healthScale.setValue((float) (save.getInt(Settings.STATS_HEALTH_MAX) / scale));
	healthScale.setConstant(true);

	magickaScale = new GLOB(SPGlobal.getGlobalPatch(), "AVMagickaScale", GLOBType.Float);
	magickaScale.setValue((float) (save.getInt(Settings.STATS_MAGIC_MAX) / scale));
	magickaScale.setConstant(true);

	staminaScale = new GLOB(SPGlobal.getGlobalPatch(), "AVStaminaScale", GLOBType.Float);
	staminaScale.setValue((float) (save.getInt(Settings.STATS_STAMINA_MAX) / scale));
	staminaScale.setConstant(true);

	speedScale = new GLOB(SPGlobal.getGlobalPatch(), "AVSpeedScale", GLOBType.Float);
	speedScale.setValue((float) (save.getInt(Settings.STATS_SPEED_MAX) / scale));
	speedScale.setConstant(true);

	tieStats = new GLOB(SPGlobal.getGlobalPatch(), "AVTieStats", GLOBType.Short);
	tieStats.setValue(save.getBool(Settings.STATS_TIE));
	tieStats.setConstant(true);
    }

    private static void setSkyProcGlobals() {
	/*
	 * Initializing Debug Log and Globals
	 */
	SPGlobal.createGlobalLog(extraPath);
	SPGlobal.debugModMerge = false;
	SPGlobal.debugExportSummary = false;
	SPGlobal.debugBSAimport = false;
	SPGlobal.debugNIFimport = false;
	SPGlobal.newSpecialLog(SpecialLogs.WARNINGS, "Warnings.txt");
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
	    // Less than .85 * max memory desired
	    if (Runtime.getRuntime().maxMemory() < AV.save.getInt(Settings.MAX_MEM) * 0.85 * 1024 * 1024) {
		NiftyFunc.allocateMoreMemory("100m", AV.save.getInt(Settings.MAX_MEM) + "m", "Automatic Variants.jar", "-nonew");
	    }
	}

	if (arguments.contains("-gather")) {
	    AVFileVars.gatherFiles();
	    return true;
	}

	return false;
    }

    public static void exitProgram() {
	SPGlobal.log(header, "Exit requested.");
	save.saveToFile();
	LDebug.wrapUpAndExit();
    }

    @Override
    public JFrame openCustomMenu() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean needsPatching() {
	//Need to check if packages have changed.
	ArrayList<File> files = Ln.generateFileList(new File(AVFileVars.AVPackagesDir), false);
	try {
	    ArrayList<String> last = AVFileVars.getAVPackagesListing();
	    ArrayList<String> lastTmp = new ArrayList<>(last);
	    if (files.size() != last.size()) {
		return true;
	    }

	    for (File f : files) {
		String path = f.getPath();
		if (!lastTmp.contains(path)) {
		    return true;
		}
	    }

	} catch (IOException ex) {
	    SPGlobal.logException(ex);
	    return true;
	}
	return false;
    }

    @Override
    public void onExit(boolean patchWasGenerated) throws IOException {
	if (!gatheringAndExiting) {
	    AVFileVars.saveAVPackagesListing();
	    AVFileVars.moveOut();
	}
    }

    @Override
    public void onStart() throws Exception {
	try {
	    AVFont = Font.createFont(Font.TRUETYPE_FONT, SettingsOther.class.getResource("Sony_Sketch_EF.ttf").openStream());
	    AVFont = AVFont.deriveFont(Font.BOLD, 19);
	} catch (IOException | FontFormatException ex) {
	    SPGlobal.logException(ex);
	    AVFont = new Font("Serif", Font.BOLD, 16);
	}
	AVFontSmall = AVFont.deriveFont(Font.PLAIN, 12);
	AVFileVars.gatherFiles();
	AVFileVars.importVariants();
    }

    public enum SpecialLogs {

	WARNINGS;
    }

    @Override
    public String getName() {
	return "Automatic Variants";
    }

    @Override
    public GRUP_TYPE[] dangerousRecordReport() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GRUP_TYPE[] importRequests() {
	return new GRUP_TYPE[]{GRUP_TYPE.NPC_, GRUP_TYPE.RACE,
		    GRUP_TYPE.ARMO, GRUP_TYPE.ARMA,
		    GRUP_TYPE.LVLN, GRUP_TYPE.TXST,
		    GRUP_TYPE.SPEL};
    }

    @Override
    public boolean hasStandardMenu() {
	return true;
    }

    @Override
    public SPMainMenuPanel getStandardMenu() {

	settingsMenu = new SPMainMenuPanel(green);
	settingsMenu.addLogo(this.getLogo());
	settingsMenu.setVersion(version, new Point(80, 88));
	settingsMenu.setBackgroundPicture(SettingsOther.class.getResource("AV background.jpg"));
	settingsMenu.setMainFont(AVFont, 25, 40, 27);
//	SUMGUI.helpPanel.setHeaderFont(new Font("Serif", Font.PLAIN, 10));

	packagesManagerPanel = new SettingsPackagesManager(settingsMenu);
	packagesOtherPanel = new SettingsPackagesOther(settingsMenu);
	packagesVariantPanel = new SettingsPackagesVariant(settingsMenu);
	packagesVariantSetPanel = new SettingsPackagesVariantSet(settingsMenu);
	packageManagerConfig = settingsMenu.addMenu(packagesManagerPanel, false, save, Settings.PACKAGES_ON);

//	heightPanel = new SettingsStatsPanel(settingsMenu);
//	settingsMenu.addMenu(heightPanel, true, save, Settings.STATS_ON);

	otherPanel = new SettingsOther(settingsMenu);
	settingsMenu.addMenu(otherPanel, false, save, Settings.AV_SETTINGS);

	settingsMenu.setWelcomePanel(new WelcomePage(settingsMenu));

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

	readInExceptions();

	SPGlobal.loggingSync(true);
	SPGlobal.logging(true);

	makeGlobals();
	makeAVQuest();

	SPProgressBarPlug.progress.setMax(numSteps);
	SPProgressBarPlug.progress.setStatus(step++, numSteps, "Initializing AV");
	Mod source = new Mod("Temporary", false);
	source.addAsOverrides(SPGlobal.getDB());

	// For all race SWITCHING variants
	// (such as texture variants)
	AVFileVars.setUpFileVariants(source);
//
//	// For all non-skin SWITCHING variants
//	// (such as height variant scripting)
//	setUpInGameScriptBasedVariants(source);
    }

    @Override
    public boolean hasCustomMenu() {
	return false;
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
	Mod patch = new Mod(getListing());
	patch.setFlag(Mod.Mod_Flags.STRING_TABLED, false);
	patch.setAuthor("Leviathan1753");
	return patch;
    }

    @Override
    public Color getHeaderColor() {
	return green;
    }

    @Override
    public ModListing getListing() {
	return new ModListing("Automatic Variants", false);
    }
}
