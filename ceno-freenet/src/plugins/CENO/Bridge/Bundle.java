package plugins.CENO.Bridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import plugins.CENO.Common.URLtoUSKTools;

public class Bundle {
	private String uri;
	private String content;
	private static volatile Set<String> breakingUrls = new HashSet<String>();

	public Bundle(String URI) {
		this.uri = URI;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setContent(byte[] content) {
		this.content = new String(content);
	}

	public void requestFromBundler() throws IOException {
		try {
			doRequest();
		} catch (ParseException e) {
			throw new IOException(e.getMessage());
		}
	}

	private synchronized void doRequest() throws IOException, ParseException {
		if (breakingUrls.contains(uri)) {
			throw new IOException("Will not request URL " + uri + "from Bundle Server for robustness reasons");
		}
		uri = URLtoUSKTools.b64EncSafe("http://" + URLtoUSKTools.validateURL(uri));
		URL url = new URL("http", "127.0.0.1", CENOBridge.bundleServerPort, "/?url=" + uri);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		StringBuffer response = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			in.close();
		} catch (IOException e) {
			breakingUrls.add(uri);
			throw new IOException("Request for URL " + uri + " killed Bundle Server, won't try to fetch again");
		}

		JSONObject jsonResponse = (JSONObject) JSONValue.parseWithException(response.toString());
		if (jsonResponse.containsKey("error")) {
			throw new IOException("Response from bundle server included error: " + jsonResponse.get("error"));
		}

		content = (String) jsonResponse.get("bundle");
		return;
	}

	public int getContentLength() {
		if (content != null) {
			return content.length();
		}
		return 0;
	}

}
