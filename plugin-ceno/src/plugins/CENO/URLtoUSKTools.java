package plugins.CENO;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import plugins.CENO.Bridge.CENOBridge;
import freenet.client.InsertException;
import freenet.keys.FreenetURI;

public class URLtoUSKTools {

	public static Map<String, String> splitURL(String requestPath) throws MalformedURLException {
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

		Map<String, String> splitMap = new HashMap<String, String>();
		splitMap.put("domain", domain);
		splitMap.put("extraPath", extraPath);
		return splitMap;
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
	public static FreenetURI computeUSKfromURL(String requestPath, String requestURI) throws MalformedURLException {
		Map<String, String> splitMap = splitURL(requestPath);
		String computedKey = requestURI.replaceFirst("SSK", "USK") + splitMap.get("domain") + "/-1/" + splitMap.get("extraPath");

		return new FreenetURI(computedKey);
	}

	public static FreenetURI computeInsertURI(String domain, String insertURI) throws MalformedURLException {
		FreenetURI insertURIconfig = new FreenetURI(insertURI);
		//String computedKey = insertURI.replaceFirst("SSK", "USK") + "-1/";
		FreenetURI result = new FreenetURI("USK", domain, insertURIconfig.getRoutingKey(), insertURIconfig.getCryptoKey(), insertURIconfig.getExtra());

		try {
			result.checkInsertURI();
		} catch (InsertException e) {
			throw new MalformedURLException("The computed URI failed checkInsertURI()");
		}
		return result;
	}

	/* Extract meta strings from FreenetURI
	StringBuilder allMetaStrings = new StringBuilder();
	for (String metaString : requestKey.getAllMetaStrings()) {
		if (!metaString.isEmpty()) {
			allMetaStrings.append("/" + metaString);
		}
	}*/	

}
