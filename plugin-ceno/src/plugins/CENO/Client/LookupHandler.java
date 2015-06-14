package plugins.CENO.Client;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import net.minidev.json.JSONObject;
import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.URLtoUSKTools;
import plugins.CENO.Client.ULPRManager.ULPRStatus;
import freenet.client.FetchException;
import freenet.keys.FreenetURI;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.Base64;
import freenet.support.IllegalBase64Exception;
import freenet.support.Logger;
import freenet.support.api.HTTPRequest;

public class LookupHandler extends AbstractCENOClientHandler {

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		boolean clientIsHtml = isClientHtml(request);

		String urlParam = request.getParam("url", "");
		if (urlParam.isEmpty()) {
			if (clientIsHtml) {
				return new CENOException(CENOErrCode.LCS_HANDLER_INVALID_URL).getMessage();
			}
			return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_INVALID_URL));
		}

		if (!clientIsHtml) {
			try {
				urlParam = Base64.decodeUTF8(urlParam);
			} catch (IllegalBase64Exception e) {
				return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_INVALID_URL));
			}
		}

		try {
			urlParam = URLtoUSKTools.validateURL(urlParam);
		} catch (MalformedURLException e) {
			if (clientIsHtml) {
				return new CENOException(CENOErrCode.LCS_HANDLER_INVALID_URL).getMessage();
			} else {
				return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_INVALID_URL));
			}
		}

		FreenetURI calculatedUSK = null;
		try {
			calculatedUSK = URLtoUSKTools.computeUSKfromURL(urlParam, CENOClient.bridgeKey);
		} catch (Exception e) {
			if (clientIsHtml) {
				return new CENOException(CENOErrCode.LCS_HANDLER_INVALID_URL).getMessage();
			} else {
				return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_INVALID_URL));
			}
		}

		String localFetchResult = localCacheLookup(calculatedUSK);

		if (localFetchResult == null) {
			ULPRStatus urlULPRStatus = ULPRManager.lookupULPR(urlParam);
			RequestSender.requestFromBridge(urlParam);
			if (urlULPRStatus == ULPRStatus.failed) {
				if (clientIsHtml) {
					return printStaticHTMLReplace("Resources/requestedFromBridge.html", "[urlRequested]", urlParam);
				} else {
					JSONObject jsonResponse = new JSONObject();
					jsonResponse.put("complete", true);
					jsonResponse.put("found", false);
					return jsonResponse.toJSONString();
				}
			} else {
				if (clientIsHtml) {
					return printStaticHTMLReplace("Resources/sentULPR.html", "[urlRequested]", urlParam);
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

	private String localCacheLookup(FreenetURI calculatedUSK) {
		ClientGetSyncCallback getSyncCallback = new ClientGetSyncCallback();
		String fetchResult = null;
		try {
			CENOClient.nodeInterface.localFetchURI(calculatedUSK, getSyncCallback);
			fetchResult = getSyncCallback.getResult(5L, TimeUnit.SECONDS);
		} catch (FetchException e) {
			if (e.getMode() == FetchException.PERMANENT_REDIRECT) {
				fetchResult = localCacheLookup(e.newURI);
			} else if (e.isFatal()) {
				Logger.warning(this, "Fatal fetch exception while looking in the local cache for USK: " + calculatedUSK);
				//TODO Throw custom CENOException
			} else {
				Logger.error(this, "Unhandled exception while looking in the local cache for USK: " + calculatedUSK);
			}
		}
		return fetchResult;
	}

	public String handleHTTPPost(HTTPRequest request)
			throws PluginHTTPException {
		return "LookupHandler: POST request received";
	}

}