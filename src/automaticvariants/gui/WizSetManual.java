/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import lev.gui.*;
import skyproc.*;
import skyproc.NPC_.NPCFlag;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPProgressBarPlug;
import skyproc.gui.SPQuestionPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WizSetManual extends WizTemplate {

    LSearchComboBox<EDIDdisplay<ARMO>> skinPicker;
    LSearchComboBox<EDIDdisplay<NPC_>> npcPicker;
    TreeSet<String> skins;
    TreeSet<String> npcs;
    LList<ARMO> newSkins;
    LLabel progressLabel;
    LProgressBar progress;
    ArrayList<String> blockedSkins = new ArrayList<>();

    public WizSetManual(SPMainMenuPanel parent_) {
	super(parent_, "Target Skins", AV.packagesManagerPanel, AV.wizSetPanel);
    }

    @Override
    protected void initialize() {
	super.initialize();

	blockedSkins.add("SkinNaked");
	blockedSkins.add("SkinNakedBeast");
	blockedSkins.add("ArmorAfflicted");
	blockedSkins.add("ArmorAstrid");
	blockedSkins.add("ArmorManakin");

	setQuestionText("Please select the skins your variant should target.");

	skinPicker = new LSearchComboBox<>("Via Skin", AV.AVFont, AV.yellow);
	skinPicker.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	skinPicker.addEnterButton("Add Skin", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		newSkins.addElement(skinPicker.getSelectedItem().m);
	    }
	});
	skinPicker.putUnder(question, x, spacing);
	skinPicker.addMouseListener(new MouseListener() {

	    @Override
	    public void mouseClicked(MouseEvent e) {
	    }

	    @Override
	    public void mousePressed(MouseEvent e) {
	    }

	    @Override
	    public void mouseReleased(MouseEvent e) {
	    }

	    @Override
	    public void mouseEntered(MouseEvent e) {
		SUMGUI.helpPanel.setDefaultPos();
		SUMGUI.helpPanel.setTitle("Pick Skin");
		SUMGUI.helpPanel.setContent("Directly pick the skin to tie to the variant.");
		SUMGUI.helpPanel.focusOn(skinPicker, 0);
	    }

	    @Override
	    public void mouseExited(MouseEvent e) {
		mainHelp();
	    }
	});
	Add(skinPicker);

	npcPicker = new LSearchComboBox<>("Via NPC's Skin", AV.AVFont, AV.yellow);
	npcPicker.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	npcPicker.putUnder(skinPicker, skinPicker.getX(), 30);
	npcPicker.addEnterButton("Add Skin", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		NPC_ npc = npcPicker.getSelectedItem().m;
		ARMO skin = (ARMO) SPDatabase.getMajor(AVFileVars.getUsedSkin(npc), GRUP_TYPE.ARMO);
		if (skin != null) {
		    newSkins.addElement(skin);
		}
	    }
	});
	npcPicker.addMouseListener(new MouseListener() {

	    @Override
	    public void mouseClicked(MouseEvent e) {
	    }

	    @Override
	    public void mousePressed(MouseEvent e) {
	    }

	    @Override
	    public void mouseReleased(MouseEvent e) {
	    }

	    @Override
	    public void mouseEntered(MouseEvent e) {
		SUMGUI.helpPanel.setDefaultPos();
		SUMGUI.helpPanel.setTitle("Pick Skin via NPC");
		SUMGUI.helpPanel.setContent("Pick an NPC and tie to the skin it uses.\n\n"
			+ "If an NPC is missing it means it doesn't use a skin, or it was "
			+ "blocked.");
		SUMGUI.helpPanel.focusOn(npcPicker, 0);
	    }

	    @Override
	    public void mouseExited(MouseEvent e) {
		mainHelp();
	    }
	});
	Add(npcPicker);

	newSkins = new LList<>("Chosen Target Skins", AV.AVFont, AV.yellow);
	newSkins.setUnique(true);
	newSkins.setSize(settingsPanel.getWidth() - 30, 150);
	newSkins.putUnder(npcPicker, x, spacing * 3);
	Add(newSkins);

	progressLabel = new LLabel("Loading up mods, please wait...", AV.AVFontSmall, AV.lightGray);
	progressLabel.centerIn(settingsPanel, question.getBottom() + spacing * 3);
	Add(progressLabel);

	progress = new LProgressBar(200, 20, AV.AVFontSmall, AV.lightGray);
	progress.centerIn(settingsPanel, progressLabel.getBottom() + 10);
	SPProgressBarPlug.addProgressBar(progress);
	Add(progress);

	setNext(AV.wizGenPanel);

	displaySwitch(true);
	SUMGUI.startImport(new SkinLoad());
    }

    @Override
    public void onOpen(SPMainMenuPanel parent) {
	mainHelp();
	editing.load(WizNewPackage.newPackage.targetPackage
		, WizNewPackage.newPackage.targetSet
		, null
		, null);
    }

    public void reset() {
	skinPicker.reset();
	npcPicker.reset();
	newSkins.clear();
    }

    void loadSkins() {
	skinPicker.removeAllItems();
	for (ARMO skin : AV.getMerger().getArmors()) {
	    if (!AVFileVars.unusedSkins.contains(skin.getForm())
		    && !blockedSkins.contains(skin.getEDID())) {
		skinPicker.addItem(new EDIDdisplay(skin));
	    }
	}
	skinPicker.reset();
    }

    void mainHelp() {
	SUMGUI.helpPanel.setDefaultPos();
	SUMGUI.helpPanel.setTitle("Manually Picking Skins");
	SUMGUI.helpPanel.setContent("Pick all the skins that are used by NPCs that you want your variants to be added to.\n\n"
		+ WizSet.multiSkin() + "\n\n"
		+ "If you are not sure if your target NPCs have multiple skins, go back and use the AV tool.");
	SUMGUI.helpPanel.hideArrow();
    }

    void loadNPCs() {
	npcPicker.removeAllItems();

	Set<String> block = new HashSet<>();
	block.add("AUDIO");
	block.add("DELETEWHENDONE");

	for (NPC_ npc : AV.getMerger().getNPCs()) {
	    boolean blocked = false;
	    String upper = npc.getEDID().toUpperCase();
	    for (String s : block) {
		if (upper.contains(s)) {
		    blocked = true;
		    break;
		}
	    }
	    if (blocked) {
		continue;
	    }

	    if (!npc.getTemplate().equals(FormID.NULL) && npc.get(NPC_.TemplateFlag.USE_TRAITS)) {
		continue;
	    }

	    ARMO skin = (ARMO) SPDatabase.getMajor(AVFileVars.getUsedSkin(npc), GRUP_TYPE.ARMO);
	    if (skin == null || blockedSkins.contains(skin.getEDID())) {
		continue;
	    }

	    npcPicker.addItem(new EDIDdisplay(npc));
	}
	npcPicker.reset();
    }

    @Override
    public boolean testNext() {
	return !newSkins.isEmpty();
    }

    @Override
    public void onNext() {
	WizNewPackage.newPackage.targetSkins = newSkins.getAll();
	AV.wizGenPanel.open();
	AV.wizGenPanel.reset();
	AV.wizGenPanel.setBack(AV.wizSetManualPanel);
    }

    void displaySwitch(Boolean start) {
	skinPicker.setVisible(!start);
	npcPicker.setVisible(!start);
	newSkins.setVisible(!start);
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
