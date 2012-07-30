/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.VariantProfile;
import java.util.Objects;

/**
 *
 * @author Justin Swanson
 */
public class ProfileDisplay implements Comparable {

    public VariantProfile profile;
    String edid;

    ProfileDisplay(VariantProfile profile) {
	this.profile = profile;
	edid = profile.race.getEDID() + " | "
		+ profile.skin.getEDID() + " | "
		+ profile.piece.getEDID();
    }

    ProfileDisplay(VariantProfile profile, String edid) {
	this.profile = profile;
	this.edid = edid;
    }

    @Override
    public String toString() {
	return edid;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final ProfileDisplay other = (ProfileDisplay) obj;
	if (!Objects.equals(this.edid, other.edid)) {
	    return false;
	}
	return true;
    }

    @Override
    public int hashCode() {
	int hash = 7;
	hash = 67 * hash + Objects.hashCode(this.edid);
	return hash;
    }

    @Override
    public int compareTo(Object o) {
	return edid.compareTo(((ProfileDisplay) o).edid);
    }
}
