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
    private LMainMenuConfig manageHeightButton;
    private ManageHeightVariants manageHeightPanel = new ManageHeightVariants(this);
//    private LButton done;

    public SettingsMainMenu(Dimension d) {
	super(d);

	menu = new LPanel(AVGUI.leftDimensions);

	manageHeightButton = new LMainMenuConfig("Height Variance", true, helpPanel, new Point(xPlacement, 170), AV.settings, AV.Settings.HEIGHT_ON);
	manageHeightButton.addActionListener(manageHeightPanel.getOpenHandler(this));
	menu.add(manageHeightButton);



//	    done = new DLLbutton("Done");
//	    done.setSize(90, 45);
//	    done.setLocation(xPlacement - done.getWidth(), debugButton.getY() + defaultSpacing);
//	    done.addActionListener(closeHandler);
//	    menu.add(done);

    }

    @Override
    public void open() {
	removeAll();
	add(menu);
	super.open();
	helpPanel.clearBottomArea();
    }
}
