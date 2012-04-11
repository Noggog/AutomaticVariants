/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.util.ArrayList;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class AVPackage extends PackageComponent {

    ArrayList<VariantSet> sets = new ArrayList<VariantSet>();
    PackageSpec spec;
    static String depth = "";

    public AVPackage(File packageFolder) {
	super(packageFolder, Type.PACKAGE);
	loadSets();
    }

    ArrayList<Variant> flatten() {
	ArrayList<Variant> out = new ArrayList<Variant>();
	for (VariantSet s : sets) {
	    out.addAll(s.flatten());
	}
	return out;
    }

    final public void loadSets() {
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
		spec = new PackageSpec(f);
	    }
	}
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "*** END package: " + src);
	}
    }

    class PackageSpec {

	File file;

	PackageSpec(File f) {
	}
    }
}
