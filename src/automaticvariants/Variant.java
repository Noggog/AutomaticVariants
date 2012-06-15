/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.*;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import skyproc.GRUP_TYPE;
import skyproc.SPGlobal;

/**
 *
 * @Author Justin Swanson
 */
public class Variant extends PackageComponent implements Serializable {

    String name;
    ArrayList<PackageComponent> textures = new ArrayList<PackageComponent>();
    TextureVariant[] TXSTs;
    public VariantSpec spec;
    static String depth = "* +   # ";

    Variant(File variantDir) {
	super(variantDir, Type.VAR);
	this.name = "";
	String[] tmp = variantDir.getPath().split("\\\\");
	for (int i = 1; i <= 4; i++) {
	    name = "_" + tmp[tmp.length - i].replaceAll(" ", "") + name;
	}
	name = "AV" + name;
	spec = new VariantSpec(variantDir);
    }

    Variant() {
	super(null, Type.VAR);
	spec = new VariantSpec();
    }

    public void load() throws FileNotFoundException, IOException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "  Adding Variant: " + src);
	}
	for (File f : src.listFiles()) {
	    if (AVFileVars.isDDS(f)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(src.getName(), depth + "    Added texture: " + f);
		}
		PackageComponent c = new PackageComponent(f, Type.TEXTURE);
		textures.add(c);
		add(c);
	    } else if (AVFileVars.isNIF(f)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(src.getName(), depth + "    Added nif: " + f);
		}
//		PackageComponent c = new PackageComponent(f, Type.)
	    } else if (AVFileVars.isSpec(f)) {
		try {
		    spec = AV.gson.fromJson(new FileReader(f), VariantSpec.class);
		    if (spec != null) {
			spec.src = f;
			if (SPGlobal.logging()) {
			    spec.printToLog(src.getName());
			}
		    }
		} catch (com.google.gson.JsonSyntaxException ex) {
		    SPGlobal.logException(ex);
		    JOptionPane.showMessageDialog(null, "Variant set " + f.getPath() + " had a bad specifications file.  Skipped.");
		}
	    } else if (AVFileVars.isReroute(f)) {
		RerouteFile c = new RerouteFile(f);
		if (AVFileVars.isDDS(c.src)) {
		    c.type = PackageComponent.Type.TEXTURE;
		    textures.add(c);
		    if (SPGlobal.logging()) {
			SPGlobal.log(src.getName(), depth + "    Added ROUTED texture: " + c.src);
		    }
		}
		add(c);
	    }
	}
    }

    public void mergeInGlobals(ArrayList<PackageComponent> globalFiles) {
	ArrayList<File> toAdd = new ArrayList<File>();
	for (PackageComponent global : globalFiles) {
	    boolean exists = false;
	    for (PackageComponent tex : textures) {
		if (global.src.getName().equalsIgnoreCase(tex.src.getName())) {
		    exists = true;
		    break;
		}
	    }
	    if (!exists) {
		toAdd.add(global.src);
	    }
	}

	for (File f : toAdd) {
	    textures.add(new PackageComponent(f, Type.TEXTURE));
	}
    }

    public class VariantSpec extends SpecFile {

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

	VariantSpec() {
	    
	}
	
	VariantSpec(File src) {
	    super(src);
	}

	@Override
	void printToLog(String header) {
	    SPGlobal.log(header, depth + "    --- Variant Specifications loaded: --");
	    if (Author != null && !Author.equals("")) {
		SPGlobal.log(header, depth + "    |   Author: " + Author);
	    }
	    if (Probability_Divider != 1) {
		SPGlobal.log(header, depth + "    |   Probability Div: 1/" + Probability_Divider);
	    }
	    if (Region_Include != null && Region_Include.length > 0) {
		SPGlobal.log(header, depth + "    |   Region Include: ");
		for (String[] s : Region_Include) {
		    String tmp = "";
		    for (String part : s) {
			tmp += part + " ";
		    }
		    SPGlobal.log(header, depth + "    |      " + tmp);
		}
		SPGlobal.log(header, depth + "    |   Exclusive Region: " + Exclusive_Region);
	    }
	    if (Health_Mult != 100) {
		SPGlobal.log(header, depth + "    |   Relative Health: " + Health_Mult);
	    }
	    if (Magicka_Mult != 100) {
		SPGlobal.log(header, depth + "    |   Relative Magicka: " + Magicka_Mult);
	    }
	    if (Stamina_Mult != 100) {
		SPGlobal.log(header, depth + "    |   Relative Stamina: " + Stamina_Mult);
	    }
	    if (Speed_Mult != 100) {
		SPGlobal.log(header, depth + "    |   Relative Speed: " + Speed_Mult);
	    }
	    if (Height_Mult != 100) {
		SPGlobal.log(header, depth + "    |   Relative Height: " + Height_Mult);
	    }
	    if (Name_Prefix != null && !Name_Prefix.equals("")) {
		SPGlobal.log(header, depth + "    |   Name Prefix: " + Name_Prefix);
	    }
	    if (Name_Affix != null && !Name_Affix.equals("")) {
		SPGlobal.log(header, depth + "    |   Name Affix: " + Name_Affix);
	    }
	    SPGlobal.log(header, depth + "    -------------------------------------");
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
		out += "Relative Height: " + (Height_Mult / 100.0) + "\n";
	    }
	    if (Health_Mult != 100) {
		out += "Relative Health: " + (Health_Mult / 100.0) + "\n";
	    }
	    if (Magicka_Mult != 100) {
		out += "Relative Magicka: " + (Magicka_Mult / 100.0) + "\n";
	    }
	    if (Stamina_Mult != 100) {
		out += "Relative Stamina: " + (Stamina_Mult / 100.0) + "\n";
	    }
	    if (Speed_Mult != 100) {
		out += "Relative Speed: " + (Speed_Mult / 100.0) + "\n";
	    }

	    return out;
	}
    }

    public Variant merge(Variant rhs) {
	Variant out = new Variant();
	out.name = name + "_" + rhs.src.getName();
	out.textures.addAll(textures);
	for (PackageComponent p : rhs.textures) {
	    if (!out.textures.contains(p)) {
		out.textures.add(p);
	    }
	}
	spec.Probability_Divider *= rhs.spec.Probability_Divider;
	return out;
    }

    @Override
    public String printSpec() {
	if (spec != null) {
	    return spec.printHelpInfo() + divider;
	} else {
	    return "BAD SPEC FILE";
	}
    }

    @Override
    public String printName() {
	PackageComponent p = (PackageComponent) this.getParent();
	return p.printName() + " - " + src.getName();
    }
}
