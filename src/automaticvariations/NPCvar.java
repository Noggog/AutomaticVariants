/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import java.io.File;
import java.util.ArrayList;
import skyproc.FormID;
import skyproc.GRUP_TYPE;
import skyproc.Mod;
import skyproc.NPC_;
import skyproc.SPDatabase;
import skyproc.SPGlobal;
import skyproc.exceptions.NotFound;
import skyproc.exceptions.Uninitialized;

/**
 *
 * @author Justin Swanson
 */
public class NPCvar extends VariantSet {

    NPC_ src;
    ArrayList<File> textureVariants = new ArrayList<File>();

    NPCvar() {
    }

    void linkToSrc(String ID) throws Uninitialized, NotFound {
        ID.trim();
        String IDstr = ID.substring(0, 6);
        String master;
        if (ID.indexOf("~~~") != -1) {
            master = ID.substring(6, ID.indexOf("~~~"));
        } else {
            master = ID.substring(6);
        }
        master.trim();

        src = (NPC_) SPDatabase.getMajor(new FormID(IDstr, master), GRUP_TYPE.NPC_);
        if (src == null) {
            throw new NotFound("NPC " + ID + " did not exist in database");
        } else {
            SPGlobal.log(ID, "Linked to: " + src);
        }

//        race = (RACE) SPDatabase.getMajor(src.getRace(), GRUP_TYPE.RACE);
//        if (race != null) {
//            SPGlobal.log(ID, "  Linked to race: " + race);
//            wornArmor = (ARMO) SPDatabase.getMajor(race.getWornArmor(), GRUP_TYPE.ARMO);
//            if (wornArmor != null) {
//                SPGlobal.log(ID, "  Linked to worn armor: " + wornArmor);
//                for (SubForm f : wornArmor.getModels()) {
//                    ARMA armature = (ARMA) SPDatabase.getMajor(f.getForm(), GRUP_TYPE.ARMA);
//                    if (armature != null) {
//                        SPGlobal.log(ID, "    Linked to armature: " + armature);
//                        wornArmorArmatures.add(armature);
//                    }
//                }
//            }
//        }
    }

}
