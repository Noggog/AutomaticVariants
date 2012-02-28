/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import skyproc.NPC_;

/**
 *
 * @author Justin Swanson
 */
public class NPC_spec {

    NPC_ npc;
    int probDiv;

    NPC_spec (NPC_ npc, ARMO_spec spec) {
	this.npc = npc;
	probDiv = spec.probDiv;
    }
}
