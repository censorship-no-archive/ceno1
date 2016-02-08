package plugins.CENO.Bridge.Signaling;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import plugins.CENO.Bridge.CENOBridge;
import plugins.CENO.Bridge.RequestReceiver;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertException;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.client.async.PersistenceDisabledException;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.support.Logger;
import freenet.support.io.ResumeFailedException;

public class Channel {
	private FreenetURI insertSSK, requestSSK;
	private Long lastKnownEdition = 0L;
	private Long lastSynced = 0L;

	public Channel() throws MalformedURLException {
		this(null, null, null);
	}

	public Channel(String insertSSK) throws MalformedURLException {
		this(insertSSK, null, null);
	}

	public Channel(String insertSSK, Long providedEdition, Long lastSynced) throws MalformedURLException {
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

		this.lastSynced = lastSynced != null ? lastSynced : 0;
	}

	public String getInsertSSK() {
		return insertSSK.toASCIIString();
	}

	public Long getLastKnownEdition() {
		return lastKnownEdition;
	}

	private void setLastKnownEdition(Long newEdition) {
		this.lastKnownEdition = newEdition;
	}

	public Long getLastSynced() {
		return lastSynced;
	}

	public void publishSyn() {
		try {
			FreenetURI synURI = new FreenetURI("USK", "syn", insertSSK.getRoutingKey(), insertSSK.getCryptoKey(), insertSSK.getExtra());
			CENOBridge.nodeInterface.insertSingleChunk(synURI, Long.toString(System.currentTimeMillis()), CENOBridge.nodeInterface.getVoidPutCallback("" +
					"Successfully inserted syn response to signalSSK ",
					"Failed to insert syn response to signalSSK " + insertSSK));
		} catch (UnsupportedEncodingException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		} catch (InsertException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		} catch (PersistenceDisabledException e) {
			Logger.error(this, "Excpetion while inserting Syn response to an insertion key provided by a client: " + e.getMessage());
		}
		this.lastSynced = System.currentTimeMillis();
	}

	public void subscribeToChannelUpdates() throws MalformedURLException {
		FreenetURI origUSK = new FreenetURI(requestSSK.getRoutingKey(), requestSSK.getCryptoKey(), requestSSK.getExtra(), "req", lastKnownEdition);
		try {
			CENOBridge.nodeInterface.fetchULPR(origUSK, new ReqCallback(origUSK, insertSSK.toString()));
		} catch (FetchException e) {
			Logger.error(this, "FetchException while starting ULPR for a signalling channel: " + e.getMessage());
		}
	}

	public static class ReqCallback implements ClientGetCallback {
		private FreenetURI uri;
		private String channelSignalSSK;

		public ReqCallback(FreenetURI uri, String channelSignalSSK) {
			this.uri = uri;
			this.channelSignalSSK = channelSignalSSK;
		}

		private void fetchNewRequests(FetchResult result) {
			try {
				String fetchedString = new String(result.asByteArray(), "UTF-8");
				RequestReceiver.signalReceived(fetchedString.split("\\r?\\n"));
			} catch (NullPointerException e) {
				Logger.warning(this, "Received new request from client but payload was empty");
			} catch (UnsupportedEncodingException e) {
				Logger.error(this, "UTF-8 is not supported");
			} catch (IOException e) {
				Logger.warning(this, "IOException while parsing batch request from client");
			}

			FreenetURI nextURI = uri.setSuggestedEdition(uri.getEdition() + 1);
			try {
				CENOBridge.nodeInterface.fetchULPR(nextURI, new ReqCallback(nextURI, this.channelSignalSSK));
			} catch (FetchException e) {
				Logger.error(this, "FetchException while starting ULPR for next edition of a signalling channel: " + e.getMessage());
			}

			return;
		}

		@Override
		public void onResume(ClientContext context) throws ResumeFailedException {
		}

		@Override
		public RequestClient getRequestClient() {
			return CENOBridge.nodeInterface.getRequestClient();
		}

		@Override
		public void onSuccess(FetchResult result, ClientGetter state) {
			ChannelManager.getInstance().getChannel(channelSignalSSK).setLastKnownEdition(uri.getSuggestedEdition());
			fetchNewRequests(result);
		}

		@Override
		public void onFailure(FetchException e, ClientGetter state) {
			switch (e.getMode()) {
			case PERMANENT_REDIRECT :
				try {
					CENOBridge.nodeInterface.fetchULPR(e.newURI, new ReqCallback(e.newURI, this.channelSignalSSK));
				} catch (FetchException e1) {
					Logger.error(this, "FetchException while starting ULPR for new edition of a signalling channel: " + e1.getMessage());
				}
				break;

			case ALL_DATA_NOT_FOUND :
			case DATA_NOT_FOUND :
				Logger.warning(Channel.class, 
						"Found new request from client but could not fetch data for USK: " + uri);
				break;

			default:
				Logger.warning(Channel.class, "Exception while fetching new request from client for USK: " + uri + ", " + e.getMessage());
				break;
			}

			if (e.isDefinitelyFatal()) {
				Logger.error(Channel.class, "Fatal error while fetching new request from client for USK: " + uri + ", " + e.getMessage());
			}
		}

	}

}
