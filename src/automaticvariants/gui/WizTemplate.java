/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPQuestionPanel;
import skyproc.gui.SPSettingPanel;

/**
 *
 * @author Justin Swanson
 */
public class WizTemplate extends SPQuestionPanel {

    int x = 15;
    int fieldHeight = 65;
    PackageEditing editing;

    public WizTemplate(SPMainMenuPanel parent_, String title, SPSettingPanel cancel, SPSettingPanel back) {
	super(parent_, title, AV.orange, cancel, back);
    }

    @Override
    protected void initialize() {
	super.initialize();

	spacing = 25;

	editing = new PackageEditing(settingsPanel);
	editing.setLocation(0, header.getBottom());
	settingsPanel.add(editing);

	question.putUnder(editing, question.getX(), 0);
	setQuestionFont(AV.AVFont);
	setQuestionCentered();
	setQuestionColor(AV.green);

    }

    @Override
    public void onCancel() {
	WizNewPackage.newPackage.clear();
	WizNewPackage.open = false;
    }

}
