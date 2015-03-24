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
			CENOClient.nodeInterface.localFetchURI(new FreenetURI("USK@XJZAi25dd5y7lrxE3cHMmM-xZ-c-hlPpKLYeLC0YG5I,8XTbR1bd9RBXlX6j-OZNednsJ8Cl6EAeBBebC3jtMFU,AQACAAE/index/486/"), new LocalLookupCallback());
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
