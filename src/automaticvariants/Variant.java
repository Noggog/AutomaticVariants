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

    String name = "";
    ArrayList<String> textureNames;
    public SpecVariant spec;
    static String depth = "* +   # ";

    public Variant(File variantDir) {
	super(variantDir, Type.VAR);
	this.name = variantDir.getName();
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
		add(c);
	    } else if (AVFileVars.isNIF(f)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(src.getName(), depth + "    Added nif: " + f);
		}
		PackageNode c = new PackageNode(f, Type.MESH);
		add(c);
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
		    JOptionPane.showMessageDialog(null, "Variant " + f.getPath() + " had a bad specifications file.  Skipped.");
		}
	    } else if (AVFileVars.isReroute(f)) {
		RerouteFile c = new RerouteFile(f);
		if (SPGlobal.logging()) {
		    SPGlobal.log(src.getName(), depth + "    Added ROUTED file: " + c.routeFile);
		}
		add(c);
	    }
	}
    }

    public void mergeInGlobals(ArrayList<PackageNode> globalFiles) {
	ArrayList<PackageNode> texs = getAll(Type.TEXTURE);
	for (PackageNode global : globalFiles) {
	    boolean exists = false;
	    for (PackageNode tex : texs) {
		if (global.src.getName().equalsIgnoreCase(tex.src.getName())) {
		    exists = true;
		    break;
		}
	    }
	    if (!exists) {
		add(new PackageNode(global.src, Type.TEXTURE));
	    }
	}
    }

    public Variant merge(Variant rhs) {
	Variant out = new Variant();
	out.name = name + "_" + rhs.src.getName();
	for (PackageNode tex : getAll(Type.TEXTURE)) {
	    out.add(new PackageNode(tex.src, Type.TEXTURE));
	}
	for (PackageNode p : rhs.getAll(Type.TEXTURE)) {
	    out.add(new PackageNode(p.src, Type.TEXTURE));
	}
	out.parent = rhs.parent;
	spec.Probability_Divider *= rhs.spec.Probability_Divider;
	return out;
    }

    public VariantGroup getGroup() {
	return (VariantGroup) getParent();
    }

    public ArrayList<File> getTextureFiles() {
	ArrayList<File> out = new ArrayList<>();
	for (PackageNode p : getAll(Type.TEXTURE)) {
	    out.add(p.src);
	}
	return out;
    }

    public ArrayList<String> getTextureNames() {
	if (textureNames == null) {
	    ArrayList<File> files = getTextureFiles();
	    textureNames = new ArrayList<>();
	    for (int i = 0; i < files.size(); i++) {
		textureNames.add(files.get(i).getName().toUpperCase());
	    }
	}
	return textureNames;
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
    public String printName(String spacer) {
	PackageNode p = (PackageNode) this.getParent();
	return p.printName(spacer) + spacer + name;
    }
}
