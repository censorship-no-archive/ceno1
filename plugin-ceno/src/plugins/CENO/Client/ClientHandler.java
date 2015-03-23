package plugins.CENO.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

public class ClientHandler {

	public static String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		return printStaticHTML("Resources/index.html");
	}

	public static String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		return "<http><body>POST request received</body></http>";
	}

	private static String printStaticHTML(String filename) {
		InputStream is = ClientHandler.class.getResourceAsStream(filename);
		if (is == null) {
			return "<http><body>HTML static file not found.</body></http>";
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = "";
		StringBuilder htmlContent = new StringBuilder();
		try {
			while ((line = br.readLine()) != null) {
				htmlContent.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "<http><body>There was an error. Please refresh.</body></http>";
		}
		return htmlContent.toString();
	}

}
