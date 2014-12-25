package plugins.CeNo;

import freenet.pluginmanager.*;

import freenet.support.Logger;

import org.eclipse.jetty.server.Server;

import java.util.Date;


public class CeNo implements FredPlugin {

    //private final static Logger LOGGER = Logger.getLogger(CeNo.class.getName());
    private PluginRespirator pluginRespirator;
	private volatile boolean goon = true;

    //Need to be read from config
    private final static Integer ceNoPluginHttpPort = 3091;

    private Server ceNoHttpServer = new Server(ceNoPluginHttpPort);

    public static final String pluginUri = "/plugins/plugins.CeNo.CeNo";
	public static final String pluginName = "CeNo";


    // /**
    //    setup the web interface in FProxy
    //    but we don't really need a web inteface as we don't use FProxy 
    //    per sa
    //  */
    // private void setupWebInterface()
    // {
	// 	webInterface = new WebInterface(this, pluginRespirator.getHLSimpleClient(), pluginRespirator.getToadletContainer());
	// 	webInterface.load();

    //     /*PluginContex pluginContext = new PluginContext(pluginRespirator);
    //     this.webInterface = new WebInterface(plginContext);

    //     pluginRespirator.getPageMaker().addNavigationCategory(basePath + "/", "WebOfTrust.menunName.name", "WebOfTrust.menuName.tooltip", this);
    //     ToadletContainer tc = pluginRespirator.getToadletContainer();

    //     ///pages
    //     Overview oc = new Overview(this, pluginRespirator.getHLSimpleClient(), basePath, db);*/
                                 
        
    // }

    public void runPlugin(PluginRespirator pr)
    {
        pluginRespirator = pr;

        Server server = new Server(3091);
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        
        }

		while(goon) {
			System.err.println("Heartbeat from CeNo-plugin: " + (new Date()));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// Who cares ?
			}
		}
        
    }

    public void terminate()
    {
		goon = false;
        Logger.normal(this, pluginName + " terminated.");
    }

}
