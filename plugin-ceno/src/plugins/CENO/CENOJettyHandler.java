package plugins.CENO;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import plugins.CENO.Bridge.CENOBridge;
import freenet.client.InsertException;
import freenet.keys.FreenetURI;

public abstract class CENOJettyHandler extends AbstractHandler {

	public abstract void handle(String arg0, Request arg1, HttpServletRequest arg2,
			HttpServletResponse arg3) throws IOException, ServletException;

	protected void writeWelcome(Request baseRequest, HttpServletResponse response, String requestPath) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().print("Welcome to CeNo.");
		baseRequest.setHandled(true);
	}

	//TODO: Define error codes that CeNo plugins will be using
	// Give a descriptive message for each of them (like Malformed URL)
	protected void writeError(Request baseRequest, HttpServletResponse response, String requestPath) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		response.getWriter().print("Error while fetching: " + requestPath);
		baseRequest.setHandled(true);
	}

	protected void writeNotFound(Request baseRequest, HttpServletResponse response, String requestPath) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		response.getWriter().print("Resource not found: " + requestPath);
		baseRequest.setHandled(true);
	}

	protected void writeJSON(Request baseRequest, HttpServletResponse response, int responseStatus, JSONObject jsonResponse) throws IOException {
		response.setStatus(responseStatus);
		response.setContentType("application/json;charset=utf-8");
		response.getWriter().print(jsonResponse.toJSONString());
		baseRequest.setHandled(true);
	}

	protected JSONObject readJSONbody(BufferedReader r) throws IOException, ParseException {
		JSONObject readJSON;
		try {
			readJSON = (JSONObject) JSONValue.parseWithException(r);
		} catch (ClassCastException e) {
			return new JSONObject();
		}
		return readJSON;
	}

}
