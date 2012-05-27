/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import lev.Ln;
import lev.gui.LSaveFile;

/**
 *
 * @author Justin Swanson
 */
public class AVSaveFile extends LSaveFile {

    @Override
    protected void initSettings() {
	Add(Settings.PACKAGES_ON, "Packages On", false, true);
	Add(Settings.PACKAGES_ORIG_AS_VAR, "Orig as Var", false, true);
	Add(Settings.DEBUG_LEVEL, "Debug Level", false, 1);
	Add(Settings.IMPORT_AT_START, "Import at Start", false, false);
	Add(Settings.HEIGHT_ON, "Height Variants On", false, true);
	Add(Settings.HEIGHT_STD, "Height Variants STD", false, 10);
	Add(Settings.MINIMIZE_PATCH, "Height Variants On", false, true);
	Add(Settings.MAX_MEM, "Max memory MB", false, 750);
    }

    @Override
    protected void initHelp() {
	helpInfo.put(Settings.PACKAGES_ON, "This feature will duplicate and reorganize records to make actors"
		+ " with different textures spawn."
		+ "\n\nThe variants will be created from the contents your enabled AV Packages."
		+ "\n\nNOTE: If you disable a package that you have been playing with, "
		+ "make sure to reset the cells of your savegame.");

	helpInfo.put(Settings.PACKAGES_DISABLE, "This will disable any selected items and their children.\n\n"
		+ "Disabled items will not be integrated into the AV patch.");

	helpInfo.put(Settings.PACKAGES_ENABLE, "This will enable any selected items and their children.\n\n"
		+ "Enabled items will be integrated into the AV patch.");

	helpInfo.put(Settings.PACKAGES_COMPRESS, "This will run AV's compression algorithm over the package,"
		+ " making the package as small as possible.  It will move common files to Variant Set positions, as"
		+ " well as replacing any duplicate files with reroute 'shortcuts' so that there is only one of each file.\n\n"
		+ "NOTES: It is recommended that you make a backup in case something goes wrong, as this function will be moving and"
		+ " deleting things.\n\n"
		+ "This function will enable the selection before compressing.\n\n"
		+ "This may take a while to process.  A popup will appear showing the results when it is complete.");
	
	helpInfo.put(Settings.PACKAGES_ORIG_AS_VAR, "This will add the non-AV actor setup as a variant, and add it to the list of options. \n\n"
		+ "If turned off, then only variants explicitly part of an AV Package will spawn.");

	helpInfo.put(Settings.PACKAGES_GATHER, "This is a utility function that should only be used if you want to modify AV Package contents "
		+ "manually.\n\n"
		+ "NOTE:  This will not generate a working patch.  Just close the program normally to do that.\n\n"
		+ "In normal operation, AV moves all of the variant textures out of the AV Packages folder and into "
		+ "the 'Data/Textures/' folder for in-game use.\n"
		+ "This button will simply gather them back to the AV Packages folder and quit, so that you can modify "
		+ "AV Packages manually in windows.  Make sure to re-run the patcher before attemting to play with AV again.");

	helpInfo.put(Settings.HEIGHT_ON, "This variant setup will give each actor "
		+ "that spawns a variance in its height.");
	
	helpInfo.put(Settings.HEIGHT_STD, "This determines the maximum difference from "
		+ "the normal height an actor can be. \n\n"
		
		+ "The probability of what height an actor will spawn "
		+ "as follows a bell curve, where normal height is most"
		+ " common, and the max/min height is fairly rare.\n\n"
		
		+ "NOTE: The Bethesda function used to set the size of an"
		+ " actor does NOT change its hitbox size.  Therefore, if you're"
		+ " playing an archer type character trying to get headshots, it is recommended you"
		+ " keep this setting fairly conservative.");

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
		+ "- Faster patching when you close the program.\n"
		+ "- More information displayed in GUI, as it will have access to the records."
		+ "\n\n"
		+ "Downsides:\n"
		+ "- catch Having this on might make the GUI respond sluggishly while it processes in the"
		+ "background.");

	helpInfo.put(Settings.MAX_MEM,
		"This will determine the max amount of megabytes of memory AV will be allowed to use.\n\n"
		+ "Current max memory: " + Ln.toMB(Runtime.getRuntime().maxMemory()) + "MB\n\n"
		+ "If AV runs out of memory the program will essentially halt as it "
		+ "tries to scrap by with too little memory. "
		+ "If you experience this, then try allocating more memory.\n\n"
		+ "NOTE:  This setting will not take effect until you restart AV.");

	helpInfo.put(Settings.MINIMIZE_PATCH,
		"This will make AV do more processing in order to minimize the patch size.");
    }

    public enum Settings {

	PACKAGES_ON,
	PACKAGES_GATHER,
	PACKAGES_ORIG_AS_VAR,
	PACKAGES_COMPRESS,
	PACKAGES_ENABLE,
	PACKAGES_DISABLE,
	HEIGHT_ON,
	HEIGHT_STD,
	DEBUG_LEVEL,
	IMPORT_AT_START,
	MINIMIZE_PATCH,
	MAX_MEM,
	AV_SETTINGS;
    }
}
