package plugins.CENO.Client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import freenet.client.FetchException;
import freenet.client.FetchResult;
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
	
	public static boolean subscribeToBridgeFeeds() {
		FreenetURI bridgeUri;
		USK feedsUSK;
		try {
			bridgeUri = new FreenetURI(CENOClient.bridgeKey.replaceFirst("SSK", "USK") + CENOClient.B64_PORTAL_DOC_NAME + "/0/");
			feedsUSK = new USK(bridgeUri.getRoutingKey(), bridgeUri.getCryptoKey(), bridgeUri.getExtra(), CENOClient.B64_PORTAL_DOC_NAME, bridgeUri.getSuggestedEdition());
		} catch (MalformedURLException e) {
			Logger.error(USKUpdateFetcher.class, "Could not calculate the USK of CENO Portal feeds json");
			return false;
		}
		
		
		CENOClient.nodeInterface.subscribeToUSK(feedsUSK, new USKUpdateFetcher.USKUpdateCb(true));

		return true;
	}

	private static class USKUpdateCb implements USKCallback {
		private boolean fetchSubList = false;
		
		public USKUpdateCb() {
		}
		
		public USKUpdateCb(boolean fetchSublist) {
			this.fetchSubList = fetchSublist;
		}

		@Override
		public void onFoundEdition(long l, USK key, ClientContext context,
				boolean metadata, short codec, byte[] data,
				boolean newKnownGood, boolean newSlotToo) {
			fetchNewEdition(key.getURI());
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
			FetchResult fetchResult = null;
			try {
				fetchResult = CENOClient.nodeInterface.fetchURI(uri.setDocName("default.html"));
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
					
				case RECENTLY_FAILED :
					try {
						Thread.sleep(TimeUnit.MINUTES.toMillis(10));
					} catch (InterruptedException e1) {
						// No big deal
					}
					subscribeFetchUSK(uri);
					break;

				default:
					Logger.warning(USKUpdateFetcher.class,
							"Exception while fetching new edition for USK: " + uri + ", " + e.getMessage());
					break;
				}
				if (e.isDefinitelyFatal()) {
					Logger.error(USKUpdateFetcher.class,
							"Fatal error while fetching new edition for USK: " + uri + ", " + e.getMessage());
					return;
				}
			}
			
			if (fetchSubList && fetchResult != null) {
				subscribeToFeeds(fetchResult);
			}
			return;
		}

		private void subscribeToFeeds(FetchResult result) {
			JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
			Object obj = null;
			try {
				obj = parser.parse(result.asByteArray());
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// TODO Continue with the parsing and fetching of articles lists
			System.out.print(obj.toString());
		}

	}

}
