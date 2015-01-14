package plugins.CeNo;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.HighLevelSimpleClient;
import freenet.keys.FreenetURI;
import freenet.node.Node;
import freenet.node.NodeClientCore;
import freenet.node.RequestStarter;

public class HighLevelSimpleClientInterface {

	private static volatile HighLevelSimpleClientInterface HLSCInterface = null;

	private HighLevelSimpleClient client;
	private Node node;

	private HighLevelSimpleClientInterface() {
	}

	public HighLevelSimpleClientInterface(Node node, NodeClientCore core) {
		synchronized (HighLevelSimpleClientInterface.class) {
			if (HLSCInterface == null) {
				HLSCInterface = new HighLevelSimpleClientInterface();
				HLSCInterface.node = node;
				HLSCInterface.client = node.clientCore.makeClient(RequestStarter.INTERACTIVE_PRIORITY_CLASS, false, false);
			}
		}
	}

	public HighLevelSimpleClientInterface(HighLevelSimpleClient hlSimpleClient) {
		synchronized (HighLevelSimpleClientInterface.class) {
			if (HLSCInterface == null) {
				HLSCInterface = new HighLevelSimpleClientInterface();
				HLSCInterface.client = hlSimpleClient;
			}
		}
	}

	/**
	 * Synchronously fetch a file from Freenet, given a FreenetURI
	 * 
	 * @param uri of the file to be fetched
	 * @return a FetchResult instance upon successful fetch
	 * @throws FetchException
	 */
	public static FetchResult fetchURI(FreenetURI uri) throws FetchException {
		FetchResult result = HLSCInterface.client.fetch(uri);
		return result;
	}

}
