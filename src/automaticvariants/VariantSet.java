/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class VariantSet {

    File setDir;
    ArrayList<VariantGroup> variants;
    ArrayList<File> commonTextures = new ArrayList<File>(2);
    VariantSetSpec spec;
    static String depth = "* +";

    VariantSet(File setDir) {
	this.setDir = setDir;
    }

    final public boolean loadVariants() {
	if (SPGlobal.logging()) {
	    SPGlobal.log(setDir.getName(), depth + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	    SPGlobal.log(setDir.getName(), depth + " Adding Variant Set: " + setDir);
	    SPGlobal.log(setDir.getName(), depth + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	    SPGlobal.log(setDir.getName(), depth);
	}
	variants = new ArrayList<VariantGroup>(setDir.listFiles().length - 1);

	for (File f : setDir.listFiles()) {
	    if (AVFileVars.isSpec(f)) {
		try {
		    spec = AVGlobal.parser.fromJson(new FileReader(f), VariantSetSpec.class);
		    spec.file = f;
		    if (SPGlobal.logging()) {
			spec.print(setDir.getName());
		    }
		} catch (com.google.gson.JsonSyntaxException ex) {
		    SPGlobal.logException(ex);
		    JOptionPane.showMessageDialog(null, "Variant set " + f.getPath() + " had a bad specifications file.  Skipped.");
		} catch (FileNotFoundException ex) {
		    SPGlobal.logException(ex);
		}

	    } else if (f.isDirectory()) {
		VariantGroup v = new VariantGroup(f);
		v.load();
		variants.add(v);

	    } else if (AVFileVars.isDDS(f)) {
		commonTextures.add(f);
		if (SPGlobal.logging()) {
		    SPGlobal.log(setDir.getName(), depth + "   Loaded common texture: " + f);
		}

	    } else if (SPGlobal.logging()) {
		SPGlobal.log(setDir.getName(), depth + "   Skipped file: " + f);
	    }
	}

	if (spec == null) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(setDir.getName(), depth + "");
		SPGlobal.log(setDir.getName(), depth + "!!++++ Variant set " + setDir.getPath() + "did not have specifications file.  Skipping.");
	    }
	    return false;
	}

	if (SPGlobal.logging()) {
	    SPGlobal.log(setDir.getName(), depth + "");
	    SPGlobal.log(setDir.getName(), depth + "++++++ END Variant Set: " + setDir);
	}
	return true;
    }

    class VariantSetSpec {

	File file;
	String[][] Target_FormIDs;
	Boolean Apply_To_Similar = true;

	void print(String set) {
	    SPGlobal.log(set, depth + "   --- Set Specifications loaded: --");
	    SPGlobal.log(set, depth + "   |   Target FormIDs: ");
	    for (String[] s : Target_FormIDs) {
		SPGlobal.log(set, depth + "   |     " + s[0] + " | " + s[1]);
	    }
	    SPGlobal.log(set, depth + "   |   Apply to Similar: " + Apply_To_Similar);
	    SPGlobal.log(set, depth + "   -------------------------------------");
	}
    }
}
