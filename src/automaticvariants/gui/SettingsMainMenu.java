/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import java.awt.Dimension;
import java.awt.Point;
import lev.gui.LPanel;

/**
 *
 * @author Justin Swanson
 */
public class SettingsMainMenu extends EncompassingPanel {

    private LPanel menu;
    LMainMenuConfig managePackagesButton;
    SettingsPackagesPanel managePackagesPanel = new SettingsPackagesPanel(this);
    LMainMenuConfig manageHeightButton;
    SettingsHeightPanel manageHeightPanel = new SettingsHeightPanel(this);

    static int spacing = 35;

    public SettingsMainMenu(Dimension d) {
	super(d);

	menu = new LPanel(AVGUI.leftDimensions);

	managePackagesButton = new LMainMenuConfig("Textures", true, helpPanel, new Point(xPlacement, 170), AV.save, AV.Settings.PACKAGES_ON);
	managePackagesButton.addActionListener(managePackagesPanel.getOpenHandler(this));
	menu.add(managePackagesButton);

	manageHeightButton = new LMainMenuConfig("Height", true, helpPanel, new Point(xPlacement, managePackagesButton.getY() + spacing), AV.save, AV.Settings.HEIGHT_ON);
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
