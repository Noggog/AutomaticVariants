/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.util.ArrayList;
import skyproc.SPGlobal;
import skyproc.WEAP;

/**
 *
 * @author Justin Swanson
 */
public class VariantProfileWEAP extends VariantProfile {

    SeedWEAP seed = new SeedWEAP();
    private ArrayList<WEAP> matchedWeapons = new ArrayList<>();

    public VariantProfileWEAP(WEAP in) {
	seed.setNifPath(in.getModelFilename());
    }
    
    public void addWeapon(WEAP in) {
	matchedWeapons.add(in);
    }

    @Override
    public void print() {
	super.print();
	SPGlobal.log(toString(), "    Matched weapons: ");
	for (WEAP weapon : matchedWeapons) {
	    SPGlobal.log(toString(), "      " + weapon.toString());
	}
	SPGlobal.log(toString(), "");
	SPGlobal.log(toString(), "");
    }

    @Override
    public void printShort() {
	SPGlobal.log(toString(), "|   NIF: " + getNifPath());
    }

    @Override
    public String profileHashCode() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean is(String nifPath) {
	return seed.getNifPath().equalsIgnoreCase(nifPath);
    }

    @Override
    public String getNifPath() {
	return seed.getNifPath();
    }

    public void setNifPath(String in) {
    }

    @Override
    public Seed getSeed() {
	return seed;
    }

    @Override
    public void generateRecords() {
    }
}
