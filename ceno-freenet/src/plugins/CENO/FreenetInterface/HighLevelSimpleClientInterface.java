package plugins.CENO.FreenetInterface;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.HighLevelSimpleClient;
import freenet.client.InsertBlock;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.BaseManifestPutter;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.ClientPutter;
import freenet.client.async.DefaultManifestPutter;
import freenet.client.async.PersistenceDisabledException;
import freenet.client.async.TooManyFilesInsertException;
import freenet.keys.FreenetURI;
import freenet.node.Node;
import freenet.node.RequestClient;
import freenet.node.RequestStarter;
import freenet.support.Logger;
import freenet.support.SimpleReadOnlyArrayBucket;
import freenet.support.api.Bucket;
import freenet.support.api.RandomAccessBucket;
import freenet.support.io.BucketTools;

public class HighLevelSimpleClientInterface {

	protected static final boolean realTimeFlag = false;

	private static volatile HighLevelSimpleClientInterface HLSCInterface = null;
	private HLSCRequestClient requestClient;

	private HighLevelSimpleClient client;
	private Node node;

	public class HLSCRequestClient implements RequestClient {

		public boolean persistent() {
			return false;
		}

		public boolean realTimeFlag() {
			return realTimeFlag;
		}

	}

	private HighLevelSimpleClientInterface() {
	}

	public HighLevelSimpleClientInterface(Node node) {
		synchronized (HighLevelSimpleClientInterface.class) {
			if (HLSCInterface == null) {
				HLSCInterface = new HighLevelSimpleClientInterface();
				HLSCInterface.node = node;
				HLSCInterface.client = node.clientCore.makeClient(RequestStarter.MAXIMUM_PRIORITY_CLASS, false, false);
				HLSCInterface.requestClient = new HLSCRequestClient();
			}
		}
	}

	public HighLevelSimpleClientInterface(Node node, HighLevelSimpleClient hlSimpleClient) {
		synchronized (HighLevelSimpleClientInterface.class) {
			if (HLSCInterface == null) {
				HLSCInterface = new HighLevelSimpleClientInterface();
				HLSCInterface.client = hlSimpleClient;
				HLSCInterface.node = node;
				HLSCInterface.requestClient = new HLSCRequestClient();
			}
		}
	}

	/**
	 * Generates a new key pair, consisting of the insert URI at index 0 and the
	 * request URI at index 1.
	 *
	 * @param docName
	 *            The document name
	 * @return An array containing the insert and request URI
	 */
	public static FreenetURI[] generateKeyPair(String docName) {
		FreenetURI[] keyPair = HLSCInterface.client.generateKeyPair(docName);
		return keyPair;
	}

	public static FetchContext getFetchContext() {
		return HLSCInterface.client.getFetchContext();
	}

	public static InsertContext getInsertContext(boolean b) {
		return HLSCInterface.client.getInsertContext(b);
	}

	public static RequestClient getRequestClient() {
		return HLSCInterface.requestClient;
	}

	/**
	 * Synchronously fetch a file from Freenet, given a FreenetURI
	 * 
	 * @param uri of the file to be fetched
	 * @return a FetchResult instance upon successful fetch
	 * @throws FetchException
	 */
	public static FetchResult fetchURI(FreenetURI uri) throws FetchException {
		FetchResult result = HLSCInterface.client.fetch(uri);
		return result;
	}

	/**
	 * Asynchronously fetch a file from Freenet, given a FreenetURI
	 * 
	 * @param uri of the file to be fetched
	 * @return a FetchResult instance upon successful fetch
	 * @throws FetchException
	 */
	public static ClientGetter fetchURI(FreenetURI uri, long maxSize, 
			ClientGetCallback callback, FetchContext fctx) throws FetchException {
		return HLSCInterface.client.fetch(uri, maxSize, callback, fctx);
	}

	//	/**
	//	 * Non-blocking insert.
	//	 * @param isMetadata If true, insert metadata.
	//	 * @param cb Will be called when the insert completes. If the request is persistent
	//	 * this will be called on the database thread with a container parameter.
	//	 * @param ctx Insert context so you can customise the insertion process.
	//	 */
	//	public static ClientPutter insert(InsertBlock insert, String filenameHint, boolean isMetadata, InsertContext ctx, ClientPutCallback cb) throws InsertException {
	//		ClientPutter clientPutter = HLSCInterface.client.insert(insert, filenameHint, isMetadata, ctx, cb);
	//		return clientPutter;
	//	}

