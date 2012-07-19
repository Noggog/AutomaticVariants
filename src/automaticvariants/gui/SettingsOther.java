/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVSaveFile;
import automaticvariants.AVSaveFile.Settings;
import lev.gui.LCheckBox;
import lev.gui.LComboBox;
import lev.gui.LLabel;
import lev.gui.LNumericSetting;
import skyproc.gui.SUMGUI;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingDefaultsPanel;

/**
 *
 * @author Justin Swanson
 */
public class SettingsOther extends SPSettingDefaultsPanel {

    LLabel debugLabel;
    LComboBox debugLevel;
    LCheckBox importOnStartup;
    LCheckBox minimize;
    LNumericSetting maxMem;

    public SettingsOther(SPMainMenuPanel parent_) {
	super(parent_, "Other Settings", AV.orange, AV.save);
    }

    @Override
    protected void initialize() {
	super.initialize();

	importOnStartup = new LCheckBox("Import Mods on Startup", AV.AVFont, AV.yellow);
	importOnStartup.tie(AVSaveFile.Settings.IMPORT_AT_START, saveFile, SUMGUI.helpPanel, true);
	importOnStartup.setOffset(2);
	importOnStartup.addShadow();
	setPlacement(importOnStartup);
	AddSetting(importOnStartup);

	minimize = new LCheckBox("Minimize Patch", AV.AVFont, AV.yellow);
	minimize.tie(AVSaveFile.Settings.MINIMIZE_PATCH, saveFile, SUMGUI.helpPanel, true);
	minimize.setOffset(2);
	minimize.addShadow();
	setPlacement(minimize);
	AddSetting(minimize);

	debugLabel = new LLabel("Debug Level", AV.AVFont, AV.yellow);

	debugLevel = new LComboBox("Debug Level");
	debugLevel.setSize(150, 25);
	debugLevel.addItem("Off");
	debugLevel.addItem("AV Debug");
	debugLevel.addItem("AV + SkyProc Debug");
	debugLevel.tie(Settings.DEBUG_LEVEL, saveFile, SUMGUI.helpPanel, true);
	setPlacement(debugLevel, last.x + debugLabel.getWidth() + 15, last.y);
	AddSetting(debugLevel);

	debugLabel.setLocation(debugLevel.getX() - debugLabel.getWidth() - 15, debugLevel.getY());
	debugLabel.addShadow();
	Add(debugLabel);

	maxMem = new LNumericSetting("Max Allocated Memory",
		AV.AVFont, AV.yellow, 250, 2000, 250);
	maxMem.tie(Settings.MAX_MEM, saveFile, SUMGUI.helpPanel, true);
	setPlacement(maxMem);
	AddSetting(maxMem);

	alignRight();

    }
}
