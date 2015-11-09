package plugins.CENO.Bridge;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import plugins.CENO.CENOL10n;
import plugins.CENO.Configuration;
import plugins.CENO.Version;
import plugins.CENO.FreenetInterface.HighLevelSimpleClientInterface;
import plugins.CENO.FreenetInterface.NodeInterface;
import freenet.keys.FreenetURI;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginRealVersioned;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.Logger;


public class CENOBridge implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned {

	private PluginRespirator pluginRespirator;

	public static final Integer cacheLookupPort = 3091;
	public static final Integer requestReceiverPort = 3093;
	public static final Integer bundleServerPort = 3094;
	public static final Integer bundleInserterPort = 3095;

	/** The HTTP Server to handle requests from other agents */
	private Server cenoHttpServer;

	// Interface objects with fred
	private HighLevelSimpleClientInterface client;
	public static NodeInterface nodeInterface;
	private RequestReceiver reqReceiver;
	private static boolean isMasterBridge = false;
	private static boolean isSignalBridge = false;

	// Plugin-specific configuration
	public static final String pluginUri = "/plugins/plugins.CENO.CENOBridge";
	public static final String pluginName = "CENOBridge";
	public static Configuration initConfig;
	private Version version = new Version(Version.PluginType.BRIDGE);
	private static final String configPath = System.getProperty("user.home") + "/.CENO/bridge.properties";

	public static final String bridgeFreemail = "DEFLECTBridge@ih5ixq57yetjdbrspbtskdp6fjake4rdacziriiefnjkwlvhgw3a.freemail";
	public static final String clientFreemail = "CENO@54u2ko3lssqgalpvfqbq44gwfquqrejm3itl4rxj5nt7v6mjy22q.freemail";
	public static final String clientIdentityRequestURI = "USK@7ymlO2uUoGAt9SwDDnDWLCkIkSzaJr5G6etn~vmJxrU,WMeRYMzx2tQHM~O8UWglUmBnjIhp~bh8xue-6g2pmps,AQACAAE/WebOfTrust/0";

	public static final String backboneIdentityRequestURI = "USK@M9UhahTX81i-bB7N8rmWwY5LKKEPfPWgoewNLNkLMmg,IqlCA047XPFoBhxb4gU7YbHWEUV-9iz9mJblXO~w9Zk,AQACAAE/WebOfTrust/0";
	public static final String backboneFreemail = "deflectbackbone@gpksc2qu27zvrp3md3g7fomwyghewkfbb56plifb5qgszwilgjua.freemail";

	public void runPlugin(PluginRespirator pr)
	{
		// Initialize interfaces with fred
		pluginRespirator = pr;
		client = new HighLevelSimpleClientInterface(pluginRespirator.getNode(), pluginRespirator.getHLSimpleClient());
		nodeInterface = new NodeInterface(pluginRespirator.getNode(), pluginRespirator);
		nodeInterface.initFetchContexts();
		CENOL10n.getInstance().setLanguageFromEnvVar("CENOLANG");

		// Read properties of the configuration file
		initConfig = new Configuration(configPath);
		initConfig.readProperties();
		// If CENO has no private key for inserting freesites,
		// generate a new key pair and store it in the configuration file
		if (initConfig.getProperty("insertURI") == null || initConfig.getProperty("insertURI").isEmpty()) {
			Logger.warning(this, "CENOBridge will generate a new public key for inserting bundles.");
			FreenetURI[] keyPair = nodeInterface.generateKeyPair();
			initConfig.setProperty("insertURI", keyPair[0].toString());
			initConfig.setProperty("requestURI", keyPair[1].toString());
			initConfig.storeProperties();
		}

		String confIsMasterBridge = initConfig.getProperty("isMasterBridge");

		if (confIsMasterBridge != null && confIsMasterBridge.equals("true")) {
			isMasterBridge = true;
		}

		String confIsSingalBridge = initConfig.getProperty("isSignalBridge");

		if (confIsSingalBridge != null && confIsSingalBridge.equals("true")) {
			isSignalBridge = true;
		}

		if (isSignalBridge) {
			nodeInterface.clearOutboxLog(bridgeFreemail, clientFreemail);
			// Initialize RequestReceiver
			reqReceiver = new RequestReceiver(new String[]{bridgeFreemail});
			// Start a thread for polling for new freemails
			reqReceiver.loopFreemailBoxes();
		}

		// Configure CENO's jetty embedded server
		cenoHttpServer = new Server();
		configHttpServer(cenoHttpServer);

		// Start server and wait until it gets interrupted
		try {
			cenoHttpServer.start();
			cenoHttpServer.join();
		} catch (InterruptedException interruptedEx) {
			Logger.normal(this, "HTTP Server interrupted. Terminating plugin...");
			terminate();
			return;
		} catch (Exception ex) {
			Logger.error(this, "HTTP Server terminated abnormally");
			Logger.error(this, ex.getMessage());
		}
	}

