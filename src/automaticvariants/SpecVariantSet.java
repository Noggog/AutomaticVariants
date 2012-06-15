/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.util.ArrayList;
import skyproc.GRUP_TYPE;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
class SpecVariantSet extends SpecFile {

    AVFileVars.VariantType type = AVFileVars.VariantType.NPC_;
    String[][] Target_FormIDs = new String[0][];

    SpecVariantSet(File src) {
	super(src);
    }

    @Override
    ArrayList<String> print() {
	ArrayList<String> out = new ArrayList<String>();
	out.add("Type: " + type);
	out.add("Target FormIDs: ");
	for (String[] s : Target_FormIDs) {
	    out.add("   " + s[0] + " | " + s[1]);
	}
	return out;
    }

    @Override
    void printToLog(String set) {
	SPGlobal.log(set, VariantSet.depth + "   --- Set Specifications loaded: --");
	for (String s : print()) {
	    SPGlobal.log(set, VariantSet.depth + s);
	}
	SPGlobal.log(set, VariantSet.depth + "   -------------------------------------");
    }

    @Override
    public String printHelpInfo() {
	String content = "Seeds:";
	for (String[] formID : Target_FormIDs) {
	    content += "\n    ";
	    content += printFormID(formID, GRUP_TYPE.NPC_);
	}
	return content;
    }
}
