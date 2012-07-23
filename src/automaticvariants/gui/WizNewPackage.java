/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.PackageNode;
import java.io.File;
import java.util.ArrayList;
import lev.Ln;
import skyproc.ARMO;

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
    ArrayList<File> varTextures;

    public void save() {
	File tmp = new File(targetVariant.src.getPath() + "\\tmp");
	Ln.makeDirs(tmp);
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
