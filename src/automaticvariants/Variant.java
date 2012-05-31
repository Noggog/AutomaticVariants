/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.*;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class Variant extends PackageComponent implements Serializable {

    String name;
    ArrayList<PackageComponent> textures = new ArrayList<PackageComponent>();
    TextureVariant[] TXSTs;
    VariantSpec spec = new VariantSpec();
    static String depth = "* +   # ";

    Variant(File variantDir) {
	super(variantDir, Type.VAR);
	this.name = "";
	String[] tmp = variantDir.getPath().split("\\\\");
	for (int i = 1; i <= 4; i++) {
	    name = "_" + tmp[tmp.length - i].replaceAll(" ", "") + name;
	}
	name = "AV" + name;
    }

    Variant() {
	super(null, Type.VAR);
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
	    } else if (AVFileVars.isSpec(f)) {
		try {
		    spec = AV.gson.fromJson(new FileReader(f), VariantSpec.class);
		    if (SPGlobal.logging()) {
			spec.print(src.getName());
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

    public class VariantSpec implements Serializable {

	int Probability_Divider = 1;
	String author;
	String[][] regionExcludeFrom;
	String[][] regionInclude;
	boolean exclusiveRegion;
	float healthMult;
	float heightMult;
	float magickaMult;
	float staminaMult;
	float speedMult;
	String nameAffix;
	String namePrefix;

	VariantSpec() {
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
	return spec.printHelpInfo() + divider;
    }
}
