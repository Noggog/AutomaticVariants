/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.io.*;

/**
 *
 * @author Justin Swanson
 */
public class RerouteFile extends PackageComponent {

    File routeFile;

    public RerouteFile (File src) throws FileNotFoundException, IOException {
	super(src, Type.REROUTE);
	BufferedReader in = new BufferedReader(new FileReader(src));
	File to = new File(in.readLine());
	in.close();
	routeFile = src;
	this.src = to;
    }
}
