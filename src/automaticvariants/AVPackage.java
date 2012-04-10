/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import lev.Ln;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class AVPackage {

    File packageName;
    ArrayList<VariantSet> sets = new ArrayList<VariantSet>();
    PackageSpec spec;
    static String depth = "";

    AVPackage(File packageFolder) {
	packageName = packageFolder;
	loadSets();
    }

    ArrayList<Variant> flatten() {
	ArrayList<Variant> out = new ArrayList<Variant>();
	for (VariantSet s : sets) {
	    out.addAll(s.flatten());
	}
	return out;
    }

    public void moveOut() {
	for (VariantSet v : sets) {
	    v.moveOut();
	}
    }

    final public void loadSets() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(packageName.getName(), depth + "************************************************************");
	    SPGlobal.log(packageName.getName(), depth + "* Loading package: " + packageName);
	    SPGlobal.log(packageName.getName(), depth + "************************************************************");
	}
	for (File f : packageName.listFiles()) {
	    if (f.isDirectory()) {
		VariantSet set = new VariantSet(f);
		if (set.loadVariants()) {
		    sets.add(set);
		}
	    } else if (AVFileVars.isSpec(f)) {
		spec = new PackageSpec(f);
	    }
	}
	if (SPGlobal.logging()) {
	    SPGlobal.log(packageName.getName(), depth + "*** END package: " + packageName);
	}
    }

    class PackageSpec {

	File file;

	PackageSpec(File f) {
	}
    }
}
