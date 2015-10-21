package plugins.CENO.Client.Signaling;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import plugins.CENO.Client.CENOClient;
import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.client.FetchResult;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.PersistenceDisabledException;
import freenet.keys.FreenetURI;
import freenet.node.FSParseException;
import freenet.node.RequestClient;
import freenet.support.Logger;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;
import freenet.support.io.ResumeFailedException;

public class ChannelMaker implements Runnable {
	private FreenetURI signalSSK;
	private FreenetURI signalSSKpub;
	private boolean channelEstablished = false;
	private ChannelStatus channelStatus = ChannelStatus.starting;

	public ChannelMaker() {
		this(null);
	}

	public ChannelMaker(FreenetURI singalSSK) {
		if (singalSSK != null && singalSSK.isSSK()) {
			this.signalSSK = singalSSK;
		} else {
			this.signalSSK = CENOClient.nodeInterface.generateKeyPair()[0];
		}
		try {
			this.signalSSKpub = signalSSK.deriveRequestURIFromInsertURI();
		} catch (MalformedURLException e) {
			this.channelStatus = ChannelStatus.fatal;
		}
	}

	@Override
	public void run() {
		if(!checkChannelEstablished()) {
			if(ChannelStatus.isFatalStatus(channelStatus)) {
				return;
			}
			establishChannel();
		}
	}

	public boolean isChannelEstablished() {
		return channelEstablished;
	}

	private boolean checkChannelEstablished() {
		FreenetURI synURI = new FreenetURI("USK", "syn", signalSSKpub.getRoutingKey(), signalSSKpub.getCryptoKey(), signalSSKpub.getExtra());
		FetchResult fetchResult = null;
		while(!channelEstablished) {
			try {
				fetchResult = CENOClient.nodeInterface.fetchURI(synURI);
			} catch (FetchException e) {
				if(e.getMode() == FetchExceptionMode.PERMANENT_REDIRECT) {
					synURI = e.newURI;
				} else if(e.isDNF() || e.isFatal()) {
					return false;
				}
			}
			if(fetchResult != null) {
				try {
					if(new String(fetchResult.asByteArray()).compareTo("syn") == 0) {
						channelEstablished = true;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	public void establishChannel() {
		if(channelEstablished) {
			return;
		}
		FreenetURI bridgeKey;
		try {
			bridgeKey = new FreenetURI(CENOClient.BRIDGE_KEY);
		} catch (MalformedURLException e1) {
			channelStatus = ChannelStatus.fatal;
			return;
		}
		FreenetURI bridgeSignalerURI = new FreenetURI("USK", "CENO-signaler", bridgeKey.getRoutingKey(), bridgeKey.getCryptoKey(), bridgeKey.getExtra());
		FetchResult bridgeSignalFreesite = null;
		while(bridgeSignalFreesite == null) {
			try {
				bridgeSignalFreesite = CENOClient.nodeInterface.fetchURI(bridgeSignalerURI);
			} catch (FetchException e) {
				if (e.mode == FetchException.FetchExceptionMode.PERMANENT_REDIRECT) {
					bridgeSignalerURI = e.newURI;
				}
				Logger.warning(this, "Exception while retrieving the bridge's signal page");
			}
		}
		SimpleFieldSet sfs;
		String question = null;
		try {
			sfs = new SimpleFieldSet(new String(bridgeSignalFreesite.asByteArray()), false, true, true);
			question = sfs.getString("question");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FSParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (question == null) {
			// CENO Client won't be able to signal the bridge
			//TODO Terminate plugin
			return;
		}
		SimpleFieldSet replySfs = new SimpleFieldSet(true);
		replySfs.put("id", (int) (Math.random() * (Integer.MAX_VALUE * 0.8)));
		replySfs.putOverwrite("insertURI", signalSSK.toASCIIString());
		//TODO Encrypt singalSSK{
		try {
			CENOClient.nodeInterface.insertSingleChunk(new FreenetURI("KSK@" + question), replySfs.toOrderedString(), new voidInputCallback());
		} catch (UnsupportedEncodingException e) {
			channelStatus = ChannelStatus.failedToPublishKSK;
		} catch (PersistenceDisabledException e) {
			channelStatus = ChannelStatus.failedToPublishKSK;
		} catch (MalformedURLException e) {
			channelStatus = ChannelStatus.failedToPublishKSK;
		} catch (InsertException e) {
			channelStatus = ChannelStatus.failedToPublishKSK;
		}
	}

	private class voidInputCallback implements ClientPutCallback {

		@Override
		public void onResume(ClientContext context)
				throws ResumeFailedException {
			// TODO Auto-generated method stub

		}

		@Override
		public RequestClient getRequestClient() {
			return CENOClient.nodeInterface.getRequestClient();
		}

		@Override
		public void onGeneratedURI(FreenetURI uri, BaseClientPutter state) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onGeneratedMetadata(Bucket metadata, BaseClientPutter state) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onFetchable(BaseClientPutter state) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSuccess(BaseClientPutter state) {
			Logger.normal(ChannelMaker.class, "Inserted private SSK key in the KSK@solution to the puzzle published by the bridge");
		}

		@Override
		public void onFailure(InsertException e, BaseClientPutter state) {
			// TODO Auto-generated method stub

		}

	}

}
