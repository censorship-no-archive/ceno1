package plugins.CENO.Client;

import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import freenet.support.Logger;

public class RequestSender {

	private static RequestSender requestSender = null;
	private Hashtable<String, Date> requestTable;
	private String[] bridgeFreemails;

	/**
	 * Time to wait since a request for a URL was originally received from CC before sending
	 * a freemail to the bridge.
	 */
	private static final long SHOULD_SEND_FREEMAIL = TimeUnit.MINUTES.toMillis(3);
	/**
	 * Time to wait before sending a new freemail request for the same URL
	 */
	private static final long REQUEST_TIMEOUT = TimeUnit.MINUTES.toMillis(40);

	private RequestSender(String[] bridgeFreemails) {
		this.requestTable = new Hashtable<String, Date>();
		this.bridgeFreemails = bridgeFreemails;
	}

	public static void init(String[] bridgeFreemails) {
		synchronized (RequestSender.class) {
			if (requestSender == null) {
				requestSender = new RequestSender(bridgeFreemails);
			}
		}
	}

	public static void requestFromBridge(String url) {
		if (url == null || url.isEmpty()) {
			return;
		}

		if (shouldSendFreemail(url)) {
			synchronized (requestSender.bridgeFreemails) {
				CENOClient.nodeInterface.sendFreemail(CENOClient.clientFreemail, requestSender.bridgeFreemails, url, "", "CENO");	
				Logger.normal(RequestSender.class, "Sent request to the bridge for URL: " + url);
			}
			requestSender.requestTable.put(url, new Date(new Date().getTime() + REQUEST_TIMEOUT - SHOULD_SEND_FREEMAIL));
		}
	}

	public static boolean shouldSendFreemail(String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}

		if (!requestSender.requestTable.containsKey(url)) {
			requestSender.requestTable.put(url, new Date());
		}

		return (new Date().getTime() - requestSender.requestTable.get(url).getTime() > SHOULD_SEND_FREEMAIL);
	}

}