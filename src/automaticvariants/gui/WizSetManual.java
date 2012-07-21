/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import java.awt.Font;
import java.util.Set;
import java.util.TreeSet;
import lev.gui.*;
import skyproc.ARMO;
import skyproc.NPC_;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPProgressBarPlug;
import skyproc.gui.SPQuestionPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WizSetManual extends SPQuestionPanel {

    LSearchComboBox skinPicker;
    LSearchComboBox npcPicker;
    TreeSet<String> skins;
    TreeSet<String> npcs;
    LLabel newSkinsLabel;
    LList newSkins;
    LButton remove;
    LButton accept;

    LLabel progressLabel;
    LProgressBar progress;

    public WizSetManual(SPMainMenuPanel parent_) {
	super(parent_, "Target Skins", AV.orange, AV.packagesManagerPanel, AV.wizSetPanel, null);
    }

    @Override
    protected void initialize() {
	super.initialize();

	spacing = 15;
	int x = 15;
	int fieldHeight = 65;

	setQuestionFont(AV.AVFont);
	setQuestionCentered();
	setQuestionColor(AV.green);
	setQuestionText("Please select the skins your variant should target.");


	skinPicker = new LSearchComboBox("Via Skin", AV.AVFont, AV.yellow);
	skinPicker.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	skinPicker.addEnterButton("Add Skin", null);
	skinPicker.putUnder(question, x, spacing);
	Add(skinPicker);

	npcPicker = new LSearchComboBox("Via NPC's Skin", AV.AVFont, AV.yellow);
	npcPicker.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	npcPicker.putUnder(skinPicker, skinPicker.getX(), 30);
	npcPicker.addEnterButton("Add Skin", null);
	Add(npcPicker);

	newSkins = new LList();
	newSkins.setSize(settingsPanel.getWidth() - 30, 100);
	newSkins.putUnder(npcPicker, x, spacing * 3);
	Add(newSkins);

	newSkinsLabel = new LLabel("Target Skins For Variant", AV.AVFont, AV.yellow);
	newSkinsLabel.setLocation(x, newSkins.getY() - 10 - newSkinsLabel.getHeight());
	newSkinsLabel.addShadow();
	Add(newSkinsLabel);

	remove = new LButton("Remove Selected");
	remove.setSize((newSkins.getWidth() - 15) / 2 , remove.getHeight());
	remove.putUnder(newSkins, x, 10);
	Add(remove);

	accept = new LButton("Accept and Next");
	accept.setSize((newSkins.getWidth() - 15) / 2 , remove.getHeight());
	accept.putUnder(newSkins, remove.getRight() + spacing, 10);
	Add(accept);

	progressLabel = new LLabel("Loading up mods, please wait...", AV.AVFontSmall, AV.lightGray);
	progressLabel.centerIn(settingsPanel, question.getBottom() + spacing * 3);
	Add(progressLabel);

	progress = new LProgressBar(200, 20, AV.AVFontSmall, AV.lightGray);
	progress.centerIn(settingsPanel, progressLabel.getBottom() + 10);
	SPProgressBarPlug.addProgressBar(progress);
	Add(progress);

	displaySwitch(true);
	SUMGUI.startImport(new SkinLoad());
    }

    @Override
    public void onOpen(SPMainMenuPanel parent) {
	skinPicker.reset();
	npcPicker.reset();
    }

    void loadSkins() {
	skinPicker.removeAllItems();
	Set<String> toAdd = new TreeSet<>();
	for (ARMO skin : AV.getMerger().getArmors()) {
	    if (!AVFileVars.unusedSkins.contains(skin.getForm())) {
		toAdd.add(skin.getEDID());
	    }
	}

	toAdd.remove("SkinNaked");
	toAdd.remove("SkinNakedBeast");
	toAdd.remove("ArmorAfflicted");
	toAdd.remove("ArmorAstrid");
	toAdd.remove("ArmorManakin");

	for (String s : toAdd) {
	    skinPicker.addItem(s);
	}
    }

    void loadNPCs() {
	npcPicker.removeAllItems();
	Set<String> toAdd = new TreeSet<>();
	for (NPC_ npc : AV.getMerger().getNPCs()) {
	    toAdd.add(npc.getEDID());
	}

	for (String s : toAdd) {
	    npcPicker.addItem(s);
	}
    }

    void displaySwitch (Boolean start) {
	skinPicker.setVisible(!start);
	npcPicker.setVisible(!start);
	newSkins.setVisible(!start);
	newSkinsLabel.setVisible(!start);
	accept.setVisible(!start);
	remove.setVisible(!start);
	progressLabel.setVisible(start);
	progress.setVisible(start);
    }

    class SkinLoad implements Runnable {

	@Override
	public void run() {
	    AVFileVars.locateUnused();
	    loadSkins();
	    loadNPCs();
	    displaySwitch(false);
	}
    }
}
