package plugins.CENO.Client;

import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

public interface ClientHandlerInterface {

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException;
	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException;

}