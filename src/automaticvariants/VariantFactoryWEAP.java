/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.SpecVariantSet.VariantType;
import skyproc.*;

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
	    WEAP templateTop = getTemplateTop(weapon);
	    if (templateTop == null) {
		continue;
	    }
	    String nifPath = templateTop.getModelFilename();
	    if (!"".equals(nifPath)) {
		Seed test = new SeedWEAP(nifPath);
		if (test.isValid()) {
		    VariantProfileWEAP profile = find(test);
		    if (profile == null) {
			profile = new VariantProfileWEAP(templateTop);
			profiles.add(profile);
		    }
		    profile.addWeapon(weapon);
		}
	    }
	}
    }
    
    public WEAP getTemplateTop(WEAP in) {
	int counter = 0;
	while (in != null && !in.getTemplate().isNull()) {
	    // Circular safeguard
	    if (counter++ > 25) {
		return null;
	    }
	    in = (WEAP) SPDatabase.getMajor(in.getTemplate(), GRUP_TYPE.WEAP);
	}
	return in;
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
