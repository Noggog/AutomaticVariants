/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import lev.LShrinkArray;
import skyproc.BSA;
import skyproc.NIF;
import skyproc.NIF.Node;
import skyproc.SPGlobal;
import skyproc.exceptions.BadParameter;

/**
 *
 * @author Justin Swanson
 */
public class AV_Nif {

    private static String header = "AV_Nif";
    ArrayList<TextureField> textureFields = new ArrayList<TextureField>();
    ArrayList<Variant> variants = new ArrayList<Variant>();

    final void load(String path) throws BadParameter, FileNotFoundException, IOException, DataFormatException {
        path = "meshes\\" + path;
        NIF nif = new NIF(BSA.getUsedFile(path));
        if (nif == null) {
            throw new FileNotFoundException("NIF file did not exist for path: " + path);
        }
        ArrayList<Node> NiTrishapes = nif.getNode(NIF.NodeType.NiTriShape);
        ArrayList<Node> BSTextureSets = nif.getNode(NIF.NodeType.BSShaderTextureSet);
        for (int i = 0 ; i < BSTextureSets.size() ; i++) {
            TextureField texField = new TextureField(BSTextureSets.get(i));
            texField.title = NiTrishapes.get(i).title;
            this.textureFields.add(texField);
        }
    }

    final void generateVariantTXSTs() throws IOException {
        for (Variant v : variants) {
            v.generateVariant(textureFields);
        }
        textureFields = null; // Not needed anymore
    }

    public void print() {
        if (SPGlobal.logging()) {
            int i = 0;
            for (TextureField set : textureFields) {
                SPGlobal.logError(header, "  Texture index " + i++ + ": " + set.title);
                int j = 0;
                for (String s : set.maps) {
                    SPGlobal.logError(header, "    " + j++ + ": " + s);
                }
            }
        }
    }

    class TextureField {
        String title;
        ArrayList<String> maps;

        TextureField (Node n) {
            int numTextures = n.data.extractInt(4);
            maps = new ArrayList<String>(numTextures);
            for (int i = 0; i < numTextures; i++) {
                maps.add(n.data.extractString(n.data.extractInt(4)));
            }
        }
    }
}
