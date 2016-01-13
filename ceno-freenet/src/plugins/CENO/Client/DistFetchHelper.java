package plugins.CENO.Client;

import plugins.CENO.FreenetInterface.NodeInterface.NewURICallback;
import freenet.client.FetchException;
import freenet.keys.FreenetURI;
import freenet.support.Logger;

public class DistFetchHelper {
	
	public static void fetchDist(FreenetURI uri, String successMsg, String failureMsg) {
		try {
			CENOClient.nodeInterface.distFetchURI(uri, CENOClient.nodeInterface.getVoidGetCallback(successMsg, failureMsg, new DistFetchNewURI()));
		} catch (FetchException e) {
			Logger.normal(DistFetchHelper.class, "FetchException while trying to fetch " + uri + " from the distributed cache: " + e.getMessage());
		}
	}

	public static class DistFetchNewURI implements NewURICallback {
		@Override
		public void handleNewURI(FreenetURI newURI, String successMessage, String failureMessage) throws FetchException {
			CENOClient.nodeInterface.distFetchURI(newURI, CENOClient.nodeInterface.getVoidGetCallback("Fetched CENO Portal feeds.json from the distributed cache",
					"Failed to fetch feeds.json from the distributed cache", new DistFetchNewURI()));
		}

	}

}
