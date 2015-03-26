package plugins.CENO.Client;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import freenet.client.FetchException;
import freenet.keys.FreenetURI;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

public class LookupHandler implements ClientHandlerInterface {

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		String localFetchResult = null;
		ClientGetSyncCallback getSyncCallback = new ClientGetSyncCallback();
		try {
			CENOClient.nodeInterface.localFetchURI(new FreenetURI("USK@CjdrIg9kZn0mzmKTPuofupZmsdvB5Ruas7wyZrekRmM,uhrRUrkJ18IOGtTraf4wVQ1LSlweOXcipZ9BNwv4Kgw,AQACAAE/cypher/5/"), getSyncCallback);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FetchException e) {
			e.printStackTrace();
		}
		localFetchResult = getSyncCallback.getResult(5L, TimeUnit.SECONDS);
		return (localFetchResult == null) ? "Not Found" : localFetchResult; 
	}

	public String handleHTTPPost(HTTPRequest request)
			throws PluginHTTPException {
		return "LookupHandler: POST request received";
	}

}
