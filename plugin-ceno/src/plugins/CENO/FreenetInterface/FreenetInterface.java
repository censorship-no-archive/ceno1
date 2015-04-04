package plugins.CENO.FreenetInterface;

import java.io.IOException;
import java.util.HashMap;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertException;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.client.async.ClientPutCallback;
import freenet.keys.FreenetURI;
import freenet.support.api.Bucket;

public interface FreenetInterface {

	FetchResult fetchURI(FreenetURI uri) throws FetchException;
	ClientGetter localFetchURI(FreenetURI uri, ClientGetCallback callback) throws FetchException;
	ClientGetter fetchULR(FreenetURI uri, ClientGetCallback callback) throws FetchException;
	FreenetURI[] generateKeyPair();
	boolean insertFreesite(FreenetURI insertURI, String docName, String content, ClientPutCallback cb) throws IOException, InsertException;
	Bucket makeBucket(int length) throws IOException;
	FreenetURI insertManifest(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName, short priorityClass) throws InsertException;
	boolean sendFreemail(String freemailFrom, String freemailTo[], String subject, String content, String password);
	boolean startIMAPMonitor(String freemail, String password, String idleFolder);
}
