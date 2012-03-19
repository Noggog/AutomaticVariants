/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import skyproc.RACE;

/**
 *
 * @author Justin Swanson
 */
public class RACE_spec {
    RACE race;
    int probDiv;

    RACE_spec(RACE race, ARMO_spec spec) {
	this.race = race;
	probDiv = spec.probDiv;
    }

}
