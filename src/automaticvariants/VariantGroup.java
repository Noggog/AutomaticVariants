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
public class VariantGroup extends PackageComponent {

    ArrayList<Variant> variants = new ArrayList<Variant>();

    static String depth = "* +   ";

    VariantGroup(File groupDir) {
	super(groupDir, Type.VARGROUP);
    }

    public void load() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "### Adding Variant Group: " + src);
	}
	for (File f : src.listFiles()) {
	    if (f.isDirectory()) {
		Variant v = new Variant(f);
		v.load();
		variants.add(v);
		add(v);
	    }
	}
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "####################################");
	}
    }

    public void mergeInGlobals(ArrayList<PackageComponent> globalFiles) {
	for (Variant v : variants) {
	    v.mergeInGlobals(globalFiles);
	}
    }


}
