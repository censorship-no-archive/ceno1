package plugins.CeNo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONObject;

import org.eclipse.jetty.server.Request;

import plugins.CeNo.BridgeInterface.Bundle;
import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.client.FetchResult;
import freenet.keys.FreenetURI;
import freenet.support.api.Bucket;
import freenet.support.io.BucketTools;

/* ------------------------------------------------------------ */
/** CeNo Plugin Http Communication and Serving server.
 *  CeNo nodes talk to CeNo through HTTP, 
 * - CeNo Client reply the url.
 * - Plugin either serve the content or DNF.
 * - In case of DNF, the client send a request asking plugin
 * - to ping a bridge to bundle the content.
 * - If Freenet being pingged, the plugin will send a 
 *   url to the CeNo bridge to bundle the content
 * - The Bridge then will serve the bundle to the plugin
 *   to insert into Freenet
 */
public class CacheLookupHandler extends CeNoHandler {

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) 
			throws IOException, ServletException {
		String requestPath = request.getPathInfo().substring(1);
		String urlParam = (request.getParameter("url") != null) ? request.getParameter("url") : requestPath;
		if (urlParam.isEmpty() && requestPath.isEmpty()) {
			writeWelcome(baseRequest, response, requestPath);
			return;
		} else if (requestPath.startsWith("USK@") || requestPath.startsWith("SSK@")) {
			FetchResult result = null;
			try {
				result = HighLevelSimpleClientInterface.fetchURI(new FreenetURI(requestPath));
			} catch (MalformedURLException e) {
				writeError(baseRequest, response, requestPath);
				return;
			} catch (FetchException e) {
				// USK key has been updated, redirect to the new URI
				if (e.getMode() == FetchExceptionMode.PERMANENT_REDIRECT) {
					String newURI = "/".concat(e.newURI.toString());
					response.sendRedirect(newURI);
				} else if (e.isDNF()) {
					JSONObject jsonResponse = new JSONObject();
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					jsonResponse.put("bundleFound", "false");
					response.setContentType("application/javascript");
					response.getOutputStream().println(jsonResponse.toJSONString());
					baseRequest.setHandled(true);
					return;
				} else{
					e.printStackTrace();
					writeError(baseRequest, response, requestPath);
					return;
				}
			}
			if (result != null) {
				// When fetching is complete, write it to the response OutputStream		
				Bundle bundle = new Bundle(urlParam);
				bundle.setContent(result.asByteArray());
				
				response.setContentType(result.getMimeType());
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("application/javascript");
				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put("bundleFound", "true");
				jsonResponse.put("bundle", bundle.getContent());
				
				response.getOutputStream().println(jsonResponse.toJSONString());
			} else {
				writeError(baseRequest, response, requestPath);
			}
		} else {
			// Translate requestPath to USK
			// Redirect request to the calculated USK
			FreenetURI calculatedUSK = null;
			try {
				calculatedUSK = computeSSKfromPath(urlParam);
			} catch (Exception e) {
				writeError(baseRequest, response, requestPath);
				return;
			}

			response.sendRedirect("/" + calculatedUSK.toString());
			baseRequest.setHandled(true);
		}
	}
}