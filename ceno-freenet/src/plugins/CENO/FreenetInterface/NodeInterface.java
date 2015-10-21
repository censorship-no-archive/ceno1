package plugins.CENO.FreenetInterface;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.freenetproject.freemail.wot.ConcurrentWoTConnection;

import plugins.CENO.FreenetInterface.ConnectionOverview.NodeConnections;
import freenet.client.ClientMetadata;
import freenet.client.DefaultMIMETypes;
import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertBlock;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.PersistenceDisabledException;
import freenet.client.async.USKCallback;
import freenet.keys.FreenetURI;
import freenet.keys.InsertableClientSSK;
import freenet.keys.USK;
import freenet.node.Node;
import freenet.node.RequestClient;
import freenet.node.RequestStarter;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.Logger;
import freenet.support.api.Bucket;
import freenet.support.api.RandomAccessBucket;
import freenet.support.io.BucketTools;
import freenet.support.io.ResumeFailedException;

public class NodeInterface implements FreenetInterface {

	private Node node;
	private PluginRespirator pr;
	private FetchContext ULPRFC, localFC;
	private ConnectionOverview connectionOverview;
	private ConcurrentWoTConnection wotConnection;

	public NodeInterface(Node node, PluginRespirator pr) {
		this.node = node;
		this.pr = pr;
		this.connectionOverview = new ConnectionOverview(node);
	}

	public void initFetchContexts() {
		// Set up a FetchContext instance for Ultra-lightweight passive requests
		this.ULPRFC = HighLevelSimpleClientInterface.getFetchContext();
		this.ULPRFC.canWriteClientCache = true;
		this.ULPRFC.maxNonSplitfileRetries = -1;
		this.ULPRFC.followRedirects = true;
		this.ULPRFC.allowSplitfiles = true;
		this.ULPRFC.maxRecursionLevel = 10;
		this.ULPRFC.maxTempLength = Long.MAX_VALUE;
		this.ULPRFC.maxOutputLength = Long.MAX_VALUE;

		this.localFC = HighLevelSimpleClientInterface.getFetchContext();
		this.localFC.localRequestOnly = true;
		this.localFC.followRedirects = true;
		this.localFC.allowSplitfiles = true;
		this.localFC.maxRecursionLevel = 10;
		this.localFC.maxTempLength = Long.MAX_VALUE;
		this.localFC.maxOutputLength = Long.MAX_VALUE;

		wotConnection = new ConcurrentWoTConnection(pr);
	}

	@Override
	public FetchResult fetchURI(FreenetURI uri) throws FetchException {
		return HighLevelSimpleClientInterface.fetchURI(uri);
	}

	@Override
	public ClientGetter localFetchURI(FreenetURI uri, ClientGetCallback callback) throws FetchException {
		return HighLevelSimpleClientInterface.fetchURI(uri, Long.MAX_VALUE, callback, localFC);
	}

	@Override
	public ClientGetter fetchULPR(FreenetURI uri, ClientGetCallback callback) throws FetchException {
		return HighLevelSimpleClientInterface.fetchURI(uri, Long.MAX_VALUE, callback, ULPRFC);
	}

	@Override
	public boolean subscribeToUSK(USK origUSK, USKCallback cb) {
		node.clientCore.uskManager.subscribe(origUSK, cb, false, getRequestClient());
		return true;
	}

	/**
	 * Generate a new key pair for SSK insertions
	 * 
	 * @return FreenetURI array where first element is the insertURI and second element is the requestURI
	 */
	@Override
	public FreenetURI[] generateKeyPair() {
		InsertableClientSSK key = InsertableClientSSK.createRandom(node.random, "");
		FreenetURI insertURI = key.getInsertURI();
		FreenetURI requestURI = key.getURI();
		return new FreenetURI[]{insertURI, requestURI};
	}

	@Override
	public RequestClient getRequestClient() {
		return HighLevelSimpleClientInterface.getRequestClient();
	}

	@Override
	public Bucket makeBucket(int length) throws IOException {
		return node.clientCore.persistentTempBucketFactory.makeBucket(length);
	}

