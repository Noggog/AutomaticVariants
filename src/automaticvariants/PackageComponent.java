/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.gui.AVGUI;
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

    public File src;
    public boolean disabled = false;
    public boolean disabledOrig = false;
    public Type type;

    public PackageComponent(File source, Type type) {
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

    @Override
    public String toString() {
	return src.getName();
    }

    public void updateHelp(LHelpPanel help) {

	String content = "";
	if (disabled) {
	    content += "DISABLED - ";
	}
	ArrayList<PackageComponent> genTextures;
	help.setBottomAreaVisible(false);
	switch (type) {
	    case PACKAGE:
		help.setSetting(src.getName());
		break;
	    case VARSET:
		help.setSetting(((PackageComponent) parent).src.getName());
		content += src.getName() + divider;

		content += printSpec();

		genTextures = getAll(Type.GENTEXTURE);
		if (genTextures.size() > 0) {
		    content += "Shared files:\n";
		    for (PackageComponent child : getAll(Type.GENTEXTURE)) {
			content += "    " + child.src.getName() + "\n";
		    }
		}
		break;
	    case GENTEXTURE:
		((PackageComponent) parent).updateHelp(help);
		return;
	    case VAR:
		PackageComponent p = ((PackageComponent) parent);
		content += p.src.getName() + " => " + src.getName() + divider;

		content += p.printSpec();

		content += printSpec();

		genTextures = p.getAll(Type.GENTEXTURE);
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
