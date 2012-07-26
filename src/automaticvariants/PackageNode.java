/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import ddsutil.DDSUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import javax.swing.SwingUtilities;
import lev.LMergeMap;
import lev.Ln;
import lev.gui.LHelpPanel;
import lev.gui.LImagePane;
import lev.gui.LSwingTreeNode;
import skyproc.SPGlobal;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class PackageNode extends LSwingTreeNode implements Comparable {

    static File lastDisplayed;
    static String divider = "\n\n";
    public static LImagePane display;
    public File src;
    public boolean disabled = false;
    public boolean disabledOrig = false;
    public Type type;
    static int tmp = 0;

    public PackageNode(File source, Type type) {
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
    public PackageNode get(LSwingTreeNode node) {
	return (PackageNode) super.get(node);
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

	if (dest != null && !this.getClass().equals(RerouteFile.class)) {
	    File destFile = new File(dest + src.getPath().substring(src.getPath().indexOf("\\") + 1));
	    pass = Ln.moveFile(src, destFile, false);
	    src = destFile;
	}

	for (PackageNode c : getAll()) {
	    pass = pass && c.moveOut();
	}

	return pass;
    }

    public ArrayList<PackageNode> getAll(Type type) {
	ArrayList<PackageNode> out = new ArrayList<PackageNode>();
	if (children != null) {
	    for (Object o : children) {
		PackageNode child = (PackageNode) o;
		if (child.type == type) {
		    out.add(child);
		}
	    }
	}
	return out;
    }

    public ArrayList<PackageNode> getAll() {
	return getAll(false);
    }

    ArrayList<PackageNode> getAll(boolean recursive) {
	ArrayList<PackageNode> out = new ArrayList<PackageNode>();
	if (children != null) {
	    for (Object o : children) {
		PackageNode child = (PackageNode) o;
		out.add(child);
		if (recursive) {
		    out.addAll(child.getAll(recursive));
		}
	    }
	}
	return out;
    }

    public ArrayList<PackageNode> flattenChildren() {
	return getAll(true);
    }

    @Override
    public String toString() {
	return src.getName();
    }

    public boolean moveNode() throws IOException {
	ArrayList<PackageNode> nodeList = getAll(true);

	// Move node files
	Set<File> toMove = new HashSet<>();
	for (PackageNode p : nodeList) {
	    if (p.disabled != p.disabledOrig) {
		if (p.src.isFile()) {
		    toMove.add(p.src);
		} else if (p.src.isDirectory()) {
		    toMove.addAll(Ln.generateFileList(p.src, 0, 0, false));
		}
	    }
	}

	boolean proper = true;
	for (File f : toMove) {
	    proper = proper && moveFile(f);
	}

//	boolean proper = true;
//	if (disabled != disabledOrig) {
//	    if (src.isDirectory()) {
//		for (File f : src.listFiles()) {
//		    if (f.getPath().toUpperCase().endsWith(".JSON")) {
////		    if (f.isFile()
////			    && !Ln.isFileType(f, "DDS")) {
//			proper = proper && moveFile(f);
//		    }
//		}
//	    }
//	    else
////	    else if (src.isFile())
//	    {
//		proper = proper && moveFile(src);
//	    }
//	}
//	for (PackageNode p : getAll()) {
//	    proper = proper && p.moveNode();
//	}
//
	return proper;
    }

    public boolean moveFile(File src) throws IOException {
	if (!src.isFile()) {
	    return false;
	}
	String prefix;
	if (src.getPath().contains(AVFileVars.inactiveAVPackagesDir)) {
	    prefix = AVFileVars.AVPackagesDir;
	} else {
	    prefix = AVFileVars.inactiveAVPackagesDir;
	}
	File dest = new File(prefix + src.getPath().substring(src.getPath().indexOf("\\")));
	// Check to see if any enabled reroute file was referring to this node
	if (RerouteFile.reroutes.containsKey(src)) {
	    for (RerouteFile r : RerouteFile.reroutes.get(src)) {
		// If a single reroute file is left enabled
		// Swap real texture with reroutes to keep real texture in enabled set
		if (!r.disabled) {
		    boolean passed = true;
		    dest = new File(dest.getPath() + ".reroute");
		    File newPrototype = new File(r.routeFile.getPath().substring(0, r.routeFile.getPath().indexOf(".reroute")));
		    for (RerouteFile r2 : RerouteFile.reroutes.get(src)) {
			r2.changeRouteTo(newPrototype);
		    }
		    passed = passed && Ln.moveFile(r.routeFile, dest, true);
		    return passed && Ln.moveFile(src, newPrototype, true);
		}
	    }
	}
	// else
	return Ln.moveFile(src, dest, true);
    }

    public void pruneDisabled() {
	if (children != null) {
	    ArrayList<Object> remove = new ArrayList<Object>();
	    for (Object o : children) {
		PackageNode p = (PackageNode) o;
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
	for (PackageNode c : getAll()) {
	    c.finalizeComponent();
	}
    }

    public void mergeIn(PackageNode rhs) {
	for (PackageNode c : getAll()) {
	    if (c.equals(rhs)) {
		for (PackageNode rhsChild : rhs.getAll()) {
		    c.mergeIn(rhsChild);
		}
		return;
	    }
	}
	// else
	add(rhs);
    }

    public boolean isReroute() {
	return false;
    }

    public void updateHelp(LHelpPanel help) {


	String content = "";
	PackageNode packageNode;
	PackageNode set;
	PackageNode group;
	if (disabled) {
	    content += "DISABLED - ";
	}
	ArrayList<PackageNode> genTextures;
	switch (type) {
	    case PACKAGE:
		help.setTitle(src.getName());
		displayFirstImage();
		break;
	    case GENTEXTURE:
		((PackageNode) parent).updateHelp(help);
		displayImage(src);
		return;
	    case VARSET:
		help.setTitle(((PackageNode) parent).src.getName());
//		content += src.getName() + divider;

		content += printSpec();

		content += printGenTextures();

		displayFirstImage();
		break;
	    case VARGROUP:
		set = (PackageNode) parent;
		packageNode = ((PackageNode) set.parent);
		help.setTitle(packageNode.src.getName());
//		content += set.src.getName() + " => " + src.getName() + divider;

		content += set.printSpec();

		content += set.printGenTextures();

		displayFirstImage();
		break;
	    case VAR:
		group = ((PackageNode) parent);
		set = ((PackageNode) group.parent);
		packageNode = ((PackageNode) set.parent);
		help.setTitle(packageNode.src.getName());
//		content += set.src.getName() + " => " + group.src.getName() + " => " + src.getName() + divider;

		content += set.printSpec();

		content += printSpec();

		genTextures = set.getAll(Type.GENTEXTURE);
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

		displayFirstImage();
		break;
	    case TEXTURE:
		((PackageNode) parent).updateHelp(help);
		displayImage(src);
		return;
	    default:
		AV.packageManagerConfig.updateHelp();
		SUMGUI.helpPanel.setBottomAreaVisible(false);
	}
	help.setContent(content);
	help.hideArrow();
    }

    void displayFirstImage() {
	ArrayList<PackageNode> flat = flattenChildren();
	for (PackageNode p : flat) {
	    if (Ln.isFileType(p.src, "DDS")
		    && !p.src.getPath().contains("_n")
		    && !p.src.getPath().contains("_g")) {
		displayImage(p.src);
		return;
	    }
	}
	for (PackageNode p : flat) {
	    if (Ln.isFileType(p.src, "DDS")) {
		displayImage(p.src);
		return;
	    }
	}
	SUMGUI.helpPanel.setBottomAreaVisible(false);
    }

    void displayImage(final File src) {
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		if (!src.equals(lastDisplayed)) {
		    SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, "TEST", "Displaying " + src.getPath());
		    try {
			BufferedImage image = DDSUtil.read(src);
			SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, "TEST", "Image is " + image);
			display.setImage(image);
			display.setLocation(SUMGUI.helpPanel.getBottomSize().width / 2 - display.getWidth() / 2, display.getY());
			lastDisplayed = src;
		    } catch (Exception ex) {
			SPGlobal.logSpecial(AV.SpecialLogs.WARNINGS, "TEST", "FAILED");
			SPGlobal.logException(ex);
			SPGlobal.logError("PackageComponent", "Could not display " + src);
		    }
		}
		SUMGUI.helpPanel.setBottomAreaVisible(true);
	    }
	});
    }

    public String printGenTextures() {
	String content = "";
	ArrayList<PackageNode> genTextures = getAll(Type.GENTEXTURE);
	if (genTextures.size() > 0) {
	    content += "Shared files:\n";
	    for (PackageNode child : getAll(Type.GENTEXTURE)) {
		content += "    " + child.src.getName() + "\n";
	    }
	}
	return content;
    }

    public void enable(boolean enable) {
	disabled = !enable;
	for (PackageNode n : getAll()) {
	    n.enable(enable);
	}
    }

    public static ArrayList<File> toFiles(ArrayList<PackageNode> files) throws FileNotFoundException, IOException {
	ArrayList<File> tmp = new ArrayList<File>(files.size());
	for (PackageNode c : files) {
	    tmp.add(c.src);
	}
	return tmp;
    }

    public long fileSize() {
	long out = src.length();
	for (PackageNode p : getAll()) {
	    out += p.fileSize();
	}
	return out;
    }

    public void consolidateCommonFiles() throws FileNotFoundException, IOException {
	if (type == Type.ROOT) {
	    for (PackageNode c : getAll(Type.PACKAGE)) {
		c.consolidateCommonFiles();
	    }
	}
    }

    public LMergeMap<File, File> getDuplicateFiles() throws FileNotFoundException, IOException {
	LMergeMap<File, File> duplicates = new LMergeMap<File, File>(false);
	if (type == Type.ROOT) {
	    for (PackageNode c : getAll(Type.PACKAGE)) {
		duplicates.addAll(c.getDuplicateFiles());
	    }
	}
	return duplicates;
    }

    public static void rerouteFiles(LMergeMap<File, File> duplicates) throws IOException {

	// Route duplicates to first on the list
	for (File key : duplicates.keySet()) {
	    ArrayList<File> values = duplicates.get(key);
	    if (!values.isEmpty()) {
		File prototype = values.get(0);
		for (int i = 1; i < values.size(); i++) {
		    RerouteFile.createRerouteFile(values.get(i), prototype);
		}
	    }
	}
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

    public String printSpec() {
	return "";
    }

    public String printName(String spacer) {
	return src.getName();
    }

    public enum Type {

	DEFAULT,
	ROOT,
	PACKAGE,
	VARSET,
	VARGROUP,
	VAR,
	TEXTURE,
	NIF,
	GENTEXTURE,
	REROUTE;
    }
}
