package plugins.CENO.Backbone;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.List;

import freenet.node.Node;
import freenet.support.Fields;
import freenet.support.SimpleFieldSet;
import freenet.support.io.Closer;

/**
 * Provides the methods for parsing node references and transforming
 * them from text to SimpleFieldSet format and the other way around.
 * Also for acquiring the own Darknet node reference.
 */
public class NodeRefHelper {
	private Node node;

	public static final String BRIDGE_NODES_FILENAME = "resources/bridgeref.txt";
	public static final String BACKBONE_NODES_FILENAME = "resources/myref.txt";

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
	 * Write own node reference in the resources/myref.txt for later use
	 *
	 * @throws IOException if the own ref file could not be open for write
	 */
	public void writeOwnRef() throws IOException {

		File ownRefFile = new File(BACKBONE_NODES_FILENAME);
		FileOutputStream fos = null;

		try {

			// if file doesn't exists, then create it
			fos = new FileOutputStream(ownRefFile);
			if (!ownRefFile.exists()) {
				ownRefFile.createNewFile();
			}

			// get the content in bytes
			byte[] contentInBytes = getNodeRef().getBytes();

			fos.write(contentInBytes);
			fos.flush();

			System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
			throw e;

		} finally {
			fos.close();
		}

	}
	/**
	 * Read the Bridge node reference from the resources
	 * 
	 * @param bridgeRefFile the name of the file which contains the bridge
	 * references
	 * 
	 * @return a list of SimpleFieldSet for the bridge node
	 *
	 * @throws FileNotFoundException if the bridge ref file is not
	 * found in the resources
	 * @throws IOException if the bridge ref file could not be
	 *  successfully parsed
	 */
	public static List<SimpleFieldSet> readBridgeRefs(String bridgeRefFile) throws FileNotFoundException, IOException {
		List<SimpleFieldSet> list = new ArrayList<SimpleFieldSet>();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(bridgeRefFile);
			if (fis == null) {
				throw new FileNotFoundException();
			}
			BufferedInputStream bis = new BufferedInputStream(fis);
			InputStreamReader isr = new InputStreamReader(bis, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			while(true) {
				try {
					SimpleFieldSet fs = new SimpleFieldSet(br, false, false, true, false);
					if(!fs.isEmpty())
						list.add(fs);
				} catch (EOFException e) {
					return list;
				}
			}
		} catch (IOException e) {
			throw e; 
			//return list; 
		} finally {
			Closer.close(fis);
		}

	}

	/**
	 *  Default value overload
	 */
	public List<SimpleFieldSet> readBridgeRefs() throws FileNotFoundException, IOException {
		return readBridgeRefs(BRIDGE_NODES_FILENAME);
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
	public String getBridgeNodesRef() throws FileNotFoundException, IOException {
		InputStream is = getClass().getResourceAsStream(BRIDGE_NODES_FILENAME);
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
		String bridgeRef = Fields.trimLines(getBridgeNodesRef());
		fs = new SimpleFieldSet(bridgeRef, false, true, true);
		if(!fs.getEndMarker().endsWith("End")) {
			throw new IOException("Trying to add noderef with end marker \""+fs.getEndMarker()+"\"");
		}
		fs.setEndMarker("End"); // It's always End ; the regex above doesn't always grok this
		return fs;
	}
}
