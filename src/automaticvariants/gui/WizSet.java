/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import automaticvariants.PackageNode;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lev.gui.*;
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

    LSearchComboBox sets;
    LLabel or;
    LTextField newSetName;
    LLabel locateTargetSkins;
    LButton manualPick;
    LButton analyzeTexture;

    public WizSet(SPMainMenuPanel parent_) {
	super(parent_, "Target Set", AV.orange, AV.packagesManagerPanel, AV.wizPackagesPanel, null);
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
	setQuestionText("Please select the set you want to add variants to.");


	sets = new LSearchComboBox("Existing Set", AV.AVFont, AV.yellow);
	sets.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	sets.putUnder(question, x, spacing);
	sets.addEnterButton("Next", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		WizNewPackage.newPackage.targetSet = (PackageNode) sets.getSelectedItem();
		AV.wizGroupPanel.open();
	    }
	});
	Add(sets);

	or = new LLabel("Or add a new set:", AV.AVFont, AV.green);
	or.addShadow();
	or.centerOn(sets, sets.getY() + sets.getHeight() + 50);
	Add(or);

	newSetName = new LTextField("New Set Name", AV.AVFont, AV.yellow);
	newSetName.setSize(settingsPanel.getWidth() - 2 * x, newSetName.getHeight());
	newSetName.putUnder(or, x, spacing);
	Add(newSetName);

	locateTargetSkins = new LLabel("Locate Target Skins:", AV.AVFont, AV.yellow);
	locateTargetSkins.putUnder(newSetName, x, spacing);
	locateTargetSkins.addShadow();
	Add(locateTargetSkins);

	analyzeTexture = new LButton("Analyze Texture to Locate Skins");
	analyzeTexture.setSize(220, 60);
	analyzeTexture.centerOn(or, locateTargetSkins.getBottom() + spacing);
	Add(analyzeTexture);

	manualPick = new LButton("Pick Skins Manually");
	manualPick.setSize(220, 60);
	manualPick.centerOn(or, analyzeTexture.getBottom() + spacing);
	manualPick.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		AV.wizSetManualPanel.open();
	    }
	});
	Add(manualPick);

    }

    @Override
    public void onOpen(SPMainMenuPanel parent_) {
	sets.reset();
	loadSets();
    }

    public void loadSets() {
	sets.setSelectedIndex(0);
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
    }

}
