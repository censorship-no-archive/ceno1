package plugins.CENO.Client;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import net.minidev.json.JSONObject;
import plugins.CENO.URLtoUSKTools;
import plugins.CENO.Client.ULPRManager.ulprStatus;
import freenet.client.FetchException;
import freenet.keys.FreenetURI;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

public class LookupHandler extends AbstractCENOClientHandler {

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		boolean clientIsHtml = isClientHtml(request);

		String urlParam = request.getParam("url", "");
		if (urlParam.isEmpty()) {
			return "Invalid URL";
		} else if (Pattern.matches("freenet:|USK@|CHK@|SSK@", urlParam)) {
			return "Lookup URL looks like a Freenet URI";
		}

		FreenetURI calculatedUSK = null;
		try {
			calculatedUSK = URLtoUSKTools.computeUSKfromURL(urlParam, CENOClient.bridgeKey);
		} catch (Exception e) {
			return "Error while calculating the USK for the lookup URL";
		}

		String localFetchResult = null;
		ClientGetSyncCallback getSyncCallback = new ClientGetSyncCallback();
		try {
			CENOClient.nodeInterface.localFetchURI(calculatedUSK, getSyncCallback);
		} catch (FetchException e) {
			e.printStackTrace();
		}
		localFetchResult = getSyncCallback.getResult(5L, TimeUnit.SECONDS);

		if (localFetchResult == null) {
			ulprStatus urlULPRStatus = ULPRManager.lookupULPR(urlParam);
			if (urlULPRStatus == ulprStatus.failed) {
				RequestSender.requestFromBridge(urlParam);
				if (clientIsHtml) {
					return printStaticHTMLReplace("Resources/requestedFromBridge.html", "[urlRequested]", urlParam);
				} else {
					JSONObject jsonResponse = new JSONObject();
					jsonResponse.put("complete", true);
					jsonResponse.put("found", "false");
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
				jsonResponse.put("complete", "true");
				jsonResponse.put("found", true);
				jsonResponse.put("bundle", localFetchResult);
				return jsonResponse.toJSONString();
			}
		}
	}

	public String handleHTTPPost(HTTPRequest request)
			throws PluginHTTPException {
		return "LookupHandler: POST request received";
	}

}