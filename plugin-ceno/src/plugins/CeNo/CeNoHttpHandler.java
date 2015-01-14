package plugins.CeNo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.client.FetchResult;
import freenet.keys.FreenetURI;
import freenet.node.BaseRequestThrottle;
import freenet.support.api.Bucket;
import freenet.support.io.BucketTools;

/* ------------------------------------------------------------ */
/** CeNo Plugin Http Communication and Serving server.
 *  CeNo nodes talk to CeNo through HTTP, 
 * - CeNo Client reply the url.
 * - Plugin either serve the content or DNF.
 * - In case of DNF, the client send a request asking plugin
 * - to ping a bridge to bundle the content.
 * - If Freenet being pingged, the plugin will send a 
 *   url to the CeNo bridge to bundle the content
 * - The Bridge then will serve the bundle to the plugin
 *   to insert into Freenet
 */
public class CeNoHttpHandler extends AbstractHandler
{
	private void writeError(Request baseRequest, HttpServletResponse response, String requestPath) throws IOException {
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		response.getWriter().println("Error while fetching: " + requestPath);
	}
	
    public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) 
        throws IOException, ServletException
    {
    	String requestPath = request.getPathInfo().substring(1);
    	if (requestPath.startsWith("USK@")){
			FetchResult result = null;
			try {
				result = HighLevelSimpleClientInterface.fetchURI(new FreenetURI(requestPath));
			} catch (MalformedURLException e) {
				writeError(baseRequest, response, requestPath);
			} catch (FetchException e) {
				// USK key has been updated, redirect to the new URI
				if (e.getMode() == FetchExceptionMode.PERMANENT_REDIRECT) {
					String newURI = "/".concat(e.newURI.toString());
					response.sendRedirect(newURI);
				} else {
					e.printStackTrace();
					writeError(baseRequest, response, requestPath);
				}
			}
			if (result != null) {
				// When fetching is complete, write it to the response OutputStream
				response.setContentType(result.getMimeType());
		        response.setStatus(HttpServletResponse.SC_OK);
				OutputStream resOutStream = response.getOutputStream();

				Bucket resultBucket = result.asBucket();
				try {
					BucketTools.copyTo(resultBucket, resOutStream, Long.MAX_VALUE);
					resOutStream.flush();
					resOutStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				resultBucket.free();
			}
    	} else {
    		response.setContentType("text/html;charset=utf-8");
    		response.setStatus(HttpServletResponse.SC_OK);
    		baseRequest.setHandled(true);
    		response.getWriter().println("Requested: " + request.getPathInfo().substring(1));
    	}
    }
}