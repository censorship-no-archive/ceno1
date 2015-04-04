package plugins.CENO.Client;

import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

public class ClientHandler extends AbstractCENOClientHandler {

	private static final String pluginPath = "/plugins/" + CENOClient.class.getName();
	private static final LookupHandler lookupHandler = new LookupHandler();

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		String path = request.getPath().replaceFirst(pluginPath, "");
		if (path.isEmpty()) {
			return printStaticHTML("Resources/index.html");
		} else if (path.startsWith("/lookup")) {
			return lookupHandler.handleHTTPGet(request);
		}
		return "404: Requested path is invalid.";
	}

	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		String path = request.getPath().replaceFirst(pluginPath, "");
		if (!path.isEmpty() && path.startsWith("/fetch")) {
			RequestSender.requestFromBridge(request.getParam("url", ""));
			return "Sent passive request";
		}
		return "404: Requested path is invalid.";
	}

}
