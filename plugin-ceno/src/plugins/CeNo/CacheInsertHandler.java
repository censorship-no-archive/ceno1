package plugins.CeNo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;

import org.eclipse.jetty.server.Request;

import plugins.CeNo.BridgeInterface.Bundle;
import freenet.keys.FreenetURI;

/* ------------------------------------------------------------ */
/** CeNo Plugin handler for requests to cache bundles
 * 	
 * CacheInsertHandler listens to {@link CeNo#cacheInsertPort} port
 * and under the "/store" route for POST requests.
 * Those POST requests include the bundled page as well as the original url.
 * The handler caches the given bundle under its signed subspace.
 * Upon successful insertion, the handler replies with "stored".
 */
public class CacheInsertHandler extends CeNoHandler {

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
			throws IOException, ServletException {
		// Only POST requests at /store route will be handled
		if (!request.getMethod().equals("POST") || !request.getPathInfo().equals("/store")) {
			writeError(baseRequest, response, "Not a POST request");
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
		String urlParam = requestJSON.get("url").toString();
		if((urlParam == null) || urlParam.isEmpty()) {
			writeError(baseRequest, response, "Invalid url attribute");
			return;
		}
		
		Bundle bundle = new Bundle(urlParam);
		bundle.setContent(requestJSON.get("bundle").toString());

		if (!bundle.getContent().isEmpty()) {
			//TODO non-blocking insert the bundle content in freenet with the computed USK
			FreenetURI insertKey = computeInsertURI(urlParam);
			CeNo.nodeInterface.insertFreesite(insertKey, bundle.getContent());
			
			response.setContentType("text/html;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("stored");
			baseRequest.setHandled(true);
		} else {
			writeError(baseRequest, response, urlParam);
		}
	}
}
