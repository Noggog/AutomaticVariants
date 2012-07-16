/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import lev.LMergeMap;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class AVPackage extends PackageNode {

    ArrayList<VariantSet> sets = new ArrayList<VariantSet>();
    SpecPackage spec;
    static String depth = "";

    public AVPackage(File packageFolder) throws FileNotFoundException, IOException {
	super(packageFolder, Type.PACKAGE);
	loadSets();
    }

    ArrayList<Variant> flatten() {
	ArrayList<Variant> out = new ArrayList<Variant>();
	for (VariantSet s : sets) {
	    out.addAll(s.multiplyAndFlatten());
	}
	return out;
    }

    @Override
    public void consolidateCommonFiles() throws FileNotFoundException, IOException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), "==============================================");
	    SPGlobal.log(src.getName(), "Consolidating Common Files " + src);
	    SPGlobal.log(src.getName(), "==============================================");
	    SPGlobal.flush();
	}
	for (VariantSet set : sets) {
	    set.consolidateCommonFiles();
	}
    }

    @Override
    public LMergeMap<File, File> getDuplicateFiles() throws FileNotFoundException, IOException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), "==============================================");
	    SPGlobal.log(src.getName(), "Creating File Shortcuts " + src);
	    SPGlobal.log(src.getName(), "==============================================");
	    SPGlobal.flush();
	}
	LMergeMap<File, File> duplicates = new LMergeMap<File, File>(false);
	for (VariantSet set : sets) {
	    duplicates.addAll(set.getDuplicateFiles());
	}

	return duplicates;
    }

    final public void loadSets() throws FileNotFoundException, IOException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "************************************************************");
	    SPGlobal.log(src.getName(), depth + "* Loading package: " + src);
	    SPGlobal.log(src.getName(), depth + "************************************************************");
	}
	for (File f : src.listFiles()) {
	    if (f.isDirectory()) {
		VariantSet set = new VariantSet(f);
		if (set.loadVariants()) {
		    sets.add(set);
		    add(set);
		}
	    } else if (AVFileVars.isSpec(f)) {
		spec = AV.gson.fromJson(new FileReader(f), SpecPackage.class);
		spec.src = f;
	    }
	}
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "*** END package: " + src);
	}
    }
}
