/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVariants;
import java.io.File;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author Justin Swanson
 */
public class SettingsPackagesPanel extends DefaultsPanel {

    PackageTree tree;

    public SettingsPackagesPanel(EncompassingPanel parent_) {
	super("Texture Variants", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    tree = new PackageTree(AVGUI.middleDimensions.width - 30,
		    AVGUI.middleDimensions.height - 200);
	    tree.setLocation(AVGUI.middleDimensions.x
		    + AVGUI.middleDimensions.width / 2 - tree.getWidth() / 2, 110);
	    tree.setMargin(10, 5);
	    tree.removeBorder();
	    add(tree);

	    refreshPackageList();

	    return true;
	}
	return false;
    }

    public void refreshPackageList() {
	PackageNode AVNode = new PackageNode(AVFileVariants.AVPackages);
	refreshPackageList(AVFileVariants.AVPackages, AVNode, false);
	refreshPackageList(AVFileVariants.inactiveAVPackages, AVNode, true);
	AVNode.sort();
	tree.setModel(new DefaultTreeModel(AVNode));
    }

    public void refreshPackageList(File dir, PackageNode root, boolean disabled) {
	if (!dir.exists()) {
	    return;
	}

	for (File packageDir : dir.listFiles()) {
	    if (packageDir.isDirectory()) {
		PackageNode packageNode = findAndReplace(root, packageDir, disabled);
		for (File set : packageDir.listFiles()) {
		    if (set.isDirectory()) {
			PackageNode setNode = findAndReplace(packageNode, set, disabled);
			for (File var : set.listFiles()) {
			    if (!var.getName().toUpperCase().endsWith(".JSON")) {
				PackageNode varNode = findAndReplace(setNode, var, disabled);
				if (var.isDirectory()) {
				    for (File f : var.listFiles()) {
					if (!f.getName().toUpperCase().endsWith(".JSON")) {
					    PackageNode fileNode = new PackageNode(f);
					    varNode.add(fileNode);
					}
				    }
				}
				setNode.add(varNode);
			    }
			}
			packageNode.add(setNode);
		    }
		}
		root.add(packageNode);
	    }
	}
    }

    public PackageNode findAndReplace(PackageNode parent, File f, boolean disabled) {
	PackageNode newPackage = new PackageNode(f);
	PackageNode old = parent.get(newPackage);
	if (old != null) {
	    return old;
	} else {
	    newPackage.disabled = disabled;
	    return newPackage;
	}
    }
}
