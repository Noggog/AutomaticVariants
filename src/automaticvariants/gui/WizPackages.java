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
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WizPackages extends WizTemplate {

    LSearchComboBox packages;
    LLabel or;
    LTextField newPackageField;

    public WizPackages(SPMainMenuPanel parent_) {
	super(parent_, "Choose Package", AV.packagesManagerPanel, null);
    }

    @Override
    protected void initialize() {
	super.initialize();

	question.putUnder(header, question.getX(), 0);
	setQuestionText("Please select the package to add your variant to.");

	packages = new LSearchComboBox("Existing Package", AV.AVFont, AV.yellow);
	packages.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	packages.putUnder(question, x, spacing);
	packages.addEnterButton("Next", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (!packages.isEmpty()) {
		    WizNewPackage.newPackage.targetPackage = (PackageNode) packages.getSelectedItem();
		    AV.wizSetPanel.open();
		    AV.wizSetPanel.reset();
		}
	    }
	});
	updateLast(packages);
	Add(packages);

	or = new LLabel("Or create a new package:", AV.AVFont, AV.green);
	or.addShadow();
	or.centerOn(packages, packages.getBottom() + 50);
	Add(or);

	newPackageField = new LTextField("New Package", AV.AVFont, AV.yellow);
	newPackageField.putUnder(or, x, spacing);
	newPackageField.setSize(settingsPanel.getWidth() - 2 * x, 50);
	newPackageField.addEnterButton("Next", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		String trimmed = newPackageField.getText().trim();
		if (!trimmed.equals("")) {
		    File f = new File(AVFileVars.AVPackagesDir + trimmed);
		    Ln.makeDirs(f);
		    PackageNode packageNode = new PackageNode(f, PackageNode.Type.PACKAGE);
		    WizNewPackage.newPackage.targetPackage = packageNode;
		    AV.wizSetPanel.open();
		    AV.wizSetPanel.reset();
		} else {
		    newPackageField.highlightChanged();
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
	editing.setVisible(false);
	SUMGUI.helpPanel.setTitle("AV Package");
	SUMGUI.helpPanel.setContent("An AV Package is a collection of textures that are organized in a way "
		+ "that AV can understand.\n\n"
		+ "A single package should contain all the variants from a single author.");
	SUMGUI.helpPanel.hideArrow();
	WizNewPackage.newPackage = new WizNewPackage();
	loadPackages();
	newPackageField.setText("");
	newPackageField.clearHighlight();
	packages.reset();
    }
}
