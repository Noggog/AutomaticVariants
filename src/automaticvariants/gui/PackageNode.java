/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import lev.gui.LSwingTreeNode;

/**
 *
 * @author Justin Swanson
 */
public class PackageNode extends LSwingTreeNode implements Comparable {

    File src;
    boolean disabled = false;

    PackageNode(File f) {
	src = f;
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
	Collections.sort(this.children, new Comparator (){

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
}
