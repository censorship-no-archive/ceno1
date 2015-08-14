package plugins.CENO.Client;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import net.minidev.json.JSONObject;
import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Client.ULPRManager.ULPRStatus;
import plugins.CENO.Common.URLtoUSKTools;
import plugins.CENO.FreenetInterface.ConnectionOverview.NodeConnections;
import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.keys.FreenetURI;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.Base64;
import freenet.support.IllegalBase64Exception;
import freenet.support.Logger;
import freenet.support.api.HTTPRequest;

/**
 * Handler for requests in the /lookup path
 */
public class LookupHandler extends AbstractCENOClientHandler {

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		// If "client" GET parameter is set to "HTML", then LCS will compose an
		// HTML response instead of the JSON Object
		boolean clientIsHtml = isClientHtml(request);

		String urlParam = request.getParam("url", "");
		if (urlParam.isEmpty()) {
			if (clientIsHtml) {
				return new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID).getMessage();
			}
			return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID));
		}

		if (!clientIsHtml) {
			try {
				urlParam = Base64.decodeUTF8(urlParam);
			} catch (IllegalBase64Exception e) {
				return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_URL_DECODE));
			}
		}

		try {
			urlParam = URLtoUSKTools.validateURL(urlParam);
		} catch (MalformedURLException e) {
			if (clientIsHtml) {
				return new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID).getMessage();
			} else {
				return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID));
			}
		}

		FreenetURI calculatedUSK = null;
		try {
			calculatedUSK = URLtoUSKTools.computeUSKfromURL(urlParam, CENOClient.bridgeKey);
		} catch (Exception e) {
			if (clientIsHtml) {
				return new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID).getMessage();
			} else {
				return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID));
			}
		}

		String localFetchResult = null;
		try {
			localFetchResult = localCacheLookup(calculatedUSK);
		} catch (CENOException e) {
			return returnErrorJSON(e);
		}

		if (localFetchResult == null) {
			NodeConnections nodeConnections = CENOClient.nodeInterface.getConnections();
			if (nodeConnections.getCurrent() == 0) {
				return returnErrorJSON(new CENOException(CENOErrCode.LCS_NODE_NOT_ENOUGH_PEERS));
			}
			ULPRStatus urlULPRStatus = ULPRManager.lookupULPR(urlParam);
			if (nodeConnections.getCurrent() < 3) {
				return returnErrorJSON(new CENOException(CENOErrCode.LCS_NODE_INITIALIZING));
			}
			RequestSender.requestFromBridge(urlParam);
			if (urlULPRStatus == ULPRStatus.failed) {
				if (clientIsHtml) {
					return printStaticHTMLReplace("resources/requestedFromBridge.html", "[urlRequested]", urlParam);
				} else {
					JSONObject jsonResponse = new JSONObject();
					jsonResponse.put("complete", true);
					jsonResponse.put("found", false);
					return jsonResponse.toJSONString();
				}
			} else {
				if (clientIsHtml) {
					return printStaticHTMLReplace("resources/sentULPR.html", "[urlRequested]", urlParam);
				} else {
					JSONObject jsonResponse = new JSONObject();
					jsonResponse.put("complete", false);
					return jsonResponse.toJSONString();
				}
			}
		} else {
			if (clientIsHtml) {
				return localFetchResult;
			} else {
				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put("complete", true);
				jsonResponse.put("found", true);
				jsonResponse.put("bundle", localFetchResult);
				return jsonResponse.toJSONString();
			}
		}
	}

	/**
	 * Perform a synchronous lookup for a USK in the node's local cache.
	 * As soon as a passive request in the distributed cache gets successfully
	 * completed, the bundle will also be available in the local cache.
	 * 
	 * @param calculatedUSK the Freenet key to lookup for the bundle
	 * @return the content of the bundle if it is found, <code>null</code>
	 * otherwise
	 */
	private String localCacheLookup(FreenetURI calculatedUSK) throws CENOException {
		// Local cache lookups do not support "-1" as the edition of a USK
		if (calculatedUSK.getSuggestedEdition() < 0) {
			calculatedUSK = calculatedUSK.setSuggestedEdition(0);
		}

		ClientGetSyncCallback getSyncCallback = new ClientGetSyncCallback();
		String fetchResult = null;
		try {
			CENOClient.nodeInterface.localFetchURI(calculatedUSK, getSyncCallback);
			fetchResult = getSyncCallback.getResult(45L, TimeUnit.SECONDS);
		} catch (FetchException e) {
			if (e.getMode() == FetchExceptionMode.PERMANENT_REDIRECT) {
				fetchResult = localCacheLookup(e.newURI);
			} else if (e.isFatal()) {
				Logger.warning(this, "Fatal fetch exception while looking in the local cache for USK: " + calculatedUSK + " Exception: " + e.getMessage());
				throw new CENOException(CENOErrCode.LCS_LOOKUP_LOCAL);
			} else {
				Logger.error(this, "Unhandled exception while looking in the local cache for USK: " + calculatedUSK + " Exception: " + e.getMessage());
			}
		}
		return fetchResult;
	}

	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		// LCS won't handle POST requests
		return "LookupHandler: POST request received";
	}

}