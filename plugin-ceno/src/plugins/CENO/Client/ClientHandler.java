package plugins.CENO.Client;

import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

public class ClientHandler extends AbstractCENOClientHandler {

	private static final String pluginPath = "/plugins/" + CENOClient.class.getName();
	private static final LookupHandler lookupHandler = new LookupHandler();

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		String path = request.getPath();
		if (path == null || path.isEmpty()) {
			return printStaticHTML("Resources/index.html");
		} else if (path.startsWith(pluginPath + "/lookup")) {
			return lookupHandler.handleHTTPGet(request);
		}
		return printStaticHTML("404: Requested path is invalid.");
	}

	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		return "<http><body>POST request received</body></http>";
	}

}
