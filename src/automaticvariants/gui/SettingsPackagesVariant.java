/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVSaveFile.Settings;
import java.awt.Color;
import lev.gui.*;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class SettingsPackagesVariant extends SPSettingPanel {
    
    LLabel editing;
    LLabel variantName;
    LTextField author;
    LNumericSetting probDiv;
    LCheckBox exclusiveRegion;
    LNumericSetting height;
    LNumericSetting health;
    LNumericSetting magicka;
    LNumericSetting stamina;
    LNumericSetting speed;
    LTextField namePrefix;
    LTextField nameAffix;
    LButton cancel;
    
    public SettingsPackagesVariant (SPMainMenuPanel parent_) {
	super("Variant Specifications", AV.save, parent_, AV.orange);
    }
    
    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    save.setVisible(false);
	    defaults.setVisible(false);

	    editing = new LLabel("EDITING", AV.settingsFont, AV.green);
	    editing.centerIn(settingsPanel, 55);
	    Add(editing);
	    
	    variantName = new LLabel("Test", AV.settingsFont, Color.LIGHT_GRAY);
	    variantName.setLocation(0, 75);
	    Add(variantName);
	    
	    author = new LTextField("Author", AV.settingsFont, AV.yellow);
	    author.linkTo(Settings.SPEC_VAR_AUTHOR, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(author, last.x, last.y + 40);
	    Add(author);
	    
	    probDiv = new LNumericSetting("Probability Divider", AV.settingsFont, AV.yellow, 1, 99, 1);
	    probDiv.linkTo(Settings.SPEC_VAR_PROB, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(probDiv, last);
	    Add(probDiv);
	    
	    exclusiveRegion = new LCheckBox("Exclusive Region", AV.settingsFont, AV.yellow);
	    exclusiveRegion.linkTo(Settings.SPEC_VAR_REGION, saveFile, SUMGUI.helpPanel, true);
	    exclusiveRegion.addShadow();
	    exclusiveRegion.setOffset(2);
	    last = setPlacement(exclusiveRegion, last);
	    Add(exclusiveRegion);
	    
	    height = new LNumericSetting("Relative Height", AV.settingsFont, AV.yellow, 1, 1000, 1);
	    height.linkTo(Settings.SPEC_VAR_HEIGHT, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(height, last);
	    Add(height);
	    
	    health = new LNumericSetting("Relative Health", AV.settingsFont, AV.yellow, 1, 1000, 1);
	    health.linkTo(Settings.SPEC_VAR_HEALTH, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(health, last);
	    Add(health);
	    
	    magicka = new LNumericSetting("Relative Magicka", AV.settingsFont, AV.yellow, 1, 1000, 1);
	    magicka.linkTo(Settings.SPEC_VAR_MAGICKA, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(magicka, last);
	    Add(magicka);
	    
	    stamina = new LNumericSetting("Relative Stamina", AV.settingsFont, AV.yellow, 1, 1000, 1);
	    stamina.linkTo(Settings.SPEC_VAR_STAMINA, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(stamina, last);
	    Add(stamina);
	    
	    speed = new LNumericSetting("Relative Speed", AV.settingsFont, AV.yellow, 1, 1000, 1);
	    speed.linkTo(Settings.SPEC_VAR_SPEED, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(speed, last);
	    Add(speed);
	    
	    namePrefix = new LTextField("Name Prefix", AV.settingsFont, AV.yellow);
	    namePrefix.linkTo(Settings.SPEC_VAR_NAME_PREFIX, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(namePrefix, last);
	    Add(namePrefix);
	    
	    nameAffix = new LTextField("Name Affix", AV.settingsFont, AV.yellow);
	    nameAffix.linkTo(Settings.SPEC_VAR_NAME_AFFIX, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(nameAffix, last);
	    Add(nameAffix);
	    
	    cancel = new LButton("Cancel");
	    cancel.centerIn(settingsPanel, defaults.getY());
	    Add(cancel);
	    
	    alignRight();

	    return true;
	}
	return false;
    }
    
    @Override
    public void specialOpen(SPMainMenuPanel parent) {
	cancel.addActionListener(AV.packagesManagerPanel.getOpenHandler(parent));
    }
    
}
