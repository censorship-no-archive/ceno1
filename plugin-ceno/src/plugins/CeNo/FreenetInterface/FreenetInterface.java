package plugins.CeNo.FreenetInterface;

import java.io.IOException;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.keys.FreenetURI;
import freenet.support.api.Bucket;

public interface FreenetInterface {

	FetchResult fetchURI(FreenetURI uri) throws FetchException;
	FreenetURI[] generateKeyPair();
	boolean insertFreesite(FreenetURI insertURI, String content) throws IOException;
	Bucket makeBucket(int length) throws IOException;
}
