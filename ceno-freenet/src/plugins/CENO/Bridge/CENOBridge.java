package plugins.CENO.Bridge;

import java.math.BigInteger;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import plugins.CENO.CENOException;
import plugins.CENO.CENOL10n;
import plugins.CENO.Configuration;
import plugins.CENO.Version;
import plugins.CENO.Bridge.Signaling.ChannelMaker;
import plugins.CENO.Bridge.Signaling.Crypto;
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
	ChannelMaker channelMaker;

	private static boolean isMasterBridge = false;
	private static boolean isSignalBridge = false;

	// Plugin-specific configuration
	public static final String PLUGIN_URI = "/plugins/plugins.CENO.CENOBridge";
	public static final String PLUGIN_NAME = "CENOBridge";
	public static Configuration initConfig;
	private static final Version VERSION = new Version(Version.PluginType.BRIDGE);
	private static final String CONFIGPATH = ".CENO/bridge.properties";

	public static final String ANNOUNCER_PATH = "CENO-signaler";

	public void runPlugin(PluginRespirator pr)
	{
		// Initialize interfaces with fred
		pluginRespirator = pr;
		client = new HighLevelSimpleClientInterface(pluginRespirator.getNode(), pluginRespirator.getHLSimpleClient());
		nodeInterface = new NodeInterface(pluginRespirator.getNode(), pluginRespirator);
		nodeInterface.initFetchContexts();
		CENOL10n.getInstance().setLanguageFromEnvVar("CENOLANG");

		// Read properties of the configuration file
		initConfig = new Configuration(CONFIGPATH);
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
		
		AsymmetricCipherKeyPair asymKeyPair;
		if (initConfig.getProperty("asymkey.privexponent") == null || initConfig.getProperty("asymkey.modulus") == null || initConfig.getProperty("asymkey.pubexponent") == null) {
			Logger.warning(this, "CENOBridge will generate a new RSA key pair for the decentralized signaling. This might take a while");
			asymKeyPair = Crypto.generateAsymKey();
			initConfig.setProperty("asymkey.privexponent", ((RSAKeyParameters) asymKeyPair.getPrivate()).getExponent().toString(23));
			initConfig.setProperty("asymkey.modulus", ((RSAKeyParameters) asymKeyPair.getPublic()).getModulus().toString(32));
			initConfig.setProperty("asymkey.pubexponent", ((RSAKeyParameters) asymKeyPair.getPublic()).getExponent().toString(32));
			initConfig.storeProperties();
		} else {
			asymKeyPair = new AsymmetricCipherKeyPair(new RSAKeyParameters(false, new BigInteger(initConfig.getProperty("asymkey.modulus"),32), new BigInteger(initConfig.getProperty("asymkey.pubexponent"), 32)),
					new RSAKeyParameters(true, new BigInteger(initConfig.getProperty("asymkey.modulus"), 32), new BigInteger(initConfig.getProperty("asymkey.privexponent"), 32)));
		}
		
		try {
			channelMaker = new ChannelMaker(initConfig.getProperty("insertURI"),asymKeyPair);
		} catch (CENOException e) {
			Logger.error(this, "Could not start decentralized signaling channel maker");
			terminate();
		}

		String confIsMasterBridge = initConfig.getProperty("isMasterBridge");

		if (confIsMasterBridge != null && confIsMasterBridge.equals("true")) {
			isMasterBridge = true;
		}

		String confIsSingalBridge = initConfig.getProperty("isSignalBridge");

		if (confIsSingalBridge != null && confIsSingalBridge.equals("true")) {
			isSignalBridge = true;
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
			terminate();
			return;
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
		return VERSION.getVersion();
	}

	public long getRealVersion() {
		return VERSION.getRealVersion();
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
		channelMaker.stopListener();
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

		Logger.normal(this, PLUGIN_NAME + " terminated.");
	}

}
