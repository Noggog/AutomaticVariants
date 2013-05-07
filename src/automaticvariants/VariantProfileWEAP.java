/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVFileVars.WEAP_spec;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import skyproc.*;
import skyproc.exceptions.BadMod;
import skyproc.exceptions.MissingMaster;

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
	for (VariantSet varSet : matchedVariantSets) {
	    if (SPGlobal.logging()) {
		SPGlobal.log(toString(), " *************> Generating set " + varSet.printName("-"));
	    }
	    ArrayList<Variant> vars = varSet.multiplyAndFlatten(getGlobalMeshes());
	    for (Variant var : vars) {
		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " ***************> Generating var " + var.printName("-"));
		}
		String varEDID = NiftyFunc.EDIDtrimmer(generateEDID(var));
		for (WEAP weapon : matchedWeapons) {
		    String edid = varEDID + weapon.getEDID() + "_weap";
		    if (SPGlobal.logging()) {
			SPGlobal.log(toString(), " * ==> Generating WEAP: " + edid);
		    }
		    WEAP weaponDup = getWeapon(edid, weapon, var);
		    setStats(weaponDup, var);
		    STAT stat = new STAT(varEDID + "_stat");
		    weaponDup.setFirstPersonModel(stat.getForm());

		    // Set Third Person
		    String nifPath = getNifPath(var, false);
		    weaponDup.setModelFilename(getCleanNifPath(nifPath));
		    //Generate and set alt textures
		    Map<String, TXST> txsts = generateTXSTs(var, nifPath);
		    if (!txsts.isEmpty()) {
			loadAltTextures(weaponDup.getAltTextures(), txsts, nifPath);
		    } else if (!var.isTemplated()) {
			SPGlobal.logError(toString(), " * Skipped because no TXSTs were generated and it was not templated.");
			continue;
		    }

		    // Set First Person
		    String firstPersonNifPath = getNifPath(var, true);
		    stat.getModelData().setFileName(getCleanNifPath(nifPath));
		    if (!firstPersonNifPath.equals(nifPath)) {
			txsts = generateTXSTs(var, firstPersonNifPath);
			if (!txsts.isEmpty()) {
			    loadAltTextures(stat.getModelData().getAltTextures(), txsts, firstPersonNifPath);
			} else if (!var.isTemplated()) {
			    SPGlobal.logError(toString(), " * Skipped because no TXSTs were generated and it was not templated.");
			    continue;
			}
		    }

		    VariantFactoryWEAP.weapons.put(weapon, new WEAP_spec(weaponDup, var.spec));

		    if (SPGlobal.logging()) {
			SPGlobal.log(toString(), " ******************************>");
			SPGlobal.log(toString(), "");
		    }
		}
	    }
	}
    }

    WEAP getWeapon(String edid, WEAP seed, Variant var) {
	if (var.isTemplated()) {
	    FormID templateF = new FormID(var.spec.Template_Form);
	    WEAP template = (WEAP) SPDatabase.getMajor(templateF, GRUP_TYPE.WEAP);
	    if (template == null) {
		AVFileVars.importTemplateMod(templateF.getMaster());
		template = (WEAP) SPDatabase.getMajor(templateF, GRUP_TYPE.WEAP);
	    }
	    if (template != null) {
		WEAP copy = (WEAP) SPGlobal.getGlobalPatch().makeCopy(template, edid);
		ArrayList<MajorRecord> copies = NiftyFunc.deepCopySubRecords(copy, templateF.getMaster());
		if (SPGlobal.logging()) {
		    SPGlobal.log(toString(), " * Copied template: " + template);
		    for (MajorRecord m : copies) {
			SPGlobal.log(toString(), " *   Deep Copied : " + m);
		    }
		}
		return copy;
	    }
	}
	return (WEAP) SPGlobal.getGlobalPatch().makeCopy(seed, edid);
    }

    void setStats(WEAP weap, Variant var) {
	if (!"".equals(var.spec.Name_Set)) {
	    weap.setName(var.spec.Name_Set);
	}
	if (!"".equals(var.spec.Name_Prefix)) {
	    weap.setName(var.spec.Name_Prefix + " " + weap.getName());
	}
	if (!"".equals(var.spec.Name_Affix)) {
	    weap.setName(weap.getName() + " " + var.spec.Name_Affix);
	}
	AVNum speed = AVNum.factory(var.spec.Speed);
	if (speed.modified()) {
	    weap.setSpeed(speed.value(weap.getSpeed()));
	}
	AVNum gold = AVNum.factory(var.spec.Gold_Value);
	if (gold.modified()) {
	    weap.setValue((int) gold.value(weap.getValue()));
	}
	AVNum enchantment = AVNum.factory(var.spec.Enchantment);
	if (enchantment.modified()) {
	    weap.setEnchantmentCharge((int) enchantment.value(weap.getEnchantmentCharge()));
	}
	if (var.spec.Enchantment_Form.length() >= 6) {
	    try {
		FormID enchantmentID = new FormID(var.spec.Enchantment_Form);
		weap.setEnchantment(enchantmentID);
	    } catch (Exception e) {
		SPGlobal.logException(e);
	    }
	}
	AVNum weight = AVNum.factory(var.spec.Weight);
	if (weight.modified()) {
	    weap.setWeight(weight.value(weap.getWeight()));
	}
	AVNum reach = AVNum.factory(var.spec.Reach);
	if (reach.modified()) {
	    weap.setReach(reach.value(weap.getReach()));
	}
	AVNum damage = AVNum.factory(var.spec.Damage);
	if (damage.modified()) {
	    weap.setDamage((int) damage.value(weap.getDamage()));
	}
	AVNum crit = AVNum.factory(var.spec.Crit);
	if (crit.modified()) {
	    weap.setCritMult(crit.value(weap.getCritMult()));
	}
	AVNum critDamage = AVNum.factory(var.spec.Crit_Damage);
	if (critDamage.modified()) {
	    weap.setCritDamage((int) critDamage.value(weap.getCritDamage()));
	}
	AVNum stagger = AVNum.factory(var.spec.Stagger);
	if (stagger.modified()) {
	    weap.setStagger(stagger.value(weap.getStagger()));
	}
	AVNum rangeMin = AVNum.factory(var.spec.Range_Min);
	if (rangeMin.modified()) {
	    weap.setMinRange(rangeMin.value(weap.getMinRange()));
	}
	AVNum rangeMax = AVNum.factory(var.spec.Range_Max);
	if (rangeMax.modified()) {
	    weap.setMaxRange(rangeMax.value(weap.getMaxRange()));
	}
	AVNum numProj = AVNum.factory(var.spec.Num_Proj);
	if (numProj.modified()) {
	    weap.setNumProjectiles((int) numProj.value(weap.getNumProjectiles()));
	}
    }

    @Override
    public String getNif(FormID id, boolean firstPerson) {
	WEAP template = (WEAP) SPDatabase.getMajor(id, GRUP_TYPE.WEAP);
	if (template == null) {
	    AVFileVars.importTemplateMod(id.getMaster());
	    template = (WEAP) SPDatabase.getMajor(id, GRUP_TYPE.WEAP);
	}
	if (template != null) {
	    if (firstPerson) {
		STAT stat = (STAT) SPDatabase.getMajor(template.getFirstPersonModel(), GRUP_TYPE.STAT);
		if (stat != null) {
		    return "MESHES\\" + stat.getModelData().getFileName();
		}
	    } else {
		return "MESHES\\" + template.getModelData().getFileName();
	    }
	}
	return "";
    }
}
