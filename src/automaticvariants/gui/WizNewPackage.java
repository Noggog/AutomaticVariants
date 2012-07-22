/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.PackageNode;
import java.io.File;
import java.util.ArrayList;
import skyproc.ARMO;

/**
 *
 * @author Justin Swanson
 */
public class WizNewPackage {

    public static WizNewPackage newPackage;

    PackageNode targetPackage;
    PackageNode targetSet;
    ArrayList<ARMO> targetSkins;
    ArrayList<File> genTextures;
    PackageNode targetGroup;
}
