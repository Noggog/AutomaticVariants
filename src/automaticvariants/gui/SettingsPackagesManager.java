/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AVSaveFile.Settings;
import automaticvariants.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import lev.Ln;
import lev.gui.LButton;
import lev.gui.LImagePane;
import lev.gui.LMenuItem;
import skyproc.SPGlobal;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class SettingsPackagesManager extends SPSettingPanel {

    public static PackageTree tree;
//    static DDSreader reader;
    LImagePane display;
    LButton enableButton;
    LButton disableButton;
    LButton otherSettings;
    JPopupMenu optionsMenu;
    LMenuItem enable;
    LMenuItem disable;
    LMenuItem compress;
    LMenuItem editSpec;

    public SettingsPackagesManager(SPMainMenuPanel parent_) {
	super("Texture Variants", parent_, AV.orange, AV.save);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    tree = new PackageTree(SUMGUI.middleDimensions.width - 30,
		    SUMGUI.middleDimensions.height - 165, SUMGUI.helpPanel);
	    tree.setLocation(SUMGUI.middleDimensions.width / 2 - tree.getWidth() / 2, last.y + 10);
	    tree.setMargin(10, 5);
	    tree.removeBorder();
	    Add(tree);


	    defaults.setVisible(false);
	    enableButton = new LButton("Enable", defaults.getSize(), defaults.getLocation());
	    enableButton.setLocation(enableButton.getX(), tree.getY() + tree.getHeight() + 15);
	    enableButton.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    enableSelection(true);
		}
	    });
	    enableButton.linkTo(Settings.PACKAGES_ENABLE, saveFile, SUMGUI.helpPanel, true);
	    enableButton.setFollowPosition(false);
	    Add(enableButton);


	    save.setVisible(false);
	    disableButton = new LButton("Disable", save.getSize(), save.getLocation());
	    disableButton.setLocation(disableButton.getX(), tree.getY() + tree.getHeight() + 15);
	    disableButton.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    enableSelection(false);
		}
	    });
	    disableButton.linkTo(Settings.PACKAGES_DISABLE, saveFile, SUMGUI.helpPanel, true);
	    disableButton.setFollowPosition(false);
	    Add(disableButton);


	    otherSettings = new LButton("Other Settings");
	    otherSettings.centerIn(settingsPanel, defaults.getY());
	    Add(otherSettings);


	    optionsMenu = new JPopupMenu();
	    enable = new LMenuItem("Enable");
	    enable.linkTo(Settings.PACKAGES_ENABLE, saveFile, SUMGUI.helpPanel, true);
	    enable.setFollowPosition(false);
	    enable.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    enableSelection(true);
		}
	    });
	    optionsMenu.add(enable.getItem());

	    disable = new LMenuItem("Disable");
	    disable.linkTo(Settings.PACKAGES_DISABLE, saveFile, SUMGUI.helpPanel, true);
	    disable.setFollowPosition(false);
	    disable.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    enableSelection(false);
		}
	    });
	    optionsMenu.add(disable.getItem());

	    compress = new LMenuItem("Compress");
	    compress.linkTo(Settings.PACKAGES_COMPRESS, saveFile, SUMGUI.helpPanel, true);
	    compress.setFollowPosition(false);
	    compress.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    compress();
		}
	    });
	    optionsMenu.add(compress.getItem());


	    editSpec = new LMenuItem("Edit Specs");
	    editSpec.linkTo(Settings.PACKAGES_EDIT, saveFile, SUMGUI.helpPanel, true);
	    editSpec.setFollowPosition(false);
	    editSpec.addActionListener(new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
		    editSpec();
		}
	    });
	    optionsMenu.add(editSpec.getItem());


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
		    PackageNode sel = (PackageNode) path.getLastPathComponent();

		    compress.setVisible(sel.type == PackageNode.Type.PACKAGE
			    || sel.type == PackageNode.Type.VARSET
			    || sel.type == PackageNode.Type.ROOT);

		    editSpec.setVisible(
			    sel.type == PackageNode.Type.VAR //			    || sel.type == PackageComponent.Type.VARSET
			    //			    || sel.type == PackageComponent.Type.PACKAGE
			    );

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

	    display = new LImagePane();
	    display.setMaxSize(SUMGUI.rightDimensions.width, 0);
	    display.allowAlpha(false);
	    display.setVisible(true);
	    PackageNode.display = display;

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
    public void specialOpen(SPMainMenuPanel parent) {
	SUMGUI.helpPanel.clearBottomArea();
	SUMGUI.helpPanel.addToBottomArea(display);
	SUMGUI.helpPanel.setBottomAreaHeight(SUMGUI.rightDimensions.width);

	otherSettings.addActionListener(AV.packagesOtherPanel.getOpenHandler());
    }

    public void enableSelection(boolean enable) {
	SUMGUI.setPatchNeeded(true);
	TreePath[] paths = tree.getSelectionPaths();
	for (TreePath p : paths) {
	    ((PackageNode) p.getLastPathComponent()).enable(enable);
	}

	// adjust folders to enable/disable based on their contents
	for (TreePath p : paths) {
	    for (int i = p.getPathCount() - 1; i >= 0; i--) {
		PackageNode node = (PackageNode) p.getPathComponent(i);
		if (node.src.isDirectory()) {
		    if (node.disabled) {
			for (PackageNode child : node.getAll()) {
			    if (!child.disabled) {
				node.disabled = false;
				break;
			    }
			}
		    } else {
			boolean allDisabled = true;
			for (PackageNode child : node.getAll()) {
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

    public PackageNode getSelectedComponent() {
	return (PackageNode) tree.getSelectionPaths()[0].getLastPathComponent();
    }

    public void compress() {
	try {
	    // Enable selecton and move files
	    int row = tree.getLeadSelectionRow();
	    PackageNode p = getSelectedComponent();
	    enableSelection(true);
	    p.moveNode();
	    loadPackageList();

	    // Select the same node
	    p = (PackageNode) tree.getPathForRow(row).getLastPathComponent();

	    // Compress
	    long before = p.fileSize();
	    if (SPGlobal.logging()) {
		SPGlobal.log(p.src.getName(), "Current file size: " + before + "->" + Ln.toMB(before) + "MB");
	    }

	    p.consolidateCommonFiles();
	    loadPackageList();
	    PackageNode.rerouteFiles(p.getDuplicateFiles());
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
		    + "</html>");
	} catch (FileNotFoundException ex) {
	    SPGlobal.logException(ex);
	} catch (IOException ex) {
	    SPGlobal.logException(ex);
	}
    }

    public void editSpec() {
	PackageNode p = getSelectedComponent();
	switch (p.type) {
	    case VAR:
		AV.packagesVariantPanel.open();
		Variant v = ((Variant) p);
		AV.packagesVariantPanel.load(v.printName(), v.spec);
		break;
	    case VARSET:
		AV.packagesVariantSetPanel.open();
		VariantSet vs = ((VariantSet) p);
		AV.packagesVariantSetPanel.load(vs.printName(), vs.spec);
		break;
	    case PACKAGE:

		break;
	}
    }

    public void loadPackageList() throws FileNotFoundException, IOException {

	boolean logging = SPGlobal.loggingAsync();
	SPGlobal.loggingAsync(false);

	File inactivePackages = new File(AVFileVars.inactiveAVPackagesDir);
	if (inactivePackages.isDirectory()) {
	    for (File packageFolder : inactivePackages.listFiles()) {
		if (packageFolder.isDirectory()) {
		    AVPackage avPackage = new AVPackage(packageFolder);
		    AVFileVars.AVPackages.mergeIn(avPackage);
		}
	    }
	}

	SPGlobal.loggingAsync(logging);

	AVFileVars.AVPackages.sort();
	tree.setModel(new DefaultTreeModel(AVFileVars.AVPackages));
    }
}
