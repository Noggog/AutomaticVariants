/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import automaticvariants.PackageNode;
import java.awt.event.*;
import java.io.File;
import javax.swing.SwingUtilities;
import lev.Ln;
import lev.gui.*;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPQuestionPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WizSet extends WizTemplate {

    LSearchComboBox sets;
    LLabel or;
    LTextField newSetName;
    LLabel locateTargetSkins;
    LButton manualPick;
    LButton analyzeTexture;

    public WizSet(SPMainMenuPanel parent_) {
	super(parent_, "Target Set", AV.packagesManagerPanel, AV.wizPackagesPanel);
    }

    @Override
    protected void initialize() {
	super.initialize();

	setQuestionText("Please select the set you want to add variants to.");

	sets = new LSearchComboBox("Existing Set", AV.AVFont, AV.yellow);
	sets.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	sets.putUnder(question, x, spacing);
	sets.addEnterButton("Next", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (!sets.isEmpty()) {
		    WizNewPackage.newPackage.targetSet = (PackageNode) sets.getSelectedItem();
		    AV.wizGenPanel.open();
		}
	    }
	});
	Add(sets);

	or = new LLabel("Or add a new set:", AV.AVFont, AV.green);
	or.addShadow();
	or.centerOn(sets, sets.getBottom() + 50);
	Add(or);

	newSetName = new LTextField("New Set Name", AV.AVFont, AV.yellow);
	newSetName.setSize(settingsPanel.getWidth() - 2 * x, newSetName.getHeight());
	newSetName.putUnder(or, x, spacing);
	Add(newSetName);

	locateTargetSkins = new LLabel("Locate Target Skins:", AV.AVFont, AV.yellow);
	locateTargetSkins.putUnder(newSetName, x, spacing);
	locateTargetSkins.addShadow();
	Add(locateTargetSkins);

	analyzeTexture = new LButton("Use Tool to Locate Skins");
	analyzeTexture.setSize(220, 60);
	analyzeTexture.centerOn(or, locateTargetSkins.getBottom() + spacing);
	analyzeTexture.setFocusable(true);
	analyzeTexture.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (checkName()) {
		}
	    }
	});
	analyzeTexture.addMouseListener(new MouseListener() {

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
		SUMGUI.helpPanel.setTitle("Use Tool To Locate Skins");
		SUMGUI.helpPanel.setContent("This tool will take in your alternate texture files and "
			+ "process your mods to find ALL the skins that use them.  This will help you if "
			+ "you have no clue what skins are associated with the NPC you are making variants for.\n\n"
			+ "Also, this tool can help you locate when there are multiple skins you need to consider.");
		SUMGUI.helpPanel.focusOn(analyzeTexture, 0);
	    }

	    @Override
	    public void mouseExited(MouseEvent e) {
		mainHelp();
	    }
	});
	Add(analyzeTexture);

	manualPick = new LButton("Pick Skins Manually");
	manualPick.setSize(220, 60);
	manualPick.centerOn(or, analyzeTexture.getBottom() + spacing);
	manualPick.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (checkName()) {
		    AV.wizSetManualPanel.open();
		    AV.wizSetManualPanel.reset();
		}
	    }
	});
	manualPick.addMouseListener(new MouseListener() {

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
		SUMGUI.helpPanel.setTitle("Pick Skins Manually");
		SUMGUI.helpPanel.setContent("If you know exactly the NPC skins you want to target,"
			+ " you can quickly choose them yourself here.");
		SUMGUI.helpPanel.focusOn(manualPick, 0);
	    }

	    @Override
	    public void mouseExited(MouseEvent e) {
		mainHelp();
	    }
	});
	Add(manualPick);

    }

    boolean checkName() {
	String trimmed = newSetName.getText().trim();
	for (PackageNode p : WizNewPackage.newPackage.targetPackage.getAll(PackageNode.Type.VARSET)) {
	    if (p.src.getName().equalsIgnoreCase(trimmed)) {
		trimmed = "";
		break;
	    }
	}
	if (!trimmed.equals("")) {
	    File f = new File(WizNewPackage.newPackage.targetPackage.src.getPath() + "\\" + trimmed);
	    Ln.makeDirs(f);
	    PackageNode packageNode = new PackageNode(f, PackageNode.Type.VARSET);
	    WizNewPackage.newPackage.targetSet = packageNode;
	    return true;
	}
	newSetName.highlightChanged();
	return false;
    }

    @Override
    public void onOpen(SPMainMenuPanel parent_) {
	mainHelp();
	editing.load(WizNewPackage.newPackage.targetPackage
		, null
		, null
		, null);
	newSetName.clearHighlight();
    }

    public void reset() {
	sets.reset();
	newSetName.setText("");
	loadSets();
    }

    void mainHelp() {
	if (AV.wizSetPanel.isVisible()) {
	    SUMGUI.helpPanel.setDefaultPos();
	    SUMGUI.helpPanel.setTitle("Variant Set");
	    SUMGUI.helpPanel.setContent("A Variant Set contains variants that all target the same NPC skin(s).\n\n"
		    + "Name your set after the Actor you are making variants for. (Wolf, Giant, Rabbit, etc)\n\n"
		    + "To complete a Variant Set, you must select the NPC skin(s) that it should target.  You can do this manually,"
		    + " or you can use the supplied AV tool if you don't know which skin is associated with the NPC you are making variants for.");
	    SUMGUI.helpPanel.hideArrow();
	}
    }

    static String multiSkin() {
	return "Sometimes there are two skins for the same type of Actor.  AV will only give the variants to Actors with skins you have selected, "
		+ "so it is important to be aware of when an Actor has multiple skins. The supplied "
		+ "AV tool will notify you when there are multiple skins involved.\n\n"
		+ "Wolves are an example case of this scenario.  They have two skins in vanilla Skyrim: White and Black. "
		+ "When making a package for them, you can pick both if you want your variant to apply to ALL wolves, or just one if you want it to "
		+ "apply ONLY to Black/White wolves.";
    }

    public void loadSets() {
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		sets.removeAllItems();
	    }
	});
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		for (PackageNode p : WizNewPackage.newPackage.targetPackage.getAll(PackageNode.Type.VARSET)) {
		    sets.addItem(p);
		}
	    }
	});
	sets.setSelectedIndex(0);
    }
}
