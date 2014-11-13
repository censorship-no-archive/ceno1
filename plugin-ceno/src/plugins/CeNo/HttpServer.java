package org.eclipse.jetty.embedded;

import org.eclipse.jetty.server.Server;

/* ------------------------------------------------------------ */
/** CeNo Plugin Http Communication and Serving server.
 * CeNo nodes talk to CeNo through HTTP, 
 * - CeNo Client relay the url.
 * - Plugin either serve the content or DNF.
 * - In case of DNF, the client send a request asking plugin
 * - to ping a bridge to bundle the content.
 * - If Freenet being pinged, the plugin will send a 
 *   url to the CeNo bridge to bundle the content
 * - The Bridge then will serve the bundle to the plugin
 *   to insert into Freenet
 */
public class CeNoPluginHttpServer
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.start();
        server.join();
    }
}
