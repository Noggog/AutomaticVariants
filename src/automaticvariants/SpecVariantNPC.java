/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import skyproc.FormID;
import skyproc.GRUP_TYPE;

/**
 *
 * @author Justin Swanson
 */
public class SpecVariantNPC extends SpecVariant {

    public String[][] Region_Include = new String[0][];
    public boolean Exclusive_Region = false;
    public int Health_Mult = 100;
    public int Height_Mult = 100;
    public int Magicka_Mult = 100;
    public int Stamina_Mult = 100;
    public int Speed_Mult = 100;
    
    SpecVariantNPC() {
	super();
    }

    public SpecVariantNPC(File src) {
	super(src);
    }
    
    public Set<FormID> getRegions() {
	Set<FormID> out = new HashSet<>();
	for (String[] formID : Region_Include) {
	    FormID id = FormID.parseString(formID);
	    if (!id.isNull()) {
		out.add(id);
	    }
	}
	return out;
    }

    @Override
    ArrayList<String> print() {
	ArrayList<String> out = super.print();
	if (Region_Include != null && Region_Include.length > 0) {
	    out.add("Region Include: ");
	    for (String[] s : Region_Include) {
		String tmp = "";
		for (String part : s) {
		    tmp += part + " ";
		}
		out.add("   " + tmp);
	    }
	    out.add("Exclusive Region: " + Exclusive_Region);
	}
	if (Health_Mult != 100) {
	    out.add("Relative Health: " + Health_Mult);
	}
	if (Magicka_Mult != 100) {
	    out.add("Relative Magicka: " + Magicka_Mult);
	}
	if (Stamina_Mult != 100) {
	    out.add("Relative Stamina: " + Stamina_Mult);
	}
	if (Speed_Mult != 100) {
	    out.add("Relative Speed: " + Speed_Mult);
	}
	if (Height_Mult != 100) {
	    out.add("Relative Height: " + Height_Mult);
	}
	return out;
    }

    @Override
    public String printHelpInfo() {
	String out = super.printHelpInfo();
	if (Region_Include.length > 0) {
	    out += "Regions To Spawn In:";
	    for (String[] formID : Region_Include) {
		out += "\n    " + printFormID(formID, GRUP_TYPE.ALCH);
	    }
	    out += "\n";
	}
	if (Height_Mult != 100) {
	    out += "Relative Height: " + Height_Mult + "%\n";
	}
	if (Health_Mult != 100) {
	    out += "Relative Health: " + Health_Mult + "%\n";
	}
	if (Magicka_Mult != 100) {
	    out += "Relative Magicka: " + Magicka_Mult + "%\n";
	}
	if (Stamina_Mult != 100) {
	    out += "Relative Stamina: " + Stamina_Mult + "%\n";
	}
	if (Speed_Mult != 100) {
	    out += "Relative Speed: " + Speed_Mult + "%\n";
	}
	return out;
    }
    
    
}