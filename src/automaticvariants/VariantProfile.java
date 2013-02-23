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
		    Map<Integer, LPair<String, ArrayList<String>>> nifTextures = VariantFactory.loadNif(nifPath, nifRawData);
		    Map<Integer, String> nifData = new HashMap<>();
		    nifInfoDatabase.put(nifPath, nifData);
		    for (Integer index : nifTextures.keySet()) {
			LPair<String, ArrayList<String>> pair = nifTextures.get(index);
			textures.put(pair.a, pair.b);
			nifData.put(index, pair.a);
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

    public abstract String profileHashCode();

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
}
