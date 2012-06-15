/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author Justin Swanson
 */
class SpecPackage extends SpecFile {

    String packager;
    String version;

    SpecPackage(File f) {
	super(f);
    }

    @Override
    void printToLog(String header) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String printHelpInfo() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    ArrayList<String> print() {
	throw new UnsupportedOperationException("Not supported yet.");
    }
}
