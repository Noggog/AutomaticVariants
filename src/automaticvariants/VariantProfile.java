/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVFileVars.ARMO_spec;
import java.io.File;
import java.util.*;
import lev.LMergeMap;
import skyproc.ARMA.AltTexture;
import skyproc.*;

/**
 *
 * @author Justin Swanson
 */
public class VariantProfile {

    public static ArrayList<VariantProfile> profiles = new ArrayList<>();
    static int nextID = 0;
    public RACE race;
    public ARMO skin;
    public ARMA piece;
    String nifPath;
    ArrayList<String> nifNodeNames = new ArrayList<>();
    Map<String, ArrayList<String>> textures = new HashMap<>();
    Map<String, ArrayList<String>> altTextures = new HashMap<>();
    Set<String> texturesFlat;
    ArrayList<VariantSet> sets = new ArrayList<>();
    public int ID;

    public VariantProfile(RACE race, ARMO skin, ARMA piece) {
	this.race = race;
	this.skin = skin;
	this.piece = piece;
    }

    VariantProfile() {
	ID = nextID++;
	profiles.add(this);
    }

    VariantProfile(VariantProfile rhs) {
	this();
	race = rhs.race;
	skin = rhs.skin;
	piece = rhs.piece;
	nifPath = rhs.nifPath;
	textures = new HashMap<>();
	nifNodeNames = new ArrayList<>(rhs.nifNodeNames);
	for (String key : rhs.textures.keySet()) {
	    ArrayList<String> list = new ArrayList<>();
	    for (String value : rhs.textures.get(key)) {
		list.add(value);
	    }
	    textures.put(key, list);
	}
	sets = new ArrayList<>(rhs.sets);
    }

