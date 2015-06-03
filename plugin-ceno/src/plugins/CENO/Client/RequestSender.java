package plugins.CENO.Client;

import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

public class RequestSender {

	private static RequestSender requestSender = null;
	private Hashtable<String, Date> requestTable;
	private String[] bridgeFreemails;

	/**
	 * Time to wait since a request for a URL was originally received from CC before sending
	 * a freemail to the bridge.
	 */
	private static final long SHOULD_SEND_FREEMAIL = TimeUnit.MINUTES.toMillis(5);
	/**
	 * Time to wait before sending a new freemail request for the same URL
	 */
	private static final long REQUEST_TIMEOUT = TimeUnit.MINUTES.toMillis(45);

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
		if (url != null && !url.isEmpty()) {
			if (!requestSender.requestTable.containsKey(url)) {
				requestSender.requestTable.put(url, new Date());
			}
			if (shouldSendFreemail(url)) {
				synchronized (requestSender.bridgeFreemails) {
					CENOClient.nodeInterface.sendFreemail(CENOClient.clientFreemail, requestSender.bridgeFreemails, url, "", "CENO");
					for (String freemailTo : requestSender.bridgeFreemails) {
						CENOClient.nodeInterface.clearOutboxMessages(CENOClient.clientFreemail, freemailTo);
					}
				}
			}
		}
	}

	private static boolean shouldSendFreemail(String url) {
		if (new Date().getTime() - requestSender.requestTable.get(url).getTime() > SHOULD_SEND_FREEMAIL) {
			requestSender.requestTable.put(url, new Date(new Date().getTime() + REQUEST_TIMEOUT - SHOULD_SEND_FREEMAIL));
			return true;
		} else {
			return false;
		}
	}

}