/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;
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
	for (int i = 1; i <= 4 ; i++) {
	    name = "_" + tmp[tmp.length - i].replaceAll(" ", "") + name;
	}
	name = "AV" + name;
    }

    public void load() {
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
		    spec = AVGlobal.parser.fromJson(new FileReader(f), VariantSpec.class);
		    if (SPGlobal.logging()) {
			spec.print(src.getName());
		    }
		} catch (com.google.gson.JsonSyntaxException ex) {
		    SPGlobal.logException(ex);
		    JOptionPane.showMessageDialog(null, "Variant set " + f.getPath() + " had a bad specifications file.  Skipped.");
		} catch (FileNotFoundException ex) {
		    SPGlobal.logException(ex);
		}
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

    @Override
    public String printSpec() {
	return spec.printHelpInfo() + divider;
    }
}
