/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import automaticvariants.AVFileVars.WEAP_spec;
import java.util.ArrayList;
import java.util.Map;
import skyproc.*;

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
		    WEAP weaponDup = (WEAP) SPGlobal.getGlobalPatch().makeCopy(weapon, edid);
		    STAT stat = new STAT(varEDID + "_stat");
		    weaponDup.setFirstPersonModel(stat.getForm());
		    // Set nif
		    String nifPath = getNifPath(var);
		    String cleanNifPath = nifPath;
		    if (cleanNifPath.indexOf("MESHES\\") == 0) {
			cleanNifPath = cleanNifPath.substring(7);
		    }
		    weaponDup.setModelFilename(cleanNifPath);
		    stat.getModelData().setFileName(cleanNifPath);

		    //Generate and set alt textures
		    Map<String, TXST> txsts = generateTXSTs(var, nifPath);
		    if (txsts.isEmpty()) {
			SPGlobal.logError(toString(), " * Skipped because no TXSTs were generated");
			continue;
		    }
		    loadAltTextures(weaponDup.getAltTextures(), txsts, nifPath);
		    loadAltTextures(stat.getModelData().getAltTextures(), txsts, nifPath);

		    VariantFactoryWEAP.weapons.put(weapon, new WEAP_spec(weaponDup, var.spec));

		    if (SPGlobal.logging()) {
			SPGlobal.log(toString(), " ******************************>");
			SPGlobal.log(toString(), "");
		    }
		}
	    }
	}
    }
}
