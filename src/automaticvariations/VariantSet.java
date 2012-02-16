/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import java.util.ArrayList;

/**
 *
 * @author Justin Swanson
 */
public class VariantSet {

    VariantType Type = VariantType.NULL;
    ArrayList<String> Target_FormIDs = new ArrayList<String>();
    Boolean Apply_To_Similar = true;

    ArrayList<Variant> variants = new ArrayList<Variant>();

    boolean isEmpty() {
        return variants.isEmpty();
    }

    enum VariantType {
        NPC,
        NULL
    }
}
