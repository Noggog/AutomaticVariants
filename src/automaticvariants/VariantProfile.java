/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.DataFormatException;
import lev.LPair;
import lev.LShrinkArray;
import skyproc.*;
import skyproc.NIF.TextureSet;

/**
 *
 * @author Justin Swanson
 */
abstract public class VariantProfile {

    static int nextID = 0;
    Map<String, Map<Integer, String>> nifInfoDatabase = new HashMap<>();
    Map<String, ArrayList<String>> textures = new HashMap<>();
    Map<String, ArrayList<String>> altTextures = new HashMap<>();
    String texturesPrintout;
    Set<String> texturesFlat;
    Set<String> textureNames;
    ArrayList<VariantSet> matchedVariantSets = new ArrayList<>();
    ArrayList<TXST> generatedTXSTs = new ArrayList<>();
    public int ID;

    VariantProfile() {
	ID = nextID++;
    }

    VariantProfile(VariantProfile rhs) {
	this();
	textures = new HashMap<>();
	nifInfoDatabase = new HashMap<>(rhs.nifInfoDatabase);
	for (String key : rhs.textures.keySet()) {
	    ArrayList<String> list = new ArrayList<>();
	    for (String value : rhs.textures.get(key)) {
		list.add(value);
	    }
	    textures.put(key, list);
	}
	matchedVariantSets = new ArrayList<>(rhs.matchedVariantSets);
    }

    public boolean loadAltTextures(ArrayList<AltTextures.AltTexture> recordAltTextures) {
	for (AltTextures.AltTexture altTex : recordAltTextures) {
	    TXST txst = (TXST) SPDatabase.getMajor(altTex.getTexture(), GRUP_TYPE.TXST);
	    if (txst == null) {
		SPGlobal.logError(toString(), "Error locating txst with formID: " + altTex.getTexture());
		continue;
	    }
	    ArrayList<String> txstTextures = txst.getTextures();
	    if (!textures.containsKey(altTex.getName())) {
		SPGlobal.logError(getNifPath(), "Skipping profile " + toString() + ", because it did not have a nif node name of: " + altTex.getName());
		return false;
	    }
	    altTextures.put(altTex.getName(), new ArrayList<String>());
	    ArrayList<String> profileAltTextures = altTextures.get(altTex.getName());
	    for (int i = 0; i < txstTextures.size(); i++) {
		if (txstTextures.get(i) == null) {
		    profileAltTextures.add("");
		} else {
		    profileAltTextures.add("TEXTURES\\" + txstTextures.get(i).toUpperCase());
		}
	    }
	}
	return true;
    }

    @Override
    public int hashCode() {
	int hash = 3;
	hash = 29 * hash + this.ID;
	return hash;
    }

    public void finalizeProfile() {
	for (String key : altTextures.keySet()) {
	    ArrayList<String> tex = textures.get(key);
	    ArrayList<String> altTex = altTextures.get(key);
	    for (int i = 0; i < tex.size() && i < altTex.size(); i++) {
		tex.set(i, altTex.get(i));
	    }
	}
	altTextures.clear();
    }

    public boolean catalogNif(String nifPath) {
	if (!nifInfoDatabase.containsKey(nifPath)) {
	    try {
		LShrinkArray nifRawData = null;
		File f = new File(nifPath);
		if (f.exists()) {
		    nifRawData = new LShrinkArray(f);
		} else {
		    nifRawData = BSA.getUsedFile(nifPath);
		}
		if (nifRawData != null) {
		    ArrayList<TextureSet> nifTextures = VariantFactory.loadNif(nifPath, nifRawData);
		    Map<Integer, String> nifData = new HashMap<>();
		    nifInfoDatabase.put(nifPath, nifData);
		    for (TextureSet t : nifTextures) {
			textures.put(t.getName(), t.getTextures());
			nifData.put(t.getIndex(), t.getName());
		    }
		    return true;
		} else {
		    SPGlobal.log(toString(), " * Could not catalog nif because it could not find file: " + nifPath);
		}
	    } catch (IOException | DataFormatException ex) {
		SPGlobal.logException(ex);
	    }
	}
	return false;
    }

    @Override
    public String toString() {
	return "ID: " + ID;
    }

    public void print() {
	SPGlobal.log(toString(), " /========================================");
	SPGlobal.log(toString(), "| Profile Records and NIF: ");
	printShort();
	SPGlobal.log(toString(), " \\========================================");
	SPGlobal.log(toString(), "    \\===== NIF nodes and Textures Used: ==");
	for (String n : textures.keySet()) {
	    SPGlobal.log(toString(), "    |===== " + n + " ====/");
	    SPGlobal.log(toString(), "    |=========================/");
	    int i = 0;
	    for (String s : textures.get(n)) {
		SPGlobal.log(toString(), "    | " + i++ + ": " + s);
	    }
	    SPGlobal.log(toString(), "    \\=====================================");
	}
	SPGlobal.log(toString(), "");
    }

    public abstract void printShort();

