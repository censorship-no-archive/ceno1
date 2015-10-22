package plugins.CENO.Bridge.Signaling;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import plugins.CENO.Bridge.CENOBridge;
import plugins.CENO.Bridge.RequestReceiver;
import freenet.client.InsertException;
import freenet.client.async.ClientContext;
import freenet.client.async.PersistenceDisabledException;
import freenet.client.async.USKCallback;
import freenet.keys.FreenetURI;
import freenet.keys.USK;
import freenet.support.Logger;

public class Channel {
	private FreenetURI insertSSK, requestSSK;
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
			this.insertSSK = channelKeyPair[0];
			this.requestSSK = channelKeyPair[1];
			return;
		}

		this.insertSSK = new FreenetURI(insertSSK);

		FreenetURI insertURI = new FreenetURI(this.insertSSK);
		this.requestSSK = insertURI.deriveRequestURIFromInsertURI();

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
					"Successfully inserted syn response to signalSSK ",
					"Failed to insert syn response to signalSSK " + insertSSK));
		} catch (UnsupportedEncodingException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		} catch (InsertException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		} catch (PersistenceDisabledException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		}
	}

	public void subscribeToChannelUpdates() throws MalformedURLException {
		//TODO Subscribe to channel updated and create callback for signaling bundle inserter
		USK origUSK = new USK(requestSSK.getRoutingKey(), requestSSK.getCryptoKey(), requestSSK.getExtra(), "", lastKnownEdition);
		CENOBridge.nodeInterface.subscribeToUSK(origUSK, new ReqCallback());
	}
	
	public static class ReqCallback implements USKCallback {

		@Override
		public void onFoundEdition(long l, USK key, ClientContext context,
				boolean metadata, short codec, byte[] data,
				boolean newKnownGood, boolean newSlotToo) {
		}

		@Override
		public short getPollingPriorityNormal() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public short getPollingPriorityProgress() {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}

}
