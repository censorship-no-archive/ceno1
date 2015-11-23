package plugins.CENO.Bridge;

import java.io.IOException;
import java.net.MalformedURLException;

import plugins.CENO.Common.URLtoUSKTools;
import freenet.client.InsertException;
import freenet.support.Logger;

public class RequestReceiver {

	private RequestReceiver() {}
	
	public static void signalReceived(String[] urlList) {
		RequestReceiver.receivedURLList(urlList);
	}

	private static void receivedURLList(String[] urlList) {
		for (String urlRequested : urlList) {
			try {
				urlRequested = URLtoUSKTools.validateURL(urlRequested);
			} catch (MalformedURLException e) {
				Logger.error(BundleInserter.class, "URL failed validation, it will not be processed: " + urlRequested);
				continue;
			}
			// Pass the request to the BundleInserter agent
			if (!BundleInserter.getInstance().shouldInsert(urlRequested)) {
				Logger.normal(BundleInserter.class, "Bundle for URL: " + urlRequested + " is not stale yet, will not re-insert");
				continue;
			}
			try {
				BundleInserter.getInstance().insertBundle(urlRequested);
			} catch (IOException e) {
				Logger.error(BundleInserter.class, "I/O exception while requesting/inserting the bundle for URL: " + urlRequested + 
						" Error: " + e.getMessage());
			} catch (InsertException e) {
				Logger.error(BundleInserter.class, "Could not insert the bundle for the URL: " + urlRequested 
						+ " Error: " + e.getMessage());
			}
		}
	}

}