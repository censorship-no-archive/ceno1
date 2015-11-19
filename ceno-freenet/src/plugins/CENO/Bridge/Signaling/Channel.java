package plugins.CENO.Bridge.Signaling;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import plugins.CENO.Bridge.CENOBridge;
import plugins.CENO.Bridge.RequestReceiver;
import plugins.CENO.Client.CENOClient;
import freenet.client.FetchException;
import freenet.client.FetchResult;
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
		USK origUSK = new USK(requestSSK.getRoutingKey(), requestSSK.getCryptoKey(), requestSSK.getExtra(), "", lastKnownEdition);
		CENOBridge.nodeInterface.subscribeToUSK(origUSK, new ReqCallback());
	}

	public static class ReqCallback implements USKCallback {

		@Override
		public void onFoundEdition(long l, USK key, ClientContext context,
				boolean metadata, short codec, byte[] data,
				boolean newKnownGood, boolean newSlotToo) {
			fetchNewRequests(key.getURI().setSuggestedEdition(l));
		}

		private void fetchNewRequests(FreenetURI uri) {
			FetchResult fetchResult = null;
			if (!uri.hasMetaStrings()) {
				uri = uri.addMetaStrings(new String[]{"default.html"});
			}
			try {
				fetchResult = CENOClient.nodeInterface.fetchURI(uri);
			} catch (FetchException e) {
				switch (e.getMode()) {
				case PERMANENT_REDIRECT :
					fetchNewRequests(e.newURI);
					break;

				case ALL_DATA_NOT_FOUND :
				case DATA_NOT_FOUND :
					Logger.warning(Channel.class, 
							"Found new request from client but could not fetch data for USK: " + uri);
					break;

				case RECENTLY_FAILED :
					try {
						Thread.sleep(TimeUnit.MINUTES.toMillis(10));
					} catch (InterruptedException e1) {
						// No big deal
					} finally {
						fetchNewRequests(uri);
					}
					return;

				default:
					Logger.warning(Channel.class, "Exception while fetching new request from client for USK: " + uri + ", " + e.getMessage());
					break;
				}
				if (e.isDefinitelyFatal()) {
					Logger.error(Channel.class, "Fatal error while fetching new request from client for USK: " + uri + ", " + e.getMessage());
					return;
				}
			}

			try {
				String fetchedString = fetchResult.toString();
				RequestReceiver.signalReceived(fetchedString.split("\\r?\\n"));
			} catch (NullPointerException e) {
				Logger.warning(this, "Received new request from client but payload was empty");
			}

			return;
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
