/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import java.io.File;
import java.util.ArrayList;
import skyproc.SPGlobal;
import skyproc.TXST;

/**
 *
 * @author Justin Swanson
 */
public class Variant {

    ArrayList<File> textures = new ArrayList<File>();
    TXST[] textureVariants;

    void generateVariant(ArrayList<ArrayList<String>> texturePack) {

        // Find out which TXSTs need to be generated
        File[][] replacements = new File[texturePack.size()][7];
        boolean[] needed = new boolean[texturePack.size()];
        for (File f : textures) {
            String fileName = f.getPath();
            fileName = fileName.substring(fileName.lastIndexOf('\\'));
            int i = 0;
            for (ArrayList<String> textureSet : texturePack) {
                int j = 0;
                for (String texture : textureSet) {
                    if (!texture.equals("") && texture.lastIndexOf('\\') != -1) {
                        String textureName = texture.substring(texture.lastIndexOf('\\'));
                        if (textureName.equalsIgnoreCase(fileName)) {
                            replacements[i][j] = f;
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
                TXST tmp = new TXST(SPGlobal.getGlobalPatch());
                tmp.setFlag(TXST.TXSTflag.FACEGEN_TEXTURES, true);
                int j = 0;
                for (String texture : textureSet) {
                    if (replacements[i][j] != null) {
                        tmp.setNthMap(j, replacements[i][j].getPath());
                    } else if (!"".equals(texture)) {
                        tmp.setNthMap(j, texture);
                    }
                    if (j == 6) {
                        break;
                    } else {
                        j++;
                    }
                }
                textureVariants[i] = tmp;
            }
            i++;
        }

        textures = null; // Free up space
    }

    boolean isEmpty() {
        return textures.isEmpty();
    }
}
