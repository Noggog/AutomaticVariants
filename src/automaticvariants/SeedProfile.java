/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.util.ArrayList;
import java.util.Objects;
import skyproc.NPC_.TemplateFlag;
import skyproc.*;

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

    SeedProfile() {
    }

    public boolean load(ArrayList<FormID> ids) {
	if (ids.size() == 1) {
	    npc = (NPC_) SPDatabase.getMajor(ids.get(0), GRUP_TYPE.NPC_);
	    origNPC = npc;
	    if (npc == null) {
		return false;
	    }
	    return loadNPC();
	} else if (ids.size() == 3) {
	    for (FormID id : ids) {
		if (race == null) {
		    race = (RACE) SPDatabase.getMajor(id, GRUP_TYPE.RACE);
		}
		if (skin == null) {
		    skin = (ARMO) SPDatabase.getMajor(id, GRUP_TYPE.ARMO);
		}
		if (piece == null) {
		    piece = (ARMA) SPDatabase.getMajor(id, GRUP_TYPE.ARMA);
		}
	    }
	    return race != null & skin != null && piece != null;
	}
	return false;
    }

    public boolean loadNPC() {

	if (npc.isTemplated() && npc.isTemplatedToLList(TemplateFlag.USE_TRAITS) != null) {
	    SPGlobal.logError("SeedProfile", "Skipped seed " + npc + " because it was templated to a LList.");
	    return false;
	}

	int counter = 0;
	while (npc != null && npc.isTemplated() && npc.get(TemplateFlag.USE_TRAITS) && counter < 25) {
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
	if (npc != null) {
	    SPGlobal.log("SeedProfile", "|   Seed: " + npc);
	    SPGlobal.log("SeedProfile", "|   Orig Seed: " + origNPC);
	}
	SPGlobal.log("SeedProfile", "|   Race: " + race);
	SPGlobal.log("SeedProfile", "|   Skin: " + skin);
	SPGlobal.log("SeedProfile", "|  Piece: " + piece);
	SPGlobal.log("SeedProfile", " \\====================================");
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