    public String profileHashCode() {
	return getSeed().getSeedHashCode();
    }

    public boolean absorb(VariantSet varSet, Collection<Seed> seeds) {
	for (Seed s : seeds) {
	    if (getSeed().equals(s)) {
		matchedVariantSets.add(varSet);
		return true;
	    }
	}
	return false;
    }

    public String generateEDID(Variant var) {
	return "AV_" + profileHashCode() + "_" + var.printName("_");
    }

    public abstract String getNifPath();

    public abstract Seed getSeed();

    public abstract void generateRecords();

    public ArrayList<VariantGlobalMesh> getGlobalMeshes() {
	ArrayList<VariantGlobalMesh> globalMeshes = new ArrayList<>();
	for (VariantSet varSet : matchedVariantSets) {
	    globalMeshes.addAll(varSet.getGlobalMeshes());
	}
	return globalMeshes;
    }

    public String getNifPath(Variant var) {
	String targetNifPath;
	ArrayList<PackageNode> varNifs = var.getAll(PackageNode.Type.MESH);
	if (varNifs.size() > 0) {
	    targetNifPath = varNifs.get(0).src.getPath();
	    catalogNif(targetNifPath);
	    SPGlobal.log(toString(), " * Using variant nif file: " + targetNifPath);
	} else {
	    targetNifPath = getNifPath();
	    SPGlobal.log(toString(), " * Using default nif file: " + targetNifPath);
	}
	return targetNifPath;
    }

    public void loadAltTextures(ArrayList<AltTextures.AltTexture> alts, Map<String, TXST> txsts, String nifPath) {
	alts.clear();

	Map<Integer, String> nifInfo = nifInfoDatabase.get(nifPath);
	for (Integer index : nifInfo.keySet()) {
	    String nifNodeName = nifInfo.get(index);
	    if (txsts.containsKey(nifNodeName)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " * | Loading TXST for " + nifNodeName + " index " + index);
		}
		alts.add(new AltTextures.AltTexture(nifNodeName, txsts.get(nifNodeName).getForm(), index));
	    }
	}
    }

    public Map<String, TXST> generateTXSTs(Variant var, String nifPath) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " * ==> Generating TXSTs");
	}
	Map<String, TXST> out = new HashMap<>();

	Map<Integer, String> nifInfo = nifInfoDatabase.get(nifPath);
	for (Integer index : nifInfo.keySet()) {
	    String nodeName = nifInfo.get(index);
	    if (shouldGenerate(var, nodeName)) {
		String edid = NiftyFunc.EDIDtrimmer(generateEDID(var) + "_" + nodeName + "_txst");
		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " * | Generating: " + edid);
		}

		// Create TXST
		TXST txst = new TXST(edid);
		txst.set(TXST.TXSTflag.FACEGEN_TEXTURES, true);

		// For each texture there normally...
		ArrayList<File> varFiles = var.getTextureFiles();
		for (int i = 0; i < textures.get(nodeName).size(); i++) {
		    String texture = textures.get(nodeName).get(i);
		    if (texture.length() < 9) {
			continue;
		    }
		    texture = texture.substring(9);
		    if (texture.equals("")) {
			continue;
		    }
		    // Then check if there is a variant file that matches
		    int set = readjustTXSTindices(i);
		    txst.setNthMap(set, texture);
		    for (File varFile : varFiles) {
			if (texture.contains(varFile.getName().toUpperCase())) {
			    // And then sub it in the TXST
			    String varTex = varFile.getPath();
			    varTex = varTex.substring(varTex.indexOf("AV Packages"));
			    txst.setNthMap(set, varTex);
			    if (SPGlobal.logging()) {
				SPGlobal.log(toString(), " * |    Loading " + i + ": " + varTex);
			    }
			    break;
			}
		    }
		}

		//Check to see if generated txst is duplicate
		for (TXST existingtxst : generatedTXSTs) {
		    if (existingtxst.deepEquals(txst)) {
			if (SPGlobal.logging()) {
			    SPGlobal.log(toString(), " * |    Discarding txst because it was duplicate with " + existingtxst);
			}
			SPGlobal.getGlobalPatch().remove(txst.getForm());
			txst = existingtxst;
			break;
		    }
		}

		out.put(nodeName, txst);
	    }
	}
	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " * =====================================>");
	}
	return out;
    }

    static int readjustTXSTindices(int j) {
	// Because nif fields map 2->3 if facegen flag is on.
	if (j == 2) {
	    return 3;
	}
	return j;
    }

    public boolean shouldGenerate(Variant var, String nodeName) {
	// Also need to check if TXST already exists with data
	return profileContainsVarTex(var, nodeName);
    }

    public boolean profileContainsVarTex(Variant var, String nodeName) {
	ArrayList<String> varTextures = var.getTextureNames();
	for (String profileTexture : textures.get(nodeName)) {
	    if (!"".equals(profileTexture)) {
		for (String varTex : varTextures) {
		    if (profileTexture.contains(varTex)) {
			return true;
		    }
		}
	    }
	}
	return false;
    }
}
