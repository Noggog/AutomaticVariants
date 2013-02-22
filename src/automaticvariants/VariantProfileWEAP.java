/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.util.Collection;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class VariantProfileWEAP extends VariantProfile {

    SeedWEAP seed = new SeedWEAP();
    
    @Override
    public void printShort() {
	SPGlobal.log(toString(), "|   NIF: " + getNifPath());
    }

    @Override
    public String profileHashCode() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean absorb(VariantSet varSet, Collection<Seed> seeds) {
	return false;
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
    
}
