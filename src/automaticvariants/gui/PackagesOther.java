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
import skyproc.SPGlobal;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class PackagesOther extends SPSettingPanel {

    LButton gatherAndExit;
    LCheckBox origAsVar;
    LButton packageManager;

    public PackagesOther(SPMainMenuPanel parent_) {
	super(parent_, "Texture Variants", AV.orange);
    }

    @Override
    protected void initialize() {
	super.initialize();

	origAsVar = new LCheckBox("Original As Variant", AV.AVFont, AV.yellow);
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
		if (SPGlobal.logging()) {
		    SPGlobal.logMain("AV", "Closing program early because of gather and exit command.");
		}
		SUMGUI.exitProgram(false);
	    }
	});
	gatherAndExit.linkTo(AVSaveFile.Settings.PACKAGES_GATHER, AV.save, SUMGUI.helpPanel, true);
	setPlacement(gatherAndExit);
	Add(gatherAndExit);

	packageManager = new LButton("Package Manager");
	packageManager.centerIn(settingsPanel, settingsPanel.getHeight() - packageManager.getHeight() - 15);
	settingsPanel.add(packageManager);

	alignRight();

    }

    @Override
    public void onOpen(SPMainMenuPanel parent_) {
	packageManager.addActionListener(AV.packagesManagerPanel.getOpenHandler());
    }
}
