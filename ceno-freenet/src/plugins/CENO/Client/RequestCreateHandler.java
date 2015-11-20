package plugins.CENO.Client;

import java.net.MalformedURLException;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Common.URLtoUSKTools;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.Base64;
import freenet.support.IllegalBase64Exception;
import freenet.support.api.HTTPRequest;

/**
 * Handler for POST requests to the /create path. Responsible for signaling
 * the Request Sender agent to forward a request for creation and insertion
 * in the distributed cache of a bundle for a given URL.
 */
public class RequestCreateHandler extends AbstractCENOClientHandler {

	@Override
	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		// RequestCreateHandler won't handle GET requests
		return "RequestCreateHandler: GET request received";
	}

	@Override
	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		// Only clients that support JSON make requests for signaling RR using this handler
		boolean clientIsHtml = false;
		
		// Check if URL parameter of the POST request is Empty
		String urlParam = request.getParam("url", "");
		if (urlParam.isEmpty()) {
			return returnError(new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID), clientIsHtml);
		}

		// Base64 Decode the URL parameter
		try {
			urlParam = Base64.decodeUTF8(urlParam);
		} catch (IllegalBase64Exception e) {
			return returnError(new CENOException(CENOErrCode.LCS_HANDLER_URL_DECODE), clientIsHtml);
		}

		// Validate the URL requested
		try {
			urlParam = URLtoUSKTools.validateURL(urlParam);
		} catch (MalformedURLException e) {
			return returnError(new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID), clientIsHtml);
		}
		
		//boolean isX_CENO_Rewrite = (request.getHeader("X-Ceno-Rewritten") != null) ? true : false;
		RequestSender.getInstance().requestFromBridge(urlParam);
		
		return "okay";
	}
	
}
