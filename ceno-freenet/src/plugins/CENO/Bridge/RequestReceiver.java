package plugins.CENO.Bridge;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import plugins.CENO.URLtoUSKTools;
import freenet.client.InsertException;
import freenet.support.Logger;

/**
 * Implements the RequestReceiver (RR) agent functionality over freemails 
 */
public class RequestReceiver {

	private static RequestReceiver requestReceiver = null;
	private volatile List<String> freemailBoxes;
	private FreemailBoxLooper fmBoxLooper;
	private Thread looperThread;

	/** The duration in seconds between two consecutive freemailBoxes polling */
	private static final long FREEMAILBOX_POLLING_PAUSE = TimeUnit.SECONDS.toMillis(40);

	private RequestReceiver() {
	}

	/**
	 * RequestReceiver constructor that follows the singleton pattern.
	 * 
	 * @param freemailBoxesArray an array of freemail boxes the request
	 * receiver will poll for freemails
	 */
	public RequestReceiver(String[] freemailBoxesArray) {
		synchronized (RequestReceiver.class) {
			if (requestReceiver == null) {
				requestReceiver = new RequestReceiver();
				requestReceiver.freemailBoxes = new LinkedList<String>();
				for (String freemailBox : freemailBoxesArray) {
					addFreemailBox(freemailBox);
				}
			}
		}
	}

	/**
	 * Starts a thread that polls freemail boxes
	 */
	public void loopFreemailBoxes() {
		if (requestReceiver.fmBoxLooper == null) {
			requestReceiver.fmBoxLooper = new FreemailBoxLooper();
			requestReceiver.looperThread = new Thread(requestReceiver.fmBoxLooper);
			requestReceiver.looperThread.start();
		}
	}

	/**
	 * Stops the running polling thread
	 */
	public void stopLooping() {
		if (requestReceiver.looperThread != null) {
			requestReceiver.looperThread.interrupt();
			Logger.normal(this, "FreemailBox Looper thread terminated.");
			requestReceiver.fmBoxLooper = null;
		}
	}

	/**
	 * Adds a freemailBox in the list of the inboxes that
	 * are being polled
	 * 
	 * @param freemailBox the freemail address to poll for new requests
	 */
	public void addFreemailBox(String freemailBox) {
		synchronized (requestReceiver.freemailBoxes) {
			requestReceiver.freemailBoxes.add(freemailBox);
		}
	}

	/**
	 * Removes a fremailBox from the list of the inboxes that
	 * are being polled
	 * 
	 * @param freemailBox the freemail address to be removed from
	 * the ones that are being polles for new requests
	 */
	public void removeFreemailBox(String freemailBox) {
		synchronized (requestReceiver.freemailBoxes) {
			requestReceiver.freemailBoxes.remove(freemailBox);
		}
	}

	/**
	 * Runnable class that does the freemailBox polling
	 */
	public class FreemailBoxLooper implements Runnable {

		private volatile boolean continueLoop;

		public FreemailBoxLooper() {
			continueLoop = true;
		}

		public void run() {
			String urlsRequested[];
			try {
				while (continueLoop) {
					for (String freemailBox : requestReceiver.freemailBoxes) {
						// Synchronously get the subject of all unread mails in the INBOX folder of that freemailBox
						urlsRequested = CENOBridge.nodeInterface.getUnreadMailsSubject(freemailBox, "CENO", "INBOX", true);

						if (urlsRequested != null && urlsRequested.length > 0) {
							for (String urlRequested : urlsRequested) {
								try {
									Logger.normal(this, "Freemail " + freemailBox + " received request for URL: " + urlRequested);
									try {
										urlRequested = URLtoUSKTools.validateURL(urlRequested);
									} catch (MalformedURLException e) {
										Logger.error(this, "URL failed validation, it will not be processed: " + urlRequested);
										continue;
									}
									// Pass the request to the BundleInserter agent
									if (!BundleInserter.shouldInsert(urlRequested)) {
										Logger.normal(this, "Bundle for URL: " + urlRequested + " is not stale yet, will not re-insert");
										continue;
									}
									BundleInserter.insertBundle(urlRequested);
								} catch (IOException e) {
									Logger.error(this, "I/O exception while requesting/inserting the bundle for URL: " + urlRequested + 
											" Error: " + e.getMessage());
								} catch (InsertException e) {
									Logger.error(this, "Could not insert the bundle for the URL: " + urlRequested 
											+ " Error: " + e.getMessage());
								}
							}
							CENOBridge.nodeInterface.clearOutboxLog(CENOBridge.bridgeFreemail, CENOBridge.clientIdentityRequestURI);
						}
					}
					// Pause the looping thread
					Thread.currentThread().sleep(FREEMAILBOX_POLLING_PAUSE);
				}
			} catch (InterruptedException e) {
				// Interrupted by another thread, normally by stopLooping()
				// Exit the loop
				continueLoop = false;
			} catch (Exception e) {
				Logger.error(this, "Freemailbox looper uncaught exception: " + e.getMessage());
			}
		}
	}

}