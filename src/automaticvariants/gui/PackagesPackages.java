/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import lev.gui.LTextPane;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;

/**
 *
 * @author Justin Swanson
 */
public class PackagesPackages extends SPSettingPanel {
    LTextPane question;

    public PackagesPackages(SPMainMenuPanel parent_) {
	super(parent_, "Choose Package", AV.orange, AV.save);
    }


}
