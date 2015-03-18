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

	// Plugin-specific configuration
	public static final String pluginUri = "/plugins/plugins.CENO.CENO";
	public static final String pluginName = "CENO";
	private  Version version;

	public void runPlugin(PluginRespirator pr)
	{
		version = new Version(Version.PluginType.CLIENT);

		// Initialize interfaces with fred
		pluginRespirator = pr;
		client = new HighLevelSimpleClientInterface(pluginRespirator.getHLSimpleClient());
		nodeInterface = new NodeInterface(pluginRespirator.getNode());
	}

	public String getVersion() {
		if (version != null) {
			return version.getVersion();
		} else {
			return "loading";
		}
	}

	public long getRealVersion() {
		if (version != null) {
			return version.getRealVersion();
		} else {
			return 1;
		}
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
		return "<http><body>Welcome to CENO.</body></http>";
	}

	public String handleHTTPPost(HTTPRequest request) throws PluginHTTPException {
		return "<http><body>Welcome to CENO.</body></http>";
	}

}
