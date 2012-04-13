/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.*;
import java.util.ArrayList;
import lev.LMergeMap;
import lev.Ln;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class AVPackage extends PackageComponent {

    ArrayList<VariantSet> sets = new ArrayList<VariantSet>();
    PackageSpec spec;
    static String depth = "";

    public AVPackage(File packageFolder) throws FileNotFoundException, IOException {
	super(packageFolder, Type.PACKAGE);
	loadSets();
    }

    ArrayList<Variant> flatten() {
	ArrayList<Variant> out = new ArrayList<Variant>();
	for (VariantSet s : sets) {
	    out.addAll(s.multiplyAndFlatten());
	}
	return out;
    }

    public void consolidateCommonFiles() throws FileNotFoundException, IOException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), "==============================================");
	    SPGlobal.log(src.getName(), "Consolidating Common Files " + src);
	    SPGlobal.log(src.getName(), "==============================================");
	    SPGlobal.flush();
	}
	for (VariantSet set : sets) {
	    set.consolidateCommonFiles();
	}
    }

    public void rerouteFiles() throws FileNotFoundException, IOException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), "==============================================");
	    SPGlobal.log(src.getName(), "Creating File Shortcuts " + src);
	    SPGlobal.log(src.getName(), "==============================================");
	    SPGlobal.flush();
	}
	LMergeMap<File, File> duplicates = new LMergeMap<File, File>(false);
	for (VariantSet set : sets) {
	    for (VariantGroup group : set.groups) {
		for (Variant var : group.variants) {
		    for (PackageComponent tex : var.textures) {
			boolean found = false;
			for (File key : duplicates.keySet()) {
			    if (Ln.validateCompare(tex.src, key, 0)) {
				if (SPGlobal.logging()) {
				    SPGlobal.log(src.getName(), "  " + tex.src);
				    SPGlobal.log(src.getName(), "  was the same as ");
				    SPGlobal.log(src.getName(), "  " + key);
				}
				duplicates.put(key, tex.src);
				found = true;
				break;
			    }
			}
			if (!found) {
			    if (SPGlobal.logging()) {
				SPGlobal.log(src.getName(), "  UNIQUE: " + tex.src);
			    }
			    duplicates.put(tex.src, tex.src);
			}
			SPGlobal.log(src.getName(), "  --------------------------");
		    }
		}
	    }
	}

	// Route duplicates to first on the list
	for (File key : duplicates.keySet()) {
	    ArrayList<File> values = duplicates.get(key);
	    if (!values.isEmpty()) {
		File prototype = values.get(0);
		for (int i = 1; i < values.size(); i++) {
		    createRerouteFile(values.get(i), prototype);
		}
	    }
	}
    }

    public File createRerouteFile(File from, File to) throws IOException {
	File reroute = new File(from.getPath() + ".reroute");
	if (!from.delete()) {
	    SPGlobal.logError(src.getName(), "Could not delete routed file " + from);
	}
	if (reroute.isFile()) {
	    reroute.delete();
	}
	BufferedWriter out = new BufferedWriter(new FileWriter(reroute));
	out.write(to.getPath());
	out.close();
	return reroute;
    }

    final public void loadSets() throws FileNotFoundException, IOException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "************************************************************");
	    SPGlobal.log(src.getName(), depth + "* Loading package: " + src);
	    SPGlobal.log(src.getName(), depth + "************************************************************");
	}
	for (File f : src.listFiles()) {
	    if (f.isDirectory()) {
		VariantSet set = new VariantSet(f);
		if (set.loadVariants()) {
		    sets.add(set);
		    add(set);
		}
	    } else if (AVFileVars.isSpec(f)) {
		spec = new PackageSpec(f);
	    }
	}
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "*** END package: " + src);
	}
    }

    class PackageSpec {

	File file;

	PackageSpec(File f) {
	}
    }
}
