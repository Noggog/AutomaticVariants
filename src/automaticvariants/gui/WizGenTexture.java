/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import lev.Ln;
import lev.gui.LList;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPQuestionPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WizGenTexture extends WizTemplate {

    LList<File> genTextures;

    public WizGenTexture(SPMainMenuPanel parent_) {
	super(parent_, "Common Textures", AV.packagesManagerPanel, AV.wizSetPanel);
    }

    @Override
    protected void initialize() {
	super.initialize();

	setNext(AV.wizGroupPanel);

	setQuestionText("Add any textures that are shared between ALL variants in your set.");

	genTextures = new LList<>("Common Textures", AV.AVFont, AV.yellow);
	genTextures.setUnique(true);
	genTextures.addEnterButton("Add Texture", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		File[] chosen = Ln.fileDialog();
		for (File f : chosen) {
		    if (Ln.isFileType(f, "DDS")) {
			genTextures.addElement(f);
		    }
		}
	    }
	});
	genTextures.putUnder(question, x, spacing);
	genTextures.setSize(settingsPanel.getWidth() - x * 2, 250);
	Add(genTextures);
    }

    @Override
    public void onNext() {
	WizNewPackage.newPackage.genTextures = genTextures.getAll();
    }

    public void reset() {
	genTextures.clear();
    }

    @Override
    public void onOpen(SPMainMenuPanel parent) {
	if (AV.wizGenPanel.isVisible()) {
	    editing.load(WizNewPackage.newPackage.targetPackage
		    , WizNewPackage.newPackage.targetSet
		    , null
		    , null);
	    SUMGUI.helpPanel.setDefaultPos();
	    SUMGUI.helpPanel.setTitle("Common Textures");
	    SUMGUI.helpPanel.setContent("Sometimes you have common textures that each variant will use.  A good example "
		    + "of this might be a custom normals texture that you only made one of, but you want each variant to have."
		    + "  Rather than adding it to each variant manually, you can add it here.\n\n"
		    + "Just press next if you don't have any common files.");
	    SUMGUI.helpPanel.hideArrow();
	}
    }
}
