/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.PackageNode;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;
import lev.gui.LSearchComboBox;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPQuestionPanel;

/**
 *
 * @author Justin Swanson
 */
public class WizGroup extends SPQuestionPanel {

    LSearchComboBox groups;

    public WizGroup(SPMainMenuPanel parent_) {
	super(parent_, "Grouping", AV.orange, AV.packagesManagerPanel, AV.wizSetPanel, null);
    }

    @Override
    protected void initialize() {
	super.initialize();

	spacing = 15;
	int x = 15;
	int fieldHeight = 65;

	setQuestionFont(AV.AVFont);
	setQuestionCentered();
	setQuestionColor(AV.green);
	setQuestionText("Please select the group you want to add variants to.");

	groups = new LSearchComboBox("Existing Group", AV.AVFont, AV.yellow);
	groups.setSize(settingsPanel.getWidth() - x * 2, fieldHeight);
	groups.putUnder(question, x, spacing);
	groups.addEnterButton("Next", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		WizNewPackage.newPackage.targetGroup = (PackageNode) groups.getSelectedItem();
//		AV.wizGroupPanel.open();
	    }
	});
	Add(groups);
    }

    @Override
    public void onOpen(SPMainMenuPanel parent_) {
	groups.reset();
	loadGroups();
    }

    public void loadGroups() {
	groups.setSelectedIndex(0);
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		groups.removeAllItems();
	    }
	});
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		for (PackageNode p : WizNewPackage.newPackage.targetSet.getAll(PackageNode.Type.VARGROUP)) {
		    groups.addItem(p);
		}
	    }
	});
    }
}
