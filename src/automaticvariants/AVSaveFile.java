/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.util.Map;
import lev.gui.LSaveFile;

/**
 *
 * @author Justin Swanson
 */
public class AVSaveFile extends LSaveFile {

    @Override
    protected void init(Map m) {
	Add(m, AV.Settings.HEIGHT_ON, "Height Variants On", false, true);
	Add(m, AV.Settings.HEIGHT_STD, "Height Variants Min", false, 12);
    }

    @Override
    protected void readInSettings() {
    }

    @Override
    protected void saveToFile() {
    }

    @Override
    protected void initHelp() {
	helpInfo.put(AV.Settings.HEIGHT_ON, "This variant setup will give each actor "
		+ "that spawns a variance in its height.");
    }
}
