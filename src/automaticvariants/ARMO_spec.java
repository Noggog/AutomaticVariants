/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import skyproc.ARMA;
import skyproc.ARMO;

/**
 *
 * @author Justin Swanson
 */
public class ARMO_spec {

    ARMO armo;
    int probDiv;
    ARMA targetArma;

    ARMO_spec (ARMO armo, ARMA_spec spec) {
	this.armo = armo;
	targetArma = spec.arma;
	probDiv = spec.probDiv;
    }
}
