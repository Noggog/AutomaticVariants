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
import lev.Ln;
import lev.gui.LList;
import lev.gui.LTextField;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPQuestionPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WizVariant extends WizTemplate {

    LTextField nameField;
    LList<File> varTextures;

    public WizVariant(SPMainMenuPanel parent_) {
	super(parent_, "Create Variant", AV.packagesManagerPanel, AV.wizGroupPanel);
    }

    @Override
    protected void initialize() {
	super.initialize();

	setQuestionText("Name your new variant and add textures.");

	nameField = new LTextField("Variant Name", AV.AVFont, AV.yellow);
	nameField.putUnder(question, x, spacing);
	nameField.setSize(settingsPanel.getWidth() - 2 * x, 50);
	Add(nameField);

	varTextures = new LList<>("Variant Textures", AV.AVFont, AV.yellow);
	varTextures.setUnique(true);
	varTextures.addEnterButton("Add Texture", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		File[] chosen = Ln.fileDialog();
		for (File f : chosen) {
		    if (Ln.isFileType(f, "DDS")) {
			varTextures.addElement(f);
		    }
		}
	    }
	});
	varTextures.putUnder(nameField, x, spacing);
	varTextures.setSize(settingsPanel.getWidth() - x * 2, 250);
	Add(varTextures);
    }

    @Override
    public void onOpen(SPMainMenuPanel parent) {
	if (AV.wizVarPanel.isVisible()) {
	    SUMGUI.helpPanel.setDefaultPos();
	    SUMGUI.helpPanel.setTitle("Create Variant");
	    SUMGUI.helpPanel.setContent("Add the textures that make this variant unique from the others.");
	    SUMGUI.helpPanel.hideArrow();
	    editing.load(WizNewPackage.newPackage.targetGroup);
	}
    }

    @Override
    public boolean testNext() {
	boolean pass = true;
	if (nameField.getText().trim().equals("")) {
	    pass = false;
	    nameField.highlightChanged();
	}
	if (varTextures.getAll().isEmpty()) {
	    pass = false;
	    varTextures.highlightChanged();
	}
	return pass;
    }
}
