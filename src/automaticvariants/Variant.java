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
 * @Author Justin Swanson
 */
public class Variant extends PackageNode implements Serializable {

    String name;
    ArrayList<PackageNode> textures = new ArrayList<PackageNode>();
    TextureVariant[] TXSTs;
    public SpecVariant spec;
    static String depth = "* +   # ";

    Variant(File variantDir) {
	super(variantDir, Type.VAR);
	this.name = "";
	String[] tmp = variantDir.getPath().split("\\\\");
	for (int i = 1; i <= 4; i++) {
	    name = "_" + tmp[tmp.length - i].replaceAll(" ", "") + name;
	}
	name = "AV" + name;
	spec = new SpecVariant(variantDir);
    }

    Variant() {
	super(null, Type.VAR);
	spec = new SpecVariant();
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
		PackageNode c = new PackageNode(f, Type.TEXTURE);
		textures.add(c);
		add(c);
	    } else if (AVFileVars.isNIF(f)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(src.getName(), depth + "    Added nif: " + f);
		}
//		PackageComponent c = new PackageComponent(f, Type.)
	    } else if (AVFileVars.isSpec(f)) {
		try {
		    spec = AV.gson.fromJson(new FileReader(f), SpecVariant.class);
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
		    c.type = PackageNode.Type.TEXTURE;
		    textures.add(c);
		    if (SPGlobal.logging()) {
			SPGlobal.log(src.getName(), depth + "    Added ROUTED texture: " + c.routeFile);
		    }
		}
		add(c);
	    }
	}
    }

    public void mergeInGlobals(ArrayList<PackageNode> globalFiles) {
	ArrayList<File> toAdd = new ArrayList<File>();
	for (PackageNode global : globalFiles) {
	    boolean exists = false;
	    for (PackageNode tex : textures) {
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
	    textures.add(new PackageNode(f, Type.TEXTURE));
	}
    }

    public Variant merge(Variant rhs) {
	Variant out = new Variant();
	out.name = name + "_" + rhs.src.getName();
	out.textures.addAll(textures);
	for (PackageNode p : rhs.textures) {
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
	    String out = spec.printHelpInfo();
	    if (!out.equals("")) {
		out += "\n";
	    }
	    return out;
	} else {
	    return "BAD SPEC FILE";
	}
    }

    @Override
    public String printName() {
	PackageNode p = (PackageNode) this.getParent();
	return p.printName() + " - " + src.getName();
    }
}
