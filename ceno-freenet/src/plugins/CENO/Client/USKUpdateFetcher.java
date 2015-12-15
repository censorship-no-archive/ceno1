package plugins.CENO.Client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import plugins.CENO.Common.URLtoUSKTools;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
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
		long suggestedEdition;
		if (uskUri.isSSK()) {
			suggestedEdition = 0L;
		} else if (uskUri.isUSK()) {
			suggestedEdition = (uskUri.getSuggestedEdition() < 0) ? 0L : uskUri.getSuggestedEdition();
		} else {
			return false;
		}

		USK usk;
		try {
			usk = new USK(uskUri.getRoutingKey(), uskUri.getCryptoKey(), uskUri.getExtra(), uskUri.getDocName(), suggestedEdition);
		} catch (MalformedURLException e) {
			Logger.warning(USKUpdateFetcher.class, "Could not subscribe to updates of Malformed USK");
			return false;
		}

		CENOClient.nodeInterface.subscribeToUSK(usk, new USKUpdateFetcher.USKUpdateCb());

		return true;
	}

	public static boolean subscribeFetchUSK(String url) {
		if (url != null && !url.isEmpty()) {
			try {
				url = URLtoUSKTools.validateURL(url);
			} catch (MalformedURLException e) {
				Logger.warning(USKUpdateFetcher.class, "URL failed validation: " + url + " msg: " + e.getMessage());
				return false;
			}
		}
		try {
			subscribeFetchUSK(URLtoUSKTools.getPortalFeedsUSK(CENOClient.BRIDGE_KEY));
		} catch (MalformedURLException e) {
			return false;
		}

		return true;
	}

	public static boolean subscribeToBridgeFeeds() {
		FreenetURI bridgeUri;
		USK feedsUSK;
		try {
			bridgeUri = URLtoUSKTools.computeUSKfromURL(URLtoUSKTools.PORTAL_DOC_NAME, CENOClient.getBridgeKey());
			if (bridgeUri.getSuggestedEdition() < 0) {
				bridgeUri = bridgeUri.setSuggestedEdition(-bridgeUri.getSuggestedEdition());
			}
			feedsUSK = new USK(bridgeUri.getRoutingKey(), bridgeUri.getCryptoKey(), bridgeUri.getExtra(), bridgeUri.getDocName(), bridgeUri.getSuggestedEdition());
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
			fetchNewEdition(key.getURI().setSuggestedEdition(l));
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
			if (!uri.hasMetaStrings()) {
				uri = uri.addMetaStrings(new String[]{"default.html"});
			}
			try {
				fetchResult = CENOClient.nodeInterface.fetchURI(uri);
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
					} finally {
						fetchNewEdition(uri);
					}
					return;

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
			JSONObject obj = null;
			try {
				obj = (JSONObject)parser.parse(result.asByteArray());
			} catch (ParseException e) {
				Logger.error(USKUpdateCb.class, "Could not parse feeds.json");
				return;
			} catch (IOException e) {
				Logger.error(USKUpdateCb.class, "IOException while parsing feeds.json");
				return;
			}

			JSONArray feeds = (JSONArray) obj.get("feeds");
			if (feeds == null) {
				Logger.warning(USKUpdateCb.class, "Retrieved feeds.json without any feeds");
				return;
			}

			for (Object feed : feeds) {
				String url = ((JSONObject)feed).get("url").toString();
				if (url != null && !url.isEmpty()) {
					subscribeFetchUSK(url);
				}
			}

		}

	}

}