	/**
	 * Configure CENO's embedded server
	 *
	 * @param cenoHttpServer the jetty server to be configured
	 */
	private void configHttpServer(Server cenoHttpServer) {
		// Create a collection of ContextHandlers for the server
		ContextHandlerCollection handlers = new ContextHandlerCollection();

		// Add a ServerConnector for the BundlerInserter agent
		ServerConnector bundleInserterConnector = new ServerConnector(cenoHttpServer);
		bundleInserterConnector.setName("bundleInserter");
		bundleInserterConnector.setHost("localhost");
		bundleInserterConnector.setPort(bundleInserterPort);

		// Add the connector to the server
		cenoHttpServer.addConnector(bundleInserterConnector);

		// Configure ContextHandlers to listen to a specific port
		// and upon request call the appropriate CENOJettyHandler subclass
		ContextHandler cacheInsertCtxHandler = new ContextHandler();
		cacheInsertCtxHandler.setMaxFormContentSize(2000000);
		cacheInsertCtxHandler.setHandler(new BundleInserterHandler());
		//cacheInsertCtxHandler.setVirtualHosts(new String[]{"@cacheInsert"});

		// Add the configured ContextHandler to the server
		handlers.addHandler(cacheInsertCtxHandler);


		//Uncomment the following block if you need a lookup handler in the bridge side
		/*
		ServerConnector httpConnector = new ServerConnector(cenoHttpServer);
		httpConnector.setName("cacheLookup");
		httpConnector.setPort(cacheLookupPort);
		cenoHttpServer.addConnector(httpConnector);

		ContextHandler cacheLookupCtxHandler = new ContextHandler();
		cacheLookupCtxHandler.setHandler(new CacheLookupHandler());
		cacheLookupCtxHandler.setVirtualHosts(new String[]{"@cacheLookup"});

		handlers.addHandler(cacheLookupCtxHandler);
		 */

		cenoHttpServer.setHandler(handlers);
	}

	public String getVersion() {
		return version.getVersion();
	}

	public long getRealVersion() {
		return version.getRealVersion();
	}

	public static boolean isMasterBridge() {
		return isMasterBridge;
	}

	/**
	 * Method called before termination of the CENO bridge plugin
	 * Terminates ceNoHttpServer and releases resources
	 */
	public void terminate()
	{
		// Stop the thread that is polling for freemails
		if (isSignalBridge) {
			reqReceiver.stopLooping();
			nodeInterface.clearOutboxLog(bridgeFreemail, clientFreemail);
		}

		// Stop cenoHttpServer and unbind ports
		if (cenoHttpServer != null) {
			try {
				cenoHttpServer.stop();
			} catch (Exception e) {
				Logger.error(this, "Exception while terminating HTTP server.");
				Logger.error(this, e.getMessage());
			}
		}

		Logger.normal(this, pluginName + " terminated.");
	}

}
