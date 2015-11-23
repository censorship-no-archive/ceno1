package plugins.CENO.Client;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import plugins.CENO.Client.Signaling.Channel;
import plugins.CENO.Common.URLtoUSKTools;

public class RequestSender {

	private static volatile RequestSender requestSender = new RequestSender();
	private Hashtable<String, Long> requestTable;
	private List<String> batchList;
	private Timer timer;
	private Boolean newUrlArrived = false;

	/**
	 * Time to wait since a request for a URL was originally received from CC before sending
	 * a request to the bridge.
	 */
	private static final long SHOULD_QUEUE_URL = TimeUnit.MINUTES.toMillis(2);

	/**
	 * Time to wait before sending a new request for the same URL
	 */
	private static final long REQUEST_TIMEOUT = TimeUnit.MINUTES.toMillis(40);

	/**
	 * Maximum size in bytes of a batch request in order to fit within the USK chunk
	 */
	private static final long MAX_BATCH_SIZE = 1024;

	private RequestSender() {
		this.requestTable = new Hashtable<String, Long>();
		batchList = new ArrayList<String>();
		timer = new Timer("BatchRequestTimer", true);
		timer.schedule(new BatchReqInserter(), 0, TimeUnit.MINUTES.toMillis(3));
	}

	public void stopTimerTasks() {
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
	}

	public static RequestSender getInstance() {
		return requestSender;
	}

	public void requestFromBridge(String url) {
		if (shouldSignalBridge(url)) {
			synchronized (newUrlArrived) {
				newUrlArrived = true;
				addInBatch(url);
				requestTable.put(url, System.currentTimeMillis() + REQUEST_TIMEOUT - SHOULD_QUEUE_URL);
			}
		}
	}

	public synchronized boolean shouldSignalBridge(String url) {
		String urlValidated = null;
		try {
			urlValidated = URLtoUSKTools.validateURL(url);
		} catch (MalformedURLException e) {
			return false;
		}

		if (!requestTable.containsKey(urlValidated)) {
			requestTable.put(url, System.currentTimeMillis());
		}

		return (System.currentTimeMillis() - requestTable.get(urlValidated) > SHOULD_QUEUE_URL);
	}

	private void addInBatch(String url) {
		synchronized (batchList) {
			if (!batchList.contains(url)) {
				batchList.add(url);
			}
		}
	}

	public void removeFromBatch(String url) {
		synchronized (batchList) {
			batchList.remove(url);
		}
	}

	private class BatchReqInserter extends TimerTask {
		public BatchReqInserter() {}

		@Override
		public void run() {
			synchronized (newUrlArrived) {
				if(!newUrlArrived || !CENOClient.channelMaker.canSend()) {
					return;
				}

				int batchSize = 0;
				StringBuilder batchListStr = new StringBuilder();

				for (int i = batchList.size() - 1; i >= 0; i--) {
					String url = batchList.get(i);
					batchSize += url.getBytes().length;
					if (batchSize < MAX_BATCH_SIZE) {
						batchListStr.append(url);
						batchListStr.append("\n");
					}
				}

				String batchReq = batchListStr.toString();
				if (batchReq.isEmpty()) {
					return;
				}

				if (Channel.insertBatch(batchReq)) {
					newUrlArrived = false;
				}
			}
		}

	}
}
