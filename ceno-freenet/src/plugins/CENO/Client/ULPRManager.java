package plugins.CENO.Client;

import java.util.Hashtable;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Common.URLtoUSKTools;
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
			RequestSender.getInstance().removeFromBatch(url);
		}

		public void onFailure(FetchException e, ClientGetter state) {
			//TODO Handle not fatal errors (like permanent redirections)
			if (e.getMode() == FetchExceptionMode.PERMANENT_REDIRECT) {
				try {
					initULPR(url, e.newURI.getEdition());
				} catch (CENOException e1) {
					// Unlikely to happen
					e1.printStackTrace();
				}
			} else {
				updateULPRStatus(url, ULPRStatus.failed);
				Logger.error(this, "ULPR failed for a url, Exception: " + e.getMessage());
				Logger.normal(this, "ULPR failed for url: " + url + " Exception: " + e.getMessage());
			}
		}

		public void onResume(ClientContext context) throws ResumeFailedException {}

		public RequestClient getRequestClient() {
			return CENOClient.nodeInterface.getRequestClient();
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

	public static ULPRStatus lookupULPR(String url) throws CENOException {
		//TODO Experiment with -1 as suggested edition
		return lookupULPR(url, 0);
	}
	
	public static ULPRStatus lookupULPR(String url, long suggestedEdition) throws CENOException {
		if (!urlExistsInTable(url)) {
			ulprManager.initULPR(url, suggestedEdition);
		}
		return getULPRStatus(url);
	}

	private static boolean urlExistsInTable(String url) {
		return ulprManager.ulprTable.containsKey(url);
	}

	public static ULPRStatus getULPRStatus(String url) {
		return ulprManager.ulprTable.get(url);
	}

	private static void updateULPRStatus(String url, ULPRStatus status) {
		ulprManager.ulprTable.put(url, status);
	}

	private void initULPR(String url, long newEdition) throws CENOException {
		updateULPRStatus(url, ULPRStatus.starting);
		FreenetURI calculatedUSK = null;
		try {
			calculatedUSK = URLtoUSKTools.computeUSKfromURL(url, CENOClient.getBridgeKey());
		} catch (Exception e) {
			Logger.error(this, "Could not calculate USK for a URL");
			Logger.normal(this, "Could not calculate USK for URL: " + url);
			updateULPRStatus(url, ULPRStatus.couldNotStart);
			throw new CENOException(CENOErrCode.LCS_HANDLER_URL_TO_USK);
		}
		calculatedUSK = calculatedUSK.setSuggestedEdition(newEdition);
		try {
			CENOClient.nodeInterface.fetchULPR(calculatedUSK, new ULPRGetCallback(url));
		} catch (FetchException e) {
			Logger.error(this, "Could not start ULPR for a URL");
			Logger.normal(this, "Could not start ULPR for URL: " + url);
			updateULPRStatus(url, ULPRStatus.couldNotStart);
			throw new CENOException(CENOErrCode.LCS_LOOKUP_ULPR_INIT);
		}
		updateULPRStatus(url, ULPRStatus.inProgress);
	}

}