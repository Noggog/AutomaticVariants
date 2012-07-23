/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.util.ArrayList;
import skyproc.ARMO;
import skyproc.GRUP_TYPE;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class SpecVariantSet extends SpecFile {

    public AVFileVars.VariantType type = AVFileVars.VariantType.NPC_;
    public String[][] Target_FormIDs = new String[0][];

    public SpecVariantSet(File src) {
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

    public void loadSkins(ArrayList<ARMO> in) {
	Target_FormIDs = new String[in.size()][2];
	for (int i = 0 ; i < in.size() ; i++) {
	    Target_FormIDs[i][0] = in.get(i).getFormStr().substring(0, 6);
	    Target_FormIDs[i][1] = in.get(i).getFormStr().substring(6);
	}
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
