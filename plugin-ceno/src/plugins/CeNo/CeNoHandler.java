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

	protected void writeError(Request baseRequest, HttpServletResponse response, String requestPath) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		response.getWriter().println("Error while fetching: " + requestPath);
		baseRequest.setHandled(true);
	}
	
	protected FreenetURI computeSSKfromPath(String requestPath) throws MalformedURLException {
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

}
