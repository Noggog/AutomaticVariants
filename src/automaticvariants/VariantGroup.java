/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import lev.Ln;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class VariantGroup extends PackageComponent {

    ArrayList<Variant> variants = new ArrayList<Variant>();
    static String depth = "* +   ";

    VariantGroup(File groupDir) {
	super(groupDir, Type.VARGROUP);
    }

    public void load() throws FileNotFoundException, IOException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "### Adding Variant Group: " + src);
	}
	for (File f : src.listFiles()) {
	    if (f.isDirectory()) {
		Variant v = new Variant(f);
		v.load();
		variants.add(v);
		add(v);
	    }
	}
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "####################################");
	}
    }

    public void mergeInGlobals(ArrayList<PackageComponent> globalFiles) {
	for (Variant v : variants) {
	    v.mergeInGlobals(globalFiles);
	}
    }

    public void deleteMatches(ArrayList<File> files) throws FileNotFoundException, IOException {
	for (File common : files) {
	    for (Variant v : variants) {
		for (PackageComponent c : v.textures) {
		    if (common.getName().equalsIgnoreCase(c.src.getName())) {
			if (SPGlobal.logging()) {
			    SPGlobal.log(src.getName(), "  ------------------------------");
			    SPGlobal.log(src.getName(), "  Comparing");
			    SPGlobal.log("Consoldiate", "  " + common);
			    SPGlobal.log(src.getName(), "    and");
			    SPGlobal.log("Consoldiate", "  " + c.src);
			    SPGlobal.flush();
			}
			if (Ln.validateCompare(common, c.src, 0)) {
			    if (isReroute()) {
				c.src.delete();
			    }
			    if (SPGlobal.logging()) {
				SPGlobal.log(src.getName(), "  Deleted " + c + " because it was a common file.");
			    }
			}
		    }
		}
	    }
	}
    }

    public ArrayList<PackageComponent> consolidateCommonFilesInternal() throws FileNotFoundException, IOException {
	ArrayList<PackageComponent> out = new ArrayList<PackageComponent>();
	if (variants.size() > 1) {
	    Variant first = variants.get(0);
	    // For each texture in the first variant
	    for (PackageComponent tex : first.textures) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(src.getName(), "  ---------------");
		    SPGlobal.log(src.getName(), "  CHECKING " + tex.src);
		    SPGlobal.log(src.getName(), "  ---------------");
		}
		boolean textureCommon = true;
		ArrayList<PackageComponent> delete = new ArrayList<PackageComponent>();
		// Check each other variant's textures.
		for (int i = 1; i < variants.size(); i++) {
		    boolean variantContained = false;
		    for (PackageComponent texRhs : variants.get(i).textures) {
			if (SPGlobal.logging()) {
			    SPGlobal.log(src.getName(), "    ------------------------------");
			    SPGlobal.log(src.getName(), "    Comparing");
			    SPGlobal.log("Consoldiate", "    " + tex.src);
			    SPGlobal.log(src.getName(), "      and");
			    SPGlobal.log("Consoldiate", "    " + texRhs.src);
			    SPGlobal.flush();
			}
			// If other variant had texture, move on to next and
			// mark that texture for deletion
			if (tex.src.getName().equalsIgnoreCase(texRhs.src.getName())
				&& Ln.validateCompare(tex.src, texRhs.src, 0)) {
			    SPGlobal.log(src.getName(), "      Matched");
			    delete.add(texRhs);
			    variantContained = true;
			    break;
			} else {
			    SPGlobal.log(src.getName(), "      DID NOT match.");
			}
		    }
		    // If one variant in group did not have texture, then
		    // it is not a common texture
		    if (!variantContained) {
			SPGlobal.log(src.getName(), "  == Was NOT a common texture: " + tex.src);
			textureCommon = false;
			break;
		    }
		}
		// If common texture, return it and delete
		// the duplicate textures
		if (textureCommon) {
		    SPGlobal.log(src.getName(), "  == WAS a common texture: " + tex.src);
		    out.add(tex);
		    for (PackageComponent p : delete) {
			if (!p.isReroute() && p.src.delete()) {
			    SPGlobal.log(src.getName(), "  " + p.src + " was deleted.");
			} else {
			    SPGlobal.logError(src.getName(), "  !!!" + p.src + " was NOT successfully deleted.");
			}
		    }
		}
	    }
	}
	return out;
    }
    
    @Override
    public String printName() {
	PackageComponent p = (PackageComponent) this.getParent();
	return p.printName() + " - " + src.getName();
    }
}
