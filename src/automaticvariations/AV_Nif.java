/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import lev.LShrinkArray;
import skyproc.BSA;
import skyproc.NIF;
import skyproc.SPGlobal;
import skyproc.exceptions.BadParameter;

/**
 *
 * @author Justin Swanson
 */
public class AV_Nif {

    private static String header = "AV_Nif";
    ArrayList<ArrayList<String>> textureFields = new ArrayList<ArrayList<String>>();
    ArrayList<Variant> variants = new ArrayList<Variant>();

    final void load(String path) throws BadParameter, FileNotFoundException, IOException, DataFormatException {
        path = "meshes\\" + path;
        NIF nif = new NIF(BSA.getUsedFile(path));
        if (nif == null) {
            throw new FileNotFoundException("NIF file did not exist for path: " + path);
        }
        ArrayList<LShrinkArray> BSTextureSets = nif.getBlocks(NIF.NodeType.BSShaderTextureSet);
        for (LShrinkArray s : BSTextureSets) {
            int numTextures = s.extractInt(4);
            ArrayList<String> textures = new ArrayList<String>(numTextures);
            for (int i = 0; i < numTextures; i++) {
                textures.add(s.extractString(s.extractInt(4)));
            }
            this.textureFields.add(textures);
        }
    }

    final void generateVariants() throws IOException {
        for (Variant v : variants) {
            v.generateVariant(textureFields);
        }
    }

    public void print() {
        if (SPGlobal.logging()) {
            int i = 0;
            for (ArrayList<String> set : textureFields) {
                SPGlobal.logError(header, "  Texture index " + i++ + ": ");
                int j = 0;
                for (String s : set) {
                    SPGlobal.logError(header, "    " + j++ + ": " + s);
                }
            }
        }
    }
}
