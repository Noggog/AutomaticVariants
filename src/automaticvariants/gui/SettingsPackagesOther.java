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

/**
 *
 * @author Justin Swanson
 */
public class SettingsPackagesOther extends DefaultsPanel {

    LButton gatherAndExit;
    LCheckBox prepAV;
    LButton packageManager;

    public SettingsPackagesOther(EncompassingPanel parent_) {
	super("Texture Variants", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    save.setVisible(false);
	    defaults.setVisible(false);

	    prepAV = new LCheckBox ("Prep AV", AVGUI.settingsFont, AVGUI.yellow);
	    prepAV.setOffset(0);
	    prepAV.tie(AVSaveFile.Settings.PACKAGES_PREP, AV.save, parent.helpPanel, true);
	    last = setPlacement(prepAV, last);
	    Add(prepAV);

	    gatherAndExit = new LButton("Gather Files and Exit");
	    gatherAndExit.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    AVFileVars.shufflePackages();
		    AVFileVars.gatherFiles();
		    AV.exitProgram();
		}
	    });
	    gatherAndExit.linkTo(AVSaveFile.Settings.PACKAGES_GATHER, saveFile, parent.helpPanel, true);
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
    public void specialOpen(EncompassingPanel parent) {
	packageManager.addActionListener(((SettingsMainMenu)parent).managePackagesPanel.getOpenHandler(parent));
    }


}
