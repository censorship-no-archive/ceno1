package plugins.CeNo.FreenetInterface;

import plugins.CeNo.HighLevelSimpleClientInterface;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.keys.FreenetURI;
import freenet.node.Node;

public class NodeInterface implements FreenetInterface {
	
	private Node node;
	
	public NodeInterface(Node node) {
		this.node = node;
	}

	public FetchResult fetchURI(FreenetURI uri) throws FetchException {
		return HighLevelSimpleClientInterface.fetchURI(uri);
	}

}
