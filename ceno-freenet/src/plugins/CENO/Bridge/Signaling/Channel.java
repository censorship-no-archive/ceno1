package plugins.CENO.Bridge.Signaling;

import java.net.MalformedURLException;

import plugins.CENO.Bridge.CENOBridge;
import freenet.keys.FreenetURI;

public class Channel {
	private String insertSSK;
	private String requestSSK;
	private Long lastKnownEdition = 0L;

	public Channel() {
		this(null, null);
	}

	public Channel(String insertSSK) {
		this(insertSSK, null);
	}

	public Channel(String insertSSK, Long providedEdition) {
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

		FreenetURI insertURI = null;
		try {
			insertURI = new FreenetURI(insertSSK);
		} catch (MalformedURLException e) {
			return;
		}

		if (providedEdition == null) {
			if (insertURI.isUSK()) {
				lastKnownEdition = insertURI.getEdition();
			}
		}
	}
	
	public void subscribeToChannelUpdates() {
		//TODO Subscribe to channel updated and create callback for signaling bundle inserter
	}

}
