package plugins.CENO.Client;

import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

public class RequestSender {

	private static RequestSender requestSender = null;
	private Hashtable<String, Date> requestTable;

	private static final long REQUEST_TIMEOUT = TimeUnit.MINUTES.toMillis(15);

	private RequestSender() {
		this.requestTable = new Hashtable<String, Date>();
	}

	public static void init() {
		synchronized (RequestSender.class) {
			if (requestSender == null) {
				requestSender = new RequestSender();
			}	
		}
	}

	public static void requestFromBridge(String url) {
		if (url != null && !url.isEmpty()) {
			if (requestExpired(url)) {
				CENOClient.nodeInterface.sendFreemail(CENOClient.clientFreemail, new String[]{CENOClient.bridgeFreemail}, url, "", "CENO");
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