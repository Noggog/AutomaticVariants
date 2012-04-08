/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVSaveFile.Settings;
import lev.gui.LComboBox;
import lev.gui.LLabel;

/**
 *
 * @author Justin Swanson
 */
public class SettingsOther extends DefaultsPanel {

    LLabel debugLabel;
    LComboBox debugLevel;

    public SettingsOther(EncompassingPanel parent_) {
	super("Other Settings", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    debugLabel = new LLabel ("Debug Level", AVGUI.settingsFont, AVGUI.yellow);
	    last = setPlacement(debugLabel, last);
	    Add(debugLabel);

	    debugLevel = new LComboBox("Debug Level");
	    debugLevel.setSize(150, 25);
	    debugLevel.addItem("Off");
	    debugLevel.addItem("AV Debug");
	    debugLevel.addItem("AV + SkyProc Debug");
	    debugLevel.tie(Settings.DEBUG_LEVEL, saveFile, parent.helpPanel);
	    last = setPlacement(debugLevel, last);
	    AddSetting(debugLevel);

	    alignRight();

	    return true;
	}
	return false;
    }

}
