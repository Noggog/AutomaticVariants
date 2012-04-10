/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.Serializable;
import skyproc.TXST;

/**
 *
 * @author Justin Swanson
 */
class TextureVariant implements Serializable {

    String nifFieldName;
    TXST textureRecord;

    TextureVariant(TXST txst, String name) {
	textureRecord = txst;
	nifFieldName = name;
    }
}