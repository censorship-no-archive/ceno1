package plugins.CENO.Client;

import java.util.Hashtable;

import plugins.CENO.URLtoUSKTools;

import com.db4o.ObjectContainer;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.keys.FreenetURI;

public class ULPRManager {
	private static volatile ULPRManager ulprManager = null;
	private Hashtable<String, ulprStatus> ulprTable;

	public enum ulprStatus {
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
			ULPRManager.updateRequestStatus(url, ulprStatus.succeeded);
		}

		public void onFailure(FetchException e, ClientGetter state,
				ObjectContainer container) {
			ULPRManager.updateRequestStatus(url, ulprStatus.failed);
		}

	}
	
	private ULPRManager() {
		this.ulprTable = new Hashtable<String, ULPRManager.ulprStatus>();
	}

	public static void init() {
		synchronized (ULPRManager.class) {
			if(ULPRManager.ulprManager == null) {
				ULPRManager.ulprManager = new ULPRManager();
			}	
		}
	}

	public static ulprStatus ulprLookup(String url) {
		if (ulprManager.ulprTable.containsKey(url)) {
			return ulprManager.ulprTable.get(url);
		} else {
			return ulprManager.initULPR(url);
		}
	}

	private ulprStatus initULPR(String url) {
		FreenetURI calculatedUSK = null;
		try {
			calculatedUSK = URLtoUSKTools.computeUSKfromURL(url, CENOClient.bridgeKey);
		} catch (Exception e) {
			return ulprStatus.couldNotStart;
		}
		try {
			CENOClient.nodeInterface.fetchULR(calculatedUSK, new ULPRGetCallback(url));
		} catch (FetchException e) {
			e.printStackTrace();
			return ulprStatus.couldNotStart;
		}
		return ulprStatus.inProgress;
	}
	
	public static void updateRequestStatus(String url, ulprStatus status) {
		if (ulprManager.ulprTable.contains(url)) {
			ulprManager.ulprTable.put(url, status);
		}
	}

}