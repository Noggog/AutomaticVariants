/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import automaticvariants.PackageNode;
import java.util.Set;
import java.util.TreeSet;
import lev.gui.LButton;
import lev.gui.LComboBox;
import lev.gui.LLabel;
import lev.gui.LList;
import skyproc.ARMO;
import skyproc.NPC_;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPQuestionPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WizSet extends SPQuestionPanel {

    LComboBox sets;
    LLabel existingSet;
    LButton nextExisting;
    LLabel or;

    LLabel skin;
    LComboBox skinPicker;
    LButton addSkin;

    LLabel npc;
    LComboBox npcPicker;
    LButton addNPC;

    TreeSet<String> skins;
    TreeSet<String> npcs;

    LList newSkins;

    public WizSet(SPMainMenuPanel parent_) {
	super(parent_, "Target Skins", AV.orange);
    }

    @Override
    protected void initialize() {
	super.initialize();

	spacing = 15;
	int x = 15;

	setQuestionFont(AV.AVFont);
	setQuestionCentered();
	setQuestionColor(AV.green);
	setQuestionText("Please select the set or skins you want to add variants to.");

	existingSet = new LLabel("Existing Variant Set", AV.AVFont, AV.yellow);
	existingSet.addShadow();
	existingSet.putUnder(question, x, spacing);
	Add(existingSet);

	sets = new LComboBox("Set Picker");
	sets.setSize(250, 25);
	sets.putUnder(existingSet, existingSet.getX(), 10);
	updateLast(sets);
	Add(sets);

	nextExisting = new LButton("Next");
	nextExisting.centerOn(settingsPanel.getWidth() - nextExisting.getWidth() - x, sets);
	sets.setSize(nextExisting.getX() - sets.getX() - 10, sets.getHeight());
	Add(nextExisting);

	or = new LLabel("Or add a new set:", AV.AVFont, AV.green);
	or.addShadow();
	setPlacement(or);
	Add(or);

	skin = new LLabel("Via Skin", AV.AVFont, AV.yellow);
	skin.addShadow();
	skin.putUnder(or, x, spacing);
	Add(skin);

	skinPicker = new LComboBox("Skin Picker");
	skinPicker.setSize(250, 25);
	skinPicker.putUnder(skin, skin.getX(), 10);
	Add(skinPicker);

	addSkin = new LButton("Add Skin");
	addSkin.centerOn(settingsPanel.getWidth() - addSkin.getWidth() - x, skinPicker);
	skinPicker.setSize(addSkin.getX() - skinPicker.getX() - 10, skinPicker.getHeight());
	Add(addSkin);

	npc = new LLabel("Via NPC's Skin", AV.AVFont, AV.yellow);
	npc.addShadow();
	npc.putUnder(addSkin, x, spacing);
	Add(npc);

	npcPicker = new LComboBox("NPC Picker");
	npcPicker.setSize(250, 25);
	npcPicker.putUnder(npc, npc.getX(), 10);
	updateLast(npcPicker);
	Add(npcPicker);

	addNPC = new LButton("Add NPC's Skin");
	addNPC.centerOn(settingsPanel.getWidth() - addNPC.getWidth() - x, npcPicker);
	npcPicker.setSize(addNPC.getX() - npcPicker.getX() - 10, npcPicker.getHeight());
	Add(addNPC);

	newSkins = new LList();
	newSkins.setSize(settingsPanel.getWidth() - 30, 150);
	setPlacement(newSkins);
	Add(newSkins);

	SUMGUI.startImport(new SkinLoad());
    }

    @Override
    public void specialOpen(SPMainMenuPanel parent_) {
	loadSets();
    }

    public void loadSets() {
	sets.removeAllItems();
	for (PackageNode p : WizNewPackage.newPackage.targetPackage.getAll(PackageNode.Type.VARSET)) {
	    sets.addItem(p);
	}
    }

    class SkinLoad implements Runnable {

	@Override
	public void run() {
	    AVFileVars.locateUnused();
	    loadSkins();
	    loadNPCs();
	}
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
}
