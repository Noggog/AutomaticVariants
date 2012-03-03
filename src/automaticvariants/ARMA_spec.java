/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.util.Iterator;
import skyproc.ARMA;

/**
 *
 * @author Justin Swanson
 */
public class ARMA_spec {
    ARMA arma;
    int probDiv;

    ARMA_spec(ARMA arma, VariantSpec spec) {
	this.arma = arma;
	probDiv = spec.Probability_Divider;
    }

}
