/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.util.ArrayList;
import lev.Ln;
import skyproc.ARMA;
import skyproc.ARMO;
import skyproc.RACE;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class VariantProfile {

    public static ArrayList<VariantProfile> profiles = new ArrayList<>();
    RACE race;
    ARMO skin;
    ARMA piece;
    String nifPath;
    ArrayList<String> textures;
    ArrayList<VariantSet> sets;
    int ID;
    static int nextID = 0;

    VariantProfile() {
	ID = nextID++;
	profiles.add(this);
    }

    VariantProfile(VariantProfile rhs) {
	this();
	race = rhs.race;
	skin = rhs.skin;
	piece = rhs.piece;
	nifPath = rhs.nifPath;
	textures = rhs.textures;
	sets = rhs.sets;
    }

    public static VariantProfile find(RACE race, ARMO skin, ARMA piece, String nifPath, ArrayList<String> textures) {
	for (int i = 0; i < profiles.size(); i++) {
	    if (profiles.get(i).is(race, skin, piece, nifPath, textures)) {
		return profiles.get(i);
	    }
	}
	return null;
    }

    public static void printProfiles() {
	SPGlobal.log("Print", "===========================================================");
	SPGlobal.log("Print", "=============      Printing all Profiles     ==============");
	SPGlobal.log("Print", "===========================================================");
	for (VariantProfile v : profiles) {
	    String id = v.toString();
	    SPGlobal.log(id, "==============================");
	    SPGlobal.log(id, "NIF: " + v.nifPath);
	    for (String s : v.textures) {
		SPGlobal.log(id, "    " + s);
	    }
	    SPGlobal.log(id, "Race: " + v.race);
	    SPGlobal.log(id, "Skin: " + v.skin);
	    SPGlobal.log(id, "Piece: " + v.piece);
	}
    }

    @Override
    public String toString() {
	return "ID: " + ID;
    }

    public boolean is(RACE race, ARMO skin, ARMA piece, String nifPath, ArrayList<String> textures) {
	if (race != null && race != this.race) {
	    return false;
	}
	if (skin != null && skin != this.skin) {
	    return false;
	}
	if (piece != null && piece != this.piece) {
	    return false;
	}
	if (nifPath != null && !nifPath.equalsIgnoreCase(this.nifPath)) {
	    return false;
	}
	if (textures != null) {
	    if (!Ln.equals(this.textures, textures, false)) {
		return false;
	    }
	}
	return true;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final VariantProfile other = (VariantProfile) obj;
	if (this.ID != other.ID) {
	    return false;
	}
	return true;
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 11 * hash + this.ID;
	return hash;
    }
}
