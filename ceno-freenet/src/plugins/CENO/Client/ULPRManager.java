package plugins.CENO.Client;

import java.util.Hashtable;

import plugins.CENO.URLtoUSKTools;

import com.db4o.ObjectContainer;

import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.client.FetchResult;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.support.Logger;
import freenet.support.io.ResumeFailedException;

public class ULPRManager {

	private static ULPRManager ulprManager = null;
	private Hashtable<String, ULPRStatus> ulprTable;

	public enum ULPRStatus {
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

		public void onSuccess(FetchResult result, ClientGetter state) {
			Logger.normal(this, "ULPR completed successfully for URL: " + url);
			updateULPRStatus(url, ULPRStatus.succeeded);
		}

		public void onFailure(FetchException e, ClientGetter state) {
			//TODO Handle not fatal errors (like permanent redirections)
			if (e.getMode() == FetchExceptionMode.PERMANENT_REDIRECT) {
				initULPR(url, e.newURI.getEdition());
			} else {
				updateULPRStatus(url, ULPRStatus.failed);
				Logger.error(this, "ULPR failed for url: " + url + " Exception: " + e.getMessage());
			}
		}

		public void onResume(ClientContext context)
				throws ResumeFailedException {
			// TODO Auto-generated method stub
			
		}

		public RequestClient getRequestClient() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private ULPRManager() {
		this.ulprTable = new Hashtable<String, ULPRManager.ULPRStatus>();
	}

	public static void init() {
		synchronized (ULPRManager.class) {
			if (ulprManager == null) {
				ulprManager = new ULPRManager();
			}	
		}
	}

	public static ULPRStatus lookupULPR(String url) {
		if (!urlExistsInTable(url)) {
			ulprManager.initULPR(url);
		}
		return getULPRStatus(url);
	}

	private static boolean urlExistsInTable(String url) {
		return ulprManager.ulprTable.containsKey(url);
	}

	private static ULPRStatus getULPRStatus(String url) {
		return ulprManager.ulprTable.get(url);
	}

	private static void updateULPRStatus(String url, ULPRStatus status) {
		ulprManager.ulprTable.put(url, status);
	}

	private void initULPR(String url) {
		initULPR(url, 0);
	}

	private void initULPR(String url, long newEdition) {
		updateULPRStatus(url, ULPRStatus.starting);
		FreenetURI calculatedUSK = null;
		try {
			calculatedUSK = URLtoUSKTools.computeUSKfromURL(url, CENOClient.bridgeKey);
		} catch (Exception e) {
			updateULPRStatus(url, ULPRStatus.couldNotStart);
			Logger.error(this, "Could not start ULPR for URL: " + url);
		}
		calculatedUSK = calculatedUSK.setSuggestedEdition(newEdition);
		try {
			CENOClient.nodeInterface.fetchULPR(calculatedUSK, new ULPRGetCallback(url));
		} catch (FetchException e) {
			e.printStackTrace();
			updateULPRStatus(url, ULPRStatus.couldNotStart);
			return;
		}
		updateULPRStatus(url, ULPRStatus.inProgress);
	}

}