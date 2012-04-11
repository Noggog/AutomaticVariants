/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.util.ArrayList;
import lev.Ln;
import lev.gui.LHelpPanel;
import lev.gui.LImagePane;
import lev.gui.LSwingTreeNode;

/**
 *
 * @author Justin Swanson
 */
public class PackageComponent extends LSwingTreeNode {

    static PackageComponent lastDisplayed;
    static LHelpPanel help;
    static LImagePane display;
    static String divider = "\n\n";
    File src;
    boolean disabled = false;
    boolean disabledOrig = false;
    Type type;

    PackageComponent(File source, Type type) {
	src = source;
	if (source.getPath().contains(AVFileVars.inactiveAVPackagesDir)) {
	    disabledOrig = true;
	    disabled = true;
	}
	this.type = type;
    }

    @Override
    public PackageComponent get(LSwingTreeNode node) {
	return (PackageComponent) super.get(node);
    }

    boolean moveOut() {
	String dest = null;
	boolean pass = true;
	switch (type) {
	    case TEXTURE:
	    case GENTEXTURE:
		dest = AVFileVars.AVTexturesDir;
		break;
	}

	if (dest != null) {
	    File destFile = new File(dest + src.getPath().substring(src.getPath().indexOf("\\") + 1));
	    pass = Ln.moveFile(src, destFile, false);
	    src = destFile;
	}

	for (PackageComponent c : getAll()) {
	    pass = pass && c.moveOut();
	}

	return pass;
    }

    public ArrayList<PackageComponent> getAll(Type type) {
	ArrayList<PackageComponent> out = new ArrayList<PackageComponent>();
	for (Object o : children) {
	    PackageComponent child = (PackageComponent) o;
	    if (child.type == type) {
		out.add(child);
	    }
	}
	return out;
    }

    public ArrayList<PackageComponent> getAll() {
	ArrayList<PackageComponent> out = new ArrayList<PackageComponent>();
	if (children != null) {
	    for (Object o : children) {
		PackageComponent child = (PackageComponent) o;
		out.add(child);
	    }
	}
	return out;
    }

    public enum Type {

	DEFAULT,
	ROOT,
	PACKAGE,
	VARSET,
	VARGROUP,
	VAR,
	TEXTURE,
	GENTEXTURE;
    }
}
