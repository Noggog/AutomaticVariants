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
import lev.gui.*;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPQuestionPanel;

/**
 *
 * @author Justin Swanson
 */
public class WizPackages extends SPQuestionPanel {

    LSearchComboBox packages;
    LLabel or;
    LLabel newPackage;
    LTextField newPackageField;
    LButton nextNew;

    public WizPackages(SPMainMenuPanel parent_) {
	super(parent_, "Choose Package", AV.orange, AV.packagesManagerPanel, null, null);
    }

    @Override
    protected void initialize() {
	super.initialize();

	spacing = 25;
	int x = 15;
	int fieldHeight = 65;

	setQuestionFont(AV.AVFont);
	setQuestionCentered();
	setQuestionColor(AV.green);
	setQuestionText("Please select the package\n"
		+ "or make a new package\n"
		+ "for this variant.");

	packages = new LSearchComboBox("Existing Package", AV.AVFont, AV.yellow);
	packages.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	packages.putUnder(question, x, spacing);
	packages.addEnterButton("Next", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		WizNewPackage.newPackage.targetPackage = (PackageNode) packages.getSelectedItem();
		AV.wizSetPanel.open();
	    }
	});
	updateLast(packages);
	Add(packages);

	or = new LLabel("-OR-", AV.AVFont, AV.green);
	or.addShadow();
	setPlacement(or);
	Add(or);

	newPackageField = new LTextField("New Package", AV.AVFont, AV.yellow);
	newPackageField.putUnder(or, x, spacing);
	newPackageField.setSize(settingsPanel.getWidth() - 2 * x, 50);
	newPackageField.addEnterButton("Next", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		String trimmed = newPackageField.getText().trim();
		if (!trimmed.equals("")) {
		    File f = new File(AVFileVars.AVPackagesDir + newPackageField.getText());
		    Ln.makeDirs(f);
		    PackageNode packageNode = new PackageNode(f, PackageNode.Type.PACKAGE);
		    WizNewPackage.newPackage.targetPackage = packageNode;
		    AV.wizSetPanel.open();
		}
	    }
	});
	Add(newPackageField);

    }

    public void loadPackages() {
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		packages.removeAllItems();
	    }
	});
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		for (PackageNode p : AVFileVars.AVPackages.getAll(PackageNode.Type.PACKAGE)) {
		    packages.addItem(p);
		}
	    }
	});
    }

    @Override
    public void onOpen(SPMainMenuPanel parent_) {
	WizNewPackage.newPackage = new WizNewPackage();
	loadPackages();
	newPackageField.setText("");
	packages.reset();
    }
}
