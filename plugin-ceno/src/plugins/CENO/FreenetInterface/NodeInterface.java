package plugins.CENO.FreenetInterface;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.freenetproject.freemail.wot.ConcurrentWoTConnection;

import plugins.CENO.Bridge.BundlerInterface.Bundle;
import freenet.client.ClientMetadata;
import freenet.client.DefaultMIMETypes;
import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertBlock;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.client.async.ClientPutCallback;
import freenet.keys.FreenetURI;
import freenet.keys.InsertableClientSSK;
import freenet.node.Node;
import freenet.node.RequestStarter;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.api.Bucket;
import freenet.support.io.BucketTools;

public class NodeInterface implements FreenetInterface {

	private Node node;
	private PluginRespirator pr;
	private FetchContext ULPRFC, localFC;
	private ConcurrentWoTConnection wotConnection;

	public NodeInterface(Node node, PluginRespirator pr) {
		this.node = node;
		this.pr = pr;

		// Set up a FetchContext instance for Ultra-lightweight passive requests
		this.ULPRFC = HighLevelSimpleClientInterface.getFetchContext();
		this.ULPRFC.maxNonSplitfileRetries = -1;
		this.ULPRFC.followRedirects = true;

		this.localFC = HighLevelSimpleClientInterface.getFetchContext();
		this.localFC.localRequestOnly = true;

		wotConnection = new ConcurrentWoTConnection(pr);
	}

	public FetchResult fetchURI(FreenetURI uri) throws FetchException {
		return HighLevelSimpleClientInterface.fetchURI(uri);
	}

	public ClientGetter localFetchURI(FreenetURI uri, ClientGetCallback callback) throws FetchException {
		return HighLevelSimpleClientInterface.fetchURI(uri, Long.MAX_VALUE, callback, localFC);
	}

	public ClientGetter fetchULPR(FreenetURI uri, ClientGetCallback callback) throws FetchException {
		return HighLevelSimpleClientInterface.fetchURI(uri, Long.MAX_VALUE, callback, ULPRFC);
	}

	/**
	 * Generate a new key pair for SSK insertions
	 * 
	 * @return FreenetURI array where first element is the insertURI and second element is the requestURI
	 */
	public FreenetURI[] generateKeyPair() {
		InsertableClientSSK key = InsertableClientSSK.createRandom(node.random, "");
		FreenetURI insertURI = key.getInsertURI();
		FreenetURI requestURI = key.getURI();
		return new FreenetURI[]{insertURI, requestURI};
	}

	public Bucket makeBucket(int length) throws IOException {
		return node.clientCore.persistentTempBucketFactory.makeBucket(length);
	}

	public FreenetURI insertBundleManifest(FreenetURI insertURI, Bundle bundle) throws IOException, InsertException {
		Bucket bucket = node.clientCore.persistentTempBucketFactory.makeBucket(bundle.getContentLength());
		bucket.getOutputStream().write(bundle.getContent().getBytes());
		bucket.setReadOnly();

		HashMap<String, Object> bucketsByName = new HashMap<String, Object>();
		bucketsByName.put("index.html", bucket);

		FreenetURI requestURI = HighLevelSimpleClientInterface.insertManifest(insertURI, bucketsByName, "index.html");

		bucket.free();
		return requestURI;
	}

	public boolean insertFreesite(FreenetURI insertURI, String docName, String content, ClientPutCallback insertCallback) throws IOException, InsertException {
		String mimeType = DefaultMIMETypes.guessMIMEType(docName, true);
		if(mimeType == null) {
			mimeType = "text/html";
		}

		Bucket bucket = node.clientCore.tempBucketFactory.makeBucket(content.length());
		BucketTools.copyFrom(bucket, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8), 0, content.length()), content.length());

		InsertBlock ib = new InsertBlock(bucket, new ClientMetadata(mimeType), insertURI);
		InsertContext ictx = HighLevelSimpleClientInterface.getInsertContext(true);
		HighLevelSimpleClientInterface.insert(ib, docName, false, ictx, insertCallback, RequestStarter.INTERACTIVE_PRIORITY_CLASS);
		return true;
	}

	public FreenetURI insertBlock(InsertBlock insert, boolean getCHKOnly, String filenameHint) throws InsertException {
		return HighLevelSimpleClientInterface.insert(insert, getCHKOnly, filenameHint);
	}

	public FreenetURI insertManifest(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName) throws InsertException {
		return HighLevelSimpleClientInterface.insertManifest(insertURI, bucketsByName, defaultName);
	}

	public FreenetURI insertManifest(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName, short priorityClass) throws InsertException {
		return HighLevelSimpleClientInterface.insertManifest(insertURI, bucketsByName, defaultName, priorityClass);
	}

	public boolean sendFreemail(String freemailFrom, String freemailTo[], String subject, String content, String password) {
		return FreemailAPI.sendFreemail(freemailFrom, freemailTo, subject, content, password);
	}

	public boolean startIMAPMonitor(String freemail, String password, String idleFolder) {
		return FreemailAPI.startIMAPMonitor(freemail, password, idleFolder);
	}

	public String[] getUnreadMailsSubject(String freemail, String password, String inboxFolder, boolean shouldDelete) {
		return FreemailAPI.getUnreadMailsSubject(freemail, password, inboxFolder, shouldDelete);
	}

	public boolean copyAccprops(String freemailAccount) {
		return FreemailAPI.copyAccprops(freemailAccount);
	}

	public boolean setRandomNextMsgNumber(String freemailAccount, String freemailTo) {
		return FreemailAPI.setRandomNextMsgNumber(freemailAccount, freemailTo);
	}

	public boolean clearOutboxLog(String freemailAccount, String identityFrom) {
		return FreemailAPI.clearOutboxLog(freemailAccount, identityFrom);
	}

	public boolean clearOutboxMessages(String freemailAccount, String freemailTo) {
		return FreemailAPI.clearOutboxMessages(freemailAccount, freemailTo);
	}

}
