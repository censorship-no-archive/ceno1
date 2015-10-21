package plugins.CENO.Bridge.Signaling;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import plugins.CENO.Bridge.CENOBridge;
import freenet.client.InsertException;
import freenet.client.async.PersistenceDisabledException;
import freenet.keys.FreenetURI;
import freenet.support.Logger;

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

		FreenetURI insertURI = new FreenetURI(this.insertSSK);
		this.requestSSK = insertURI.deriveRequestURIFromInsertURI().toASCIIString();

		if (providedEdition != null) {
			this.lastKnownEdition = providedEdition;
			return;
		}

		if (insertURI.isUSK()) {
			this.lastKnownEdition = insertURI.getEdition();
		}
	}

	public void publishSyn() {
		try {
			CENOBridge.nodeInterface.insertSingleChunk(new FreenetURI(insertSSK), Long.toString(System.currentTimeMillis()), CENOBridge.nodeInterface.getVoidPutCallback("" +
					"Successfully inserted syn response to signalSSK " + insertSSK,
					"Failed to insert syn response to signalSSK " + insertSSK));
		} catch (UnsupportedEncodingException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		} catch (MalformedURLException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		} catch (InsertException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		} catch (PersistenceDisabledException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		}
	}

	public void subscribeToChannelUpdates() {
		//TODO Subscribe to channel updated and create callback for signaling bundle inserter
	}

}
