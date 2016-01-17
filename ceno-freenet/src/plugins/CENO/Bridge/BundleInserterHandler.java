package plugins.CENO.Bridge;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;

import org.eclipse.jetty.server.Request;

import plugins.CENO.Common.CENOJettyHandler;
import plugins.CENO.Common.URLtoUSKTools;
import freenet.client.InsertException;
import freenet.support.Logger;

/* ------------------------------------------------------------ */
/** CENOBridge Plugin handler for requests to cache bundles
 * 	
 * BundleInserterHandler listens to {@link CENOBridge#bundleInserterPort} port
 * and under the "/store" route for POST requests.
 * Those POST requests include the bundled page as well as the original url.
 * The handler caches the given bundle under its signed subspace.
 * Upon successful insertion, the handler replies with "stored".
 */
public class BundleInserterHandler extends CENOJettyHandler {

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
			throws IOException, ServletException {
		// Only POST requests at /store route will be handled
		if (!request.getMethod().equals("POST")) {
			writeError(baseRequest, response, "Not a POST request at /store route");
			return;
		}

		// Get the data from the POST request
		//TODO Add support for multi-part POST requests
		JSONObject requestJSON;
		try {
			requestJSON = readJSONbody(request.getReader());
		} catch (ParseException e) {
			writeError(baseRequest, response, "Could not parse JSON");
			return;
		}
		if(!requestJSON.containsKey("url")) {
			writeError(baseRequest, response, "No url attribute in request body");
			return;
		}
		if(!requestJSON.containsKey("bundle")) {
			writeError(baseRequest, response, "No bundle attribute in request body");
			return;
		}

		String urlParam = requestJSON.get("url").toString();
		if((urlParam == null) || urlParam.isEmpty()) {
			writeError(baseRequest, response, "Invalid url attribute");
			return;
		}

		try {
			urlParam = URLtoUSKTools.validateURL(urlParam);
		} catch (MalformedURLException e) {
			writeError(baseRequest, response, "URL failed validation and a bundle will not be inserted for: " + urlParam);
		}

		if (!CENOBridge.isMasterBridge() && URLtoUSKTools.isFeedURL(urlParam)) {
			Logger.normal(this, "Got request to insert CENO portal feeds but won't handle since this is not the master bridge");
			writeMessage("Not a Master bridge,  won't insert", baseRequest, response, urlParam);
			return;
		}

		Bundle bundle = new Bundle(urlParam);
		if ((requestJSON.get("bundle") != null)) {
			bundle.setContent(requestJSON.get("bundle").toString());
		} else {
			writeError(baseRequest, response, "Bundle attribute was null");
			return;
		}

		if (bundle.getContent().isEmpty()) {
			writeError(baseRequest, response, "Bundle content empty for URL: " + urlParam);
			return;
		}

		try {
			BundleInserter.getInstance().insertBundle(urlParam, bundle);
		} catch (InsertException e) {
			writeError(baseRequest, response, "Error during insertion for URL: " + urlParam + " Error: " + e.getMessage());
		} catch (IOException e) {
			writeError(baseRequest, response, "IOException during insertion for URL: " + urlParam);
			return;
		}

		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("stored");
		baseRequest.setHandled(true);
	}
}
