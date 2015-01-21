package plugins.CeNo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import plugins.CeNo.BridgeInterface.Bundle;
import plugins.CeNo.BridgeInterface.BundleRequest;
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
public class CacheLookupHandler extends AbstractHandler {
	private void writeWelcome(Request baseRequest, HttpServletResponse response, String requestPath) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("Welcome to CeNo.");
		baseRequest.setHandled(true);
	}

	private void writeError(Request baseRequest, HttpServletResponse response, String requestPath) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("Error while fetching: " + requestPath);
		baseRequest.setHandled(true);
	}

	private FreenetURI computeSSKfromPath(String requestPath) throws MalformedURLException {
		requestPath = requestPath.replaceFirst("http://|https://", "");

		String domain, extraPath;
		int slashIndex = requestPath.indexOf('/');
		if (slashIndex < 1 || slashIndex == requestPath.length()) {
			domain = requestPath;
			extraPath = "";
		} else {
			domain = requestPath.substring(0, slashIndex);
			extraPath = requestPath.substring(slashIndex + 1, requestPath.length());
		}

		return new FreenetURI("USK@XJZAi25dd5y7lrxE3cHMmM-xZ-c-hlPpKLYeLC0YG5I,8XTbR1bd9RBXlX6j-OZNednsJ8Cl6EAeBBebC3jtMFU,AQACAAE/" + domain + "/-1/" + extraPath);
	}

	private void pathDNF(Request baseRequest, HttpServletRequest request, HttpServletResponse response, String requestPath) throws IOException {
		// Request a bundle from node.js for the given URI
		// return the bundle content as a result
		FreenetURI requestKey = new FreenetURI(requestPath);
		StringBuilder allMetaStrings = new StringBuilder();
		for (String metaString : requestKey.getAllMetaStrings()) {
			if (!metaString.isEmpty()) {
				allMetaStrings.append("/" + metaString);
			}
		}
		Bundle bundle = BundleRequest.requestURI(requestKey.getDocName() + allMetaStrings);
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(bundle.getContent());
		baseRequest.setHandled(true);

		//TODO non-blocking insert the bundle content in freenet with the computed USK
	}

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) 
			throws IOException, ServletException {
		String requestPath = request.getPathInfo().substring(1);
		if (requestPath.isEmpty() || requestPath.equals("")) {
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
					pathDNF(baseRequest, request, response, requestPath);
					return;
				} else{
					e.printStackTrace();
					writeError(baseRequest, response, requestPath);
					return;
				}
			}
			if (result != null) {
				// When fetching is complete, write it to the response OutputStream
				response.setContentType(result.getMimeType());
				response.setStatus(HttpServletResponse.SC_OK);
				OutputStream resOutStream = response.getOutputStream();

				Bucket resultBucket = result.asBucket();
				try {
					BucketTools.copyTo(resultBucket, resOutStream, Long.MAX_VALUE);
					resOutStream.flush();
					resOutStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				resultBucket.free();
				baseRequest.setHandled(true);
			} else {
				writeError(baseRequest, response, requestPath);
			}
		} else {
			// Translate requestPath to USK
			// Redirect request to the calculated USK
			FreenetURI calculatedUSK = null;
			try {
				calculatedUSK = computeSSKfromPath(requestPath);
			} catch (Exception e) {
				writeError(baseRequest, response, requestPath);
				return;
			}

			response.sendRedirect("/" + calculatedUSK.toString());
			baseRequest.setHandled(true);
		}
	}
}