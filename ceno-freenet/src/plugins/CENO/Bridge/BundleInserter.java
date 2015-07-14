package plugins.CENO.Bridge;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import plugins.CENO.URLtoUSKTools;
import plugins.CENO.Bridge.BundlerInterface.Bundle;

import com.db4o.ObjectContainer;

import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientPutCallback;
import freenet.keys.FreenetURI;
import freenet.support.Logger;
import freenet.support.api.Bucket;

public class BundleInserter {

	private static Hashtable<String, Date> insertTable = new Hashtable<String, Date>();

	private static final long SHOULD_REINSERT = TimeUnit.HOURS.toMillis(5);

	public static class InsertCallback implements ClientPutCallback {
		protected FreenetURI cachedURI;
		protected String uri;

		public void setUri(String uri) {
			this.uri = uri;
		}

		public void onMajorProgress(ObjectContainer container) {
		}

		public void onGeneratedURI(FreenetURI freenetUri, BaseClientPutter state, ObjectContainer container) {
			this.cachedURI = freenetUri;
		}

		public void onGeneratedMetadata(Bucket metadata, BaseClientPutter state, ObjectContainer container) {
		}

		public void onFetchable(BaseClientPutter state, ObjectContainer container) {
		}

		public void onSuccess(BaseClientPutter state, ObjectContainer container) {
			Logger.normal(this, "Bundle caching for URL " + uri + " successful: " + cachedURI);
		}

		public void onFailure(InsertException e, BaseClientPutter state, ObjectContainer container) {
			Logger.error(this, "Failed to insert bundle for URL " + uri + " Error Message: " + e);
			e.printStackTrace();
		}

	}

	public static void updateTableEntry(String url) {
		insertTable.put(url, new Date(new Date().getTime()));
	}

	public static void insertBundle(String url) throws IOException, InsertException {
		InsertCallback insertCb = new InsertCallback();
		insertCb.setUri(url);
		insertBundle(url, insertCb);
	}

	public static void insertBundle(String url, ClientPutCallback insertCallback) throws IOException, InsertException {
		Bundle bundle = new Bundle(url);
		bundle.requestFromBundler();
		insertBundle(url, bundle, insertCallback);
	}

	public static void insertBundle(String url, Bundle bundle, ClientPutCallback insertCallback) throws IOException, InsertException {
		if (bundle.getContent().isEmpty()) {
			throw new IOException("Bundle content for url " + url + " was empty.");
		}

		FreenetURI insertKey = URLtoUSKTools.computeInsertURI(url, CENOBridge.initConfig.getProperty("insertURI"));
		Logger.normal(BundleInserter.class, "Initiating bundle insertion for URL: " + url);
		insertBundleManifest(url, insertKey, null, bundle.getContent(), insertCallback);
	}

	public static void insertBundleManifest(String url, FreenetURI insertURI, String docName, String content, ClientPutCallback insertCallback) throws IOException, InsertException {
		CENOBridge.nodeInterface.insertBundleManifest(insertURI, content, docName, insertCallback);
		updateTableEntry(url);
	}

	public static void insertFreesite(FreenetURI insertURI, String docName, String content, ClientPutCallback insertCallback) throws IOException, InsertException {
		CENOBridge.nodeInterface.insertFreesite(insertURI, docName, content, insertCallback);
	}

	public static boolean shouldInsert(String url) {
		if (!insertTable.containsKey(url) || new Date().getTime() - insertTable.get(url).getTime() > SHOULD_REINSERT) {
			return true;
		} else {
			return false;
		}
	}

}