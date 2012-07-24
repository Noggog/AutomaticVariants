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
import java.util.*;
import java.util.zip.DataFormatException;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import lev.LMergeMap;
import lev.LShrinkArray;
import lev.Ln;
import lev.gui.*;
import skyproc.BSA.FileType;
import skyproc.*;
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
    LTextPane exception;
    LList<EDIDdisplay> seeds;
    LLabel armorPieceTitle;
    LLabel armorPiece;
    LComboSearchBox<EDIDdisplay> potential;
    static Set<String> usedNIFs;
    static Set<String> processed;
    static LMergeMap<File, File> SRCtoNIF;
    static LMergeMap<File, File> NIFtoSRC;
    LMergeMap<FormID, FormID> ARMAtoNPC;
    static int attempt = 1;

    public WizSetTool(SPMainMenuPanel parent_) {
	super(parent_, "Seed NPCs", AV.packagesManagerPanel, AV.wizSetPanel);
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

	armorPieceTitle = new LLabel("Tmp", AV.AVFont, AV.yellow);
	armorPieceTitle.setLocation(x, 150);
	armorPieceTitle.addShadow();
	Add(armorPieceTitle);

	armorPiece = new LLabel("Tmp", AV.AVFont, AV.orange);
	armorPiece.putUnder(armorPieceTitle, x, 0);
	armorPiece.addShadow();
	Add(armorPiece);

	potential = new LComboSearchBox<>("Potential Seeds", AV.AVFont, AV.yellow);
	potential.setSize(settingsPanel.getWidth() - 2 * x, 65);
	potential.putUnder(armorPiece, x, 25);
	Add(potential);

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
	clearData();
    }

    void clearData() {
	usedNIFs = new HashSet<>();
	processed = new HashSet<>();
	SRCtoNIF = new LMergeMap<>(false);
	NIFtoSRC = new LMergeMap<>(false);
	ARMAtoNPC = new LMergeMap<>(false);
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
		armorPieceTitle.setVisible(stage == 2);
		armorPiece.setVisible(stage == 2);
		potential.setVisible(stage == 2);
	    }
	});
    }

    void armorPiece(int cur) {
	if (cur < ARMAtoNPC.size()) {
	    armorPieceTitle.setText("For Armor Piece (" + (cur + 1) + "/" + ARMAtoNPC.size() + "):");
	    FormID armaForm = (FormID) ARMAtoNPC.keySet().toArray()[cur];
	    ARMA arma = (ARMA) SPDatabase.getMajor(armaForm, GRUP_TYPE.ARMA);
	    armorPiece.setText(arma.getEDID());
	    potential.removeAllItems();
	    for (FormID npcForm : ARMAtoNPC.get(armaForm)) {
		NPC_ npc = (NPC_) SPDatabase.getMajor(npcForm, GRUP_TYPE.NPC_);
		potential.addItem(new EDIDdisplay(npc));
	    }
	}
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
	    try {
		AVFileVars.locateUnused();
		SPGlobal.newLog("Set Tool/Run " + attempt++ + ".txt");
		printSourceTextures();
		loadUsedNIFs();
		checkLooseFiles();
		checkBSAs();

		SPGlobal.log("SrcToNifs", "Printing Sources to their Nifs:");
		for (String s : SRCtoNIF.print()) {
		    SPGlobal.log("SrcToNifs", s);
		}
		NIFtoSRC = SRCtoNIF.flip();

		work();
		displaySwitch(2);
	    } catch (Exception e) {
		SPGlobal.logException(e);
		displaySwitch(3);
	    }
	}

	void printSourceTextures() {
	    SPGlobal.log("Set Tool", "Original files:");
	    for (File f : textures.getAll()) {
		SPGlobal.log("Set Tool", "  " + f.getPath());
	    }
	}
    }

    void loadUsedNIFs() {
	for (ARMO armo : AV.getMerger().getArmors()) {
	    if (!AVFileVars.unusedSkins.contains(armo.getForm())) {
		for (FormID piece : armo.getArmatures()) {
		    if (!AVFileVars.unusedPieces.get(armo.getForm()).contains(piece)) {
			ARMA arma = (ARMA) SPDatabase.getMajor(piece, GRUP_TYPE.ARMA);
			usedNIFs.add("MESHES\\" + arma.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).toUpperCase());
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
	    if (usedNIFs.contains(nifPath)) {
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
			if (!processed.contains(nifPath) && usedNIFs.contains(nifPath)) {
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

    // Copied from my very old analyzer and spruced up.  Pretty ugly.
    void work() {

	// ARMA
	LMergeMap<File, FormID> NIFToARMAs = new LMergeMap<>(false);
	for (ARMA arma : AV.getMerger().getArmatures()) {
	    File model = new File("MESHES/" + arma.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).toUpperCase());

	    if (NIFtoSRC.containsKey(model)) {

		// Check for alt textures
		ArrayList<ARMA.AltTexture> altTextures = arma.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON);
		Set<File> altSrcs = new HashSet<File>();
		if (!altTextures.isEmpty()) {
		    for (ARMA.AltTexture t : altTextures) {
			TXST txst = (TXST) SPDatabase.getMajor(t.getTexture(), GRUP_TYPE.TXST);
			for (String path : txst) {
			    if (path != null) {
				File pathFile = new File(path);
				for (File src : textures.getAll()) {
				    if (pathFile.getName().equalsIgnoreCase(src.getName())) {
					altSrcs.add(src);
				    }
				}
			    }
			}
		    }
		}

		if (altSrcs.isEmpty()) {
		    NIFToARMAs.put(model, arma.getForm());
		} else {
		    // Split variant
		    File altModel = new File(model.getPath() + "ALT-" + arma.getFormStr());
		    for (File altSrc : altSrcs) {
			NIFtoSRC.put(altModel, altSrc);
		    }
		    NIFToARMAs.put(altModel, arma.getForm());
		}
	    }

	}
	Map<FormID, File> ARMAtoNIF = NIFToARMAs.flip().flatten();
	SPGlobal.log("ARMAs", "Printing ARMAs to their sources:");
	for (FormID arma : ARMAtoNIF.keySet()) {
	    SPGlobal.log("Set Tool", SPDatabase.getMajor(arma, GRUP_TYPE.ARMA).toString());
	    SPGlobal.log("Set Tool", "  " + ARMAtoNIF.get(arma));
	}

	// ARMO
	LMergeMap<FormID, FormID> srcToARMOs = new LMergeMap<>(false);
	for (ARMO armo : AV.getMerger().getArmors()) {
	    for (FormID armaf : armo.getArmatures()) {
		if (ARMAtoNIF.containsKey(armaf)) {
		    ARMA arma = (ARMA) SPDatabase.getMajor(armaf);
		    if (armo.getRace().equals(arma.getRace())) {
			srcToARMOs.put(armaf, armo.getForm());
		    }
		}
	    }
	}
	Map<FormID, FormID> ARMOtoARMA = srcToARMOs.flip().flatten();
	SPGlobal.log("ARMOs", "Printing ARMOs to their sources:");
	for (FormID armo : ARMOtoARMA.keySet()) {
	    SPGlobal.log("Set Tool", SPDatabase.getMajor(armo, GRUP_TYPE.ARMO).toString());
	    SPGlobal.log("Set Tool", "  " + SPDatabase.getMajor(ARMOtoARMA.get(armo), GRUP_TYPE.ARMA));
	}

	// NPCs
	LMergeMap<FormID, FormID> ARMOToNPC = new LMergeMap<>(false);
	for (NPC_ npc : AV.getMerger().getNPCs()) {
	    if ((!npc.isTemplated() || !npc.get(NPC_.TemplateFlag.USE_TRAITS)) // Make sure NPC isn't templated with traits
		    && !npc.getEDID().toUpperCase().contains("AUDIO")) // Just skip audio NPCs
	    {
		FormID skin = AVFileVars.getUsedSkin(npc);
		if (ARMOtoARMA.containsKey(skin)) {
		    ARMOToNPC.put(skin, npc.getForm());
		}
	    }
	}
	Map<FormID, FormID> NPCtoARMO = ARMOToNPC.flip().flatten();
	SPGlobal.log("NPCs", "Printing NPCs to their sources:");
	for (FormID npc : NPCtoARMO.keySet()) {
	    SPGlobal.log("Set Tool", SPDatabase.getMajor(npc, GRUP_TYPE.NPC_).toString());
	    SPGlobal.log("Set Tool", "  " + SPDatabase.getMajor(NPCtoARMO.get(npc), GRUP_TYPE.ARMO));
	}

	ARMAtoNPC = new LMergeMap<>(false);
	for (FormID npc : NPCtoARMO.keySet()) {
	    FormID armo = NPCtoARMO.get(npc);
	    FormID arma = ARMOtoARMA.get(armo);
	    if (ARMAtoNPC.containsKey(arma)) {
		ARMAtoNPC.put(arma, npc);
	    } else {
		ARMA ARMAtest = (ARMA) SPDatabase.getMajor(arma, GRUP_TYPE.ARMA);
		boolean added = false;
		SPGlobal.log("Linking", "================================================");
		SPGlobal.log("Linking", "Arma test: " + ARMAtest);
		// Check to see if there's already a matching ARMA
		for (FormID armaK : ARMAtoNPC.keySet()) {
		    ARMA ARMAkey = (ARMA) SPDatabase.getMajor(armaK, GRUP_TYPE.ARMA);
		    SPGlobal.log("Linking", "  Arma key: " + ARMAkey);
		    //If have same model path
		    if (ARMAtest.getModelPath(Gender.MALE, Perspective.THIRD_PERSON).equalsIgnoreCase(ARMAkey.getModelPath(Gender.MALE, Perspective.THIRD_PERSON))) {

			// if neither have alt textures
			if (ARMAtest.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()
				&& ARMAkey.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()) {
			    SPGlobal.log("Linking", "  Neither had Alt Textures, merging.");
			    ARMAtoNPC.put(armaK, npc);
			    added = true;
			    break;

			    // if both have alt textures
			} else if (!ARMAtest.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()
				&& !ARMAkey.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON).isEmpty()) {
			    boolean exists = true;

			    // check if both alt texture lists are the same
			    for (ARMA.AltTexture test : ARMAtest.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON)) {
				boolean matched = false;
				ArrayList<ARMA.AltTexture> keyTmp = new ArrayList<ARMA.AltTexture>();
				keyTmp.addAll(ARMAkey.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON));
				for (ARMA.AltTexture key : keyTmp) {
				    if (key.equals(test)) {
					SPGlobal.log("Linking", "  Alt texture " + test + " matched with " + key);
					matched = true;
					keyTmp.remove(key);
					break;
				    }
				}
				if (!matched) {
				    SPGlobal.log("Linking", "  Alt texture " + test + " could not find a match.");
				    exists = false;
				    break;
				}
			    }
			    // If alt textures are the same, merge
			    if (exists) {
				SPGlobal.log("Linking", "  Alt textures matched, merging.");
				ARMAtoNPC.put(armaK, npc);
				added = true;
				break;
			    }
			} else {
			    SPGlobal.log("Linking", "  One had alt textures while the other did not.");
			}
		    } else {
			SPGlobal.log("Linking", "  Different model paths:");
			SPGlobal.log("Linking", "  Test: " + ARMAtest.getModelPath(Gender.MALE, Perspective.THIRD_PERSON));
			SPGlobal.log("Linking", "  Key: " + ARMAkey.getModelPath(Gender.MALE, Perspective.THIRD_PERSON));
		    }
		    SPGlobal.log("Linking", "  ---------------------------------------------------");
		}

		// If no arma exists that is equal with same alt textures, then add new
		if (!added) {
		    SPGlobal.log("Linking", "No existing ARMA matched, making new split.");
		    ARMAtoNPC.put(arma, npc);
		}
	    }
	}

	// Toss suggestions that dont use ALL the src files
	Set<FormID> keys = new HashSet<>(ARMAtoNPC.keySet());
	for (FormID arma : keys) {
	    File nif = ARMAtoNIF.get(arma);
	    if (NIFtoSRC.get(nif).size() != textures.getAll().size()) {
		SPGlobal.log("Suggestions", "   Tossing " + SPDatabase.getMajor(arma) + " because it didn't use all src files.");
		ARMAtoNPC.remove(arma);
	    }
	}

	SPGlobal.log("Suggestions", "==================================");
	int group = 1;
	for (FormID arma : ARMAtoNPC.keySet()) {
	    SPGlobal.log("Suggestions", "Group " + group++ + ": " + SPDatabase.getMajor(arma, GRUP_TYPE.ARMA) + "");
	    SPGlobal.log("Suggestions", "   Seed choices (pick one):");
	    for (FormID npc : ARMAtoNPC.get(arma)) {
		NPC_ npc_ = (NPC_) SPDatabase.getMajor(npc, GRUP_TYPE.NPC_);
		SPGlobal.log("Suggestions", "\t\t[\"" + npc.getFormStr().substring(0, 6) + "\",\"" + npc.getFormStr().substring(6) + "\"]"
			+ "   //" + npc_.getEDID());
	    }
	    SPGlobal.log("Suggestions", "   Source textures to include (include all of them):");
	    File nif = ARMAtoNIF.get(arma);
	    for (File src : NIFtoSRC.get(nif)) {
		SPGlobal.log("Suggestions", "      " + src);
	    }
	    SPGlobal.log("Suggestions", "-------------------------------------------------------------------");
	}

	armorPiece(0);
    }
}
