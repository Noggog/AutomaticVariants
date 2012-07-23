/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import automaticvariants.PackageNode;
import automaticvariants.SpecVariant;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import lev.Ln;
import skyproc.ARMO;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class WizNewPackage {

    static boolean open = false;

    public static WizNewPackage newPackage = new WizNewPackage();
    PackageNode targetPackage;
    PackageNode targetSet;
    ArrayList<ARMO> targetSkins;
    ArrayList<File> genTextures;
    PackageNode targetGroup;
    PackageNode targetVariant;
    SpecVariant varSpec;
    ArrayList<File> varTextures;

    public void save() {
	// Variant directory
	File tmp = new File(targetVariant.src.getPath() + "\\tmp");
	Ln.makeDirs(tmp);

	// Copy common textures
	for (File from : genTextures) {
	    try {
		Ln.copyFileToDir(from, targetSet.src);
	    } catch (IOException ex) {
		SPGlobal.logException(ex);
	    }
	}

	// Copy Variant textures
	for (File from : varTextures) {
	    try {
		Ln.copyFileToDir(from, targetVariant.src);
	    } catch (IOException ex) {
		SPGlobal.logException(ex);
	    }
	}

	// Save Var Spec file
	AV.wizVarSpecPanel.save();

	
    }

    public void clear() {
	targetPackage = null;
	targetSet = null;
	targetSkins = null;
	genTextures = null;
	targetGroup = null;
	targetVariant = null;
	varTextures = null;
    }
}
