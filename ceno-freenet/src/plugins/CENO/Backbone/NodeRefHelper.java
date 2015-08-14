package plugins.CENO.Backbone;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import freenet.node.Node;
import freenet.support.Fields;
import freenet.support.SimpleFieldSet;

/**
 * Provides the methods for parsing node references and transforming
 * them from text to SimpleFieldSet format and the other way around.
 * Also for acquiring the own Darknet node reference.
 */
public class NodeRefHelper {
	private Node node;
	
	public NodeRefHelper (Node node) {
		this.node = node;
	}

	/**
	 * Gets a string representation of the darknet public
	 * reference of a node.
	 * 
	 * @return the own Darknet node reference
	 */
	public String getNodeRef() {
		SimpleFieldSet fs = getNodeRefFS();
		return fs.toOrderedStringWithBase64();
	}

	/**
	 * Gets the darknet public reference of a node so that
	 * it can be exchanged with other friends in order to
	 * become friends
	 * 
	 * @return a SimpleFieldSet with the own Darkent node refernece
	 */
	public SimpleFieldSet getNodeRefFS() {
		return node.exportDarknetPublicFieldSet();
	}

	/**
	 * Read the Bridge node reference from the resources
	 * 
	 * @return a text representation of the main bridge's node reference
	 * @throws FileNotFoundException if the bridgeref.txt file is not
	 * found in the resources
	 * @throws IOException if the bridgeref.txt file could not be
	 * successfully parsed
	 */
	public String getBridgeNodeRef() throws FileNotFoundException, IOException {
		InputStream is = getClass().getResourceAsStream("resources/bridgeref.txt");
		if (is == null) {
			throw new FileNotFoundException();
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = "";
		StringBuilder bridgeRef = new StringBuilder();
		while ((line = br.readLine()) != null) {
			bridgeRef.append(line);
			bridgeRef.append("\n");
		}
		
		return bridgeRef.toString().trim().concat("End");
	}

	/**
	 * Get a SimpleFieldSet for the bridge node reference
	 * included in the resources.
	 * 
	 * @return the SimpleFieldSet for the bridge node
	 * @throws FileNotFoundException if the bridge node reference
	 * file was not found in the resources
	 * @throws IOException if the bridge node reference file in
	 * the resources could not be successfully parsed
	 */
	public SimpleFieldSet getBridgeNodeRefFS() throws FileNotFoundException, IOException {
		SimpleFieldSet fs;
		String bridgeRef = Fields.trimLines(getBridgeNodeRef());
		fs = new SimpleFieldSet(bridgeRef, false, true, true);
		if(!fs.getEndMarker().endsWith("End")) {
			throw new IOException("Trying to add noderef with end marker \""+fs.getEndMarker()+"\"");
		}
		fs.setEndMarker("End"); // It's always End ; the regex above doesn't always grok this
		return fs;
	}
}
