package plugins.CeNo.FreenetInterface;

import plugins.CeNo.HighLevelSimpleClientInterface;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.keys.FreenetURI;
import freenet.keys.InsertableClientSSK;
import freenet.node.Node;

public class NodeInterface implements FreenetInterface {
	
	private Node node;
	
	public NodeInterface(Node node) {
		this.node = node;
	}

	public FetchResult fetchURI(FreenetURI uri) throws FetchException {
		return HighLevelSimpleClientInterface.fetchURI(uri);
	}

	/**
	 * Generate a new key pair for SSK insertions
	 * 
	 * @return FreenetURI array where 1st element is the insertURI and second element is the requestURI
	 */
	public FreenetURI[] generateKeyPair() {
		InsertableClientSSK key = InsertableClientSSK.createRandom(node.random, "");
    	FreenetURI insertURI = key.getInsertURI();
    	FreenetURI requestURI = key.getURI();
    	return new FreenetURI[]{insertURI, requestURI};
	}

}
