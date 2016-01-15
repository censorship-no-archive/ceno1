package plugins.CENO.Bridge;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Security;
import java.util.List;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import plugins.CENO.CENOException;
import plugins.CENO.CENOL10n;
import plugins.CENO.Configuration;
import plugins.CENO.Version;
import plugins.CENO.Bridge.BridgeDatabase;

import plugins.CENO.Bridge.Signaling.Channel;
import plugins.CENO.Bridge.Signaling.ChannelMaker;
import plugins.CENO.Bridge.Signaling.ChannelManager;
import plugins.CENO.Common.Crypto;
import plugins.CENO.FreenetInterface.NodeInterface;
import freenet.client.InsertException;
import freenet.keys.FreenetURI;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginRealVersioned;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.IllegalBase64Exception;
import freenet.support.Logger;


public class CENOBridge implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned {

	public static final Integer cacheLookupPort = 3091;
	public static final Integer requestReceiverPort = 3093;
	public static final Integer bundleServerPort = 3094;
	public static final Integer bundleInserterPort = 3095;

	/** The HTTP Server to handle requests from other agents */
	private Server cenoHttpServer;

	// Interface objects with fred
	public static NodeInterface nodeInterface;
	BridgeDatabase bridgeDatabase;
	ChannelMaker channelMaker;

	private static boolean isMasterBridge = false;
	private static boolean isSignalBridge = false;

	// Plugin-specific configuration
	public static final String PLUGIN_URI = "/plugins/plugins.CENO.CENOBridge";
	public static final String PLUGIN_NAME = "CENOBridge";
	public static Configuration initConfig;
	private static final Version VERSION = new Version(Version.PluginType.BRIDGE);
	private static final String CONFIGPATH = ".CENO/bridge.properties";
	private static final String DBPATH = ".CENO/bridge.db";

	public static final String ANNOUNCER_PATH = "CENO-signaler";

	public void runPlugin(PluginRespirator pr)
	{
		// Initialize interfaces with Freenet node
		nodeInterface = new NodeInterface(pr.getNode(), pr);
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

		String confIsMasterBridge = initConfig.getProperty("isMasterBridge");
		if (confIsMasterBridge != null && confIsMasterBridge.equals("true")) {
			isMasterBridge = true;
		}

		String confIsSingalBridge = initConfig.getProperty("isSignalBridge");
		if (confIsSingalBridge != null && confIsSingalBridge.equals("true")) {
			isSignalBridge = true;

			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			// Read RSA keypair and modulus from configuration file or, if not available, create a new one
			KeyPair asymKeyPair = null;
			try {
				if (!Crypto.isValidKeypair(initConfig.getProperty("asymkey.pubKey"), initConfig.getProperty("asymkey.privKey"))) {
					Logger.warning(this, "CENOBridge will generate a new RSA key pair for the decentralized signaling. This might take a while");
					asymKeyPair = Crypto.generateAsymKey();
					initConfig.setProperty("asymkey.pubKey", Crypto.savePublicKey(asymKeyPair.getPublic()));
					initConfig.setProperty("asymkey.privKey", Crypto.savePrivateKey(asymKeyPair.getPrivate()));
					initConfig.storeProperties();
				} else {
					asymKeyPair = new KeyPair(Crypto.loadPublicKey(initConfig.getProperty("asymkey.pubKey")), Crypto.loadPrivateKey(initConfig.getProperty("asymkey.privKey")));
					Logger.normal(this, "Found RSA key in configuration file");
				}
			} catch (UnsupportedEncodingException e) {
				Logger.error(this, "Unsupported Encoding Exception during RSA key validation: " + e.getMessage());
			} catch (GeneralSecurityException e) {
				Logger.error(this, "General Security Exception: " + e.getMessage());
			} catch (IllegalBase64Exception e) {
				Logger.error(this, "Failed to base64 encode/decode: " + e.getMessage());
			} finally {
				if (asymKeyPair == null) {
					terminate();
					return;
				}
			}

			String confBridgeDB = initConfig.getProperty("bridgeDB");
			if (confBridgeDB == null) {
				confBridgeDB = DBPATH;
				initConfig.setProperty("bridgeDB", DBPATH);
				initConfig.storeProperties();
			}
			try {
				bridgeDatabase = new BridgeDatabase(DBPATH);
			} catch (CENOException e) {
				Logger.error(this, "Could not open bridge database");
				terminate();
				return;
			}

			try {
				channelMaker = new ChannelMaker(initConfig.getProperty("insertURI"), asymKeyPair);
				channelMaker.publishNewPuzzle();
			} catch (IOException e) {
				Logger.error(this, "Could not start channel listener for the given insertURI: " + e.getMessage());
				terminate();
				return;
			} catch (InsertException e) {
				Logger.error(this, "Could not start announcement channel insertion for the given insertURI: " + e.getMessage());
				terminate();
				return;
			} catch (GeneralSecurityException e) {
				Logger.error(this, "The given public RSA key is invalid");
				terminate();
				return;
			} catch (CENOException e) {
				Logger.error(this, "Could not start decentralized signaling channel maker: " + e.getMessage());
				terminate();
				return;
			}

			//Retrieve and poll previously established channels from the bridge.database
			try {
				List<Channel> databaseChannels = bridgeDatabase.retrieveChannels();
				int counter = ChannelManager.getInstance().addChannels(databaseChannels);
				Logger.normal(this, "Retrieved " + counter + " previously established channels from the database, failed to subscribe updates from " + 
						Integer.toString(databaseChannels.size() - counter));
			} catch (CENOException e) {
				Logger.error(this, "Exception while retrieving previously established channels from the database: " + e.getMessage());
			}
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
		// Stop the thread that is polling for new channel requests
		if (isSignalBridge && channelMaker != null) {
			channelMaker.stopListeners();
			for (Channel channel : ChannelManager.getInstance().getAllChannels()) {
				try {
					bridgeDatabase.storeChannel(channel);
				} catch (CENOException e) {
					Logger.warning(this, "Failed to save signaling channels with SSK: " + channel.getInsertSSK());
				}
			}
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
