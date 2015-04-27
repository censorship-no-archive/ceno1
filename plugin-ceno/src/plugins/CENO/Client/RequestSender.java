package plugins.CENO.Client;

import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

public class RequestSender {

	private static RequestSender requestSender = null;
	private Hashtable<String, Date> requestTable;
	private String[] bridgeFreemails;

	private static final long REQUEST_TIMEOUT = TimeUnit.MINUTES.toMillis(30);

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
			if (requestExpired(url)) {
				CENOClient.nodeInterface.sendFreemail(CENOClient.clientFreemail, requestSender.bridgeFreemails, url, "", "CENO");
				requestSender.requestTable.put(url, new Date());
			}
		}
	}

	private static boolean requestExpired(String url) {
		if (requestSender.requestTable.containsKey(url) && new Date().getTime() - requestSender.requestTable.get(url).getTime() < REQUEST_TIMEOUT) {
			return false;
		} else {
			return true;
		}
	}

}