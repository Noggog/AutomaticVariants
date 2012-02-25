/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import skyproc.BSA;
import skyproc.SPGlobal;
import skyproc.TXST;

/**
 *
 * @author Justin Swanson
 */
public class Variant {

    String name;
    ArrayList<String> textures = new ArrayList<String>();
    TXST[] textureVariants;

    void generateVariant(ArrayList<ArrayList<String>> texturePack) throws IOException {

        // Find out which TXSTs need to be generated
        String[][] replacements = new String[texturePack.size()][7];
        boolean[] needed = new boolean[texturePack.size()];
        for (String s : textures) {
            String fileName = s;
            fileName = fileName.substring(fileName.lastIndexOf('\\'));
            int i = 0;
            for (ArrayList<String> textureSet : texturePack) {
                int j = 0;
                for (String texture : textureSet) {
                    if (!texture.equals("") && texture.lastIndexOf('\\') != -1) {
                        String textureName = texture.substring(texture.lastIndexOf('\\'));
                        if (textureName.equalsIgnoreCase(fileName)) {
                            replacements[i][j] = s;
                            needed[i] = true;
                        }
                    }
                    if (j == 6) {
                        break;
                    } else {
                        j++;
                    }
                }
                i++;
            }
        }

        // Make new TXSTs
        textureVariants = new TXST[texturePack.size()];
        int i = 0;
        for (ArrayList<String> textureSet : texturePack) {
            if (needed[i]) {
                // New TXST
                TXST tmpTXST = new TXST(SPGlobal.getGlobalPatch());
                tmpTXST.setFlag(TXST.TXSTflag.FACEGEN_TEXTURES, true);
                tmpTXST.setEDID(name);

                // Set maps
                int j = 0;
                for (String texture : textureSet) {
                    if (replacements[i][j] != null) {
                        tmpTXST.setNthMap(j, replacements[i][j]);
                    } else if (!"".equals(texture)) {
                        tmpTXST.setNthMap(j, texture.substring(texture.indexOf('\\') + 1));
                    }
                    if (j == 6) {
                        break;
                    } else {
                        j++;
                    }
                }
                textureVariants[i] = tmpTXST;
            }
            i++;
        }

        textures = null; // Free up space
    }

    void setName (File file) {
        String[] tmp = file.getPath().split("\\\\");
        name = tmp[tmp.length - 4] + "-" + tmp[tmp.length - 3] + "-" + tmp[tmp.length - 2];
    }

    boolean isEmpty() {
        return textures.isEmpty();
    }
}
