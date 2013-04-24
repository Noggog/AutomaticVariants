/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVFileVars.WEAP_spec;
import automaticvariants.SpecVariantSet.VariantType;
import java.util.ArrayList;
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
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder() + "Substitute In.txt");
	    SPGlobal.debugStream = false;
	}
	for (WEAP weap : weapons.keySet()) {
	    LVLI replacement = llists.get(weap);
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "Replacing " + weap + " with " + replacement);
	    }

	    // Replace in existing LLists
	    for (LVLI existingList : source.getLeveledItems()) {
		int num = existingList.replace(weap, replacement);
		if (num > 0) {
		    SPGlobal.getGlobalPatch().addRecord(existingList);
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Replaced " + num + " times in " + existingList);
		    }
		}
	    }

	    // Replace in containers
	    for (CONT cont : source.getContainers()) {
		int num = cont.replace(weap, replacement);
		if (num > 0) {
		    SPGlobal.getGlobalPatch().addRecord(cont);
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Replaced " + num + " times in " + cont);
		    }
		}
	    }

	    // Replace in form lists
	    ArrayList<FormID> replaceList = new ArrayList<>(replacement.getEntries().size());
	    for (LeveledEntry e : replacement.getEntries()) {
		replaceList.add(e.getForm());
	    }
	    FormID[] replaceArray = replaceList.toArray(new FormID[0]);
	    for (FLST flst : source.getFormLists()) {
		int result = NiftyFunc.replaceAll(flst.getFormIDEntries(), weap.getForm(), replaceArray);
		if (result > 0) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Replaced " + result + " times in " + flst);
		    }
		    SPGlobal.getGlobalPatch().addRecord(flst);
		}
	    }

	    // Replace in NPC inventories
	    for (NPC_ npc : source.getNPCs()) {
		int num = 0;
		ArrayList<ItemListing> items = npc.getItems();
		for (ItemListing item : items) {
		    if (item.getForm().equals(weap.getForm())) {
			item.setForm(replacement.getForm());
			num++;
		    }
		}
		if (num > 0) {
		    SPGlobal.getGlobalPatch().addRecord(npc);
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Replaced " + num + " times in " + npc);
		    }
		}
	    }
	}
	SPGlobal.debugStream = true;
    }

    public void generateLLists() {
	if (SPGlobal.logging()) {
	    SPGlobal.newLog(debugFolder() + "Generate LLists.txt");
	}
	for (WEAP weapSrc : weapons.keySet()) {
	    LVLI list = new LVLI(weapSrc.getEDID() + "_llist");
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "Generating for " + weapSrc);
		SPGlobal.log(header, "  Generating " + list);
	    }
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

    @Override
    public String debugName() {
	return "Weapon";
    }
}
