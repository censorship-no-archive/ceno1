package plugins.CeNo;

import freenet.pluginmanager.*;
import freenet.support.Logger;

import org.eclipse.jetty.server.Server;

import plugins.CeNo.FreenetInterface.NodeInterface;

import java.util.Date;


public class CeNo implements FredPlugin {

    //private final static Logger LOGGER = Logger.getLogger(CeNo.class.getName());
    private PluginRespirator pluginRespirator;
	private volatile boolean goon = true;

    //Need to be read from config
    private final static Integer ceNoPluginHttpPort = 3091;

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
        
        CeNoHttpHandler handler = new CeNoHttpHandler();
        ceNoHttpServer.setHandler(handler);

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
