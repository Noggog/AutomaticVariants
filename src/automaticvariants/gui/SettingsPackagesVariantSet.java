/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVSaveFile;
import lev.gui.LFormIDPicker;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class SettingsPackagesVariantSet extends SettingsPackagesSpecs {
    
    LFormIDPicker seeds;
    
    public SettingsPackagesVariantSet (SPMainMenuPanel parent_) {
	super(parent_, "Variant Set Specifications");
    }
    
    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    seeds = new LFormIDPicker("Seed NPCs", AV.settingsFont, AV.yellow);
	    seeds.linkTo(AVSaveFile.Settings.SPEC_VAR_AUTHOR, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(seeds, last);
	    Add(seeds);
	    
	    alignRight();

	    return true;
	}
	return false;
    }
}
