package plugins.CENO.Bridge;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import plugins.CENO.Common.URLtoUSKTools;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.support.Logger;
import freenet.support.api.Bucket;
import freenet.support.io.ResumeFailedException;

public class BundleInserter {

	private static final BundleInserter INSTANCE = new BundleInserter();

	private static final long SHOULD_REINSERT = TimeUnit.HOURS.toMillis(5);
	private final int MAX_CONCURRENT_INSERTIONS = 10;

	private volatile Integer concurrentInsertions = 0;
	private Hashtable<String, Date> insertTable = new Hashtable<String, Date>();
	private Queue<InsertionStruct> insertionsQueue = new ConcurrentLinkedQueue<InsertionStruct>();

	private BundleInserter() {}

	public static synchronized BundleInserter getInstance() {
		return INSTANCE;
	}


	private void addToQueue(InsertionStruct insertionStruct) {
		synchronized (concurrentInsertions) {
			if (concurrentInsertions <= MAX_CONCURRENT_INSERTIONS) {
				// If Bundle Inserter has not reached the maximum concurrent insertions,
				// then insert the bundle immediately rather than offering it to the queue
				doInsert(insertionStruct);
			} else {
				insertionsQueue.add(insertionStruct);
			}
		}
	}

	private void processQueue() {
		synchronized (concurrentInsertions) {
			while (concurrentInsertions <= MAX_CONCURRENT_INSERTIONS && !insertionsQueue.isEmpty()) {
				doInsert(insertionsQueue.poll());
			}
		}
	}

	public boolean shouldInsert(String url) {
		if (!insertTable.containsKey(url) || new Date().getTime() - insertTable.get(url).getTime() > SHOULD_REINSERT) {
			return true;
		} else {
			return false;
		}
	}

	public void updateTableEntry(String url) {
		insertTable.put(url, new Date(new Date().getTime()));
	}

	private synchronized void doInsert(InsertionStruct insertionStruct) {
		if (shouldInsert(insertionStruct.url)) {
			try {
				CENOBridge.nodeInterface.insertBundleManifest(insertionStruct.insertURI, insertionStruct.content, insertionStruct.docName, insertionStruct.insertCb);
			} catch (IOException e) {
				Logger.error(this, "Failed to initiate insertion for URL: " + insertionStruct.url + " IO Error: " + e.getMessage());
				return;
			} catch (InsertException e) {
				Logger.error(this, "Failed to initiate insertion for URL: " + insertionStruct.url + " Insert Error: " + e.getMessage());
				addToQueue(insertionStruct);
				return;
			}
			concurrentInsertions++;
			updateTableEntry(insertionStruct.url);
		}
	}

	public void insertBundle(String url) throws IOException, InsertException {
		InsertCallback insertCb = new InsertCallback(url);
		insertBundle(url, insertCb);
	}

	public void insertBundle(String url, ClientPutCallback insertCallback) throws IOException, InsertException {
		Bundle bundle = new Bundle(url);
		bundle.requestFromBundler();
		insertBundle(url, bundle, insertCallback);
	}

	public void insertBundle(String url, Bundle bundle) throws IOException, InsertException {
		insertBundle(url, bundle, new InsertCallback(url));
	}

	public void insertBundle(String url, Bundle bundle, ClientPutCallback insertCallback) throws IOException, InsertException {
		if (bundle.getContent().isEmpty()) {
			throw new IOException("Bundle content for url " + url + " was empty, will not insert");
		}

		FreenetURI insertKey = URLtoUSKTools.computeInsertURI(url, CENOBridge.initConfig.getProperty("insertURI"));
		Logger.normal(BundleInserter.class, "Initiating bundle insertion for URL: " + url);
		insertBundleManifest(url, insertKey, null, bundle.getContent(), insertCallback);
	}

	private void insertBundleManifest(String url, FreenetURI insertURI, String docName, String content, ClientPutCallback insertCallback) throws IOException, InsertException {
		addToQueue(new InsertionStruct(content, url, docName, insertCallback, insertURI));
	}

	private class InsertionStruct {
		String content, url, docName;
		ClientPutCallback insertCb;
		FreenetURI insertURI;

		public InsertionStruct(String content, String url, String docName, ClientPutCallback insertCallback, FreenetURI insertURI) {
			this.content = content;
			this.url = url;
			this.docName = docName;
			this.insertCb = insertCallback;
			this.insertURI = insertURI;
		}
	}

	public class InsertCallback implements ClientPutCallback {
		protected FreenetURI cachedURI;
		protected String url;

		public InsertCallback(String url) {
			this.url = url;
		}

		public void onGeneratedURI(FreenetURI freenetUri, BaseClientPutter state) {
			this.cachedURI = freenetUri;
		}

		public void onGeneratedMetadata(Bucket metadata, BaseClientPutter state) {
		}

		public void onFetchable(BaseClientPutter state) {
		}

		public void onSuccess(BaseClientPutter state) {
			Logger.normal(this, "Bundle caching for URL " + url + " successful: " + cachedURI);
			synchronized (concurrentInsertions) {
				concurrentInsertions--;
				processQueue();
			}
		}

		public void onFailure(InsertException e, BaseClientPutter state) {
			Logger.error(this, "Failed to insert bundle for URL " + url + " Error Message: " + e);
			synchronized (concurrentInsertions) {
				concurrentInsertions--;
				processQueue();
			}
		}

		public void onResume(ClientContext context) throws ResumeFailedException {
		}

		public RequestClient getRequestClient() {
			return CENOBridge.nodeInterface.getRequestClient();
		}

	}

}
