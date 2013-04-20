/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVFileVars.SpecHolder;
import automaticvariants.SpecVariantSet.VariantType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import lev.LPair;
import lev.LShrinkArray;
import lev.Ln;
import skyproc.*;
import skyproc.exceptions.BadParameter;

/**
 *
 * @author Justin Swanson
 */
abstract public class VariantFactory<T extends VariantProfile> {

    static String header = "VariantFactory";
    public ArrayList<T> profiles = new ArrayList<>();

    public void createVariants(Mod source) {
	prepProfiles();
	dropVariantSetsInProfiles();
	clearUnusedProfiles();
	createVariantRecords(source);
	if (AV.save.getBool(AVSaveFile.Settings.PACKAGES_ORIG_AS_VAR)) {
	    implementOriginalAsVar();
	}
	createStructureRecords(source);
    }

    public void prepProfiles() {
	BSA.loadInBSAs(BSA.FileType.NIF, BSA.FileType.DDS);
	locateUnused();
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(AVFileVars.debugFolder + AVFileVars.debugNumber++ + " - Load Variant Profiles.txt");
	}
	createProfileShells();
	loadProfileNifs();
	loadProfileRecords();
	finalizeProfiles();
	printProfiles();
    }

    public void loadProfileNifs() {
	for (VariantProfile profile : new ArrayList<>(profiles)) {
	    try {
		LShrinkArray nifRawData = BSA.getUsedFile(profile.getNifPath());
		if (nifRawData != null) {
		    Map<Integer, LPair<String, ArrayList<String>>> nifTextures = loadNif(profile.getNifPath(), nifRawData);
		    Map<Integer, String> nifData = new HashMap<>();
		    profile.nifInfoDatabase.put(profile.getNifPath(), nifData);
		    for (Integer index : nifTextures.keySet()) {
			LPair<String, ArrayList<String>> pair = nifTextures.get(index);
			profile.textures.put(pair.a, pair.b);
			nifData.put(index, pair.a);
		    }
		    if (profile.textures.isEmpty()) {
			remove(profile);
			SPGlobal.log(profile.toString(), "Removing profile with nif because it had no textures: " + profile.getNifPath());
		    }
		} else {
		    remove(profile);
		    SPGlobal.logError(header, "Error locating nif file: " + profile.getNifPath() + ", removing profile.");
		}
	    } catch (IOException | DataFormatException ex) {
		SPGlobal.logException(ex);
	    }
	}
    }

    public static Map<Integer, LPair<String, ArrayList<String>>> loadNif(String nifPath, LShrinkArray in) {
	Map<Integer, LPair<String, ArrayList<String>>> nifTextures = new HashMap<>();
	try {
	    NIF nif = new NIF(nifPath, in);
	    nifTextures = nif.extractTextures();

	    // To uppercase
	    for (Integer index : nifTextures.keySet()) {
		LPair<String, ArrayList<String>> pair = nifTextures.get(index);
		for (int i = 0; i < pair.b.size(); i++) {
		    pair.b.set(i, pair.b.get(i).toUpperCase());
		}
	    }

	} catch (BadParameter | java.nio.BufferUnderflowException ex) {
	    SPGlobal.logException(ex);
	}
	return nifTextures;
    }

    public void remove(VariantProfile p) {
	profiles.remove((T) p);
    }

    public abstract void locateUnused();

    public abstract void createProfileShells();

    public abstract void loadProfileRecords();

    void finalizeProfiles() {
	for (VariantProfile p : profiles) {
	    p.finalizeProfile();
	}
    }

    void clearUnusedProfiles() {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(AVFileVars.debugFolder + AVFileVars.debugNumber++ + " - Clear Unused Profiles.txt");
	}
	for (VariantProfile profile : new ArrayList<>(profiles)) {
	    if (profile.matchedVariantSets.isEmpty()) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "Removing profile " + profile + " because it was empty.");
		    profile.print();
		}
		remove(profile);
	    }
	}
    }

    abstract boolean isUnused(FormID id);

    public void dropVariantSetsInProfiles() {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(AVFileVars.debugFolder + AVFileVars.debugNumber++ + " - Processing Variant Seeds.txt");
	}
	for (PackageNode avPackageC : AVFileVars.AVPackages.getAll(PackageNode.Type.PACKAGE)) {
	    AVPackage avPackage = (AVPackage) avPackageC;
	    for (PackageNode varSetP : avPackage.getAll(PackageNode.Type.VARSET)) {
		VariantSet varSet = (VariantSet) varSetP;
		if (varSet.spec.getType() != getType()) {
		    continue;
		} else if (SPGlobal.logging()) {
		    SPGlobal.log("SortVariantSets", " /====================================");
		    SPGlobal.log("SortVariantSets", "| Processing: " + varSet.src);
		    SPGlobal.log("SortVariantSets", "|==============\\");
		    SPGlobal.log("SortVariantSets", "|== Files: =====|");
		    SPGlobal.log("SortVariantSets", "|==============/");
		    for (String s : varSet.getTextures()) {
			SPGlobal.log("SortVariantSets", "|    " + s);
		    }
		    SPGlobal.log("SortVariantSets", "|==============\\");
		    SPGlobal.log("SortVariantSets", "|== Seeds: =====|");
		    SPGlobal.log("SortVariantSets", "|==============/");
		}

		ArrayList<Seed> seeds = varSet.getSeeds();

		if (SPGlobal.logging()) {
		    for (Seed s : seeds) {
			s.print();
		    }
		}

		boolean absorbed = false;
		for (VariantProfile varProfile : profiles) {
		    if (varProfile.absorb(varSet, seeds)) {
			SPGlobal.log("Absorb", "  /======================================");
			SPGlobal.log("Absorb", " /=== " + varSet.src + " absorbed by:");
			SPGlobal.log("Absorb", "|=======================================");
			varProfile.printShort();
			SPGlobal.log("Absorb", " \\======================================");
			absorbed = true;
		    }
		}

		if (SPGlobal.logging()) {
		    if (!absorbed) {
			SPGlobal.logError("Absorbing", "Variant Set " + varSet.src + " could not be absorbed by any profile.");
		    } else {
			// Spacing in logs
			SPGlobal.log(header, "");
			SPGlobal.log(header, "");
			SPGlobal.log(header, "");
			SPGlobal.log(header, "");
		    }
		}
	    }
	}
    }

    public void printProfiles() {
	SPGlobal.log("Print", "===========================================================");
	SPGlobal.log("Print", "=============      Printing all Profiles     ==============");
	SPGlobal.log("Print", "===========================================================");
	SPGlobal.log("Print", "");
	for (VariantProfile v : profiles) {
	    v.print();
	}
    }

    public void createVariantRecords(Mod source) {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(AVFileVars.debugFolder + AVFileVars.debugNumber++ + " - Generate Variants.txt");
	}
	for (VariantProfile profile : profiles) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(toString(), " ***********> Generating profile " + profile.ID);
	    }
	    profile.generateRecords();
	}
    }

    public abstract void implementOriginalAsVar();

    public abstract void createStructureRecords(Mod source);

    public abstract VariantType getType();

    public T find(Seed s) {
	for (T profile : profiles) {
	    if (s.equals(profile.getSeed())) {
		return profile;
	    }
	}
	return null;
    }

    static int calcLCM(ArrayList specs) {
	int[] divs = new int[specs.size()];
	for (int i = 0; i < divs.length; i++) {
	    SpecHolder holder = ((SpecHolder)specs.get(i));
	    divs[i] = holder.spec.Probability_Divider;
	}
	return Ln.lcmm(divs);
    }
}
