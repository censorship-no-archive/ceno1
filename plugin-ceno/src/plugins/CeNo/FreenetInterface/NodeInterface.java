package plugins.CeNo.FreenetInterface;

import java.io.IOException;

import plugins.CeNo.CacheInsertHandler;
import plugins.CeNo.CacheInsertHandler.InsertCallback;
import plugins.CeNo.HighLevelSimpleClientInterface;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertBlock;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.ClientPutter;
import freenet.keys.FreenetURI;
import freenet.keys.InsertableClientSSK;
import freenet.node.Node;
import freenet.node.RequestStarter;
import freenet.support.api.Bucket;
import freenet.support.api.RandomAccessBucket;

public class NodeInterface implements FreenetInterface {

	private Node node;
	
	public void replyStored() {
		return;
	}

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

	public boolean insertFreesite(FreenetURI insertURI, String content, InsertCallback insertCallback) throws IOException, InsertException {
		RandomAccessBucket bucket = node.clientCore.persistentTempBucketFactory.makeBucket(content.length());
		bucket.getOutputStream().write(content.getBytes());
		bucket.setReadOnly();
		
		InsertBlock ib = new InsertBlock(bucket, null, insertURI);
		InsertContext ictx = HighLevelSimpleClientInterface.getInsertContext(true);
		HighLevelSimpleClientInterface.insert(ib, insertURI.getDocName(), false, ictx, insertCallback, RequestStarter.IMMEDIATE_SPLITFILE_PRIORITY_CLASS);
		return false;
	}

}
