package plugins.CENO;

import freenet.pluginmanager.FredPluginRealVersioned;
import freenet.pluginmanager.FredPluginVersioned;

public class Version implements FredPluginVersioned, FredPluginRealVersioned {

	// Versions of the plugins, in human-readable and "real" format
	public static final String CLIENT_VERSION = "1.0.0";
	public static final int CLIENT_REAL_VERSION = 15;

	public static final String BRIDGE_VERSION = "1.0.0";
	public static final int BRIDGE_REAL_VERSION = 15;

	public static final String BACKBONE_VERSION = "1.0.0";
	public static final int BACKBONE_REAL_VERSION = 15;

	/** Revision number of Version.java as read from CVS */
	public static final String cvsRevision = "@custom@";

	public enum PluginType { CLIENT, BRIDGE, BACKBONE };

	private PluginType pluginType;

	public Version(PluginType pluginType) {
		this.pluginType = pluginType;
	}

	public long getRealVersion() {
		switch(pluginType) {
		case BRIDGE:
			return BRIDGE_REAL_VERSION;
		case CLIENT:
			return CLIENT_REAL_VERSION;
		case BACKBONE:
			return BACKBONE_REAL_VERSION;
		}
		return -1;
	}

	public String getVersion() {
		switch(pluginType) {
		case BRIDGE:
			return BRIDGE_VERSION;
		case CLIENT:
			return CLIENT_VERSION;
		case BACKBONE:
			return BACKBONE_VERSION;
		}
		return "Unknown";
	}

}
