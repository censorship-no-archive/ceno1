package plugins.CENO.FreenetInterface;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import plugins.CENO.FreenetInterface.ConnectionOverview.NodeConnections;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertBlock;
import freenet.client.InsertException;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.PersistenceDisabledException;
import freenet.client.async.USKCallback;
import freenet.keys.FreenetURI;
import freenet.keys.USK;
import freenet.node.RequestClient;
import freenet.support.api.Bucket;

public interface FreenetInterface {

	FetchResult fetchURI(FreenetURI uri) throws FetchException;
	ClientGetter localFetchURI(FreenetURI uri, ClientGetCallback callback) throws FetchException;
	ClientGetter distFetchURI(FreenetURI uri, ClientGetCallback callback) throws FetchException;
	ClientGetter fetchULPR(FreenetURI uri, ClientGetCallback callback) throws FetchException;
	boolean subscribeToUSK(USK origUSK, USKCallback cb);
	FreenetURI[] generateKeyPair();
	RequestClient getRequestClient();
	FreenetURI insertBlock(InsertBlock insert, boolean getCHKOnly, String filenameHint) throws InsertException;
	boolean insertFreesite(FreenetURI insertURI, String docName, String content, ClientPutCallback cb) throws IOException, InsertException;
	FreenetURI insertBundleManifest(FreenetURI insertURI, String content, String defaultName, ClientPutCallback cb) throws IOException, InsertException;
	Bucket makeBucket(int length) throws IOException;
	FreenetURI insertManifest(FreenetURI insertURI, HashMap<String, Object> bucketsByName, String defaultName, short priorityClass) throws InsertException;
	FreenetURI insertSingleChunk(FreenetURI uri, String content, ClientPutCallback cb) throws InsertException, PersistenceDisabledException, UnsupportedEncodingException;
	NodeConnections getConnections();
	ClientGetCallback getVoidGetCallback(String successMessage, String failureMessage);
	ClientPutCallback getVoidPutCallback(String successMessage, String failureMessage);
}
