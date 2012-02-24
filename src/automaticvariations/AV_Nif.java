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
import lev.Ln;
import lev.ShrinkArray;
import skyproc.BSA;
import skyproc.NIF;
import skyproc.SPGlobal;
import skyproc.exceptions.BadParameter;

/**
 *
 * @author Justin Swanson
 */
public class AV_Nif {

    ArrayList<String> textureFields = new ArrayList<String>();
    ArrayList<Variant> variants = new ArrayList<Variant>();

    final void load(String path) throws BadParameter, FileNotFoundException, IOException, DataFormatException {
        path = "meshes\\" + path;
        File outsideBSA = new File(SPGlobal.pathToData + path);
        NIF nif = null;
        if (outsideBSA.isFile()) {
            nif = new NIF(new ShrinkArray(outsideBSA));
            SPGlobal.logError("NIFtexture", "  Nif loaded from outside BSAs");
        } else {
            for (BSA b : AutomaticVariations.BSAs) {
                if (b.contains(BSA.FileType.NIF) && b.hasFile(path)) {
                    nif = new NIF(b.getFile(path));
                    SPGlobal.logError("NIFtexture", "  Nif loaded from BSA " + b.getFilePath());
                    break;
                }
            }
            if (nif == null) {
                throw new FileNotFoundException ("NIF file did not exist for path: " + path);
            }
        }
        ArrayList<ShrinkArray> textureFields = nif.getBlocks(NIF.NodeType.BSShaderTextureSet);
        for (ShrinkArray s : textureFields) {
            String textureData = Ln.arrayToString(s.getAllBytes());
            int ddsIndex = textureData.indexOf(".dds");
            if (ddsIndex != -1) {
                String adding = ".dds";
                for (int i = ddsIndex - 1; i >= 0; i--) {
                    if (textureData.charAt(i) == '\\') {
                        break;
                    } else {
                        adding = textureData.charAt(i) + adding;
                    }
                }
                this.textureFields.add(adding);
            } else {
                SPGlobal.logError(path, "  Texture index did not have .dds: " + textureData);
            }
        }
    }

    public void print() {
        if (SPGlobal.logging()) {
            int i = 0;
            for (String s : textureFields) {
                SPGlobal.logError("NIFtexture", "  Texture index " + i++ + ": " + s);
            }
        }
    }
}
