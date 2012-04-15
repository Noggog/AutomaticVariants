/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.*;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import lev.LMergeMap;
import lev.Ln;
import skyproc.*;

/**
 *
 * @author Justin Swanson
 */
public class VariantSet extends PackageComponent implements Serializable {

    ArrayList<VariantGroup> groups;
    ArrayList<PackageComponent> commonTextures = new ArrayList<PackageComponent>(2);
    public VariantSetSpec spec;
    static String depth = "* +";
    ArrayList<Variant> flat;

    VariantSet(File setDir) {
	super(setDir, Type.VARSET);
    }

    final public boolean loadVariants() throws FileNotFoundException, IOException {
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	    SPGlobal.log(src.getName(), depth + " Adding Variant Set: " + src);
	    SPGlobal.log(src.getName(), depth + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	    SPGlobal.log(src.getName(), depth);
	}
	groups = new ArrayList<VariantGroup>();

	for (File f : src.listFiles()) {
	    if (AVFileVars.isSpec(f)) {
		try {
		    spec = AVGlobal.parser.fromJson(new FileReader(f), VariantSetSpec.class);
		    spec.file = f;
		    if (SPGlobal.logging()) {
			spec.print(src.getName());
		    }
		} catch (com.google.gson.JsonSyntaxException ex) {
		    SPGlobal.logException(ex);
		    JOptionPane.showMessageDialog(null, "Variant set " + f.getPath() + " had a bad specifications file.  Skipped.");
		} catch (FileNotFoundException ex) {
		    SPGlobal.logException(ex);
		}

	    } else if (f.isDirectory()) {
		VariantGroup v = new VariantGroup(f);
		v.load();
		groups.add(v);
		add(v);

	    } else if (AVFileVars.isDDS(f)) {
		PackageComponent c = new PackageComponent(f, Type.GENTEXTURE);
		commonTextures.add(c);
		add(c);
		if (SPGlobal.logging()) {
		    SPGlobal.log(src.getName(), depth + "   Loaded common texture: " + f);
		}

	    } else if (AVFileVars.isReroute(f)) {
		RerouteFile c = new RerouteFile(f);
		if (AVFileVars.isDDS(c.src)) {
		    c.type = PackageComponent.Type.GENTEXTURE;
		    if (SPGlobal.logging()) {
			SPGlobal.log(src.getName(), depth + "   Loaded ROUTED common texture: " + c.src);
		    }
		}
		add(c);
	    } else if (SPGlobal.logging()) {
		SPGlobal.log(src.getName(), depth + "   Skipped file: " + f);
	    }
	}

	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), depth + "");
	    SPGlobal.log(src.getName(), depth + "++++++ END Variant Set: " + src);
	}
	return true;
    }

    ArrayList<Variant> multiplyAndFlatten() {
	if (flat == null) {
	    mergeInGlobals();
	    flat = new ArrayList<Variant>();
	    if (!groups.isEmpty()) {
		flat.addAll(groups.get(0).variants);
		for (int i = 1; i < groups.size(); i++) {
		    ArrayList<Variant> tmp = new ArrayList<Variant>(flat.size() * groups.get(i).variants.size());
		    for (Variant a : flat) {
			for (Variant b : groups.get(i).variants) {
			    tmp.add(a.merge(b));
			}
		    }
		    flat = tmp;
		}
	    }
	}
	return flat;
    }

    @Override
    public void consolidateCommonFiles() throws FileNotFoundException, IOException {
	// First, check current common textures and see if any groups have them
	SPGlobal.log(src.getName(), "Consolidating common files");
	SPGlobal.flush();
	for (VariantGroup g : groups) {
	    g.deleteMatches(toFiles(commonTextures));
	}

	// Check each Variant against each other and return any files that are common to all.
	ArrayList<PackageComponent> commonFiles = new ArrayList<PackageComponent>();
	ArrayList<File> moved = new ArrayList<File>();
	for (VariantGroup g : groups) {
	    commonFiles.addAll(g.consolidateCommonFilesInternal());
	}
	if (SPGlobal.logging()) {
	    SPGlobal.log(src.getName(), "Common Files:");
	    for (PackageComponent c : commonFiles) {
		SPGlobal.log(src.getName(), "  " + c.src.getName());
	    }
	}
	for (PackageComponent c : commonFiles) {
	    File dest = new File(c.src.getParentFile().getParentFile().getParent() + "/" + c.src.getName());
	    if (!Ln.moveFile(c.src, dest, true)) {
		SPGlobal.logError(src.getName(), "!!!" + c.src + " was NOT successfully moved.");
	    }
	    moved.add(dest);
	}

	// See if any new common textures are present in other groups
	for (VariantGroup g : groups) {
	    g.deleteMatches(moved);
	}
    }

    @Override
    public LMergeMap<File, File> getDuplicateFiles() throws FileNotFoundException, IOException {
	LMergeMap<File, File> duplicates = new LMergeMap<File, File>(false);
	for (VariantGroup group : groups) {
	    for (Variant var : group.variants) {
		for (PackageComponent tex : var.textures) {
		    if (!tex.getClass().equals(RerouteFile.class)) {
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
	return duplicates;
    }

    public boolean isEmpty() {
	if (groups.isEmpty()) {
	    return true;
	}
	for (VariantGroup g : groups) {
	    if (!g.variants.isEmpty()) {
		return false;
	    }
	}
	return true;
    }

    public class VariantSetSpec implements Serializable {

	File file;
	String[][] Target_FormIDs;
	Boolean Apply_To_Similar = true;

	void print(String set) {
	    SPGlobal.log(set, depth + "   --- Set Specifications loaded: --");
	    SPGlobal.log(set, depth + "   |   Target FormIDs: ");
	    for (String[] s : Target_FormIDs) {
		SPGlobal.log(set, depth + "   |     " + s[0] + " | " + s[1]);
	    }
	    SPGlobal.log(set, depth + "   |   Apply to Similar: " + Apply_To_Similar);
	    SPGlobal.log(set, depth + "   -------------------------------------");
	}

	public String printHelpInfo() {
	    String content = "Seeds:";
	    for (String[] formID : Target_FormIDs) {
		content += "\n    ";
		FormID id = new FormID(formID[0], formID[1]);
		NPC_ npc = (NPC_) SPDatabase.getMajor(id, GRUP_TYPE.NPC_);
		if (npc != null) {
		    content += npc.getEDID() + "  |  ";
		}
		content += id.getFormStr();
	    }
	    return content;
	}
    }

    @Override
    public void finalizeComponent() {
	mergeInGlobals();
	super.finalizeComponent();
    }

    public void mergeInGlobals() {
	for (VariantGroup g : groups) {
	    g.mergeInGlobals(commonTextures);
	}
    }

    @Override
    public String printSpec() {
	if (spec != null) {
	    return spec.printHelpInfo() + divider;
	} else {
	    return "MISSING SPEC FILE!" + divider;
	}
    }
}