    public void loadAltTextures(ArrayList<AltTexture> recordAltTextures) {
	for (AltTexture altTex : recordAltTextures) {
	    TXST txst = (TXST) SPDatabase.getMajor(altTex.getTexture(), GRUP_TYPE.TXST);
	    if (txst == null) {
		SPGlobal.logError(toString(), "Error locating txst with formID: " + altTex.getTexture());
		continue;
	    }
	    ArrayList<String> txstTextures = txst.getTextures();
	    if (!textures.containsKey(altTex.getName())) {
		SPGlobal.logError(nifPath, "Skipping profile " + toString() + ", because it did not have a nif node name of: " + altTex.getName());
		VariantProfile.profiles.remove(this);
		return;
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

    public boolean isValid() {
	return race != null && skin != null && piece != null;
    }

    public static VariantProfile find(RACE race, ARMO skin, ARMA piece, String nifPath) {
	for (int i = 0; i < profiles.size(); i++) {
	    if (profiles.get(i).is(race, skin, piece, nifPath)) {
		return profiles.get(i);
	    }
	}
	return null;
    }

    public static void printProfiles() {
	SPGlobal.log("Print", "===========================================================");
	SPGlobal.log("Print", "=============      Printing all Profiles     ==============");
	SPGlobal.log("Print", "===========================================================");
	SPGlobal.log("Print", "");
	for (VariantProfile v : profiles) {
	    v.print();
	}
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

    public void printShort() {
	SPGlobal.log(toString(), "|  Race: " + race);
	SPGlobal.log(toString(), "|  Skin: " + skin);
	SPGlobal.log(toString(), "| Piece: " + piece);
	SPGlobal.log(toString(), "|   NIF: " + nifPath);
    }

    public boolean is(RACE race, ARMO skin, ARMA piece, String nifPath) {
	if (race != null && race != this.race) {
	    return false;
	}
	if (skin != null && skin != this.skin) {
	    return false;
	}
	if (piece != null && piece != this.piece) {
	    return false;
	}
	if (nifPath != null && !nifPath.equalsIgnoreCase(this.nifPath)) {
	    return false;
	}
	return true;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final VariantProfile other = (VariantProfile) obj;
	if (this.ID != other.ID) {
	    return false;
	}
	return true;
    }

    public boolean absorb(VariantSet varSet, Collection<SeedProfile> seeds) {

	for (SeedProfile seed : seeds) {
	    if (seed.race.equals(race)
		    && seed.skin.equals(skin)
		    && seed.piece.equals(piece)) {
		if (!hasCommonTexture(varSet)) {
		    return false;
		}
		sets.add(varSet);
		return true;
	    }
	}
	return false;
    }

    public boolean hasCommonTexture(VariantSet varSet) {
	Set<String> profileTexFlat = getTexturesFlat();
	Set<String> varTexFlat = varSet.getTextures();
	for (String s : varTexFlat) {
	    for (String s2 : profileTexFlat) {
		if (s2.contains(s)) {
		    return true;
		}
	    }
	}
	return false;
    }

    public boolean hasAllTexturePaths(Collection<String> inTextures) {
	getTexturesFlat();
	for (String texture : inTextures) {
	    if (!texturesFlat.contains(texture)) {
		return false;
	    }
	}
	return true;
    }

    public Set<String> getTexturesFlat() {
	if (texturesFlat == null) {
	    texturesFlat = new HashSet<>();
	    for (ArrayList<String> list : textures.values()) {
		texturesFlat.addAll(list);
	    }
	    texturesFlat.remove("");
	}
	return texturesFlat;
    }

    public boolean hasTexture(String texture) {
	for (String tex : getTexturesFlat()) {
	    if (tex.contains(texture)) {
		return true;
	    }
	}
	return false;
    }

    public boolean hasTexture(File texture) {
	return hasTexture(texture.getName().toUpperCase());
    }

    public void generateRecords() {
	for (VariantSet varSet : sets) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(toString(), " *************> Generating for " + varSet.printName("-"));
	    }
	    ArrayList<Variant> vars = varSet.multiplyAndFlatten();
	    for (Variant var : vars) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " *************> Generating for " + var.printName("-"));
		}

		Map<String, TXST> txsts = generateTXSTs(var);
		if (txsts.isEmpty()) {
		    SPGlobal.logError(toString(), " * Skipped because no TXSTs were generated");
		    continue;
		}
		ARMA arma = generateARMA(var, txsts);
		ARMO armo = generateARMO(var, arma);

		if (AV.save.getBool(AVSaveFile.Settings.PACKAGES_ORIG_AS_VAR)) {
		}

		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " ******************************>");
		    SPGlobal.log(toString(), "");
		}
	    }
	}
    }

    public boolean shouldGenerate(Variant var, String nodeName) {
	ArrayList<String> varTextures = var.getTextureNames();
	for (String profileTexture : textures.get(nodeName)) {
	    for (String varTex : varTextures) {
		if (profileTexture.contains(varTex)) {
		    return true;
		}
	    }
	}
	return false;
    }

    public Map<String, TXST> generateTXSTs(Variant var) {
	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " * ==> Generating TXSTs");
	}
	Map<String, TXST> out = new HashMap<>();
	for (String nodeName : nifNodeNames) {
	    if (shouldGenerate(var, nodeName)) {
		String edid = NiftyFunc.EDIDtrimmer(generateEDID(var) + "_" + nodeName + "_txst");
		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " * | Generating: " + edid);
		}

		// Create TXST
		TXST txst = new TXST(SPGlobal.getGlobalPatch(), edid);
		txst.set(TXST.TXSTflag.FACEGEN_TEXTURES, true);

		// For each texture there normally...
		ArrayList<File> varFiles = var.getTextureFiles();
		for (int i = 0; i < textures.get(nodeName).size(); i++) {
		    String texture = textures.get(nodeName).get(i);
		    if (texture.equals("")) {
			continue;
		    }
		    // Then check if there is a variant file that matches
		    int set = readjustTXSTindices(i);
		    txst.setNthMap(set, texture.substring(9));
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

    public ARMA generateARMA(Variant var, Map<String, TXST> txsts) {
	String edid = NiftyFunc.EDIDtrimmer(generateEDID(var) + "_arma");
	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " * ==> Generating ARMA: " + edid);
	}
	ARMA arma = (ARMA) SPGlobal.getGlobalPatch().makeCopy(piece, edid);
	arma.setRace(race.getForm());
	arma.clearAdditionalRaces();

	ArrayList<ARMA.AltTexture> alts = arma.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON);
	alts.clear();

	int i = 0;
	for (String nifNodeName : nifNodeNames) {
	    if (txsts.containsKey(nifNodeName)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " * | Loading TXST for " + nifNodeName + " index " + i);
		}
		alts.add(new ARMA.AltTexture(nifNodeName, txsts.get(nifNodeName).getForm(), i));
	    }
	    i++;
	}

	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " * =====================================>");
	}

	return arma;
    }

    public ARMO generateARMO(Variant var, ARMA arma) {
	String edid = NiftyFunc.EDIDtrimmer(generateEDID(var) + "_armo");
	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " * ==> Generating ARMO: " + edid);
	}
	ARMO armo = (ARMO) SPGlobal.getGlobalPatch().makeCopy(skin, edid);
	armo.setRace(arma.getRace());

	armo.removeArmature(piece.getForm());
	armo.addArmature(arma.getForm());

	if (!AVFileVars.armors.containsKey(skin.getForm())) {
	    AVFileVars.armors.put(skin.getForm(), new LMergeMap<FormID, ARMO_spec>(false));
	}
	AVFileVars.armors.get(skin.getForm()).put(arma.getRace(), new ARMO_spec(armo, var.spec));

	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " * =====================================>");
	}

	return armo;
    }

    public String profileHashCode() {
	int hash = 7;
	hash = 29 * hash + Objects.hashCode(this.race);
	hash = 29 * hash + Objects.hashCode(this.skin);
	hash = 29 * hash + Objects.hashCode(this.piece);
	if (hash >= 0) {
	    return Integer.toString(hash);
	} else {
	    return "n" + Integer.toString(-hash);
	}
    }

    public String generateEDID(Variant var) {
	return "AV_" + profileHashCode() + "_" + var.printName("_");
    }

    @Override
    public int hashCode() {
	int hash = 3;
	hash = 29 * hash + this.ID;
	return hash;
    }
}
