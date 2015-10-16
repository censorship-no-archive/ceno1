package plugins.CENO.Bridge.Signaling;

import java.net.MalformedURLException;

import plugins.CENO.Bridge.CENOBridge;
import freenet.keys.FreenetURI;

public class Channel {
	private String insertSSK;
	private String requestSSK;
	private Long lastKnownEdition = 0L;

	public Channel() throws MalformedURLException {
		this(null, null);
	}

	public Channel(String insertSSK) throws MalformedURLException {
		this(insertSSK, null);
	}

	public Channel(String insertSSK, Long providedEdition) throws MalformedURLException {
		if (insertSSK == null) {
			FreenetURI[] channelKeyPair = CENOBridge.nodeInterface.generateKeyPair();
			this.insertSSK = channelKeyPair[0].toString();
			this.requestSSK = channelKeyPair[1].toString();
			return;
		}

		this.insertSSK = insertSSK;

		if (providedEdition != null) {
			this.lastKnownEdition = providedEdition;
			return;
		}

		FreenetURI insertURI = new FreenetURI(insertSSK);

		if (insertURI.isUSK()) {
			this.lastKnownEdition = insertURI.getEdition();
		}
	}
	
	public void subscribeToChannelUpdates() {
		//TODO Subscribe to channel updated and create callback for signaling bundle inserter
	}

}