	public static FreenetURI insert(InsertBlock insert, boolean getCHKOnly, String filenameHint) throws InsertException {
		return HLSCInterface.client.insert(insert, getCHKOnly, filenameHint);
	}

	/**
	 * Non-blocking insert.
	 * @param isMetadata If true, insert metadata.
	 * @param cb Will be called when the insert completes. If the request is persistent
	 * this will be called on the database thread with a container parameter.
	 * @param ctx Insert context so you can customise the insertion process.
	 */
	public static ClientPutter insert(InsertBlock insert, String filenameHint, boolean isMetadata, InsertContext ctx, ClientPutCallback cb, short priority) throws InsertException {
		return HLSCInterface.client.insert(insert, filenameHint, isMetadata, ctx, cb, priority);
	}

	public static FreenetURI insertManifest(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName) throws InsertException {
		return HLSCInterface.client.insertManifest(insertURI, bucketsByName, defaultName);
	}

	public static FreenetURI insertManifest(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName, short priorityClass) throws InsertException {
		return HLSCInterface.client.insertManifest(insertURI, bucketsByName, defaultName, priorityClass);
	}

	public static FreenetURI insertManifestCb(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName, short priorityClass, byte[] forceCryptoKey, ClientPutCallback insertCb) throws InsertException {
		DefaultManifestPutter putter;
		try {
			putter = new DefaultManifestPutter(insertCb, BaseManifestPutter.bucketsByNameToManifestEntries(bucketsByName), priorityClass, insertURI, defaultName, getInsertContext(true), false, forceCryptoKey, HLSCInterface.node.clientCore.clientContext);
		} catch (TooManyFilesInsertException e) {
			Logger.warning(HighLevelSimpleClientInterface.class, "TooManyFiles in a single directory to fit in a single Manifest file, will not insert URI: " + insertURI.toString());
			return null;
		}
		try {
			HLSCInterface.node.clientCore.clientContext.start(putter);
		} catch (PersistenceDisabledException e) {
			Logger.warning(HighLevelSimpleClientInterface.class, "Could not start Manifest insertion for URI: " + insertURI.toString() + " Error: " + e.getMessage());
			return null;
		}
		return insertURI;
	}

	public static FreenetURI insertSingleChunk(FreenetURI uri, String content, ClientPutCallback cb) throws InsertException, PersistenceDisabledException, UnsupportedEncodingException {
		RandomAccessBucket b = new SimpleReadOnlyArrayBucket(content.getBytes("UTF-8"));

		InsertContext ctx = HLSCInterface.node.clientCore.makeClient((short)0, true, false).getInsertContext(true);

		ClientPutter clientPutter = new ClientPutter(cb, b, uri,
				null, // Modern ARKs easily fit inside 1KB so should be pure SSKs => no MIME type; this improves fetchability considerably
				ctx,
				RequestStarter.INTERACTIVE_PRIORITY_CLASS, false, null, false, HLSCInterface.node.clientCore.clientContext, null, -1L);
		HLSCInterface.node.clientCore.clientContext.start(clientPutter);
		return uri;
	}

	static Bucket getBucketFromString(String content) throws IOException {
		// If the content is an HTML/XML file and there are hidden characters before the <html> opening tag, remove them
		int bucketLength, index = 0;
		if (content.indexOf("<") != 0) {
			if (content.substring(0, 50).matches("(?ui)[\\s\\S]*\\<(!DOCTYPE)?\\s*(html|\\?xml)[\\s\\S]*")) {
				//FIXME: Instead of creating a substring, find the index of the byte corresponding to "<" and assign it
				// to the index variable. indexOf("<") won't work because of multi-byte UTF-8 characters
				content = content.substring(content.indexOf("<"));
			}
		}

		bucketLength = content.getBytes().length - index;
		Bucket bucket = HLSCInterface.node.clientCore.tempBucketFactory.makeBucket(bucketLength);
		BucketTools.copyFrom(bucket, new ByteArrayInputStream(content.getBytes(), index, bucketLength), bucketLength);
		return bucket;
	}

}
