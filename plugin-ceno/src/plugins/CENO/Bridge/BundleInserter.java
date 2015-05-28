package plugins.CENO.Bridge;

import java.io.IOException;
import java.util.Map;

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

		Map<String, String> splitMap = URLtoUSKTools.splitURL(url);

		FreenetURI insertKey = URLtoUSKTools.computeInsertURI(splitMap.get("domain"), CENOBridge.initConfig.getProperty("insertURI"));
		insertFreesite(insertKey, splitMap.get("extraPath"), bundle.getContent(), insertCallback);
	}

	public static void insertFreesite(FreenetURI insertURI, String docName, String content, ClientPutCallback insertCallback) throws IOException, InsertException {
		CENOBridge.nodeInterface.insertFreesite(insertURI, docName, content, insertCallback);
	}

}