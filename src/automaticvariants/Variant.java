/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class Variant implements Serializable {

    File dir;
    String name;
    ArrayList<File> textures = new ArrayList<File>();
    TextureVariant[] TXSTs;
    VariantSpec spec;
    static String depth = "* +   # ";

    Variant(File variantDir) {
	this.dir = variantDir;
	this.name = variantDir.getName();
	String[] tmp = variantDir.getPath().split("\\\\");
	for (int i = 1; i <= 3; i++) {
	    name = "_" + tmp[tmp.length - i].replaceAll(" ", "") + name;
	}
	name = "AV" + name;
    }

    public void load() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(dir.getName(), depth + "  Adding Variant: " + dir);
	}
	for (File f : dir.listFiles()) {
	    if (AVFileVars.isDDS(f)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(dir.getName(), depth + "    Added texture: " + f);
		}
		textures.add(f);
	    } else if (AVFileVars.isSpec(f)) {
		try {
		    spec = AVGlobal.parser.fromJson(new FileReader(f), VariantSpec.class);
		    spec.file = f;
		    if (SPGlobal.logging()) {
			spec.print(dir.getName());
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

    public class VariantSpec implements Serializable {

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

	public String printHelpInfo() {
	    return "Relative Probability: 1/" + Probability_Divider;
	}
    }
}
