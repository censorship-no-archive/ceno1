package plugins.CeNo.BridgeInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import plugins.CeNo.CeNo;

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

	public void requestFromBundler() {
		try {
			doRequest();
		} catch (IOException e) {
			content = "Error while requesting bundle:\n" + e.toString();
			e.printStackTrace();
		}
	}

	private void doRequest() throws IOException {
		URL url = new URL("http", "127.0.0.1", CeNo.bundlerPort, "/?url=" + uri);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		StringBuffer response = new StringBuffer(); 
		while ((line = in.readLine()) != null) {
			response.append(line);
			response.append('\r');
		}
		in.close();
		content = response.toString();
		return;
	}

	public int getContentLength() {
		if (content != null) {
			return content.length();
		}
		return 0;
	}

}
