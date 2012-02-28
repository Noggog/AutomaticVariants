/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import java.io.File;
import java.util.ArrayList;
import skyproc.TXST;

/**
 *
 * @author Justin Swanson
 */
public class Variant {

    String name;
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

    void setName(File file) {
        String[] tmp = file.getPath().split("\\\\");
        name = "AV_" + tmp[tmp.length - 3].replaceAll(" ", "") + "_" + tmp[tmp.length - 2].replaceAll(" ", "") + "_" + tmp[tmp.length - 1].replaceAll(" ", "");
    }

    boolean isEmpty() {
        return variantTexturePaths.isEmpty();
    }
}
