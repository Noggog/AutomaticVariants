/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AVFileVars;
import automaticvariants.AVGlobal;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import lev.gui.LHelpPanel;
import lev.gui.LImagePane;
import lev.gui.LSwingTreeNode;

/**
 *
 * @author Justin Swanson
 */
public class PackageNode extends LSwingTreeNode implements Comparable {

    static PackageNode lastDisplayed;
    File src;
    boolean disabled = false;
    boolean disabledOrig = false;
    Type type;
    LHelpPanel help;
    LImagePane display;
    File spec;
    static String divider = "\n\n";

    PackageNode(File f, LHelpPanel help) {
	src = f;
	this.help = help;
	type = Type.DEFAULT;
    }

    @Override
    public String toString() {
	return src.getName();
    }

    @Override
    public PackageNode get(LSwingTreeNode node) {
	return (PackageNode) super.get(node);
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final PackageNode other = (PackageNode) obj;
	if (this.src != other.src) {
	    if (this.src == null) {
		return true;
	    }
	    String path = src.getPath().substring(src.getPath().indexOf("\\"));
	    String pathRhs = other.src.getPath().substring(other.src.getPath().indexOf("\\"));
	    if (!path.equalsIgnoreCase(pathRhs)) {
		return false;
	    }
	}
	return true;
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 83 * hash + (this.src != null ? this.src.hashCode() : 0);
	return hash;
    }

    @Override
    public int compareTo(Object arg0) {
	PackageNode rhs = (PackageNode) arg0;
	if (!src.isDirectory() && rhs.src.isDirectory()) {
	    return -1;
	}
	if (src.isDirectory() && !rhs.src.isDirectory()) {
	    return 1;
	}

	return src.getName().compareTo(rhs.src.getName());
    }

    public void sort() {
	if (this.children == null) {
	    return;
	}
	Collections.sort(this.children, new Comparator() {

	    @Override
	    public int compare(Object arg0, Object arg1) {
		PackageNode node = (PackageNode) arg0;
		return node.compareTo(arg1);
	    }
	});
	for (Object child : this.children) {
	    ((PackageNode) child).sort();
	}
    }

    public void updateHelp() {
	String content = "";
	if (disabled) {
	    content += "DISABLED - ";
	}
	ArrayList<PackageNode> genTextures;
	help.setBottomAreaVisible(false);
	switch (type) {
	    case PACKAGE:
		help.setSetting(src.getName());
		break;
	    case VARSET:
		help.setSetting(((PackageNode) parent).src.getName());
		content += src.getName() + divider;

		content += printSpec();

		genTextures = getAll(Type.GENTEXTURE);
		if (genTextures.size() > 0) {
		    content += "Shared files:\n";
		    for (PackageNode child : getAll(Type.GENTEXTURE)) {
			content += "    " + child.src.getName() + "\n";
		    }
		}
		break;
	    case GENTEXTURE:
		((PackageNode) parent).updateHelp();
		return;
	    case VAR:
		PackageNode p = ((PackageNode) parent);
		content += p.src.getName() + " => " + src.getName() + divider;

		content += p.printSpec();

		content += printSpec();

		genTextures = p.getAll(Type.GENTEXTURE);
		if (genTextures.size() > 0) {
		    content += "Inherited files:";
		    for (PackageNode gen : genTextures) {
			content += "\n    " + gen.src.getName();
		    }
		    content += divider;
		}


		content += "Exclusive files:\n";
		for (PackageNode child : getAll(Type.TEXTURE)) {
		    content += "    " + child.src.getName() + "\n";
		}

		break;
	    case TEXTURE:
//		if (!equals(lastDisplayed)) {
//		    try {
//			BufferedImage image = SettingsPackagesPanel.reader.read(src.toURL());
//			display.setImage(image);
//			help.setBottomAreaVisible(true);
//			lastDisplayed = this;
//		    } catch (Exception ex) {
//			SPGlobal.logError("PackageNode", "Could not display " + src);
//		    }
//		}
		((PackageNode) parent).updateHelp();
		return;
	    default:
		AVGUI.settingsMenu.managePackagesButton.updateHelp();
		return;
	}
	help.setContent(content);
	help.hideArrow();
    }

    ArrayList<PackageNode> getAll(Type type) {
	ArrayList<PackageNode> out = new ArrayList<PackageNode>();
	for (Object o : children) {
	    PackageNode child = (PackageNode) o;
	    if (child.type == type) {
		out.add(child);
	    }
	}
	return out;
    }

    ArrayList<PackageNode> getAll() {
	ArrayList<PackageNode> out = new ArrayList<PackageNode>();
	if (children != null) {
	    for (Object o : children) {
		PackageNode child = (PackageNode) o;
		out.add(child);
	    }
	}
	return out;
    }

    String printSpec() {
	String content = "";
	switch (type) {
	    case VARSET:
		if (spec == null) {
		    content += "MISSING SPECIFICATION FILE!";
		} else {
		    try {
			AVFileVars.VariantSet specFile = AVGlobal.parser.fromJson(new FileReader(spec), AVFileVars.VariantSet.class);
			content += specFile.printHelpInfo();
		    } catch (Exception ex) {
			content += "ERROR LOADING SPEC FILE!";
		    }
		}
		content += divider;
		break;
	    case VAR:
		if (spec != null) {
		    try {
			AVFileVars.VariantSpec specFile = AVGlobal.parser.fromJson(new FileReader(spec), AVFileVars.VariantSpec.class);
			content += specFile.printHelpInfo();
		    } catch (Exception ex) {
			content += "ERROR LOADING SPEC FILE!";
		    }
		    content += divider;
		}
		break;
	}
	return content;
    }

    enum Type {

	DEFAULT,
	PACKAGE,
	VARSET,
	VAR,
	TEXTURE,
	GENTEXTURE;
    }
}
