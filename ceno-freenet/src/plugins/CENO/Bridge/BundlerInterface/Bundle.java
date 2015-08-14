package plugins.CENO.Bridge.BundlerInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import freenet.support.Base64;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import plugins.CENO.Bridge.CENOBridge;
import plugins.CENO.Common.URLtoUSKTools;

public class Bundle {
	private String uri;
	private String content;

	public Bundle(String URI) {
		this.uri = URI;
		content = "Bundle for URI " + URI;
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

	public void requestFromBundlerSafe() {
		try {
			doRequest();
		} catch (IOException e) {
			content = "Error while requesting bundle:\n" + e.toString();
			e.printStackTrace();
		} catch (ParseException e) {
			content = "Error while parsing response from bundle server: " + e.getMessage();
		}
	}

	private void doRequest() throws IOException, ParseException {
		uri = Base64.encodeStandardUTF8("http://" + URLtoUSKTools.validateURL(uri));
		URL url = new URL("http", "127.0.0.1", CENOBridge.bundleServerPort, "/?url=" + uri);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		StringBuffer response = new StringBuffer(); 
		while ((line = in.readLine()) != null) {
			response.append(line);
			response.append('\r');
		}
		in.close();

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
