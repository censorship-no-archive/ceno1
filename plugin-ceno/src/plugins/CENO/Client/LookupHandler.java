package plugins.CENO.Client;

import java.net.MalformedURLException;

import com.db4o.ObjectContainer;

import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.async.ClientGetCallback;
import freenet.client.async.ClientGetter;
import freenet.keys.FreenetURI;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

public class LookupHandler implements ClientHandlerInterface {

	public class LocalLookupCallback implements ClientGetCallback {

		public void onMajorProgress(ObjectContainer container) {
			// TODO Auto-generated method stub
			
		}

		public void onSuccess(FetchResult result, ClientGetter state,
				ObjectContainer container) {
			System.out.print(result.toString());
		}

		public void onFailure(FetchException e, ClientGetter state,
				ObjectContainer container) {
			// TODO Auto-generated method stub
			
		}
		
	}

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		try {
			CENOClient.nodeInterface.localFetchURI(new FreenetURI("USK@OKVCCIFp~eVDo8R1x3czZavtHl1r2~8lCWqeBT6JNgk,ztmJxsWpmOHzyHsjtZJ913oX0ATabON8zWfpbCQhMAo,AQACAAE/garrett00/12/"), new LocalLookupCallback());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FetchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Not found";
	}

	public String handleHTTPPost(HTTPRequest request)
			throws PluginHTTPException {
		return "LookupHandler: POST request received";
	}

}
