/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.util.ArrayList;

/**
 *
 * @author Justin Swanson
 */
public class VariantSet {

    String[][] Target_FormIDs;
    Boolean Apply_To_Similar = true;
    ArrayList<Variant> variants = new ArrayList<Variant>();

    boolean isEmpty() {
        return variants.isEmpty();
    }
}
