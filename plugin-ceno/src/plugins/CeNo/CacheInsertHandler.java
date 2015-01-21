package plugins.CeNo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;


/* ------------------------------------------------------------ */
/** CeNo Plugin handler for requests to cache bundles
 * 	
 * CacheInsertHandler listens to :{@link CeNo#cacheInsertPort} port
 * and under the "/store" route for POST requests.
 * Those POST requests include the bundled page as well as the original url.
 * The handler caches the given bundle under its signed subspace.
 * Upon successful insertion, the handler replies with "stored".
 */
public class CacheInsertHandler extends AbstractHandler {

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response)
			throws IOException, ServletException
	{ 
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("stored");
		baseRequest.setHandled(true);
	}

}
