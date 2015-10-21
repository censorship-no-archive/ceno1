package plugins.CENO.Client;

import plugins.CENO.CENOL10n;
import plugins.CENO.Configuration;
import plugins.CENO.Version;
import plugins.CENO.FreenetInterface.HighLevelSimpleClientInterface;
import plugins.CENO.FreenetInterface.NodeInterface;
import freenet.keys.FreenetURI;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginHTTP;
import freenet.pluginmanager.FredPluginRealVersioned;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginHTTPException;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.Logger;
import freenet.support.api.HTTPRequest;


/**
 * CENO Client plugin
 * 
 * Implements the Local Cache Server (LCS) and Request Sender (RS)
 * CENO agents.
 */
public class CENOClient implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned, FredPluginHTTP, FredPluginThreadless {

	// Interface objects with fred
	public static NodeInterface nodeInterface;
	private static final ClientHandler clientHandler = new ClientHandler();

	// Plugin-specific configuration
	public static final String PLUGIN_URI = "/plugins/plugins.CENO.CENO";
	public static final String PLUGIN_NAME = "CENO";
	private static final Version VERSION = new Version(Version.PluginType.CLIENT);
	
	public static Configuration initConfig;
	private static final String CONFIGPATH = ".CENO/client.properties";

	// Bridge and freemail-specific constants
	public static final String BRIDGE_KEY = "SSK@mlfLfkZmWIYVpKbsGSzOU~-XuPp~ItUhD8GlESxv8l4,tcB-IHa9c4wpFudoSm0k-iTaiE~INdeQXvcYP2M1Nec,AQACAAE/";
	public static final String BRIDGE_IDENTITY_REQUEST_URI = "USK@QfqLw7-BJpGGMnhnJQ3~KkCiciMAsoihBCtSqy6nNbY,-lG83h70XIJ03r4ckdNnsY4zIQ-J8qTqwzSBeIG5q3s,AQACAAE/WebOfTrust/0";
	public static final String BRIDGE_FREEMAIL = "DEFLECTBridge@ih5ixq57yetjdbrspbtskdp6fjake4rdacziriiefnjkwlvhgw3a.freemail";

	public static final String CLIENT_IDENTITY_INSERT_URI = "USK@SNS-BKGDFS4ciG3HV6o5MQjvIdCDn9G8DfIeIK~7kBQ,WMeRYMzx2tQHM~O8UWglUmBnjIhp~bh8xue-6g2pmps,AQECAAE/WebOfTrust/0";
	public static final String CLIENT_FREEMAIL = "CENO@54u2ko3lssqgalpvfqbq44gwfquqrejm3itl4rxj5nt7v6mjy22q.freemail";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void runPlugin(PluginRespirator pr)
	{
		// Initialize interfaces with Freenet node
		//TODO initialized within NodeInterface, do not expose HLSC but only via nodeInterface
		new HighLevelSimpleClientInterface(pr.getNode());
		nodeInterface = new NodeInterface(pr.getNode(), pr);
		new CENOL10n("CENOLANG");
		
		initConfig = new Configuration(CONFIGPATH);
		initConfig.readProperties();

		// Initialize LCS
		nodeInterface.initFetchContexts();
		ULPRManager.init();

		// Initialize RS - Make a new class ChannelManager that handles ChannelMaker
		//ChannelMaker channelMaker = new ChannelMaker();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getVersion() {
		return VERSION.getVersion();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getRealVersion() {
		return VERSION.getRealVersion();
	}

	/**
	 * Method called before termination of the CENO plugin
	 */
	@Override
	public void terminate()
	{
		// Clear the CENO client freemail outbox directory
		nodeInterface.clearOutboxMessages(CLIENT_FREEMAIL, BRIDGE_FREEMAIL);
		//TODO Release ULPRs' resources
		Logger.normal(this, PLUGIN_NAME + " terminated.");
	}

	@Override
	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		return clientHandler.handleHTTPGet(request);
	}

	@Override
	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		return clientHandler.handleHTTPPost(request);
	}

}
