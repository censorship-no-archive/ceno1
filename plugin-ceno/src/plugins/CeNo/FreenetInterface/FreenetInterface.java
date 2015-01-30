package plugins.CeNo.FreenetInterface;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.keys.FreenetURI;

public interface FreenetInterface {
	
	FetchResult fetchURI(FreenetURI uri) throws FetchException;
}
