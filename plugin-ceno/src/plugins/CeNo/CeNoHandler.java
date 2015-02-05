package plugins.CeNo;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import freenet.keys.FreenetURI;

public abstract class CeNoHandler extends AbstractHandler {

	public abstract void handle(String arg0, Request arg1, HttpServletRequest arg2,
			HttpServletResponse arg3) throws IOException, ServletException;

	protected void writeWelcome(Request baseRequest, HttpServletResponse response, String requestPath) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("Welcome to CeNo.");
		baseRequest.setHandled(true);
	}

	//TODO: Define error codes that CeNo plugins will be using
	// Give a descriptive message for each of them (like Malformed URL)
	protected void writeError(Request baseRequest, HttpServletResponse response, String requestPath) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		response.getWriter().println("Error while fetching: " + requestPath);
		baseRequest.setHandled(true);
	}

	/**
	 * Computes the USK for a given URL so that:
	 * <ul>
	 *   <li> CeNo can lookup if this URL has been cached before</li>
	 *   <li> CeNo knows the insert USK to use when caching a bundle</li>
	 * </ul>
	 * 
	 * @param requestPath the URL requested by the user/bundler
	 * @return the calculated FreenetURI that corresponds to that resource
	 * @throws MalformedURLException
	 */
	protected FreenetURI computeUSKfromURL(String requestPath) throws MalformedURLException {
		// Remove protocol from URL
		requestPath = requestPath.replaceFirst("http://|https://", "");

		// Extract domain and extra path
		String domain, extraPath;
		int slashIndex = requestPath.indexOf('/');
		// Support for URLs with queries that follow right after <host> without a slash /
		int queryIndex = requestPath.indexOf('?');
		if (queryIndex > -1 && queryIndex < slashIndex) {
			slashIndex = queryIndex;
		}
		if (slashIndex < 1 || slashIndex == requestPath.length()) {
			domain = requestPath;
			extraPath = "";
		} else {
			domain = requestPath.substring(0, slashIndex);
			extraPath = requestPath.substring(slashIndex + 1, requestPath.length());
		}

		String requestURI = CeNo.initConfig.getProperty("requestURI");
		String computedKey = requestURI.replaceFirst("SSK", "USK") + domain + "/-1/" + extraPath;

		return new FreenetURI(computedKey);
	}

	/* Extract meta strings from FreenetURI
	StringBuilder allMetaStrings = new StringBuilder();
	for (String metaString : requestKey.getAllMetaStrings()) {
		if (!metaString.isEmpty()) {
			allMetaStrings.append("/" + metaString);
		}
	}*/	

}
