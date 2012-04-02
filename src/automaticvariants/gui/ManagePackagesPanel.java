/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import lev.gui.LSwingTree;

/**
 *
 * @author Justin Swanson
 */
public class ManagePackagesPanel extends DefaultsPanel {

    LSwingTree activePackages;

    public ManagePackagesPanel(EncompassingPanel parent_) {
	super("Texture Variants", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    activePackages = new LSwingTree(AVGUI.middleDimensions.width - 30,
		    (AVGUI.middleDimensions.height - 200 ) * 3 / 5  );
	    activePackages.setLocation(AVGUI.middleDimensions.x 
		    + AVGUI.middleDimensions.width / 2 - activePackages.getWidth() / 2, 120);
	    activePackages.setMargin(10, 5);
	    activePackages.removeBorder();
	    add(activePackages);

	    return true;
	}
	return false;
    }
}
