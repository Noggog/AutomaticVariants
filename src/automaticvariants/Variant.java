/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class Variant {

    File variantName;
    ArrayList<File> textures = new ArrayList<File>();
    VariantSpec spec;
    static String depth = "* +   # ";

    Variant(File variantDir) {
	variantName = variantDir;
    }

    public void load() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(variantName.getName(), depth + "  Adding Variant: " + variantName);
	}
	for (File f : variantName.listFiles()) {
	    if (AVFileVars.isDDS(f)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(variantName.getName(), depth + "    Added texture: " + f);
		}
		textures.add(f);
	    } else if (AVFileVars.isSpec(f)) {
		try {
		    spec = AVGlobal.parser.fromJson(new FileReader(f), VariantSpec.class);
		    spec.file = f;
		    if (SPGlobal.logging()) {
			spec.print(variantName.getName());
		    }
		} catch (com.google.gson.JsonSyntaxException ex) {
		    SPGlobal.logException(ex);
		    JOptionPane.showMessageDialog(null, "Variant set " + f.getPath() + " had a bad specifications file.  Skipped.");
		} catch (FileNotFoundException ex) {
		    SPGlobal.logException(ex);
		}
	    }
	}
    }

    public void mergeInGlobals(ArrayList<File> globalFiles) {
	ArrayList<File> toAdd = new ArrayList<File>();
	for (File global : globalFiles) {
	    boolean exists = false;
	    for (File src : textures) {
		if (global.getName().equalsIgnoreCase(src.getName())) {
		    exists = true;
		    break;
		}
	    }
	    if (!exists) {
		toAdd.add(global);
	    }
	}
    }

    class VariantSpec {

	File file;
	int Probability_Divider = 1;

	VariantSpec(File f) {
	    file = f;
	}

	void print(String header) {
	    SPGlobal.log(header, depth + "    --- Variant Specifications loaded: --");
	    SPGlobal.log(header, depth + "    |   Probability Div: 1/" + Probability_Divider);
	    SPGlobal.log(header, depth + "    -------------------------------------");
	}
    }
}
