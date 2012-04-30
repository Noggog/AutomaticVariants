/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import skyproc.gui.SPSettingPanel;
import skyproc.gui.SPMainMenuPanel;
import automaticvariants.AV;
import automaticvariants.AVFileVars;
import automaticvariants.AVSaveFile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import lev.gui.LButton;
import lev.gui.LCheckBox;
import skyproc.gui.SPComplexGUI;

/**
 *
 * @author Justin Swanson
 */
public class SettingsPackagesOther extends SPSettingPanel {

    LButton gatherAndExit;
    LCheckBox prepAV;
    LCheckBox origAsVar;
    LButton packageManager;

    public SettingsPackagesOther(SPMainMenuPanel parent_) {
	super("Texture Variants", AV.save, parent_, AV.orange);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    save.setVisible(false);
	    defaults.setVisible(false);

	    prepAV = new LCheckBox ("Prep AV", AV.settingsFont, AV.yellow);
	    prepAV.setOffset(0);
	    prepAV.tie(AVSaveFile.Settings.PACKAGES_PREP, AV.save, SPComplexGUI.helpPanel, true);
	    prepAV.addShadow();
	    last = setPlacement(prepAV, last);
	    Add(prepAV);

	    origAsVar = new LCheckBox ("Original As Variant", AV.settingsFont, AV.yellow);
	    origAsVar.setOffset(0);
	    origAsVar.tie(AVSaveFile.Settings.PACKAGES_ORIG_AS_VAR, AV.save, SPComplexGUI.helpPanel, true);
	    origAsVar.addShadow();
	    last = setPlacement(origAsVar, last);
	    Add(origAsVar);

	    gatherAndExit = new LButton("Gather Files and Exit");
	    gatherAndExit.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    AVFileVars.shufflePackages();
		    AVFileVars.gatherFiles();
		    SPComplexGUI.exitProgram();
		}
	    });
	    gatherAndExit.linkTo(AVSaveFile.Settings.PACKAGES_GATHER, saveFile, SPComplexGUI.helpPanel, true);
	    last = setPlacement(gatherAndExit, last);
	    Add(gatherAndExit);

	    packageManager = new LButton("Package Manager");
	    packageManager.centerIn(settingsPanel, defaults.getY());
	    settingsPanel.add(packageManager);

	    alignRight();

	    return true;
	}
	return false;
    }

    @Override
    public void specialOpen(SPMainMenuPanel parent) {
	packageManager.addActionListener(AV.packagesManagerPanel.getOpenHandler(parent));
    }


}
