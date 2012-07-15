/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import automaticvariants.AVSaveFile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import lev.gui.LButton;
import lev.gui.LCheckBox;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class SettingsPackagesOther extends SPSettingPanel {

    LButton gatherAndExit;
    LCheckBox origAsVar;
    LButton packageManager;

    public SettingsPackagesOther(SPMainMenuPanel parent_) {
	super("Texture Variants", parent_, AV.orange, AV.save);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    save.setVisible(false);
	    defaults.setVisible(false);

	    origAsVar = new LCheckBox ("Original As Variant", AV.settingsFont, AV.yellow);
	    origAsVar.setOffset(0);
	    origAsVar.tie(AVSaveFile.Settings.PACKAGES_ORIG_AS_VAR, AV.save, SUMGUI.helpPanel, true);
	    origAsVar.addShadow();
	    setPlacement(origAsVar);
	    Add(origAsVar);

	    gatherAndExit = new LButton("Gather Files and Exit");
	    gatherAndExit.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    AVFileVars.shufflePackages();
		    AVFileVars.gatherFiles();
		    AV.gatheringAndExiting = true;
		    SUMGUI.exitProgram(false);
		}
	    });
	    gatherAndExit.linkTo(AVSaveFile.Settings.PACKAGES_GATHER, saveFile, SUMGUI.helpPanel, true);
	    setPlacement(gatherAndExit);
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
	packageManager.addActionListener(AV.packagesManagerPanel.getOpenHandler());
    }


}