	@Override
	public FreenetURI insertBundleManifest(FreenetURI insertURI, String content, String defaultName, ClientPutCallback insertCb) throws IOException, InsertException {
		String defName;
		if (defaultName == null || defaultName.isEmpty()) {
			defName = "default.html";
		} else {
			defName = defaultName;
		}

		Bucket bucket = node.clientCore.tempBucketFactory.makeBucket(content.length());
		BucketTools.copyFrom(bucket, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8), 0, content.length()), content.length());

		HashMap<String, Object> bucketsByName = new HashMap<String, Object>();
		bucketsByName.put(defName, bucket);

		FreenetURI requestURI = HighLevelSimpleClientInterface.insertManifestCb(insertURI, bucketsByName, defName, RequestStarter.INTERACTIVE_PRIORITY_CLASS, null, insertCb);
		return requestURI;
	}

	@Override
	public boolean insertFreesite(FreenetURI insertURI, String docName, String content, ClientPutCallback insertCallback) throws IOException, InsertException {
		String mimeType = DefaultMIMETypes.guessMIMEType(docName, true);
		if(mimeType == null) {
			mimeType = "text/html";
		}

		RandomAccessBucket bucket = node.clientCore.tempBucketFactory.makeBucket(content.length());
		BucketTools.copyFrom(bucket, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8), 0, content.length()), content.length());

		InsertBlock ib = new InsertBlock(bucket, new ClientMetadata(mimeType), insertURI);
		InsertContext ictx = HighLevelSimpleClientInterface.getInsertContext(true);
		HighLevelSimpleClientInterface.insert(ib, docName, false, ictx, insertCallback, RequestStarter.INTERACTIVE_PRIORITY_CLASS);
		return true;
	}

	@Override
	public FreenetURI insertBlock(InsertBlock insert, boolean getCHKOnly, String filenameHint) throws InsertException {
		return HighLevelSimpleClientInterface.insert(insert, getCHKOnly, filenameHint);
	}

	@Override
	public FreenetURI insertManifest(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName, short priorityClass) throws InsertException {
		return HighLevelSimpleClientInterface.insertManifest(insertURI, bucketsByName, defaultName, priorityClass);
	}

	@Override
	public FreenetURI insertSingleChunk(FreenetURI uri, String content, ClientPutCallback cb) throws InsertException, PersistenceDisabledException, UnsupportedEncodingException {
		return HighLevelSimpleClientInterface.insertSingleChunk(uri, content, cb);
	}

	@Override
	public NodeConnections getConnections() {
		return connectionOverview.getConnections();
	}

	@Override
	public boolean sendFreemail(String freemailFrom, String freemailTo[], String subject, String content, String password) {
		return FreemailAPI.sendFreemail(freemailFrom, freemailTo, subject, content, password);
	}

	@Override
	public String[] getUnreadMailsSubject(String freemail, String password, String inboxFolder, boolean shouldDelete) {
		return FreemailAPI.getUnreadMailsSubject(freemail, password, inboxFolder, shouldDelete);
	}

	@Override
	public String[] getMailsContentFrom(String freemail, String freemailFrom, String password, String mailFolder) {
		return FreemailAPI.getMailsContentFrom(freemail, freemailFrom, password, mailFolder);
	}

	@Override
	public boolean copyAccprops(String freemailAccount) {
		return FreemailAPI.copyAccprops(freemailAccount);
	}

	@Override
	public boolean setRandomNextMsgNumber(String freemailAccount, String freemailTo) {
		return FreemailAPI.setRandomNextMsgNumber(freemailAccount, freemailTo);
	}

	@Override
	public boolean clearOutboxLog(String freemailAccount, String identityFrom) {
		return FreemailAPI.clearOutboxLog(freemailAccount, identityFrom);
	}

	@Override
	public boolean clearOutboxMessages(String freemailAccount, String freemailTo) {
		return FreemailAPI.clearOutboxMessages(freemailAccount, freemailTo);
	}

	@Override
	public ClientGetCallback getVoidGetCallback(String successMessage, String failureMessage) {
		return new VoidGetCallback(successMessage, failureMessage);
	}

	@Override
	public ClientPutCallback getVoidPutCallback(String successMessage, String failureMessage) {
		return new VoidPutCallback(successMessage, failureMessage);
	}

	private class VoidPutCallback implements ClientPutCallback {
		String successMessage, failureMessage = null;

		public VoidPutCallback(String successMessage, String failureMessage) {
			this.successMessage = successMessage;
			this.failureMessage = failureMessage;
		}

		@Override
		public void onResume(ClientContext context)	throws ResumeFailedException {
		}

		@Override
		public RequestClient getRequestClient() {
			return getRequestClient();
		}

		@Override
		public void onGeneratedURI(FreenetURI uri, BaseClientPutter state) {
		}

		@Override
		public void onGeneratedMetadata(Bucket metadata, BaseClientPutter state) {
		}

		@Override
		public void onFetchable(BaseClientPutter state) {
		}

		@Override
		public void onSuccess(BaseClientPutter state) {
			Logger.normal(this, successMessage);
		}

		@Override
		public void onFailure(InsertException e, BaseClientPutter state) {
			Logger.error(this, failureMessage + ": " + e.getMessage());
		}

	}

	private class VoidGetCallback implements ClientGetCallback {
		String successMessage, failureMessage = null;

		public VoidGetCallback(String successMessage, String failureMessage) {
			this.successMessage = successMessage;
			this.failureMessage = failureMessage;
		}

		@Override
		public void onResume(ClientContext context) throws ResumeFailedException {
		}

		@Override
		public RequestClient getRequestClient() {
			return getRequestClient();
		}

		@Override
		public void onSuccess(FetchResult result, ClientGetter state) {
			Logger.normal(this, successMessage);
		}

		@Override
		public void onFailure(FetchException e, ClientGetter state) {
			Logger.error(this, failureMessage + ": " + e.getMessage());
		}

	}
}
