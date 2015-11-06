package plugins.CENO.Client;

import java.net.MalformedURLException;

import freenet.client.FetchException;
import freenet.client.async.ClientContext;
import freenet.client.async.USKCallback;
import freenet.keys.FreenetURI;
import freenet.keys.USK;
import freenet.support.Logger;

public class USKUpdateFetcher {

	public static boolean subscribeFetchUSK(FreenetURI uskUri) {
		if (!uskUri.isUSK() && !uskUri.isSSK()) {
			return false;
		}
		USK usk;
		try {
			usk = new USK(uskUri.getRoutingKey(), uskUri.getCryptoKey(), uskUri.getExtra(), uskUri.getDocName(), uskUri.getSuggestedEdition());
		} catch (MalformedURLException e) {
			Logger.warning(USKUpdateFetcher.class, "Could not subscribe to updates of Malformed USK");
			return false;
		}

		CENOClient.nodeInterface.subscribeToUSK(usk, new USKUpdateFetcher.USKUpdateCb());

		return true;
	}

	private static class USKUpdateCb implements USKCallback {

		@Override
		public void onFoundEdition(long l, USK key, ClientContext context,
				boolean metadata, short codec, byte[] data,
				boolean newKnownGood, boolean newSlotToo) {
			FreenetURI lookupKey = null;
			try {
				lookupKey = new FreenetURI(key.toString());
			} catch (MalformedURLException e) {
				// Unlike to happen
				e.printStackTrace();
			}
			fetchNewEdition(lookupKey);
		}

		@Override
		public short getPollingPriorityNormal() {
			return 0;
		}

		@Override
		public short getPollingPriorityProgress() {
			return 0;
		}

		private void fetchNewEdition(FreenetURI uri) {
			try {
				CENOClient.nodeInterface.fetchURI(uri);
			} catch (FetchException e) {
				switch (e.getMode()) {
				case PERMANENT_REDIRECT :
					fetchNewEdition(e.newURI);
					break;

				case ALL_DATA_NOT_FOUND :
				case DATA_NOT_FOUND :
					Logger.warning(USKUpdateFetcher.class, 
							"Found new edition of USK but could not fetch data for USK: " + uri);
					break;

				default:
					Logger.warning(USKUpdateFetcher.class,
							"Exception while fetching new edition for USK: " + uri + ", " + e.getMessage());
					break;
				}
			}
		}

	}

}
