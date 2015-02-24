package plugins.CeNo.FreenetInterface;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import plugins.CeNo.CacheInsertHandler.InsertCallback;
import freenet.client.ClientMetadata;
import freenet.client.DefaultMIMETypes;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertBlock;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.keys.FreenetURI;
import freenet.keys.InsertableClientSSK;
import freenet.node.Node;
import freenet.node.RequestClient;
import freenet.node.RequestStarter;
import freenet.support.api.Bucket;
import freenet.support.api.RandomAccessBucket;
import freenet.support.io.BucketTools;
import freenet.support.io.FileBucket;

public class NodeInterface implements FreenetInterface {

	private Node node;

	public NodeInterface(Node node) {
		this.node = node;
	}

	public FetchResult fetchURI(FreenetURI uri) throws FetchException {
		return HighLevelSimpleClientInterface.fetchURI(uri);
	}

	/**
	 * Generate a new key pair for SSK insertions
	 * 
	 * @return FreenetURI array where 1st element is the insertURI and second element is the requestURI
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

/*
	public boolean insertFreesite(FreenetURI insertURI, String docName, String content, InsertCallback insertCallback) throws IOException, InsertException {
		RandomAccessBucket bucket = node.clientCore.persistentTempBucketFactory.makeBucket(content.length());
		bucket.getOutputStream().write(content.getBytes());
		bucket.setReadOnly();

		HashMap<String, Object> bucketsByName = new HashMap<String, Object>();
		bucketsByName.put("default.html", bucket);

		insertManifest(insertURI, bucketsByName, "default.html", RequestStarter.IMMEDIATE_SPLITFILE_PRIORITY_CLASS);
		return true;
	}
*/

	public boolean insertFreesite(FreenetURI insertURI, String docName, String content, InsertCallback insertCallback) throws IOException, InsertException {
		String mimeType = DefaultMIMETypes.guessMIMEType(docName, false);
		if(mimeType == null) {
			mimeType = "text/html";
		}
		
		RandomAccessBucket bucket = node.clientCore.persistentTempBucketFactory.makeBucket(content.length());
		BucketTools.copyFrom(bucket, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8), 0, content.length()), content.length());

		InsertBlock ib = new InsertBlock(bucket, new ClientMetadata("text/html"), insertURI);
		InsertContext ictx = HighLevelSimpleClientInterface.getInsertContext(true);
		HighLevelSimpleClientInterface.insert(ib, docName, false, ictx, insertCallback, RequestStarter.IMMEDIATE_SPLITFILE_PRIORITY_CLASS);
		return true;
	}
	
	public RequestClient getClient() {
		return node.nonPersistentClientRT;
	}

	public FreenetURI insertManifest(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName, short priorityClass) throws InsertException {
		return HighLevelSimpleClientInterface.insertManifest(insertURI, bucketsByName, defaultName, priorityClass);
	}

}
