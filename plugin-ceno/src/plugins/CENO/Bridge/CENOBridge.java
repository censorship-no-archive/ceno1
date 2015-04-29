package plugins.CENO.Bridge;

import freenet.keys.FreenetURI;
import freenet.pluginmanager.*;
import freenet.support.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import plugins.CENO.Configuration;
import plugins.CENO.Version;
import plugins.CENO.FreenetInterface.HighLevelSimpleClientInterface;
import plugins.CENO.FreenetInterface.NodeInterface;


public class CENOBridge implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned {

	private PluginRespirator pluginRespirator;

	// Need to be read from config
	//public static final Integer cacheLookupPort = 3091;
	public static final Integer requestReceiverPort = 3093;
	public static final Integer bundleServerPort = 3094;
	public static final Integer bundleInserterPort = 3095;

	private Server ceNoHttpServer;

	// Interface objects with fred
	private HighLevelSimpleClientInterface client;
	public static NodeInterface nodeInterface;
	private RequestReceiver reqReceiver;

	// Plugin-specific configuration
	public static final String pluginUri = "/plugins/plugins.CENO.CENOBridge";
	public static final String pluginName = "CENOBridge";
	public static Configuration initConfig;
	private Version version = new Version(Version.PluginType.BRIDGE);
	private static final String configPath = System.getProperty("user.home") + "/.CENO/bridge.properties";

	public static final String bridgeFreemail = "DEFLECTBridge@3s74bxq5cuap2sbco47w2yqpwmoavk4goi7brihcmebo6l5xiija.freemail";
	public static final String clientFreemail = "ceno@yay4m6a3z5hwu3fq2j7nyyhmyn6hu5s3uzqyuacdqxoak4jwggta.freemail";


	public void runPlugin(PluginRespirator pr)
	{
		// Initialize interfaces with fred
		pluginRespirator = pr;
		client = new HighLevelSimpleClientInterface(pluginRespirator.getHLSimpleClient());
		nodeInterface = new NodeInterface(pluginRespirator.getNode(), pluginRespirator);

		// Read properties of the configuration file
		initConfig = new Configuration(configPath);
		initConfig.readProperties();
		// If CeNo has no private key for inserting freesites,
		// generate a new key pair and store it in the configuration file
		if (initConfig.getProperty("insertURI") == null) {
			FreenetURI[] keyPair = nodeInterface.generateKeyPair();
			initConfig.setProperty("insertURI", keyPair[0].toString());
			initConfig.setProperty("requestURI", keyPair[1].toString());
			initConfig.storeProperties();
		}

		// Initialize RequestReceiver
		reqReceiver = new RequestReceiver(new String[]{bridgeFreemail});
		// Start a thread for polling for new freemails
		reqReceiver.loopFreemailBoxes();

		// Configure the CeNo's jetty embedded server
		ceNoHttpServer = new Server();
		configHttpServer(ceNoHttpServer);

		// Start server and wait until it gets interrupted
		try {
			ceNoHttpServer.start();
			ceNoHttpServer.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Configure CeNo's embedded server
	 * 
	 * @param ceNoHttpServer the jetty server to be configured
	 */
	private void configHttpServer(Server ceNoHttpServer) {     
		// Add a ServerConnector for each port
		/*ServerConnector httpConnector = new ServerConnector(ceNoHttpServer);
		httpConnector.setName("cacheLookup");
		httpConnector.setPort(cacheLookupPort);*/
		ServerConnector cacheConnector = new ServerConnector(ceNoHttpServer);
		cacheConnector.setName("cacheInsert");
		cacheConnector.setPort(bundleInserterPort);

		// Set server's connectors the ones configured above
		ceNoHttpServer.setConnectors(new ServerConnector[]{cacheConnector});

		// Create a collection of ContextHandlers for the server
		ContextHandlerCollection handlers = new ContextHandlerCollection();
		ceNoHttpServer.setHandler(handlers);

		// Configure ContextHandlers to listen to a specific port
		// and upon request call the appropriate AbstractHandler subclass	
		/*ContextHandler cacheLookupCtxHandler = new ContextHandler();
		cacheLookupCtxHandler.setHandler(new CacheLookupHandler());
		cacheLookupCtxHandler.setVirtualHosts(new String[]{"@cacheLookup"});*/
		ContextHandler cacheInsertCtxHandler = new ContextHandler();
		cacheInsertCtxHandler.setHandler(new BundleInserterHandler());
		cacheInsertCtxHandler.setVirtualHosts(new String[]{"@cacheInsert"});

		// Add the configured ContextHandlers to the server
		handlers.addHandler(cacheInsertCtxHandler);
	}

	public String getVersion() {
		return version.getVersion();
	}

	public long getRealVersion() {
		return version.getRealVersion();
	}

	/**
	 * Method called before termination of the CENO bridge plugin
	 * Terminates ceNoHttpServer and releases resources
	 */
	public void terminate()
	{
		// Stop the thread that is polling for freemails
		reqReceiver.stopLooping();

		// Stop ceNoHttpServer and unbind ports
		if (ceNoHttpServer != null) {
			try {
				ceNoHttpServer.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Logger.normal(this, pluginName + " terminated.");
	}

}
