/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVFileVars.ARMO_spec;
import automaticvariants.PackageNode.Type;
import java.io.File;
import java.util.*;
import lev.LMergeMap;
import lev.gui.LFileTree;
import skyproc.AltTextures.AltTexture;
import skyproc.*;

/**
 *
 * @author Justin Swanson
 */
public class VariantProfileNPC extends VariantProfile {

    public SeedNPC seed;
    String nifPath;

    public VariantProfileNPC(RACE race, ARMO skin, ARMA piece) {
	super();
	seed = new SeedNPC(race, skin, piece);
    }

    VariantProfileNPC() {
	super();
	seed = new SeedNPC();
    }

    VariantProfileNPC(VariantProfileNPC rhs) {
	super(rhs);
	nifPath = rhs.nifPath;
	seed = new SeedNPC(rhs.seed);
    }

    public boolean isValid() {
	return seed.isValid();
    }

    @Override
    public void printShort() {
	SPGlobal.log(toString(), "|  Race: " + getRace());
	SPGlobal.log(toString(), "|  Skin: " + getSkin());
	SPGlobal.log(toString(), "| Piece: " + getPiece());
	SPGlobal.log(toString(), "|   NIF: " + nifPath);
    }

    public boolean is(RACE race, ARMO skin, ARMA piece, String nifPath) {
	if (race != null && race != getRace()) {
	    return false;
	}
	if (skin != null && skin != getSkin()) {
	    return false;
	}
	if (piece != null && piece != getPiece()) {
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
	final VariantProfileNPC other = (VariantProfileNPC) obj;
	if (this.ID != other.ID) {
	    return false;
	}
	return true;
    }

    @Override
    public boolean absorb(VariantSet varSet, Collection<Seed> seeds) {
	for (Seed seedG : seeds) {
	    SeedNPC seed = (SeedNPC) seedG;
	    if (seed.race.equals(getRace())
		    && seed.skin.equals(getSkin())
		    && seed.piece.equals(getPiece())) {
		matchedVariantSets.add(varSet);
		return true;
	    }
	}
	return false;
    }

    public boolean hasCommonTexture(VariantSet varSet) {
	Set<String> varTexFlat = varSet.getTextures();
	for (String s : varTexFlat) {
	    if (hasTexture(s)) {
		return true;
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

    public Set<String> getTextureNames() {
	if (textureNames == null) {
	    Set<String> tex = getTexturesFlat();
	    textureNames = new HashSet<>(tex.size());
	    for (String s : tex) {
		int index = s.lastIndexOf("\\") + 1;
		if (index < s.length()) {
		    textureNames.add(s.substring(s.lastIndexOf("\\") + 1));
		}
	    }
	}
	return textureNames;
    }

    public String printAllTextures() {
	if (texturesPrintout == null) {
	    LFileTree fileTree = new LFileTree();
	    for (String s : getTexturesFlat()) {
		fileTree.addFile(s);
	    }
	    texturesPrintout = fileTree.print("  ").toLowerCase();
	}
	return texturesPrintout;
    }

    public boolean hasTexture(String texture) {
	for (String tex : getTextureNames()) {
	    if (tex.equalsIgnoreCase(texture)) {
		return true;
	    }
	}
	return false;
    }

    public boolean hasTexture(File texture) {
	return hasTexture(texture.getName().toUpperCase());
    }

    public ArrayList<VariantGlobalMesh> getGlobalMeshes() {
	ArrayList<VariantGlobalMesh> globalMeshes = new ArrayList<>();
	for (VariantSet varSet : matchedVariantSets) {
	    globalMeshes.addAll(varSet.getGlobalMeshes());
	}
	return globalMeshes;
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
		TXST txst = new TXST(SPGlobal.getGlobalPatch(), edid);
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

    public void generateARMOs() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " ***********> Generating profile " + ID);
	}
	for (VariantSet varSet : matchedVariantSets) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(toString(), " *************> Generating set " + varSet.printName("-"));
	    }
	    ArrayList<Variant> vars = varSet.multiplyAndFlatten(getGlobalMeshes());
	    for (Variant var : vars) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " ***************> Generating var " + var.printName("-"));
		}

