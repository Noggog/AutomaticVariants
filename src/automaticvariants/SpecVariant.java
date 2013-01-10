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
    public String Name_Affix = "";
    public String Name_Prefix = "";

    public static SpecVariantNPC prototype = new SpecVariantNPC();

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
	return out;
    }
}
