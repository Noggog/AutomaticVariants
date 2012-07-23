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
public class SpecVariant extends SpecFile {

    public int Probability_Divider = 1;
    public String Author = "";
    public String[][] Region_Include = new String[0][0];
    public boolean Exclusive_Region = false;
    public int Health_Mult = 100;
    public int Height_Mult = 100;
    public int Magicka_Mult = 100;
    public int Stamina_Mult = 100;
    public int Speed_Mult = 100;
    public String Name_Affix = "";
    public String Name_Prefix = "";

    public static SpecVariant prototype = new SpecVariant();

    SpecVariant() {
	super();
    }

    public SpecVariant(File src) {
	super(src);
    }

    @Override
    ArrayList<String> print() {
	ArrayList<String> out = new ArrayList<String>();
	if (Author != null && !Author.equals("")) {
	    out.add("Author: " + Author);
	}
	if (Probability_Divider != 1) {
	    out.add("Probability Div: 1/" + Probability_Divider);
	}
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
	if (Name_Prefix != null && !Name_Prefix.equals("")) {
	    out.add("Name Prefix: " + Name_Prefix);
	}
	if (Name_Affix != null && !Name_Affix.equals("")) {
	    out.add("Name Affix: " + Name_Affix);
	}
	return out;
    }

    @Override
    void printToLog(String header) {
	SPGlobal.log(header, Variant.depth + "    --- Variant Specifications loaded: --");
	for (String s : print()) {
	    SPGlobal.log(header, Variant.depth + "    |   " + s);
	}
	SPGlobal.log(header, Variant.depth + "    -------------------------------------");
    }

    @Override
    public String printHelpInfo() {
	String out = "";
	if (!Name_Affix.equals("") || !Name_Prefix.equals("")) {
	    out += "Spawning Name: " + Name_Prefix + " [NAME] " + Name_Affix + "\n";
	}
	if (!Author.equals("")) {
	    out += "Author: " + Author + "\n";
	}
	if (Probability_Divider != 1) {
	    out += "Relative Probability: 1/" + Probability_Divider + "\n";
	}
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