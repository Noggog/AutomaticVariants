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
public class VariantGroup {

    File groupName;
    ArrayList<Variant> variants = new ArrayList<Variant>();

    static String depth = "* +   ";

    VariantGroup(File groupDir) {
	groupName = groupDir;
    }

    public void load() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(groupName.getName(), depth + "### Adding Variant Group: " + groupName);
	}
	for (File f : groupName.listFiles()) {
	    if (f.isDirectory()) {
		Variant v = new Variant(f);
		v.load();
		variants.add(v);
	    }
	}
	if (SPGlobal.logging()) {
	    SPGlobal.log(groupName.getName(), depth + "####################################");
	}
    }
}
