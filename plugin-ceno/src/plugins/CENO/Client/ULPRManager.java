package plugins.CENO.Client;

import java.util.Hashtable;

import plugins.CENO.URLtoUSKTools;

import com.db4o.ObjectContainer;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.keys.FreenetURI;
import freenet.support.Logger;

public class ULPRManager {

	private static ULPRManager ulprManager = null;
	private Hashtable<String, ulprStatus> ulprTable;

	public enum ulprStatus {
		starting,
		inProgress,
		succeeded,
		failed,
		couldNotStart
	};

	private class ULPRGetCallback implements ClientGetCallback {
		private String url;

		public ULPRGetCallback(String url) {
			this.url = url;
		}

		public void onMajorProgress(ObjectContainer container) {
		}

		public void onSuccess(FetchResult result, ClientGetter state,
				ObjectContainer container) {
			updateULPRStatus(url, ulprStatus.succeeded);
		}

		public void onFailure(FetchException e, ClientGetter state,
				ObjectContainer container) {
			//TODO Handle not fatal errors (like permanent redirections)
			if (e.getMode() == FetchException.PERMANENT_REDIRECT) {
				initULPR(url, e.newURI.getEdition());
			} else {
				updateULPRStatus(url, ulprStatus.failed);
			}
		}

	}

	private ULPRManager() {
		this.ulprTable = new Hashtable<String, ULPRManager.ulprStatus>();
	}

	public static void init() {
		synchronized (ULPRManager.class) {
			if (ulprManager == null) {
				ulprManager = new ULPRManager();
			}	
		}
	}

	public static ulprStatus lookupULPR(String url) {
		if (!urlExistsInTable(url)) {
			ulprManager.initULPR(url);
		}
		return getULPRStatus(url);
	}

	private static boolean urlExistsInTable(String url) {
		return ulprManager.ulprTable.containsKey(url);
	}

	private static ulprStatus getULPRStatus(String url) {
		return ulprManager.ulprTable.get(url);
	}

	private static void updateULPRStatus(String url, ulprStatus status) {
		ulprManager.ulprTable.put(url, status);
	}

	private void initULPR(String url) {
		initULPR(url, 0);
	}

	private void initULPR(String url, long newEdition) {
		updateULPRStatus(url, ulprStatus.starting);
		FreenetURI calculatedUSK = null;
		try {
			calculatedUSK = URLtoUSKTools.computeUSKfromURL(url, CENOClient.bridgeKey);
		} catch (Exception e) {
			updateULPRStatus(url, ulprStatus.couldNotStart);
			Logger.error(this, "Could not start ULPR for URL: " + url);
		}
		calculatedUSK = calculatedUSK.setSuggestedEdition(newEdition);
		try {
			CENOClient.nodeInterface.fetchULPR(calculatedUSK, new ULPRGetCallback(url));
		} catch (FetchException e) {
			e.printStackTrace();
			updateULPRStatus(url, ulprStatus.couldNotStart);
			return;
		}
		updateULPRStatus(url, ulprStatus.inProgress);
	}

}