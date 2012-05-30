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
	Add(Settings.PACKAGES_ON,		false, true);
	Add(Settings.PACKAGES_ORIG_AS_VAR,	false, true);
	Add(Settings.DEBUG_LEVEL,		false, 1);
	Add(Settings.IMPORT_AT_START,		false, false);
	Add(Settings.STATS_ON,			false, true);
	Add(Settings.STATS_HEIGHT_MAX,		false, 15);
	Add(Settings.STATS_HEALTH_MAX,		false, 25);
	Add(Settings.STATS_MAGIC_MAX,		false, 25);
	Add(Settings.STATS_STAMINA_MAX,		false, 25);
	Add(Settings.STATS_SPEED_MAX,		false, 10);
	Add(Settings.STATS_TIE,			false, true);
	Add(Settings.MINIMIZE_PATCH,		false, true);
	Add(Settings.MAX_MEM,			false, 750);
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

	helpInfo.put(Settings.STATS_ON, "This variant setup will randomly skew the stats of an actor "
		+ "so that each spawn has a different height, health, speed, etc.\n\n"
		+ "This is applied in addition to the variant-specific stat differences that modders can"
		+ " put on their specific variants.  So, for example, if one of your AV Packages introduces "
		+ "a red troll with 15% more health, then any red troll spawns will have 15% bonus health that "
		+ "the modder desired, but will then be skewed by this setup to be slightly higher or lower for "
		+ "each spawn.");
	
	helpInfo.put(Settings.STATS_HEIGHT_MAX, "This determines the maximum percentage difference from "
		+ "the normal height an actor can be. \n\n"
		
		+ "The probability of what height an actor will spawn "
		+ "as follows a bell curve, where normal height is most"
		+ " common, and the (max / min) height is very rare (about 0.5% chance).\n\n"
		
		+ "NOTE: The Bethesda function used to set the size of an"
		+ " actor does NOT change its hitbox size.  Therefore, if you're"
		+ " playing an archer type character trying to get headshots, it is recommended you"
		+ " keep this setting fairly conservative.");
	
	helpInfo.put(Settings.STATS_HEALTH_MAX, "This determines the maximum percentage difference from "
		+ "the normal health pool an actor can have. \n\n"
		
		+ "The probability of how much health an actor will spawn "
		+ "with follows a bell curve, where normal health is most"
		+ " common, and the (max / min) health is very rare (about 0.5% chance).");
	
	helpInfo.put(Settings.STATS_MAGIC_MAX, "This determines the maximum percentage difference from "
		+ "the normal mana pool an actor can have. \n\n"
		
		+ "The probability of how much mana an actor will spawn "
		+ "with follows a bell curve, where normal size is most"
		+ " common, and the (max / min) size is very rare (about 0.5% chance).");
	
	helpInfo.put(Settings.STATS_STAMINA_MAX, "This determines the maximum percentage difference from "
		+ "the normal stamina pool an actor can have. \n\n"
		
		+ "The probability of how much stamina an actor will spawn "
		+ "with follows a bell curve, where normal stamina is most"
		+ " common, and the (max / min) stamina is very rare (about 0.5% chance).");
	
	helpInfo.put(Settings.STATS_SPEED_MAX, "This determines the maximum percentage difference from "
		+ "the normal speed an actor can have. \n\n"
		
		+ "The probability of what speed an actor will spawn "
		+ "with follows a bell curve, where normal speed is most"
		+ " common, and the (max / min) speed is very rare (about 0.5% chance).");
	
	helpInfo.put(Settings.STATS_TIE, "This setting will tie all the stat differences together, "
		+ "so that more often units will spawn either strong or weak in all areas, rather than"
		+ " randomly getting differences for each stat.\n\n"
		+ "For example, with this setting you will more likely encounter units that have more health and speed "
		+ "if they are taller, and less health and speed if they are shorter.");
	
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
	STATS_ON,
	STATS_HEIGHT_MAX,
	STATS_HEALTH_MAX,
	STATS_MAGIC_MAX,
	STATS_STAMINA_MAX,
	STATS_SPEED_MAX,
	STATS_TIE,
	DEBUG_LEVEL,
	IMPORT_AT_START,
	MINIMIZE_PATCH,
	MAX_MEM,
	AV_SETTINGS;
    }
}
