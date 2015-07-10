package plugins.CENO.Bridge;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONObject;

import org.eclipse.jetty.server.Request;

import plugins.CENO.CENOJettyHandler;
import plugins.CENO.URLtoUSKTools;
import plugins.CENO.Bridge.BundlerInterface.Bundle;
import plugins.CENO.FreenetInterface.HighLevelSimpleClientInterface;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.keys.FreenetURI;

/* ------------------------------------------------------------ */
/** CENO Plugin Http Communication and Serving server.
 *  CENO nodes talk to CENO through HTTP, 
 * - CENO Client reply the url.
 * - Plugin either serve the content or DNF.
 * - In case of DNF, the client send a request asking plugin
 * - to ping a bridge to bundle the content.
 * - If Freenet being pingged, the plugin will send a 
 *   url to the CENO bridge to bundle the content
 * - The Bridge then will serve the bundle to the plugin
 *   to insert into Freenet
 */
public class CacheLookupHandler extends CENOJettyHandler {

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) 
			throws IOException, ServletException {
		String requestPath = request.getPathInfo().substring(1);
		String urlParam = (request.getParameter("url") != null) ? request.getParameter("url") : requestPath;
		if (urlParam.isEmpty() && requestPath.isEmpty()) {
			writeWelcome(baseRequest, response, requestPath);
			return;
		}
		if (requestPath.startsWith("freenet:")) {
			requestPath.replaceFirst("freenet:", "");
		}
		if (requestPath.startsWith("USK@") || requestPath.startsWith("SSK@")) {
			FetchResult result = null;
			try {
				result = HighLevelSimpleClientInterface.fetchURI(new FreenetURI(requestPath));
			} catch (MalformedURLException e) {
				writeError(baseRequest, response, requestPath);
				return;
			} catch (FetchException e) {
				// USK key has been updated, redirect to the new URI
				if (e.getMode() == FetchException.PERMANENT_REDIRECT) {
					String newURI = "/".concat(e.newURI.toString());
					response.sendRedirect(newURI);
				} else if (e.isDNF()) {
					// The requested URL has not been found in the cache
					// Return JSON {"bundleFound": "false"}
					JSONObject jsonResponse = new JSONObject();
					jsonResponse.put("bundleFound", false);
					writeJSON(baseRequest, response, HttpServletResponse.SC_NOT_FOUND, jsonResponse);
					return;
				} else if (e.isFatal()) {
					e.printStackTrace();
					// Fatal error while fetching the freesite
					JSONObject jsonResponse = new JSONObject();
					jsonResponse.put("bundleFound", true);
					jsonResponse.put("bundle", "<html><body>There was a fatal error (" + e.getMode() + ") while fetching the bundle from freenet. Retrying will not fix this issue.</body></html>");
					writeJSON(baseRequest, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonResponse);
					return;
				} else{
					e.printStackTrace();
					JSONObject jsonResponse = new JSONObject();
					jsonResponse.put("bundleFound", true);
					jsonResponse.put("bundle", "<html><body>There was an error (" + e.getMode() + ") while fetching the bundle from freenet. Please try again.</body></html>");
					writeJSON(baseRequest, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonResponse);
					return;
				}
			}
			if (result != null) {
				// Bundler for the requested URL has been successfully retrieved
				Bundle bundle = new Bundle(urlParam);
				bundle.setContent(result.asByteArray());

				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put("bundleFound", true);
				jsonResponse.put("bundle", bundle.getContent());
				writeJSON(baseRequest, response, HttpServletResponse.SC_OK, jsonResponse);
				return;
			} else {
				// Error while retrieving the bundle from the cache
				writeError(baseRequest, response, requestPath);
				return;
			}
			// Stop background requests normally made by browsers for website resources,
			// that could start a time-consuming lookup in freenet
		} else if (requestPath.endsWith("favicon.ico")) {
			writeNotFound(baseRequest, response, requestPath);
			return;
		} else {
			// Request path is in form of URL
			// Calculate its USK and redirect the request
			FreenetURI calculatedUSK = null;
			try {
				calculatedUSK = URLtoUSKTools.computeUSKfromURL(urlParam, CENOBridge.initConfig.getProperty("requestURI"));
			} catch (Exception e) {
				writeError(baseRequest, response, requestPath);
				return;
			}

			response.sendRedirect("/" + calculatedUSK.toString());
			baseRequest.setHandled(true);
		}
	}
}