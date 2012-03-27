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
	Add(m, AV.Settings.HEIGHT_MIN, "Height Variants Min", false, (float) 0);
    }

    @Override
    protected void readInSettings() {
    }

    @Override
    protected void saveToFile() {
    }
}
