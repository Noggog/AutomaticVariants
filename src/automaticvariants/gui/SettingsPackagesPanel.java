/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import lev.Ln;
import lev.gui.LButton;
import lev.gui.LImagePane;
import lev.gui.LPanel;

/**
 *
 * @author Justin Swanson
 */
public class SettingsPackagesPanel extends DefaultsPanel {

    static PackageTree tree;
//    static DDSreader reader;
    LImagePane display;
    LButton enable;
    LButton disable;
    LButton editSpec;
    LPanel packagePanel;
    
    
    LPanel varSetSpecPanel;

    public SettingsPackagesPanel(EncompassingPanel parent_) {
	super("Texture Variants", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    defaults.setVisible(false);
	    enable = new LButton("Enable", defaults.getSize(), defaults.getLocation());
	    enable.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    enableSelection(true);
		}
	    });

	    save.setVisible(false);
	    disable = new LButton("Disable", save.getSize(), save.getLocation());
	    disable.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    enableSelection(false);
		}
	    });

	    editSpec = new LButton("Edit Specs", save.getSize());
	    editSpec.setLocation(AVGUI.middleDimensions.x / 2 - editSpec.getWidth() / 2, disable.getY() - editSpec.getHeight() - 15);
	    editSpec.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    editSpec();
		}
	    });

	    tree = new PackageTree(AVGUI.middleDimensions.width - 30,
		    AVGUI.middleDimensions.height - 200);
	    tree.setLocation(AVGUI.middleDimensions.width / 2 - tree.getWidth() / 2, last.y + 10);
	    tree.setMargin(10, 5);
	    tree.removeBorder();

	    packagePanel = new LPanel(AVGUI.middleDimensions);
	    packagePanel.setLocation(0, 0);
	    packagePanel.Add(enable);
	    packagePanel.Add(disable);
	    packagePanel.Add(tree);
	    packagePanel.Add(editSpec);
	    Add(packagePanel);

	    
	    varSetSpecPanel = new LPanel(AVGUI.middleDimensions);
	    varSetSpecPanel.setLocation(0, 0);
	    Add(varSetSpecPanel);
	    
//	    reader = new DDSreader();
//	    display = new LImagePane();
//	    display.setMaxSize(AVGUI.rightDimensions.width - 50, 0);
//	    display.setVisible(true);

//	    parent.helpPanel.addToBottomArea(display);
//	    parent.helpPanel.setBottomAreaHeight(250);

	    loadPackageList();

	    return true;
	}
	return false;
    }

    @Override
    public void specialOpen(EncompassingPanel parent) {
//	parent.helpPanel.addToBottomArea(display);
	parent.helpPanel.setBottomAreaHeight(AVGUI.rightDimensions.width - 50);
    }

    public void enableSelection(boolean enable) {
	TreePath[] paths = tree.getSelectionPaths();
	for (TreePath p : paths) {
	    enable((PackageNode) p.getLastPathComponent(), enable);
	}
	tree.repaint();
    }
    
    public void enable (PackageNode node, boolean enable) {
	node.disabled = !enable;
	for (PackageNode n : node.getAll()) {
	    enable(n, enable);
	}
    }

    public boolean enableB(PackageNode node) {
	boolean proper = true;
	if (node.disabled && node.src.exists()) {
	    File folder = null;
	    if (node.src.isFile()) {
		String path = node.src.getPath();
		File dest = new File(AVFileVars.AVPackages + "\\" + path.substring(path.indexOf("\\") + 1));
		proper = proper && Ln.moveFile(node.src, dest, true);
		folder = node.src.getParentFile();

	    } else if (node.src.isDirectory()) {
		folder = node.src;
		for (PackageNode n : node.getAll()) {
		    proper = proper && enableB(n);
		}
	    }

	    // Move all spec files to active
	    if (folder.exists()) {
		for (File f : folder.listFiles()) {
		    if (f.getPath().toUpperCase().endsWith(".JSON")) {
			String path = f.getPath();
			File dest = new File(AVFileVars.AVPackages + "\\" + path.substring(path.indexOf("\\") + 1));
 			proper = proper && Ln.moveFile(f, dest, true);
		    }
		}
	    }
	}
	return proper;
    }

    public boolean disableB(PackageNode node) {
	boolean proper = true;
	if (!node.disabled && node.src.exists()) {
	    File folder = null;

	    if (node.src.isFile()) {
		String path = node.src.getPath();
		File dest = new File(AVFileVars.inactiveAVPackages + "\\" + path.substring(path.indexOf("\\") + 1));
		proper = proper && Ln.moveFile(node.src, dest, true);
		folder = node.src.getParentFile();

	    } else if (node.src.isDirectory()) {
		folder = node.src;
		for (PackageNode n : node.getAll()) {
		    proper = proper && disableB(n);
		}
	    }

	    // If only non-displaying files left, then move to inactive
	    if (folder.exists() && folder.listFiles().length == 1) {
		File f = folder.listFiles()[0];
		if (f.getPath().toUpperCase().endsWith(".JSON")) {
		    String path = f.getPath();
		    File dest = new File(AVFileVars.inactiveAVPackages + "\\" + path.substring(path.indexOf("\\") + 1));
		    proper = proper && Ln.moveFile(f, dest, true);
		}
	    }
	}
	return proper;
    }

    public void editSpec() {

    }

    public void loadPackageList() {
	PackageNode AVNode = new PackageNode(AVFileVars.AVPackages, parent.helpPanel);
	loadPackageList(AVFileVars.AVPackages, AVNode, false);
	loadPackageList(AVFileVars.inactiveAVPackages, AVNode, true);
	AVNode.sort();
	tree.setModel(new DefaultTreeModel(AVNode));
    }

    public void loadPackageList(File dir, PackageNode root, boolean disabled) {
	if (!dir.exists()) {
	    return;
	}

	for (File packageDir : dir.listFiles()) {
	    if (packageDir.isDirectory()) {
		PackageNode packageNode = findAndReplace(root, packageDir, disabled);
		packageNode.type = PackageNode.Type.PACKAGE;
		for (File set : packageDir.listFiles()) {
		    if (set.isDirectory()) {
			PackageNode setNode = findAndReplace(packageNode, set, disabled);
			setNode.type = PackageNode.Type.VARSET;
			for (File var : set.listFiles()) {
			    PackageNode varNode = findAndReplace(setNode, var, disabled);
			    if (var.isDirectory()) {
				varNode.type = PackageNode.Type.VAR;
				for (File f : var.listFiles()) {
				    if (f.getName().toUpperCase().endsWith(".DDS")) {
					PackageNode fileNode = new PackageNode(f, parent.helpPanel);
					fileNode.type = PackageNode.Type.TEXTURE;
					fileNode.disabled = varNode.disabled;
					varNode.add(fileNode);
				    } else if (var.getName().toUpperCase().endsWith(".JSON")) {
					varNode.spec = f;
				    }
				}
				setNode.add(varNode);
			    } else if (var.getName().toUpperCase().endsWith(".DDS")) {
				varNode.type = PackageNode.Type.GENTEXTURE;
				setNode.add(varNode);
			    } else if (var.getName().toUpperCase().endsWith(".JSON")) {
				setNode.spec = var;
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
	PackageNode newPackage = new PackageNode(f, parent.help);
	newPackage.display = display;
	PackageNode old = parent.get(newPackage);
	if (old != null) {
	    return old;
	} else {
	    newPackage.disabled = disabled;
	    newPackage.disabledOrig = disabled;
	    return newPackage;
	}
    }
}
