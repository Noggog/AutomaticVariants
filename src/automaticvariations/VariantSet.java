/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import java.util.ArrayList;
import skyproc.FormID;
import skyproc.GRUP_TYPE;
import skyproc.MajorRecord;
import skyproc.SPDatabase;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class VariantSet {

    ArrayList<String> Target_FormIDs = new ArrayList<String>();
    Boolean Apply_To_Similar = true;
    ArrayList<Variant> variants = new ArrayList<Variant>();

    boolean isEmpty() {
        return variants.isEmpty();
    }
}
