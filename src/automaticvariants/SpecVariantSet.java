/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.gui.ProfileDisplay;
import java.io.File;
import java.util.ArrayList;
import skyproc.*;

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
	out.add("   | Type: " + type);
	out.add("   | Target FormIDs: ");
	for (String[] s : Target_FormIDs) {
	    out.add("   |   " + s[0] + " | " + s[1]);
	}
	return out;
    }

    public void loadSkins(ArrayList<ProfileDisplay> in) {
	Target_FormIDs = new String[in.size()][6];
	for (int i = 0 ; i < in.size() ; i++) {
	    VariantProfile profile = in.get(i).profile;
	    RACE race = profile.race;
	    ARMO skin = profile.skin;
	    ARMA piece = profile.piece;
	    Target_FormIDs[i][0] = race.getFormStr().substring(0, 6);
	    Target_FormIDs[i][1] = race.getFormStr().substring(6);
	    Target_FormIDs[i][2] = skin.getFormStr().substring(0, 6);
	    Target_FormIDs[i][3] = skin.getFormStr().substring(6);
	    Target_FormIDs[i][4] = piece.getFormStr().substring(0, 6);
	    Target_FormIDs[i][5] = piece.getFormStr().substring(6);
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
