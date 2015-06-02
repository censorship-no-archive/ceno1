package plugins.CENO.Client;

import plugins.CENO.Version;
import plugins.CENO.FreenetInterface.HighLevelSimpleClientInterface;
import plugins.CENO.FreenetInterface.NodeInterface;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginHTTP;
import freenet.pluginmanager.FredPluginRealVersioned;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginHTTPException;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.Logger;
import freenet.support.api.HTTPRequest;


public class CENOClient implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned, FredPluginHTTP, FredPluginThreadless {

	private PluginRespirator pluginRespirator;

	// Interface objects with fred
	private HighLevelSimpleClientInterface client;
	public static NodeInterface nodeInterface;
	private static final ClientHandler clientHandler = new ClientHandler();

	// Plugin-specific configuration
	public static final String pluginUri = "/plugins/plugins.CENO.CENO";
	public static final String pluginName = "CENO";
	private static final Version version = new Version(Version.PluginType.CLIENT);

	// Bridge and freemail-specific constants
	public static final String bridgeKey = "SSK@Rx6x6Ik1y93wGk8OtTvZaMQ~Ni6uqxFMclGP8BHrk5g,aBMErm8fkZ7xuFnSzSLnBKgHmjk6PR1Ng4V8ITxXzk8,AQACAAE/";
	public static final String bridgeIdentityRequestURI = "USK@QfqLw7-BJpGGMnhnJQ3~KkCiciMAsoihBCtSqy6nNbY,-lG83h70XIJ03r4ckdNnsY4zIQ-J8qTqwzSBeIG5q3s,AQACAAE/WebOfTrust/0";
	public static final String bridgeFreemail = "DEFLECTBridge@ih5ixq57yetjdbrspbtskdp6fjake4rdacziriiefnjkwlvhgw3a.freemail";
	
	public static final String clientIdentityInsertURI = "USK@SNS-BKGDFS4ciG3HV6o5MQjvIdCDn9G8DfIeIK~7kBQ,WMeRYMzx2tQHM~O8UWglUmBnjIhp~bh8xue-6g2pmps,AQECAAE/WebOfTrust/0";
	public static final String clientFreemail = "CENO@54u2ko3lssqgalpvfqbq44gwfquqrejm3itl4rxj5nt7v6mjy22q.freemail";

	public void runPlugin(PluginRespirator pr)
	{
		// Initialize interfaces with fred
		pluginRespirator = pr;
		client = new HighLevelSimpleClientInterface(pluginRespirator.getHLSimpleClient());
		ULPRManager.init();
		RequestSender.init(new String[]{bridgeFreemail});
		nodeInterface = new NodeInterface(pluginRespirator.getNode(), pluginRespirator);
		nodeInterface.copyAccprops(clientFreemail);
	}

	public String getVersion() {
		return version.getVersion();
	}

	public long getRealVersion() {
		return version.getRealVersion();
	}

	/**
	 * Method called before termination of the CeNo plugin
	 * Terminates ceNoHttpServer and releases resources
	 */
	public void terminate()
	{
		Logger.normal(this, pluginName + " terminated.");
	}

	public String handleHTTPGet(HTTPRequest request) throws PluginHTTPException {
		return clientHandler.handleHTTPGet(request);
	}

	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		return clientHandler.handleHTTPPost(request);
	}

}
