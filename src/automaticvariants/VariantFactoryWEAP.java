/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVFileVars.WEAP_spec;
import automaticvariants.SpecVariantSet.VariantType;
import java.util.HashMap;
import java.util.Map;
import lev.LMergeMap;
import skyproc.*;

/**
 *
 * @author Justin Swanson
 */
public class VariantFactoryWEAP extends VariantFactory<VariantProfileWEAP> {

    static LMergeMap<WEAP, WEAP_spec> weapons = new LMergeMap<>(false);
    static Map<WEAP, LVLI> llists = new HashMap<>();

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
    public void implementOriginalAsVar() {
	for (WEAP weapSrc : weapons.keySet()) {
	    weapons.put(weapSrc, new WEAP_spec(weapSrc));
	}
    }

    @Override
    public void createStructureRecords(Mod source) {
	generateLLists();

	subIn(source);
    }

    public void subIn(Mod source) {
	for (WEAP weap : weapons.keySet()) {
	    LVLI replacement = llists.get(weap);

	    // Replace in existing LLists
	    for (LVLI existingList : source.getLeveledItems()) {
		existingList.replace(weap, replacement);
		SPGlobal.getGlobalPatch().addRecord(existingList);
	    }

	    // Replace in NPC inventories
	    for (NPC_ npc : source.getNPCs()) {
		for (SubFormInt item : npc.getItems()) {
		    if (item.getForm().equals(weap.getForm())) {
			item.setForm(replacement.getForm());
			SPGlobal.getGlobalPatch().addRecord(npc);
		    }
		}
	    }
	}
    }

    public void generateLLists() {
	for (WEAP weapSrc : weapons.keySet()) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "  Generating for " + weapSrc);
	    }
	    LVLI list = new LVLI(weapSrc.getEDID() + "_llist");
	    llists.put(weapSrc, list);
	    int lcm = calcLCM(weapons.get(weapSrc));
	    for (WEAP_spec weapNew : weapons.get(weapSrc)) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(header, "    Generating " + (lcm / weapNew.spec.Probability_Divider) + " entries for " + weapNew.weap);
		}
		for (int i = 0; i < lcm / weapNew.spec.Probability_Divider; i++) {
		    list.addEntry(weapNew.weap.getForm(), 1, 1);
		}
	    }
	}
    }

    @Override
    public VariantType getType() {
	return VariantType.WEAP;
    }
}
