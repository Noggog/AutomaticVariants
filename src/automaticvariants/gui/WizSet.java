/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import automaticvariants.PackageNode;
import lev.gui.LButton;
import lev.gui.LComboBox;
import lev.gui.LLabel;
import lev.gui.LTextField;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPQuestionPanel;

/**
 *
 * @author Justin Swanson
 */
public class WizSet extends SPQuestionPanel {

    LComboBox packages;
    LLabel existingPackage;
    LButton nextExisting;
    LLabel or;
    LLabel newPackage;
    LTextField newPackagePicker;
    LButton nextNew;

    public WizSet(SPMainMenuPanel parent_) {
	super(parent_, "Choose Target Skins", AV.orange, AV.save);
    }

    @Override
    protected void initialize() {
	super.initialize();

	spacing = 30;
	int x = 25;

	setQuestionFont(AV.AVFont);
	setQuestionCentered();
	setQuestionColor(AV.yellow);
	setQuestionText("Please select the package\n"
		+ "or make a new package\n"
		+ "for this variant.");

	existingPackage = new LLabel("Existing Package", AV.AVFont, AV.yellow);
	existingPackage.putUnder(question, x, spacing);
	Add(existingPackage);

	packages = new LComboBox("Package Picker");
	packages.setSize(250, 40);
	packages.putUnder(existingPackage, existingPackage.getX(), 10);
	updateLast(packages);
	Add(packages);

	nextExisting = new LButton("Next");
	nextExisting.centerOn(settingsPanel.getWidth() - nextExisting.getWidth() - 10, packages);
	packages.setSize(nextExisting.getX() - packages.getX() - 10, packages.getHeight());
	Add(nextExisting);

	or = new LLabel ("-OR-", AV.AVFont, AV.yellow);
	setPlacement(or);
	Add(or);

	newPackage = new LLabel("New Package", AV.AVFont, AV.yellow);
	newPackage.putUnder(or, x, spacing);
	Add(newPackage);

	newPackagePicker = new LTextField("New Package Field");
	newPackagePicker.putUnder(newPackage, newPackage.getX(), 10);
	Add(newPackagePicker);

	nextNew = new LButton("Next");
	nextNew.centerOn(settingsPanel.getWidth() - nextNew.getWidth() - 10, newPackagePicker);
	newPackagePicker.setSize(nextNew.getX() - newPackagePicker.getX() - 10, newPackagePicker.getHeight());
	Add(nextNew);

	loadPackages();
    }

    public void loadPackages (){
	packages.removeAllItems();
	for (PackageNode p : AVFileVars.AVPackages.getAll(PackageNode.Type.PACKAGE)) {
	    packages.addItem(p);
	}
    }

}
