/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVSaveFile.Settings;
import automaticvariants.Variant;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import lev.gui.*;
import skyproc.FormID;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @Author Justin Swanson
 */
public class SettingsPackagesVariant extends SPSettingPanel {

    LLabel editing;
    LLabel packageName;
    LLabel variantName;
    LTextField author;
    LNumericSetting probDiv;
    LFormIDPicker region;
    LCheckBox exclusiveRegion;
    LNumericSetting height;
    LNumericSetting health;
    LNumericSetting magicka;
    LNumericSetting stamina;
    LNumericSetting speed;
    LTextField namePrefix;
    LTextField nameAffix;
    LButton saveSpec;
    LButton cancel;
    
    Variant target;

    public SettingsPackagesVariant(SPMainMenuPanel parent_) {
	super("Variant Specifications", AV.save, parent_, AV.orange);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    save.setVisible(false);
	    defaults.setVisible(false);

	    editing = new LLabel("EDITING", AV.settingsFont, AV.green);
	    editing.addShadow();
	    editing.setLocation(15, 55);
	    Add(editing);

	    packageName = new LLabel("Test", AV.settingsFontSmall, Color.LIGHT_GRAY);
	    packageName.setLocation(0, 55);
	    Add(packageName);

	    variantName = new LLabel("Test", AV.settingsFontSmall, Color.LIGHT_GRAY);
	    variantName.setLocation(0, 68);
	    Add(variantName);

	    author = new LTextField("Author", AV.settingsFont, AV.yellow);
	    author.linkTo(Settings.SPEC_VAR_AUTHOR, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(author, last.x, last.y + 15);
	    Add(author);

	    probDiv = new LNumericSetting("Probability Divider", AV.settingsFont, AV.yellow, 1, 99, 1);
	    probDiv.linkTo(Settings.SPEC_VAR_PROB, saveFile, SUMGUI.helpPanel, true);
	    last = setPlacement(probDiv, last);
	    Add(probDiv);

//	    region = new LFormIDPicker("Regions to Spawn in", AV.settingsFont, AV.yellow);
//	    region.linkTo(Settings.SPEC_VAR_REGION, saveFile, SUMGUI.helpPanel, true);
//	    last = setPlacement(region, last.x, last.y - 5);
//	    Add(region);
//
//	    exclusiveRegion = new LCheckBox("Exclusive Region", AV.settingsFont, AV.yellow);
//	    exclusiveRegion.linkTo(Settings.SPEC_VAR_REGION_EXCLUDE, saveFile, SUMGUI.helpPanel, true);
//	    exclusiveRegion.addShadow();
//	    exclusiveRegion.setOffset(2);
//	    last = setPlacement(exclusiveRegion, last.x, last.y - 5);
//	    Add(exclusiveRegion);

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

	    saveSpec = new LButton("Save");
	    saveSpec.setLocation(defaults.getLocation());
	    saveSpec.setSize(defaults.getSize());
	    saveSpec.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent arg0) {
		    save();
		}
	    });
	    Add(saveSpec);
	    
	    cancel = new LButton("Cancel");
	    cancel.setLocation(save.getLocation());
	    cancel.setSize(save.getSize());
	    Add(cancel);
	    
	    alignRight();

	    return true;
	}
	return false;
    }

    @Override
    public void specialOpen(SPMainMenuPanel parent) {
	cancel.addActionListener(AV.packagesManagerPanel.getOpenHandler(parent));
	saveSpec.addActionListener(AV.packagesManagerPanel.getOpenHandler(parent));
    }

    public void load(Variant v) {
	// Title alignment
	String name = v.printName();
	packageName.setText(name.substring(0, name.indexOf(" - ")));
	variantName.setText(name.substring(name.indexOf(" - ") + 3));
	int totalLength;
	if (variantName.getWidth() > packageName.getWidth()) {
	    totalLength = variantName.getWidth();
	} else {
	    totalLength = packageName.getWidth();
	}
	totalLength += editing.getWidth() + 10;
	editing.setLocation(settingsPanel.getWidth() / 2 - totalLength / 2, editing.getY());
	packageName.setLocation(editing.getX() + editing.getWidth() + 10, packageName.getY());
	variantName.setLocation(packageName.getX(), variantName.getY());
	
	author.setText(v.spec.Author);
	
	probDiv.setValue(v.spec.Probability_Divider);
	
//	region.load(v.spec.Region_Include);
//	
//	exclusiveRegion.setSelected(v.spec.Exclusive_Region);
	
	health.setValue(v.spec.Health_Mult);
	magicka.setValue(v.spec.Magicka_Mult);
	stamina.setValue(v.spec.Stamina_Mult);
	speed.setValue(v.spec.Speed_Mult);
	height.setValue(v.spec.Height_Mult);
	
	namePrefix.setText(v.spec.Name_Prefix);
	
	nameAffix.setText(v.spec.Name_Affix);
	
	target = v;
    }
    
    public void save() {
	if (target == null) {
	    return;
	}
	
	target.spec.Author = author.getText();
	target.spec.Probability_Divider = probDiv.getValue();
//	ArrayList<FormID> regionsList = region.getPickedIDs();
//	String[][] regions = new String[regionsList.size()][];
//	for (int i = 0 ; i < regionsList.size() ; i++) {
//	    String id = regionsList.get(i).getFormStr();
//	    regions[i][0] = id.substring(0, 6);
//	    regions[i][1] = id.substring(6);
//	}
//	target.spec.Region_Include = regions;
//	target.spec.Exclusive_Region = this.exclusiveRegion.isSelected();
	target.spec.Health_Mult = health.getValue();
	target.spec.Magicka_Mult = magicka.getValue();
	target.spec.Stamina_Mult = stamina.getValue();
	target.spec.Speed_Mult = speed.getValue();
	target.spec.Height_Mult = height.getValue();
	target.spec.Name_Prefix = this.namePrefix.getText();
	target.spec.Name_Affix = this.nameAffix.getText();
	try {
	    target.spec.export();
	} catch (IOException ex) {
	    JOptionPane.showMessageDialog(null, "There was an error exporting the spec file, please contact Leviathan1753");
	}
    }
}
