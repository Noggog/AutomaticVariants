/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.DataFormatException;
import javax.swing.SwingUtilities;
import lev.LMergeMap;
import lev.LShrinkArray;
import lev.Ln;
import lev.gui.*;
import skyproc.*;
import skyproc.BSA.FileType;
import skyproc.exceptions.BadParameter;
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
    LLabel progressLabel;
    LProgressBar progress;
    LLabel test;
    static LMergeMap<String, ARMO> usedNIFs = new LMergeMap<>(false);
    static Set<String> processed = new HashSet<>();
    static LMergeMap<File, File> SRCtoNIF = new LMergeMap<>(false);
    static LMergeMap<File, File> NIFtoSRC;

    public WizSetTool(SPMainMenuPanel parent_) {
	super(parent_, "Target Skins", AV.packagesManagerPanel, AV.wizSetPanel);
    }

    @Override
    protected void initialize() {
	super.initialize();

	spacing = 40;

	setQuestionText("Drop in and analyze your textures to narrow down skin options.");

	textures = new LList<>("Textures to Analyze", AV.AVFont, AV.yellow);
	textures.setUnique(true);
	textures.addEnterButton("Add Texture", new ActionListener() {

	    @Override
	    public void actionPerformed(ActionEvent e) {
		File[] chosen = Ln.fileDialog();
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

	progressLabel = new LLabel("Loading up mods, please wait...", AV.AVFontSmall, AV.lightGray);
	progressLabel.centerIn(settingsPanel, question.getBottom() + spacing * 3);
	Add(progressLabel);

	progress = new LProgressBar(200, 20, AV.AVFontSmall, AV.lightGray);
	progress.centerIn(settingsPanel, progressLabel.getBottom() + 10);
	SPProgressBarPlug.addProgressBar(progress);
	Add(progress);

	test = new LLabel("test", AV.AVFont, AV.yellow);
	Add(test);


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
		textures.setVisible(stage == 0);
		analyze.setVisible(stage == 0);
		progress.setVisible(stage == 1);
		progressLabel.setVisible(stage == 1);
		test.setVisible(stage == 2);
	    }
	});
    }

    void mainHelp() {
	if (AV.wizSetToolPanel.isVisible()) {
	    SUMGUI.helpPanel.setDefaultPos();
	    SUMGUI.helpPanel.setTitle("Skin Locator Tool");
	    SUMGUI.helpPanel.setContent("Pick all the skins that are used by NPCs that you want your variants to be added to.\n\n"
		    + WizSet.multiSkin() + "\n\n"
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
	    AVFileVars.locateUnused();
	    loadUsedNIFs();
	    checkLooseFiles();
	    checkBSAs();
	    NIFtoSRC = SRCtoNIF.flip();
	    locateAssociatedSkins();
	    displaySwitch(2);
	}
    }

    void loadUsedNIFs() {
	for (ARMO armo : AV.getMerger().getArmors()) {
	    if (!AVFileVars.unusedSkins.contains(armo.getForm())) {
		for (FormID piece : armo.getArmatures()) {
		    if (!AVFileVars.unusedPieces.get(armo.getForm()).contains(piece)) {
			ARMA arma = (ARMA) SPDatabase.getMajor(piece, GRUP_TYPE.ARMA);
			usedNIFs.put("MESHES\\" + arma.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).toUpperCase(), armo);
		    }
		}
	    }
	}
    }

    void checkLooseFiles() {
	ArrayList<File> meshes = Ln.generateFileList(new File(SPGlobal.pathToData + "MESHES\\"), false);
	SPProgressBarPlug.setMax(meshes.size());
	for (File f : meshes) {
	    String nifPath = f.getPath();
	    nifPath = nifPath.substring(nifPath.indexOf(SPGlobal.pathToData) + SPGlobal.pathToData.length());
	    nifPath = nifPath.toUpperCase();
	    if (usedNIFs.containsKey(nifPath)) {
		checkNif(f);
		processed.add(nifPath);
		SPProgressBarPlug.setStatus("Processed: " + f.getPath());
	    }
	    SPProgressBarPlug.incrementBar();
	}
	SPProgressBarPlug.done();
    }

    void checkBSAs() {
	//For ProgressBar
	int numFiles = 0;
	for (BSA b : BSA.loadInBSAs(FileType.NIF)) {
	    numFiles += b.numFiles();
	}
	SPProgressBarPlug.setMax(numFiles);

	for (BSA b : BSA.loadInBSAs(FileType.NIF)) {
	    for (String folderPath : b.getFiles().keySet()) {
		ArrayList<String> files = b.getFiles().get(folderPath);
		for (String fileName : files) {
		    if (fileName.endsWith(".NIF")) {
			String nifPath = folderPath + fileName;
			if (usedNIFs.containsKey(nifPath)) {
			    try {
				checkNif(new File(nifPath), b.getFile(folderPath + fileName));
				SPProgressBarPlug.setStatus("Processed: " + fileName);
			    } catch (IOException | DataFormatException ex) {
				SPGlobal.logException(ex);
			    }
			}
		    }
		    SPProgressBarPlug.incrementBar();
		}
	    }
	}
	SPProgressBarPlug.done();
    }

    void checkNif(File file) {
	try {
	    checkNif(file, new LShrinkArray(file));
	} catch (IOException ex) {
	    SPGlobal.logException(ex);
	}
    }

    void checkNif(File file, LShrinkArray in) {
	NIF nif;
	try {
	    nif = new NIF(file.getPath(), in);
	    ArrayList<ArrayList<String>> nifTextures = new ArrayList<>();

	    ArrayList<ArrayList<NIF.Node>> NiTriShapes = nif.getNiTriShapePackages();
	    for (ArrayList<NIF.Node> nodes : NiTriShapes) {
		for (NIF.Node n : nodes) {
		    if (n.type == NIF.NodeType.BSSHADERTEXTURESET) {
			nifTextures.add(NIF.extractBSTextures(n));
		    }
		}
	    }

	    for (ArrayList<String> list : nifTextures) {
		for (String nifPath : list) {
		    for (File src : textures.getAll()) {
			if (nifPath.toUpperCase().contains(src.getName().toUpperCase())) {
			    SRCtoNIF.put(src, file);
			}
		    }
		}
	    }

	} catch (BadParameter | java.nio.BufferUnderflowException ex) {
	    SPGlobal.logException(ex);
	}
    }

    void locateAssociatedSkins() {
	LMergeMap<ARMO, File> associatedSkins = new LMergeMap<>(false);
	for (File nif : NIFtoSRC.keySet()) {
//	    usedNIFs.get(nif.getPath())
	}
    }
}
