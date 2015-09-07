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
 * Handler for requests to the /lookup path. Responsible for determining whether
 * a bundle for a URL exists in the local or the distributed cache, as well as
 * for its retrieval.
 */
public class LookupHandler extends AbstractCENOClientHandler {

	@Override
	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		// If "client" GET parameter is set to "HTML", then LCS will compose an
		// HTML response instead of the JSON Object
		boolean clientIsHtml = isClientHtml(request);

		// Check if URL parameter of the GET request is Empty
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

		// Calculate the retrieval address of the inserted bundle for this URL
		FreenetURI calculatedUSK = null;
		try {
			calculatedUSK = URLtoUSKTools.computeUSKfromURL(urlParam, CENOClient.bridgeKey);
		} catch (Exception e) {
			return returnError(new CENOException(CENOErrCode.LCS_HANDLER_URL_INVALID), clientIsHtml);
		}

		// Make a synchronous lookup in the local cache for the bundled version of the URL
		String localFetchResult = null;
		try {
			localFetchResult = localCacheLookup(calculatedUSK);
		} catch (CENOException e) {
			return returnError(e, clientIsHtml);
		}

		/*
		 * If the bundle was found and retrieved from the the local cache, return it
		 * to the agent that made the request.
		 * Resources of Ultra Light Passive Requests (ULPRs) in the distributed cache, once
		 * successfully retrieved, are also available in the local cache.
		 */
		if (localFetchResult != null) {
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

		// If the bundle was not found in the local cache:
		NodeConnections nodeConnections = CENOClient.nodeInterface.getConnections();
		if (nodeConnections.getCurrent() == 0) {
			// The node is not connected to any peers yet. Could it be a firewall/connectivity issue?
			return returnError(new CENOException(CENOErrCode.LCS_NODE_NOT_ENOUGH_PEERS), clientIsHtml);
		}

		// The node is in state of performing ULPRs and one is initiated for the calculated SSK
		ULPRStatus urlULPRStatus;
		try {
			urlULPRStatus = ULPRManager.lookupULPR(urlParam);
		} catch (CENOException e) {
			return returnError(e, clientIsHtml);
		}

		if (urlULPRStatus == ULPRStatus.failed) {
			// Unlikely to happen
			return returnError(new CENOException(CENOErrCode.LCS_LOOKUP_ULPR_FAILED), clientIsHtml);
		}

		// If the Freenet node is connected to less than 5 peers, the process will be slow
		// and we inform the users appropriately
		if (nodeConnections.getCurrent() < 5) {
			return returnError(new CENOException(CENOErrCode.LCS_NODE_INITIALIZING), clientIsHtml);
		}

		// Check whether the request timeout has expired
		if (RequestSender.shouldSendFreemail(urlParam)) {
			if (clientIsHtml) {
				// HTML client cannot signal RS to make requests for bundles to the bridge, so LookupHandler
				// initiates such a request
				//boolean isX_CENO_Rewrite = (request.getHeader("X-Ceno-Rewritten") != null) ? true : false;
				RequestSender.requestFromBridge(urlParam);
				return printStaticHTMLReplace("resources/requestedFromBridge.html", "[urlRequested]", urlParam);
			} else {
				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put("complete", true);
				jsonResponse.put("found", false);
				return jsonResponse.toJSONString();
			}
		}

		// Request to the bridge for the requested URL has not timed out and a ULPR is in progress.
		if (clientIsHtml) {
			return printStaticHTMLReplace("resources/sentULPR.html", "[urlRequested]", urlParam);
		} else {
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("complete", false);
			return jsonResponse.toJSONString();
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

	@Override
	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		// LCS won't handle POST requests
		return "LookupHandler: POST request received on /lookup path";
	}

}