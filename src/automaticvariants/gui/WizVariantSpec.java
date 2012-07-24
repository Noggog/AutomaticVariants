/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.*;
import automaticvariants.AVSaveFile.Settings;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lev.gui.*;
import skyproc.SPGlobal;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @Author Justin Swanson
 */
public class WizVariantSpec extends PackagesSpecs {

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

    public WizVariantSpec(SPMainMenuPanel parent_) {
	super(parent_, "Variant Spec");
    }

    @Override
    protected void initialize() {
	super.initialize();

	author = new LTextField("Author", AV.AVFont, AV.yellow);
	author.linkTo(Settings.SPEC_VAR_AUTHOR, AV.save, SUMGUI.helpPanel, true);
	setPlacement(author);
	Add(author);

	probDiv = new LNumericSetting("Probability Divider", AV.AVFont, AV.yellow, 1, 99, 1);
	probDiv.linkTo(Settings.SPEC_VAR_PROB, AV.save, SUMGUI.helpPanel, true);
	setPlacement(probDiv);
	Add(probDiv);

//	    region = new LFormIDPicker("Regions To Spawn In", AV.settingsFont, AV.yellow);
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

//	height = new LNumericSetting("Relative Height", AV.AVFont, AV.yellow, 1, 1000, 1);
//	height.linkTo(Settings.SPEC_VAR_HEIGHT, AV.save, SUMGUI.helpPanel, true);
//	setPlacement(height);
//	Add(height);
//
//	health = new LNumericSetting("Relative Health", AV.AVFont, AV.yellow, 1, 1000, 1);
//	health.linkTo(Settings.SPEC_VAR_HEALTH, AV.save, SUMGUI.helpPanel, true);
//	setPlacement(health);
//	Add(health);
//
//	magicka = new LNumericSetting("Relative Magicka", AV.AVFont, AV.yellow, 1, 1000, 1);
//	magicka.linkTo(Settings.SPEC_VAR_MAGICKA, AV.save, SUMGUI.helpPanel, true);
//	setPlacement(magicka);
//	Add(magicka);
//
//	stamina = new LNumericSetting("Relative Stamina", AV.AVFont, AV.yellow, 1, 1000, 1);
//	stamina.linkTo(Settings.SPEC_VAR_STAMINA, AV.save, SUMGUI.helpPanel, true);
//	setPlacement(stamina);
//	Add(stamina);
//
//	speed = new LNumericSetting("Relative Speed", AV.AVFont, AV.yellow, 1, 1000, 1);
//	speed.linkTo(Settings.SPEC_VAR_SPEED, AV.save, SUMGUI.helpPanel, true);
//	setPlacement(speed);
//	Add(speed);
//
//	namePrefix = new LTextField("Name Prefix", AV.AVFont, AV.yellow);
//	namePrefix.linkTo(Settings.SPEC_VAR_NAME_PREFIX, AV.save, SUMGUI.helpPanel, true);
//	setPlacement(namePrefix);
//	    Add(namePrefix);

//	nameAffix = new LTextField("Name Affix", AV.AVFont, AV.yellow);
//	nameAffix.linkTo(Settings.SPEC_VAR_NAME_AFFIX, AV.save, SUMGUI.helpPanel, true);
//	setPlacement(nameAffix);
//	    Add(nameAffix);

	alignRight();

    }

    @Override
    public void load(PackageNode n) {
	super.load(n);
	Variant v = (Variant) n;
	load(v.spec);
    }

    public void load(SpecVariant s) {

	author.setText(s.Author);

	probDiv.setValue(s.Probability_Divider);

//	region.load(v.spec.Region_Include);
//
//	exclusiveRegion.setSelected(v.spec.Exclusive_Region);

//	health.setValue(s.Health_Mult);
//	magicka.setValue(s.Magicka_Mult);
//	stamina.setValue(s.Stamina_Mult);
//	speed.setValue(s.Speed_Mult);
//	height.setValue(s.Height_Mult);
//
//	namePrefix.setText(s.Name_Prefix);
//
//	nameAffix.setText(s.Name_Affix);

	target = s;
    }

    @Override
    public void onOpen(SPMainMenuPanel parent) {
	if (WizNewPackage.open) {
	    load(new SpecVariant(WizNewPackage.newPackage.targetVariant.src));
	}
    }

    @Override
    public void save() {
	if (target == null) {
	    return;
	}

	SpecVariant v = (SpecVariant) target;

	v.Author = author.getText();
	v.Probability_Divider = probDiv.getValue();
//	ArrayList<FormID> regionsList = region.getPickedIDs();
//	String[][] regions = new String[regionsList.size()][];
//	for (int i = 0 ; i < regionsList.size() ; i++) {
//	    String id = regionsList.get(i).getFormStr();
//	    regions[i][0] = id.substring(0, 6);
//	    regions[i][1] = id.substring(6);
//	}
//	target.spec.Region_Include = regions;
//	target.spec.Exclusive_Region = this.exclusiveRegion.isSelected();
//	v.Health_Mult = health.getValue();
//	v.Magicka_Mult = magicka.getValue();
//	v.Stamina_Mult = stamina.getValue();
//	v.Speed_Mult = speed.getValue();
//	v.Height_Mult = height.getValue();
//	v.Name_Prefix = this.namePrefix.getText();
//	v.Name_Affix = this.nameAffix.getText();

	super.save();
    }
}