package plugins.CeNo;

import freenet.pluginmanager.*;
import freenet.support.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import plugins.CeNo.FreenetInterface.NodeInterface;


public class CeNo implements FredPlugin {

    //private final static Logger LOGGER = Logger.getLogger(CeNo.class.getName());
    private PluginRespirator pluginRespirator;

    //Need to be read from config
    private final static Integer ceNoPluginHttpPort = 3091;
    private final static Integer ceNoPluginCachePort = 3092;

    private Server ceNoHttpServer;
    
    private HighLevelSimpleClientInterface client;
    public NodeInterface nodeInterface;

    public static final String pluginUri = "/plugins/plugins.CeNo.CeNo";
	public static final String pluginName = "CeNo";


    public void runPlugin(PluginRespirator pr)
    {
        pluginRespirator = pr;
        client = new HighLevelSimpleClientInterface(pr.getHLSimpleClient());
        nodeInterface = new NodeInterface(pr.getNode());

        ceNoHttpServer = new Server(ceNoPluginHttpPort);
        
        ServerConnector httpConnector = new ServerConnector(ceNoHttpServer);
        httpConnector.setName("http");
        httpConnector.setPort(ceNoPluginHttpPort);
        ServerConnector cacheConnector = new ServerConnector(ceNoHttpServer);
        cacheConnector.setName("cache");
        cacheConnector.setPort(ceNoPluginCachePort);
        
        ceNoHttpServer.setConnectors(new ServerConnector[]{httpConnector, cacheConnector});

        ContextHandlerCollection handlers = new ContextHandlerCollection();
        ceNoHttpServer.setHandler(handlers);
        
        ContextHandler httpCtxHandler = new ContextHandler();
        httpCtxHandler.setHandler(new CeNoHttpHandler());
        httpCtxHandler.setVirtualHosts(new String[]{"@http"});
        ContextHandler cacheCtxHandler = new ContextHandler();
        cacheCtxHandler.setHandler(new CeNoCacheHanlder());
        cacheCtxHandler.setVirtualHosts(new String[]{"@cache"});
        
        
        handlers.addHandler(httpCtxHandler);
        handlers.addHandler(cacheCtxHandler);

        try {
        	ceNoHttpServer.start();
        	ceNoHttpServer.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void terminate()
    {
    	if (ceNoHttpServer != null) {
			try {
				ceNoHttpServer.stop();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
        Logger.normal(this, pluginName + " terminated.");
    }

}
