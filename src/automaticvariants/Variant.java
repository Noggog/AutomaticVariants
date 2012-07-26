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
    ArrayList<PackageNode> texturesNode = new ArrayList<>();
    ArrayList<File> textureFiles;
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
		texturesNode.add(c);
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
		    texturesNode.add(c);
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
	    for (PackageNode tex : texturesNode) {
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
	    texturesNode.add(new PackageNode(f, Type.TEXTURE));
	}
    }

    public Variant merge(Variant rhs) {
	Variant out = new Variant();
	out.name = name + "_" + rhs.src.getName();
	out.texturesNode.addAll(texturesNode);
	for (PackageNode p : rhs.texturesNode) {
	    if (!out.texturesNode.contains(p)) {
		out.texturesNode.add(p);
	    }
	}
	out.parent = rhs.parent;
	spec.Probability_Divider *= rhs.spec.Probability_Divider;
	return out;
    }

    public VariantGroup getGroup() {
	return (VariantGroup) getParent();
    }

    public ArrayList<File> getTextures() {
	if (textureFiles == null) {
	    textureFiles = new ArrayList<>();
	    for (PackageNode p : texturesNode) {
		textureFiles.add(p.src);
	    }
	}
	return textureFiles;
    }

    public ArrayList<String> getTextureNames() {
	if (textureNames == null) {
	    ArrayList<File> files = getTextures();
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
