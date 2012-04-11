/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVSaveFile.Settings;
import java.awt.Dimension;
import java.awt.Point;
import lev.gui.LPanel;

/**
 *
 * @author Justin Swanson
 */
public class SettingsMainMenu extends EncompassingPanel {

    private LPanel menu;
    public LMainMenuConfig managePackagesButton;
    SettingsPackagesPanel managePackagesPanel = new SettingsPackagesPanel(this);
    LMainMenuConfig manageHeightButton;
    SettingsHeightPanel manageHeightPanel = new SettingsHeightPanel(this);
    LMainMenuConfig manageSettingsButton;
    SettingsOther manageSettingsPanel = new SettingsOther(this);
    static int spacing = 35;

    public SettingsMainMenu(Dimension d) {
	super(d);

	menu = new LPanel(AVGUI.leftDimensions);

	managePackagesButton = new LMainMenuConfig("Textures", true, true, helpPanel, new Point(xPlacement, 170), AV.save, Settings.PACKAGES_ON);
	managePackagesButton.addActionListener(managePackagesPanel.getOpenHandler(this));
	menu.add(managePackagesButton);


	manageSettingsButton = new LMainMenuConfig("Other Settings", false, true, helpPanel, new Point(xPlacement, managePackagesButton.getY() + spacing), AV.save, Settings.AV_SETTINGS);
	manageSettingsButton.addActionListener(manageSettingsPanel.getOpenHandler(this));
	menu.add(manageSettingsButton);

	manageHeightButton = new LMainMenuConfig("Height", true, true, helpPanel, new Point(xPlacement, manageSettingsButton.getY() + spacing), AV.save, Settings.HEIGHT_ON);
	manageHeightButton.addActionListener(manageHeightPanel.getOpenHandler(this));
//	menu.add(manageHeightButton);
    }

    @Override
    public void open() {
	removeAll();
	add(menu);
	super.open();
	helpPanel.clearBottomArea();
    }
}
