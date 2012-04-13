/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.AVFileVars;
import automaticvariants.AVPackage;
import automaticvariants.PackageComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import lev.gui.LButton;
import lev.gui.LImagePane;
import lev.gui.LPanel;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class SettingsPackagesPanel extends DefaultsPanel {

    public static PackageTree tree;
//    static DDSreader reader;
    LImagePane display;
    LButton enable;
    LButton disable;
    LButton gatherAndExit;
    LButton compress;
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

	    gatherAndExit = new LButton("Gather Files and Exit");
	    gatherAndExit.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    AVFileVars.gatherFiles();
		    AV.exitProgram();
		}
	    });


	    compress = new LButton("Compress");
	    compress.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    for (PackageComponent p : AVFileVars.AVPackages.getAll(PackageComponent.Type.PACKAGE)) {
			try {
			    ((AVPackage)p).compress();
			} catch (FileNotFoundException ex) {
			    SPGlobal.logException(ex);
			} catch (IOException ex) {
			    SPGlobal.logException(ex);
			}
		    }
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
		    AVGUI.middleDimensions.height - 200, parent.helpPanel);
	    tree.setLocation(AVGUI.middleDimensions.width / 2 - tree.getWidth() / 2, last.y + 10);
	    tree.setMargin(10, 5);
	    tree.removeBorder();

	    packagePanel = new LPanel(AVGUI.middleDimensions);
	    packagePanel.setLocation(0, 0);
	    packagePanel.Add(enable);
	    packagePanel.Add(disable);
	    packagePanel.Add(tree);
	    gatherAndExit.centerIn(packagePanel, defaults.getY() - gatherAndExit.getHeight() - 15);
	    packagePanel.Add(gatherAndExit);
	    compress.centerIn(packagePanel, gatherAndExit.getY() - compress.getHeight() - 15);
	    packagePanel.Add(compress);
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
	    ((PackageComponent) p.getLastPathComponent()).enable(enable);
	}

	// adjust folders to enable/disable based on their contents
	for (TreePath p : paths) {
	    for (int i = p.getPathCount() - 1; i >= 0; i--) {
		PackageComponent node = (PackageComponent) p.getPathComponent(i);
		if (node.src.isDirectory()) {
		    if (node.disabled) {
			for (PackageComponent child : node.getAll()) {
			    if (!child.disabled) {
				node.disabled = false;
				break;
			    }
			}
		    } else {
			boolean allDisabled = true;
			for (PackageComponent child : node.getAll()) {
			    if (!child.disabled) {
				allDisabled = false;
				break;
			    }
			}
			if (allDisabled) {
			    node.disabled = true;
			}
		    }
		}
	    }
	}
	tree.repaint();
    }

    public void editSpec() {
    }

    public void loadPackageList() {

	boolean logging = SPGlobal.logging();
	SPGlobal.logging(false);

	AVFileVars.importVariants();

	File inactivePackages = new File(AVFileVars.inactiveAVPackagesDir);
	if (inactivePackages.isDirectory()) {
	    for (File packageFolder : inactivePackages.listFiles()) {
		if (packageFolder.isDirectory()) {
		    AVPackage avPackage = new AVPackage(packageFolder);
		    AVFileVars.AVPackages.mergeIn(avPackage);
		}
	    }
	}

	SPGlobal.logging(logging);

	AVFileVars.AVPackages.sort();
	tree.setModel(new DefaultTreeModel(AVFileVars.AVPackages));
    }
}
