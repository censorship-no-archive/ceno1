package plugins.CENO.Backbone;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import freenet.node.Node;
import freenet.support.Fields;
import freenet.support.SimpleFieldSet;

public class NodeRefHelper {
	private Node node;
	
	public NodeRefHelper (Node node) {
		this.node = node;
	}

	public String getNodeRef() {
		SimpleFieldSet fs = getNodeRefFS();
		return fs.toOrderedStringWithBase64();
	}

	public SimpleFieldSet getNodeRefFS() {
		return node.exportDarknetPublicFieldSet();
	}

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

	public SimpleFieldSet getBridgeNodeRefFS() throws IOException {
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
