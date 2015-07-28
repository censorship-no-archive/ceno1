package plugins.CENO.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.minidev.json.JSONObject;
import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import freenet.pluginmanager.FredPluginHTTP;
import freenet.pluginmanager.PluginHTTPException;
import freenet.support.api.HTTPRequest;

public abstract class AbstractCENOClientHandler implements FredPluginHTTP {

	public abstract String handleHTTPGet(HTTPRequest request) throws PluginHTTPException;
	public abstract String handleHTTPPost(HTTPRequest request) throws PluginHTTPException;
	
	protected String printStaticHTML(String filename) {
		InputStream is = AbstractCENOClientHandler.class.getResourceAsStream(filename);
		if (is == null) {
			return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_STATIC_NOT_FOUND));
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
			return returnErrorJSON(new CENOException(CENOErrCode.LCS_HANDLER_STATIC_IO));
		}
		return htmlContent.toString();
	}
	
	protected String printStaticHTMLReplace(String filename, String target, String replacement) {
		return printStaticHTML(filename).replace(target, replacement);
	}
	
	protected boolean isClientHtml(HTTPRequest request) {
		String clientType = request.getParam("client");
		if (clientType.compareToIgnoreCase("html") == 0) {
			return true;
		}
		return false;
	}
	
	protected String returnErrorJSON(CENOException cenoEx) {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("errCode", cenoEx.getErrCode().getDocCode());
		jsonResponse.put("errMsg", cenoEx.getMessage());
		return jsonResponse.toJSONString();
	}

}