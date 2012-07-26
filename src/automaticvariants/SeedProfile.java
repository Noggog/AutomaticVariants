/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.util.Objects;
import skyproc.*;
import skyproc.NPC_.TemplateFlag;

/**
 *
 * @author Justin Swanson
 */
public class SeedProfile {

    NPC_ origNPC;
    NPC_ npc;
    ARMO skin;
    ARMA piece;
    RACE race;

    SeedProfile(NPC_ npc) {
	this.npc = npc;
	this.origNPC = npc;
    }

    public boolean load () {

	if (npc.isTemplated() && npc.isTemplatedToLList(TemplateFlag.USE_TRAITS) != null) {
	    SPGlobal.logError("SeedProfile", "Skipped seed " + npc + " because it was templated to a LList.");
	    return false;
	}

	int counter = 0;
	while (npc.isTemplated() && npc.get(TemplateFlag.USE_TRAITS) && counter < 25) {
	    npc = (NPC_) SPDatabase.getMajor(npc.getTemplate(), GRUP_TYPE.NPC_);
	    counter++;
	}

	if (counter == 25) {
	    SPGlobal.logError("SeedProfile", "Skipped seed " + npc + " because it entered a template loop.");
	    return false;
	}

	race = (RACE) SPDatabase.getMajor(npc.getRace(), GRUP_TYPE.RACE);
	if (race == null) {
	    SPGlobal.logError("SeedProfile", "Skipped seed " + npc + " because it had no race.");
	    return false;
	}

	skin = (ARMO) SPDatabase.getMajor(AVFileVars.getUsedSkin(npc), GRUP_TYPE.ARMO);
	if (skin == null) {
	    SPGlobal.logError("SeedProfile", "Skipped seed " + npc + " because it had no skin.");
	    return false;
	}

	for (FormID pieceForm : skin.getArmatures()) {
	    ARMA potentialPiece = (ARMA) SPDatabase.getMajor(pieceForm, GRUP_TYPE.ARMA);
	    if (potentialPiece.getRace().equals(race.getForm())) {
		piece = potentialPiece;
		break;
	    }
	}
	if (piece == null) {
	    SPGlobal.logError("SeedProfile", "Skipped seed " + npc + " because it had no skin piece.");
	    return false;
	}

	return true;
    }

    public void print() {
	SPGlobal.log(npc.getEDID(), "    Seed: " + npc);
	SPGlobal.log(npc.getEDID(), "    Orig Seed: " + origNPC);
	SPGlobal.log(npc.getEDID(), "    Race: " + race);
	SPGlobal.log(npc.getEDID(), "    Skin: " + skin);
	SPGlobal.log(npc.getEDID(), "   Piece: " + piece);
	SPGlobal.log(npc.getEDID(), "   ===================================");
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final SeedProfile other = (SeedProfile) obj;
	if (!Objects.equals(this.skin, other.skin)) {
	    return false;
	}
	if (!Objects.equals(this.piece, other.piece)) {
	    return false;
	}
	if (!Objects.equals(this.race, other.race)) {
	    return false;
	}
	return true;
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 97 * hash + Objects.hashCode(this.skin);
	hash = 97 * hash + Objects.hashCode(this.piece);
	hash = 97 * hash + Objects.hashCode(this.race);
	return hash;
    }


}
