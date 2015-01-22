package plugins.CeNo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import plugins.CeNo.BridgeInterface.Bundle;
import plugins.CeNo.BridgeInterface.BundleRequest;
import freenet.keys.FreenetURI;

/* ------------------------------------------------------------ */
/** CeNo Plugin handler for requests to cache bundles
 * 	
 * CacheInsertHandler listens to :{@link CeNo#cacheInsertPort} port
 * and under the "/store" route for POST requests.
 * Those POST requests include the bundled page as well as the original url.
 * The handler caches the given bundle under its signed subspace.
 * Upon successful insertion, the handler replies with "stored".
 */
public class CacheInsertHandler extends CeNoHandler {

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
			throws IOException, ServletException {
		// Request a bundle from bundler/transporter for the given URI
		// return the bundle content as a result
		String requestPath = request.getPathInfo().substring(1);
		FreenetURI requestKey = new FreenetURI(requestPath);
		StringBuilder allMetaStrings = new StringBuilder();
		for (String metaString : requestKey.getAllMetaStrings()) {
			if (!metaString.isEmpty()) {
				allMetaStrings.append("/" + metaString);
			}
		}
		Bundle bundle = BundleRequest.requestURI(requestKey.getDocName() + allMetaStrings);

		//TODO non-blocking insert the bundle content in freenet with the computed USK

		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("stored");
		baseRequest.setHandled(true);
	}
}
