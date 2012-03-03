/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
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
    String name;
    String path;
    ArrayList<TextureField> textureFields = new ArrayList<TextureField>();
    ArrayList<Variant> variants = new ArrayList<Variant>();

    AV_Nif(String path) {
	name = path;
	this.path = "meshes\\" + path;
    }

    final void load() throws BadParameter, FileNotFoundException, IOException, DataFormatException {
	NIF nif = new NIF(BSA.getUsedFile(path));
	if (nif == null) {
	    throw new FileNotFoundException("NIF file did not exist for path: " + path);
	}
	ArrayList<ArrayList<Node>> NiTriShapes = nif.getNiTriShapePackages();
	TextureField last = new TextureField();
	for (int i = 0; i < NiTriShapes.size(); i++) {
	    TextureField next = new TextureField(last);
	    next.title = NiTriShapes.get(i).get(0).title;
	    if (SPGlobal.logging()) {
		SPGlobal.log(header, "  Loaded NiTriShapes: " + next.title);
	    }
	    for (Node n : NiTriShapes.get(i)) {
		if (n.type == NIF.NodeType.BSSHADERTEXTURESET) {
		    if (SPGlobal.logging()) {
			SPGlobal.log(header, "  Loaded new texture maps");
		    }
		    next.maps = extractBSTextures(n);
		    if (!next.equals(last)) {
			next.unique = true;
		    }
		}
	    }
	    this.textureFields.add(next);
	    last = next;
	}
    }

    public void print() {
	if (SPGlobal.logging()) {
	    int i = 0;
	    for (TextureField set : textureFields) {
		SPGlobal.log(header, "  Texture index " + i++ + ": " + set.title);
		int j = 0;
		for (String s : set.maps) {
		    SPGlobal.log(header, "    " + j++ + ": " + s);
		}
	    }
	}
    }

    static ArrayList<String> extractBSTextures(Node n) {
	int numTextures = n.data.extractInt(4);
	ArrayList<String> maps = new ArrayList<String>(numTextures);
	for (int i = 0; i < numTextures; i++) {
	    maps.add(n.data.extractString(n.data.extractInt(4)));
	}
	return maps;
    }

    class TextureField {

	String title = "No Title";
	ArrayList<String> maps = new ArrayList<String>(0);
	boolean unique = false;

	TextureField() {
	}

	TextureField(TextureField in) {
	    this.title = in.title;
	    this.maps = in.maps;
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    final TextureField other = (TextureField) obj;
	    if (this.maps != other.maps && (this.maps == null || !this.maps.equals(other.maps))) {
		return false;
	    }
	    return true;
	}

	@Override
	public int hashCode() {
	    int hash = 7;
	    hash = 71 * hash + (this.maps != null ? this.maps.hashCode() : 0);
	    return hash;
	}
    }
}
