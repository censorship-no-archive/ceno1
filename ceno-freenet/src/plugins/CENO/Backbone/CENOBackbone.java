package plugins.CENO.Backbone;

import plugins.CENO.Version;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginRealVersioned;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;

public class CENOBackbone implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned, FredPluginThreadless {

	private static final Version version = new Version(Version.PluginType.BACKBONE);

	public static final String bridgeIdentityRequestURI = "USK@QfqLw7-BJpGGMnhnJQ3~KkCiciMAsoihBCtSqy6nNbY,-lG83h70XIJ03r4ckdNnsY4zIQ-J8qTqwzSBeIG5q3s,AQACAAE/WebOfTrust/0";
	public static final String bridgeFreemail = "DEFLECTBridge@ih5ixq57yetjdbrspbtskdp6fjake4rdacziriiefnjkwlvhgw3a.freemail";

	public String getVersion() {
		return version.getVersion();
	}

	public long getRealVersion() {
		return version.getRealVersion();
	}

	@Override
	public void terminate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void runPlugin(PluginRespirator pr) {
		// TODO Auto-generated method stub

	}

}
