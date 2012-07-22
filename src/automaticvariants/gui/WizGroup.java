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
import java.io.File;
import javax.swing.SwingUtilities;
import lev.Ln;
import lev.gui.LLabel;
import lev.gui.LSearchComboBox;
import lev.gui.LTextField;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPQuestionPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WizGroup extends WizTemplate {

    LSearchComboBox groups;
    LLabel or;
    LTextField newGroupField;

    public WizGroup(SPMainMenuPanel parent_) {
	super(parent_, "Grouping", AV.packagesManagerPanel, AV.wizGenPanel);
    }

    @Override
    protected void initialize() {
	super.initialize();

	setQuestionText("Please select the group you want to add variants to.");

	groups = new LSearchComboBox("Existing Group", AV.AVFont, AV.yellow);
	groups.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	groups.putUnder(question, x, spacing);
	groups.addEnterButton("Next", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		WizNewPackage.newPackage.targetGroup = (PackageNode) groups.getSelectedItem();
		AV.wizVarPanel.open();
	    }
	});
	Add(groups);

	or = new LLabel("Or add a new group:", AV.AVFont, AV.green);
	or.addShadow();
	or.centerOn(groups, groups.getBottom() + 50);
	Add(or);

	newGroupField = new LTextField("New Group", AV.AVFont, AV.yellow);
	newGroupField.putUnder(or, x, spacing);
	newGroupField.setSize(settingsPanel.getWidth() - 2 * x, 50);
	newGroupField.addEnterButton("Next", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		String trimmed = newGroupField.getText().trim();
		for (PackageNode p : WizNewPackage.newPackage.targetSet.getAll(PackageNode.Type.VARGROUP)) {
		    if (p.src.getName().equalsIgnoreCase(trimmed)) {
			trimmed = "";
			break;
		    }
		}
		if (!trimmed.equals("")) {
		    File f = new File(WizNewPackage.newPackage.targetSet.src.getPath() + "\\" + trimmed);
		    Ln.makeDirs(f);
		    PackageNode packageNode = new PackageNode(f, PackageNode.Type.VARGROUP);
		    WizNewPackage.newPackage.targetGroup = packageNode;
		    AV.wizVarPanel.open();
		} else {
		    newGroupField.highlightChanged();
		}
	    }
	});
	Add(newGroupField);
    }

    @Override
    public void onOpen(SPMainMenuPanel parent_) {
	groups.reset();
	loadGroups();
	editing.load(WizNewPackage.newPackage.targetPackage
		, WizNewPackage.newPackage.targetSet
		, null
		, null);
	mainHelp();
    }

    public void mainHelp() {
	SUMGUI.helpPanel.setDefaultPos();
	SUMGUI.helpPanel.setTitle("Variant Groups");
	SUMGUI.helpPanel.setContent("Variant Groups are an optional but powerful part of AV Packages.  Variants in separate groups will get \"multiplied\" together"
		+ " to yield one variant of each combination.  While the names of your groups don't matter, it is standard to name them things like \"Skin\" or \"Eyes\".\n\n"
		+ "An example of how Groups are used to make your life easier: You could make one Group called \"Skin\" and put 5 variants with skin textures in them, "
		+ "and then another Group called \"Eyes\" with 5 variants with eye textures.  The end result will be 25 variants, one variant for each combination of "
		+ "Skin + Eye textures.\n\n"
		+ "You can see a good real world example of Groups at work in Bellyache's Werewolf Set.\n\n"
		+ "NOTE:  If you just want a standard setup, just make a single Group named anything and press next.");
	SUMGUI.helpPanel.hideArrow();
    }

    public void loadGroups() {
	groups.setSelectedIndex(0);
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		groups.removeAllItems();
	    }
	});
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		for (PackageNode p : WizNewPackage.newPackage.targetSet.getAll(PackageNode.Type.VARGROUP)) {
		    groups.addItem(p);
		}
	    }
	});
    }
}
