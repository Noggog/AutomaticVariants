/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.PackageNode;
import automaticvariants.SpecFile;
import java.io.IOException;
import javax.swing.JOptionPane;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class PackagesSpecs extends WizTemplate {

    SpecFile target;

    public PackagesSpecs(SPMainMenuPanel parent_, String title) {
	super(parent_, title, AV.packagesManagerPanel, null);
    }

    @Override
    protected void initialize() {
	super.initialize();

	spacing = 12;
	nextButton.setText("Save");
	nextButton.setSize(45, nextButton.getHeight());
	nextButton.setLocation(settingsPanel.getWidth() - nextButton.getWidth() - 15, nextButton.getY());
    }

    @Override
    public void onNext() {
	save();
    }

    public void save() {
	if (target == null) {
	    return;
	}
	try {
	    SUMGUI.setPatchNeeded(true);
	    target.export();
	} catch (IOException ex) {
	    JOptionPane.showMessageDialog(null, "There was an error exporting the spec file, please contact Leviathan1753");
	}
    }

    public void load(PackageNode n) {
	editing.load(n);
    }
}
