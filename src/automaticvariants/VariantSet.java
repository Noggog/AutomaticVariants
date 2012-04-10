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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.swing.JOptionPane;
import lev.LMergeMap;
import lev.Ln;
import skyproc.*;
import skyproc.exceptions.BadParameter;

/**
 *
 * @author Justin Swanson
 */
public class VariantSet {

    File setDir;
    ArrayList<VariantGroup> groups;
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
	groups = new ArrayList<VariantGroup>(setDir.listFiles().length - 1);

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
		groups.add(v);

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

	for (VariantGroup g : groups) {
	    g.mergeInGlobals(commonTextures);
	}

	if (SPGlobal.logging()) {
	    SPGlobal.log(setDir.getName(), depth + "");
	    SPGlobal.log(setDir.getName(), depth + "++++++ END Variant Set: " + setDir);
	}
	return true;
    }

    ArrayList<Variant> flatten() {
	ArrayList<Variant> out = new ArrayList<Variant>();
	for (VariantGroup g : groups) {
	    out.addAll(g.variants);
	}
	return out;
    }

    public boolean isEmpty() {
	if (groups.isEmpty()) {
	    return true;
	}
	for (VariantGroup g : groups) {
	    if (!g.variants.isEmpty()) {
		return false;
	    }
	}
	return true;
    }

    public class VariantSetSpec {

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

	public String printHelpInfo() {
	    String content = "Seeds:";
	    for (String[] formID : Target_FormIDs) {
		content += "\n    ";
		FormID id = new FormID(formID[0], formID[1]);
		NPC_ npc = (NPC_) SPDatabase.getMajor(id, GRUP_TYPE.NPC_);
		if (npc != null) {
		    content += npc.getEDID() + "  |  ";
		}
		content += id.getFormStr();
	    }
	    return content;
	}
    }
}
