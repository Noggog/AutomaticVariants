/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.PackageNode;
import automaticvariants.SpecFile;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JOptionPane;
import lev.gui.LButton;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class PackagesSpecs extends SPSettingPanel {

    PackageEditing editing;
    LButton saveSpec;
    LButton cancel;
    SpecFile target;

    public PackagesSpecs(SPMainMenuPanel parent_, String title) {
	super(parent_, title, AV.orange);
    }

    @Override
    protected void initialize() {
	super.initialize();

	last = new Point(last.x, last.y + 15);

	editing = new PackageEditing(settingsPanel);
	editing.setLocation(0, header.getBottom());
	Add(editing);

	saveSpec = new LButton("Save");
	cancel = new LButton("Cancel");
	saveSpec.setLocation(getSpacing(saveSpec, cancel, true));
	saveSpec.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent arg0) {
		save();
	    }
	});
	settingsPanel.add(saveSpec);

	cancel.setLocation(getSpacing(saveSpec, cancel, false));
	settingsPanel.add(cancel);

    }

    @Override
    public void onOpen(SPMainMenuPanel parent_) {
	cancel.addActionListener(AV.packagesManagerPanel.getOpenHandler());
	saveSpec.addActionListener(AV.packagesManagerPanel.getOpenHandler());
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
