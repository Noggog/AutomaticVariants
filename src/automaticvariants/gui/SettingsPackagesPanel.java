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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import lev.Ln;
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
    LButton enableButton;
    LButton disableButton;
    LButton gatherAndExit;
    LButton editSpec;
    LPanel packagePanel;
    LPanel varSetSpecPanel;
    JPopupMenu optionsMenu;
    JMenuItem enable;
    JMenuItem compress;

    public SettingsPackagesPanel(EncompassingPanel parent_) {
	super("Texture Variants", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    defaults.setVisible(false);
	    enableButton = new LButton("Enable", defaults.getSize(), defaults.getLocation());
	    enableButton.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    enableSelection(true);
		}
	    });

	    save.setVisible(false);
	    disableButton = new LButton("Disable", save.getSize(), save.getLocation());
	    disableButton.addActionListener(new ActionListener() {

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


	    editSpec = new LButton("Edit Specs", save.getSize());
	    editSpec.setLocation(AVGUI.middleDimensions.x / 2 - editSpec.getWidth() / 2, disableButton.getY() - editSpec.getHeight() - 15);
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
	    packagePanel.Add(enableButton);
	    packagePanel.Add(disableButton);
	    packagePanel.Add(tree);
	    gatherAndExit.centerIn(packagePanel, defaults.getY() - gatherAndExit.getHeight() - 15);
	    packagePanel.Add(gatherAndExit);
	    Add(packagePanel);


	    varSetSpecPanel = new LPanel(AVGUI.middleDimensions);
	    varSetSpecPanel.setLocation(0, 0);
	    Add(varSetSpecPanel);

	    optionsMenu = new JPopupMenu();
	    enable = new JMenuItem("Enable");
	    enable.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    enableSelection(enable.getText().equals("Enable"));
		}
	    });
	    optionsMenu.add(enable);
	    compress = new JMenuItem("Compress");
	    compress.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    compress();
		}
	    });
	    optionsMenu.add(compress);

	    //Add popup listener
	    MouseAdapter ma = new MouseAdapter() {

		private void myPopupEvent(MouseEvent e) {
		    int x = e.getX();
		    int y = e.getY();
		    JTree tree = (JTree) e.getSource();
		    TreePath path = tree.getPathForLocation(x, y);
		    if (path == null) {
			return;
		    }

		    tree.setSelectionPath(path);
		    PackageComponent sel = (PackageComponent) path.getLastPathComponent();

		    compress.setVisible(sel.type == PackageComponent.Type.PACKAGE
			    || sel.type == PackageComponent.Type.VARSET
			    || sel.type == PackageComponent.Type.ROOT);

		    if (sel.disabled) {
			enable.setText("Enable");
		    } else {
			enable.setText("Disable");
		    }

		    optionsMenu.show(tree, x + 10, y);
		}

		@Override
		public void mousePressed(MouseEvent e) {
		    if (e.isPopupTrigger()) {
			myPopupEvent(e);
		    }
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		    if (e.isPopupTrigger()) {
			myPopupEvent(e);
		    }
		}
	    };
	    tree.addMouseListener(ma);

	    //	    reader = new DDSreader();
	    //	    display = new LImagePane();
	    //	    display.setMaxSize(AVGUI.rightDimensions.width - 50, 0);
	    //	    display.setVisible(true);

	    //	    parent.helpPanel.addToBottomArea(display);
	    //	    display.setVisible(true);

	    try {
		loadPackageList();
	    } catch (FileNotFoundException ex) {
		SPGlobal.logException(ex);
	    } catch (IOException ex) {
		SPGlobal.logException(ex);
	    }

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

    public void compress() {
	try {
	    PackageComponent p = (PackageComponent) tree.getSelectionPaths()[0].getLastPathComponent();
	    long before = p.fileSize();
	    if (SPGlobal.logging()) {
		SPGlobal.log(p.src.getName(), "Current file size: " + before + "->" + Ln.toMB(before) + "MB");
	    }

	    p.consolidateCommonFiles();
	    loadPackageList();
	    PackageComponent.rerouteFiles(p.getDuplicateFiles());
	    loadPackageList();

	    long after = p.fileSize();
	    if (SPGlobal.logging()) {
		SPGlobal.log(p.src.getName(), "After file size: " + after + "->" + Ln.toMB(after) + "MB");
		SPGlobal.log(p.src.getName(), "Reduced size by: " + (before - after) + "->" + Ln.toMB(before - after) + "MB");
	    }
	    JOptionPane.showMessageDialog(null, "<html>"
		    + "Compressed: " + p.src.getPath() + "<br>"
		    + "  Starting size: " + Ln.toMB(before) + " MB<br>"
		    + "    Ending size: " + Ln.toMB(after) + " MB<br>"
		    + "    Saved space: " + Ln.toMB(before - after) + " MB"
		    + "</html>"
		    );
	} catch (FileNotFoundException ex) {
	    SPGlobal.logException(ex);
	} catch (IOException ex) {
	    SPGlobal.logException(ex);
	}
    }

    public void editSpec() {
    }

    public void loadPackageList() throws FileNotFoundException, IOException {

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
