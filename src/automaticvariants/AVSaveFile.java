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
	Add(m, AV.Settings.HEIGHT_MIN, "Height Variants Min", false, 0);
	Add(m, AV.Settings.HEIGHT_MAX, "Height Variants Max", false, 98);
	Add(m, AV.Settings.HEIGHT_WIDTH, "Height Variants Width", false, 5);
	Add(m, AV.Settings.HEIGHT_INTENSITY, "Height Variants Intensity", false, 9);
    }

    @Override
    protected void readInSettings() {
    }

    @Override
    protected void saveToFile() {
    }
}
