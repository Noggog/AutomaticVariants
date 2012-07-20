/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.SpecFile;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JOptionPane;
import lev.gui.LButton;
import lev.gui.LLabel;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class PackagesSpecs extends SPSettingPanel {

    LLabel editing;
    LLabel packageName;
    LLabel variantName;
    LButton saveSpec;
    LButton cancel;
    SpecFile target;

    public PackagesSpecs(SPMainMenuPanel parent_, String title) {
	super(parent_, title, AV.orange);
    }

    @Override
    protected void initialize() {
	super.initialize();

	editing = new LLabel("EDITING", AV.AVFont, AV.green);
	editing.addShadow();
	editing.setLocation(15, 55);
	Add(editing);

	packageName = new LLabel("Test", AV.AVFontSmall, Color.LIGHT_GRAY);
	packageName.setLocation(0, 55);
	Add(packageName);

	variantName = new LLabel("Test", AV.AVFontSmall, Color.LIGHT_GRAY);
	variantName.setLocation(0, 68);
	Add(variantName);

	last = new Point(last.x, last.y + 15);


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
    public void specialOpen(SPMainMenuPanel parent_) {
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

    public void load(String name, SpecFile spec) {
	packageName.setText(name.substring(0, name.indexOf(" - ")));
	variantName.setText(name.substring(name.indexOf(" - ") + 3));
	int totalLength;
	if (variantName.getWidth() > packageName.getWidth()) {
	    totalLength = variantName.getWidth();
	} else {
	    totalLength = packageName.getWidth();
	}
	totalLength += editing.getWidth() + 10;
	editing.setLocation(settingsPanel.getWidth() / 2 - totalLength / 2, editing.getY());
	packageName.setLocation(editing.getX() + editing.getWidth() + 10, packageName.getY());
	variantName.setLocation(packageName.getX(), variantName.getY());

    }
}
