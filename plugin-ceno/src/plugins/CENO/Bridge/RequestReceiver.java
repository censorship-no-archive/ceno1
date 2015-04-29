package plugins.CENO.Bridge;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import freenet.client.InsertException;
import freenet.support.Logger;

public class RequestReceiver {

	protected volatile List<String> freemailBoxes;
	protected FreemailBoxLooper fmBoxLooper;
	private Thread looperThread;

	public RequestReceiver(String[] freemailBoxesArray) {
		this.freemailBoxes = new LinkedList<String>();
		for (String freemailBox : freemailBoxesArray) {
			addFreemailBox(freemailBox);
		}
	}

	public void loopFreemailBoxes() {
		fmBoxLooper = new FreemailBoxLooper();
		looperThread = new Thread(fmBoxLooper);
		looperThread.start();
	}

	public void stopLooping() {
		looperThread.interrupt();
	}

	public void addFreemailBox(String freemailBox) {
		synchronized (freemailBoxes) {
			this.freemailBoxes.add(freemailBox);
		}
	}

	public void removeFreemailBox(String freemailBox) {
		synchronized (freemailBoxes) {
			this.freemailBoxes.remove(freemailBox);
		}
	}

	public class FreemailBoxLooper implements Runnable {

		private volatile boolean continueLoop;

		public FreemailBoxLooper() {
			continueLoop = true;
		}

		public void run() {
			String urlsRequested[];
			try {
				while (continueLoop) {
					for (String freemailBox : freemailBoxes) {
						urlsRequested = CENOBridge.nodeInterface.getUnreadMailsSubject(freemailBox, "CENO", "INBOX", true);
						if (urlsRequested != null && urlsRequested.length > 0) {
							for (String urlRequested : urlsRequested) {
								try {
									Logger.normal(this, "Received request for URL: " + urlRequested);
									BundleInserter.insertBundle(urlRequested);
								} catch (IOException e) {
									Logger.error(this, "Error while requesting the bundle from BS for URL: " + urlRequested);
									e.printStackTrace();
								} catch (InsertException e) {
									Logger.error(this, "Could not start the insertion of the bundle for the URL: " + urlRequested);
									e.printStackTrace();
								}
							}
						}
					}
					Thread.currentThread().sleep(40000);
				}
			} catch (InterruptedException e) {
				continueLoop = false;
			}
			Logger.normal(this, "FreemailBox Looper thread terminated.");
		}
	}

}