/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import java.awt.Color;
import lev.gui.LNumericSetting;

/**
 *
 * @author Justin Swanson
 */
public class ManagePackagesPanel extends DefaultsPanel {

    LNumericSetting stdDevSetting;

    public ManagePackagesPanel(EncompassingPanel parent_) {
	super("Texture Variants", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    stdDevSetting = new LNumericSetting("Height Difference", AVGUI.settingsFont, AVGUI.light,
		    0, 100, 1, AV.Settings.HEIGHT_STD, AV.save, parent.helpPanel);
	    last = setPlacement(stdDevSetting, last);
	    AddSetting(stdDevSetting);

	    alignRight();

	    return true;
	}
	return false;
    }
}
