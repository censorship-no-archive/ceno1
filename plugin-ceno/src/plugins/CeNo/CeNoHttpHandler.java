import java.io.IOException;
 
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

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
    public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println("<h1>Hello World</h1>");
    }
}


