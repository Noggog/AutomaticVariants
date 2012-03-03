/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.File;
import java.util.ArrayList;
import skyproc.TXST;

/**
 *
 * @author Justin Swanson
 */
public class Variant {

    String name = "";
    ArrayList<String> variantTexturePaths = new ArrayList<String>();
    TextureVariant[] textureVariants;
    static int numSupportedTextures = 8;
    VariantSpec specs = new VariantSpec();

    static class TextureVariant {

	String nifFieldName;
	TXST textureRecord;

	TextureVariant(TXST txst, String name) {
	    textureRecord = txst;
	    nifFieldName = name;
	}
    }

    void setName(File file, int places) {
	String[] tmp = file.getPath().split("\\\\");
	for (int i = 1; i <= places; i++) {
	    name = "_" + tmp[tmp.length - i].replaceAll(" ", "") + name;
	}
	name = "AV" + name;
    }

    boolean isEmpty() {
	return variantTexturePaths.isEmpty();
    }
}
