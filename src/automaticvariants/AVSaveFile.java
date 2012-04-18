/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.*;
import java.util.Map;
import javax.swing.JOptionPane;
import lev.gui.LSaveFile;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class AVSaveFile extends LSaveFile {

    @Override
    protected void init(Map m) {
	Add(m, Settings.PACKAGES_ON, "Packages On", false, true);
	Add(m, Settings.PACKAGES_PREP, "Package Prep", false, true);
	Add(m, Settings.DEBUG_LEVEL, "Debug Level", false, 1);
	Add(m, Settings.IMPORT_AT_START, "Import at Start", false, false);
	Add(m, Settings.HEIGHT_ON, "Height Variants On", false, true);
	Add(m, Settings.HEIGHT_STD, "Height Variants Min", false, 10);
	Add(m, Settings.MINIMIZE_PATCH, "Height Variants On", false, true);
    }

    @Override
    public void readInSettings() {
	File f = new File(SPGlobal.pathToInternalFiles + "Savefile");
	SPGlobal.log("SaveFile Import", "Starting import");
	if (f.exists()) {
	    try {
		BufferedReader input = new BufferedReader(new FileReader(f));
		input.readLine();  //title
		String inStr;
		String settingTitle;
		while (input.ready()) {
		    inStr = input.readLine();
		    settingTitle = inStr.substring(4, inStr.indexOf(" to "));
		    for (Enum s : saveSettings.keySet()) {
			if (saveSettings.containsKey(s)) {
			    if (saveSettings.get(s).getTitle().equals(settingTitle)) {
				saveSettings.get(s).readSetting(inStr);
				curSettings.get(s).readSetting(inStr);
			    }
			}
		    }
		}

	    } catch (Exception e) {
		JOptionPane.showMessageDialog(null, "Error in reading in save file. Reverting to default settings.");
		super.init();
	    }
	}
    }

    @Override
    public void saveToFile() {
	SPGlobal.log("SaveFile Export", "Starting export");

	File f = new File(SPGlobal.pathToInternalFiles);
	if (!f.isDirectory()) {
	    f.mkdirs();
	}
	f = new File(SPGlobal.pathToInternalFiles + "Savefile");
	if (f.isFile()) {
	    f.delete();
	}

	try {
	    BufferedWriter output = new BufferedWriter(new FileWriter(f));
	    output.write("AV savefile used for the application.\n");
	    for (Enum s : curSettings.keySet()) {
		if (!curSettings.get(s).get().equals("")) {
		    SPGlobal.log("SaveFile Export", "Exporting to savefile: ", curSettings.get(s).getTitle());
		    curSettings.get(s).write(output);
		} else {
		    defaultSettings.get(s).write(output);
		}
	    }
	    output.close();
	} catch (java.io.IOException e) {
	    JOptionPane.showMessageDialog(null, "The application couldn't open the save file output stream.  Your DLL settings were not saved.");
	}
    }

    @Override
    protected void initHelp() {
	helpInfo.put(Settings.PACKAGES_ON, "This feature will duplicate and reorganize records to make actors"
		+ " with different textures spawn."
		+ "\n\nThe variants will be created from the contents your enabled AV Packages."
		+ "\n\nNOTE: If you disable a package that you have been playing with, "
		+ "make sure to reset the cells of your savegame.");

	helpInfo.put(Settings.PACKAGES_PREP, "This will prep AV to be able to safely add/remove packages "
		+ "without bugging out.  It is still highly recommended that you start a new game when installing "
		+ "AV;  However, once a game is 'established' with AV Prepping, then it should be safe to enable/disable "
		+ "new packages without consequence.\n\n"

		+ "This will make every NPC a \"variant of one\" so that variants can be added/removed from the list without "
		+ "drastically changing the record structure.\n\n"

		+ "It is highly recommended that you keep this setting on.");

	helpInfo.put(Settings.PACKAGES_GATHER, "This is a utility function that should only be used if you want to modify AV Package contents "
		+ "manually.\n\n"
		+ "NOTE:  This will not generate a working patch.  Just close the program normally to do that.\n\n"
		+ "In normal operation, AV moves all of the variant textures out of the AV Packages folder and into "
		+ "the 'Data/Textures/' folder for in-game use.\n"
		+ "This button will simply gather them back to the AV Packages folder and quit, so that you can modify "
		+ "AV Packages manually in windows.  Make sure to re-run the patcher before attemting to play with AV again.");

	helpInfo.put(Settings.HEIGHT_ON, "This variant setup will give each actor "
		+ "that spawns a variance in its height.");

	helpInfo.put(Settings.AV_SETTINGS,
		"These are AV settings related to this patcher program.");

	helpInfo.put(Settings.DEBUG_LEVEL,
		"This affects which debug messages will be logged. "
		+ "The less debug messages printed, the quicker it will process.\n\n"
		+ "NOTE: This setting will not take effect until the next time the program is run.\n\n"
		+ "AV Debug \n"
		+ "Print information regarding reading in the AV"
		+ " Packages, and duplicating and processing records.\n\n"
		+ "SkyProc Debug \n"
		+ "Print information regarding the importing of "
		+ "mods on your load order.");

	helpInfo.put(Settings.IMPORT_AT_START,
		"If enabled, AV will begin importing your mods when the program starts.\n"
		+ "If turned off, the program will wait until it is necessary before importing.\n\n"

		+ "NOTE: This setting will not take effect until the next time the program is run.\n\n"

		+ "Benefits:\n"
		+ "- Faster patching when you close the program."
		+ "- More information displayed in GUI, as it will have access to the records."
		+ "\n\n"

		+ "Downsides:\n"
		+ "Having this on might make the GUI respond sluggishly while it processes in the"
		+ "background.");
    }

    public enum Settings {

	PACKAGES_ON,
	PACKAGES_PREP,
	PACKAGES_GATHER,

	HEIGHT_ON,
	HEIGHT_STD,

	DEBUG_LEVEL,
	IMPORT_AT_START,
	MINIMIZE_PATCH,
	AV_SETTINGS;
    }
}
