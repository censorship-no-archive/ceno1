package plugins.CENO.Client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import freenet.client.FetchException;
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

public class ChannelMaker {
	private FreenetURI signalSSK;

	public ChannelMaker(FreenetURI singalSSK) throws MalformedURLException {
		if (singalSSK != null && singalSSK.isSSK()) {
			this.signalSSK = singalSSK;
		} else {
			this.signalSSK = CENOClient.nodeInterface.generateKeyPair()[0];
		}
	}

	public void establishChannel() throws MalformedURLException, InsertException {
		FreenetURI bridgeKey = new FreenetURI(CENOClient.bridgeKey);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PersistenceDisabledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
