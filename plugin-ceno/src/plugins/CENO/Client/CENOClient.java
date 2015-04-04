package plugins.CENO.Client;

import javax.mail.Message;
import javax.mail.MessagingException;

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

	// Freemail-specific constants
	public static final String bridgeKey = "SSK@Rx6x6Ik1y93wGk8OtTvZaMQ~Ni6uqxFMclGP8BHrk5g,aBMErm8fkZ7xuFnSzSLnBKgHmjk6PR1Ng4V8ITxXzk8,AQACAAE/";
	public static final String bridgeFreemail = "DEFLECTBridge@3s74bxq5cuap2sbco47w2yqpwmoavk4goi7brihcmebo6l5xiija.freemail";
	public static final String clientFreemail = "ceno@yay4m6a3z5hwu3fq2j7nyyhmyn6hu5s3uzqyuacdqxoak4jwggta.freemail";

	public void runPlugin(PluginRespirator pr)
	{
		// Initialize interfaces with fred
		pluginRespirator = pr;
		client = new HighLevelSimpleClientInterface(pluginRespirator.getHLSimpleClient());
		ULPRManager.init();
		RequestSender.init();
		nodeInterface = new NodeInterface(pluginRespirator.getNode());
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
