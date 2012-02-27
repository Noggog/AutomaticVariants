/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import automaticvariations.AV_Nif.TextureField;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import skyproc.SPGlobal;
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

    void generateVariant(ArrayList<TextureField> texturePack) throws IOException {

        // Find out which TXSTs need to be generated
        String[][] replacements = new String[texturePack.size()][numSupportedTextures];
        boolean[] needed = new boolean[texturePack.size()];
        for (String s : variantTexturePaths) {
            String fileName = s;
            fileName = fileName.substring(fileName.lastIndexOf('\\'));
            int i = 0;
            for (TextureField textureSet : texturePack) {
                int j = 0;
                for (String texture : textureSet.maps) {
                    if (!texture.equals("") && texture.lastIndexOf('\\') != -1) {
                        String textureName = texture.substring(texture.lastIndexOf('\\'));
                        if (textureName.equalsIgnoreCase(fileName)) {
                            replacements[i][j] = s;
                            needed[i] = true;
                        }
                    }
                    if (j == numSupportedTextures - 1) {
                        break;
                    } else {
                        j++;
                    }
                }
                i++;
            }
        }

        // Make new TXSTs
        textureVariants = new TextureVariant[texturePack.size()];
        int i = 0;
        for (TextureField textureSet : texturePack) {
            if (needed[i]) {
                // New TXST
                TXST tmpTXST = new TXST(SPGlobal.getGlobalPatch());
//                tmpTXST.setFlag(TXST.TXSTflag.FACEGEN_TEXTURES, true);
                tmpTXST.setEDID(name + "_txst");

                // Set maps
                int j = 0;
                for (String texture : textureSet.maps) {
                    if (replacements[i][j] != null) {
                        tmpTXST.setNthMap(j, replacements[i][j]);
                    } else if (!"".equals(texture)) {
                        SPGlobal.log("TEST", texture);
                        tmpTXST.setNthMap(j, texture.substring(texture.indexOf('\\') + 1));
                    }
                    if (j == numSupportedTextures - 1) {
                        break;
                    } else {
                        j++;
                    }
                }
                textureVariants[i] = new TextureVariant(tmpTXST, textureSet.title);
            }
            i++;
        }

        variantTexturePaths = null; // Free up space
    }

    class TextureVariant {
        String nifFieldName;
        TXST textureRecord;

        TextureVariant (TXST txst, String name) {
            textureRecord = txst;
            nifFieldName = name;
        }
    }

    void setName (File file) {
        String[] tmp = file.getPath().split("\\\\");
        name = "AV_" + tmp[tmp.length - 4].replaceAll(" ", "") + "_" + tmp[tmp.length - 3].replaceAll(" ", "") + "_" + tmp[tmp.length - 2].replaceAll(" ", "");
    }

    boolean isEmpty() {
        return variantTexturePaths.isEmpty();
    }
}
