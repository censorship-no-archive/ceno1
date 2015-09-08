package plugins.CENO.Client;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

public class ClientHandler extends AbstractCENOClientHandler {

	private static final String pluginPath = "/plugins/" + CENOClient.class.getName();
	private static final LookupHandler lookupHandler = new LookupHandler();
	private static final RequestCreateHandler createHandler = new RequestCreateHandler();

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		String path = request.getPath().replaceFirst(pluginPath, "");
		if (path.isEmpty() || path.equals("/") || path.equals("/index.html")) {
			return printStaticHTML("resources/index.html");
		} else if (path.startsWith("/lookup")) {
			return lookupHandler.handleHTTPGet(request);
		}
		if (isClientHtml(request)) {
			return "404: Requested path is invalid or does not accept GET requests.";
		} else {
			return returnError(new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID), isClientHtml(request));
		}
	}

	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		String path = request.getPath().replaceFirst(pluginPath, "");
		if (path.startsWith("/create")) {
			createHandler.handleHTTPPost(request);
		}
		return "404: Requested path is invalid or does not accept POST requests.";
	}

}
