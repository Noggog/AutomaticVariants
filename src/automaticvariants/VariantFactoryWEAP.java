/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.SpecVariantSet.VariantType;
import skyproc.FormID;
import skyproc.Mod;
import skyproc.SPGlobal;
import skyproc.WEAP;

/**
 *
 * @author Justin Swanson
 */
public class VariantFactoryWEAP extends VariantFactory<VariantProfileWEAP> {

    @Override
    public void locateUnused() {
    }

    @Override
    public void createProfileShells() {
	SPGlobal.log(header, "====================================================================");
	SPGlobal.log(header, "===================      Creating WEAP Profiles     =================");
	SPGlobal.log(header, "====================================================================");
	for (WEAP weapon : AV.getMerger().getWeapons()) {
	    if (weapon.getTemplate().equals(FormID.NULL)) {
		String nifPath = weapon.getModelFilename();
		if (!"".equals(nifPath)) {
		    Seed test = new SeedWEAP(nifPath);
		    if (test.isValid()
			    && find(test) == null) {
			VariantProfileWEAP profile = new VariantProfileWEAP();
			profiles.add(profile);
			profile.seed.setNifPath(weapon.getModelFilename());
		    }
		}
	    }
	}
    }

    @Override
    public void loadProfileRecords() {
    }

    @Override
    boolean isUnused(FormID id) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void createVariantRecords(Mod source) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void implementOriginalAsVar() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void createStructureRecords(Mod source) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VariantType getType() {
	return VariantType.WEAP;
    }
}
