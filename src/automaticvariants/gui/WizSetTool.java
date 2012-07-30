/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import automaticvariants.VariantProfile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import lev.LMergeMap;
import lev.Ln;
import lev.gui.*;
import skyproc.*;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPProgressBarPlug;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class WizSetTool extends WizTemplate {

    LList<File> textures;
    LButton analyze;
    LCheckBox partialMatch;
    LLabel progressLabel;
    LProgressBar progress;
    LTextPane exception;
    LList<EDIDdisplay> seeds;
    Set<VariantProfile> matchingProfiles;
    Set<VariantProfile> halfMatchingProfiles;
    LCheckList potential;

    static int attempt = 1;

    public WizSetTool(SPMainMenuPanel parent_) {
	super(parent_, "Seed NPCs", AV.packagesManagerPanel, AV.wizSetPanel);
    }

    @Override
    protected void initialize() {
	super.initialize();

	spacing = 40;

	setQuestionText("Drop in and analyze textures to narrow down seed options.");

	textures = new LList<>("Textures to Analyze", AV.AVFont, AV.yellow);
	textures.setUnique(true);
	textures.addEnterButton("Add Texture", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		JFileChooser fd = new JFileChooser(lastQuery);
		fd.setMultiSelectionEnabled(true);
		File[] chosen = new File[0];
		if (fd.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
		    lastQuery = fd.getSelectedFile().getParentFile();
		    chosen = fd.getSelectedFiles();
		}
		for (File f : chosen) {
		    if (Ln.isFileType(f, "DDS")) {
			textures.addElement(f);
		    }
		}
	    }
	});
	textures.putUnder(question, x, spacing);
	textures.setSize(settingsPanel.getWidth() - x * 2, 250);
	Add(textures);

	analyze = new LButton("Analyze");
	analyze.setSize(100, analyze.getHeight());
	analyze.centerOn(textures, textures.getBottom() + 15);
	analyze.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (!textures.isEmpty()) {
		    displaySwitch(1);
		    SUMGUI.startImport(new Analyze(textures.getAll()));
		} else {
		    textures.highlightChanged();
		}
	    }
	});
	Add(analyze);

	partialMatch = new LCheckBox("Display Partially Matching", AV.AVFont, AV.yellow);
	partialMatch.addShadow();
	partialMatch.centerOn(analyze, analyze.getBottom() + spacing);
	Add(partialMatch);

	progressLabel = new LLabel("Loading up mods, please wait...", AV.AVFontSmall, AV.lightGray);
	progressLabel.centerIn(settingsPanel, question.getBottom() + spacing * 3);
	Add(progressLabel);

	progress = new LProgressBar(200, 20, AV.AVFontSmall, AV.lightGray);
	progress.centerIn(settingsPanel, progressLabel.getBottom() + 10);
	SPProgressBarPlug.addProgressBar(progress);
	Add(progress);

	exception = new LTextPane(settingsPanel.getWidth() - 30, 300, AV.yellow);
	exception.setText("An exception has occured.  Please submit a debug report to Leviathan.");
	exception.putUnder(question, 15, 50);
	Add(exception);

	seeds = new LList<>("Chosen Seed NPCs", AV.AVFont, AV.yellow);
	seeds.setSize(settingsPanel.getWidth() - 2 * x, 150);
	seeds.setLocation(x, backButton.getY() - seeds.getHeight() - 20);
	Add(seeds);

	SUMGUI.startImport();
    }

    @Override
    public void onOpen(SPMainMenuPanel parent) {
	mainHelp();
	editing.load(WizNewPackage.newPackage.targetPackage, WizNewPackage.newPackage.targetSet, null, null);
    }

    void reset() {
	textures.clearHighlight();
	textures.clear();
	displaySwitch(0);
    }

    void displaySwitch(final int stage) {
	SwingUtilities.invokeLater(new Runnable() {

	    public void run() {
		if (stage == 0) {
		    setQuestionText("Drop in and analyze your textures to narrow down skin options.");
		}
		if (stage == 2) {
		    setQuestionText("Pick a single seed from each armor piece that makes sense.");
		}
		textures.setVisible(stage == 0);
		analyze.setVisible(stage == 0);
		progress.setVisible(stage == 1);
		progressLabel.setVisible(stage == 1);
		seeds.setVisible(stage == 2);
		exception.setVisible(stage == 3);
		partialMatch.setVisible(stage == 2);
	    }
	});
    }

    void mainHelp() {
	if (AV.wizSetToolPanel.isVisible()) {
	    SUMGUI.helpPanel.setDefaultPos();
	    SUMGUI.helpPanel.setTitle("Skin Locator Tool");
	    SUMGUI.helpPanel.setContent("Pick all the skins that are used by NPCs that you want your variants to be added to.\n\n"
		    + "If you are not sure if your target NPCs have multiple skins, go back and use the AV tool.");
	    SUMGUI.helpPanel.hideArrow();


	}
    }

    class Analyze implements Runnable {

	ArrayList<File> files;

	public Analyze(ArrayList<File> in) {
	    files = in;
	}

	@Override
	public void run() {
	    try {
		AVFileVars.prepProfiles();
		SPGlobal.newLog("Set Tool/Run " + attempt++ + ".txt");
		printSourceTextures();

		matchingProfiles = new HashSet<>();
		halfMatchingProfiles = new HashSet<>();
		ArrayList<String> textureList = new ArrayList<>();
		for (File f : textures.getAll()) {
		    textureList.add(f.getName().toUpperCase());
		}

		for (VariantProfile profile : VariantProfile.profiles) {
		    matchingProfiles.add(profile);
		    for (String s : textureList) {
			if (profile.hasTexture(s)) {
			    halfMatchingProfiles.add(profile);
			} else {
			    matchingProfiles.remove(profile);
			}
		    }
		    halfMatchingProfiles.removeAll(matchingProfiles);
		}

		displaySwitch(2);
	    } catch (Exception e) {
		SPGlobal.logException(e);
		displaySwitch(3);
	    }
	}
    }

    void printSourceTextures() {
	SPGlobal.log("Set Tool", "Original files:");
	for (File f : textures.getAll()) {
	    SPGlobal.log("Set Tool", "  " + f.getPath());
	}
    }
}