		String targetNifPath;
		ArrayList<PackageNode> varNifs = var.getAll(Type.MESH);
		if (varNifs.size() > 0) {
		    targetNifPath = varNifs.get(0).src.getPath();
		    catalogNif(targetNifPath);
		    SPGlobal.log(toString(), " * Using variant nif file: " + targetNifPath);
		} else {
		    targetNifPath = nifPath;
		    SPGlobal.log(toString(), " * Using default nif file: " + targetNifPath);
		}

		Map<String, TXST> txsts = generateTXSTs(var, targetNifPath);
		if (txsts.isEmpty()) {
		    SPGlobal.logError(toString(), " * Skipped because no TXSTs were generated");
		    continue;
		}
		ARMA arma = generateARMA(var, txsts, targetNifPath);
		ARMO armo = generateARMO(var, arma);

		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " ******************************>");
		    SPGlobal.log(toString(), "");
		}
	    }
	}
    }

    public ARMA generateARMA(Variant var, Map<String, TXST> txsts, String nifPath) {
	String edid = NiftyFunc.EDIDtrimmer(generateEDID(var) + "_arma");
	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " * ==> Generating ARMA: " + edid);
	}
	ARMA arma = (ARMA) SPGlobal.getGlobalPatch().makeCopy(getPiece(), edid);
	arma.setRace(getRace().getForm());
	arma.clearAdditionalRaces();

	String cleanNifPath = nifPath;
	if (cleanNifPath.indexOf("MESHES\\") == 0) {
	    cleanNifPath = cleanNifPath.substring(7);
	}
	arma.setModelPath(cleanNifPath, Gender.MALE, Perspective.THIRD_PERSON);

	ArrayList<AltTexture> alts = arma.getAltTextures(Gender.MALE, Perspective.THIRD_PERSON);
	alts.clear();

	Map<Integer, String> nifInfo = nifInfoDatabase.get(nifPath);
	for (Integer index : nifInfo.keySet()) {
	    String nifNodeName = nifInfo.get(index);
	    if (txsts.containsKey(nifNodeName)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " * | Loading TXST for " + nifNodeName + " index " + index);
		}
		alts.add(new AltTexture(nifNodeName, txsts.get(nifNodeName).getForm(), index));
	    }
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
	ARMO armo = (ARMO) SPGlobal.getGlobalPatch().makeCopy(getSkin(), edid);
	armo.setRace(arma.getRace());

	armo.removeArmature(getPiece().getForm());
	armo.addArmature(arma.getForm());

	//Ensure it won't show up as wearable
	armo.getBodyTemplate().set(BodyTemplate.GeneralFlags.NonPlayable, true);

	if (!VariantFactoryNPC.armors.containsKey(getSkin().getForm())) {
	    VariantFactoryNPC.armors.put(getSkin().getForm(), new LMergeMap<FormID, ARMO_spec>(false));
	}
	VariantFactoryNPC.armors.get(getSkin().getForm()).put(arma.getRace(), new ARMO_spec(armo, var.spec));

	if (SPGlobal.logging()) {
	    SPGlobal.log(toString(), " * =====================================>");
	}

	return armo;
    }

    @Override
    public String profileHashCode() {
	int hash = 7;
	hash = 29 * hash + Objects.hashCode(getRace());
	hash = 29 * hash + Objects.hashCode(getSkin());
	hash = 29 * hash + Objects.hashCode(getPiece());
	if (hash >= 0) {
	    return Integer.toString(hash);
	} else {
	    return "n" + Integer.toString(-hash);
	}
    }

    public RACE getRace() {
	return seed.race;
    }

    public ARMO getSkin() {
	return seed.skin;
    }

    public ARMA getPiece() {
	return seed.piece;
    }

    @Override
    public String getNifPath() {
	return nifPath;
    }

    @Override
    public Seed getSeed() {
	return seed;
    }

    @Override
    public void generateRecords() {
	generateARMOs();
    }
}
