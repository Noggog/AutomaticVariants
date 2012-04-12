/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.gui.AVGUI;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import lev.Ln;
import lev.gui.LHelpPanel;
import lev.gui.LImagePane;
import lev.gui.LSwingTreeNode;

/**
 *
 * @author Justin Swanson
 */
public class PackageComponent extends LSwingTreeNode implements Comparable {

    static PackageComponent lastDisplayed;
    static LHelpPanel help;
    static LImagePane display;
    static String divider = "\n\n";
    public File src;
    public boolean disabled = false;
    public boolean disabledOrig = false;
    public Type type;

    public PackageComponent(File source, Type type) {
	if (source != null) {
	    src = source;
	    if (source.getPath().contains(AVFileVars.inactiveAVPackagesDir)) {
		disabledOrig = true;
		disabled = true;
	    }
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

    @Override
    public String toString() {
	return src.getName();
    }

    public boolean moveNode() {
	boolean proper = true;
	if (disabled != disabledOrig) {
	    if (src.isFile()) {
		proper = proper && moveFile(src);
	    } else if (src.isDirectory()) {
		for (File f : src.listFiles()) {
		    if (f.getPath().toUpperCase().endsWith(".JSON")) {
			proper = proper && moveFile(f);
		    }
		}
	    }
	}
	for (PackageComponent p : getAll()) {
	    proper = proper && p.moveNode();
	}
	return proper;
    }

    public boolean moveFile(File src) {
	String prefix;
	if (disabled) {
	    prefix = AVFileVars.inactiveAVPackagesDir;
	} else {
	    prefix = AVFileVars.AVPackagesDir;
	}
	File dest = new File(prefix + src.getPath().substring(src.getPath().indexOf("\\")));
	return Ln.moveFile(src, dest, true);
    }

    public void pruneDisabled() {
	if (children != null) {
	    ArrayList<Object> remove = new ArrayList<Object>();
	    for (Object o : children) {
		PackageComponent p = (PackageComponent) o;
		if (p.disabled) {
		    remove.add(o);
		} else {
		    p.pruneDisabled();
		}
	    }
	    for (Object o : remove) {
		children.remove(o);
	    }
	}
    }

    public void finalizeComponent() {
	for (PackageComponent c : getAll()) {
	    c.finalizeComponent();
	}
    }

    public void mergeIn(PackageComponent rhs) {
	for (PackageComponent c : getAll()) {
	    if (c.equals(rhs)) {
		for (PackageComponent rhsChild : rhs.getAll()) {
		    c.mergeIn(rhsChild);
		}
		return;
	    }
	}
	// else
	add(rhs);
    }

    public void updateHelp(LHelpPanel help) {

	String content = "";
	PackageComponent set;
	PackageComponent group;
	if (disabled) {
	    content += "DISABLED - ";
	}
	ArrayList<PackageComponent> genTextures;
	help.setBottomAreaVisible(false);
	switch (type) {
	    case PACKAGE:
		help.setSetting(src.getName());
		break;
	    case GENTEXTURE:
		((PackageComponent) parent).updateHelp(help);
		return;
	    case VARSET:
		help.setSetting(((PackageComponent) parent).src.getName());
		content += src.getName() + divider;

		content += printSpec();

		content += printGenTextures();

		break;
	    case VARGROUP:
		set = (PackageComponent) parent;
		content += set.src.getName() + " => " + src.getName() + divider;

		content += set.printSpec();

		content += set.printGenTextures();

		break;
	    case VAR:
		group = ((PackageComponent) parent);
		set = ((PackageComponent) group.parent);
		content += set.src.getName() + " => " + group.src.getName() + " => " + src.getName() + divider;

		content += group.printSpec();

		content += printSpec();

		genTextures = set.getAll(Type.GENTEXTURE);
		if (genTextures.size() > 0) {
		    content += "Inherited files:";
		    for (PackageComponent gen : genTextures) {
			content += "\n    " + gen.src.getName();
		    }
		    content += divider;
		}


		content += "Exclusive files:\n";
		for (PackageComponent child : getAll(Type.TEXTURE)) {
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
//			SPGlobal.logError("PackageComponent", "Could not display " + src);
//		    }
//		}
		((PackageComponent) parent).updateHelp(help);
		return;
	    default:
		AVGUI.settingsMenu.managePackagesButton.updateHelp();
	}
	help.setContent(content);
	help.hideArrow();
    }

    public String printGenTextures() {
	String content = "";
	ArrayList<PackageComponent> genTextures = getAll(Type.GENTEXTURE);
	if (genTextures.size() > 0) {
	    content += "Shared files:\n";
	    for (PackageComponent child : getAll(Type.GENTEXTURE)) {
		content += "    " + child.src.getName() + "\n";
	    }
	}
	return content;
    }

    public void enable(boolean enable) {
	disabled = !enable;
	for (PackageComponent n : getAll()) {
	    n.enable(enable);
	}
    }

    public static ArrayList<File> toFiles(ArrayList<PackageComponent> files) throws FileNotFoundException, IOException {
	ArrayList<File> tmp = new ArrayList<File>(files.size());
	for (PackageComponent c : files) {
	    tmp.add(c.src);
	}
	return tmp;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final PackageComponent other = (PackageComponent) obj;
	if (this.src != other.src && (this.src == null || !this.src.getName().equalsIgnoreCase(other.src.getName()))) {
	    return false;
	}
	return true;
    }

    @Override
    public int hashCode() {
	return src.getName().toUpperCase().hashCode();
    }

    @Override
    public int compareTo(Object arg0) {
	PackageComponent rhs = (PackageComponent) arg0;
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
		PackageComponent node = (PackageComponent) arg0;
		return node.compareTo(arg1);
	    }
	});
	for (Object child : this.children) {
	    ((PackageComponent) child).sort();
	}
    }

    public String printSpec() {
	return "";
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