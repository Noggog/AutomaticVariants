/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVSaveFile;
import automaticvariants.AVSaveFile.Settings;
import lev.gui.LCheckBox;
import lev.gui.LComboBox;
import lev.gui.LLabel;

/**
 *
 * @author Justin Swanson
 */
public class SettingsOther extends DefaultsPanel {

    LLabel debugLabel;
    LComboBox debugLevel;
    LCheckBox importOnStartup;

    public SettingsOther(EncompassingPanel parent_) {
	super("Other Settings", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    importOnStartup = new LCheckBox("Import Mods on Startup", AVGUI.settingsFont, AVGUI.yellow);
	    importOnStartup.tie(AVSaveFile.Settings.IMPORT_AT_START, saveFile, parent.helpPanel, true);
	    importOnStartup.setOffset(2);
	    last = setPlacement(importOnStartup, last);
	    AddSetting(importOnStartup);

	    debugLabel = new LLabel ("Debug Level", AVGUI.settingsFont, AVGUI.yellow);

	    debugLevel = new LComboBox("Debug Level");
	    debugLevel.setSize(150, 25);
	    debugLevel.addItem("Off");
	    debugLevel.addItem("AV Debug");
	    debugLevel.addItem("AV + SkyProc Debug");
	    debugLevel.tie(Settings.DEBUG_LEVEL, saveFile, parent.helpPanel, true);
	    last = setPlacement(debugLevel, last.x + debugLabel.getWidth() + 15,last.y);
	    AddSetting(debugLevel);

	    debugLabel.setLocation(debugLevel.getX() - debugLabel.getWidth() - 15, debugLevel.getY());
	    Add(debugLabel);


	    alignRight();

	    return true;
	}
	return false;
    }

}
