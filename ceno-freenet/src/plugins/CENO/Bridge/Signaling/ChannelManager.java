package plugins.CENO.Bridge.Signaling;

import java.util.ArrayList;
import java.util.List;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

public class ChannelManager {
	List<Channel> channelList;
	private static volatile ChannelManager channelManager;
	
	public ChannelManager() {
		if (ChannelManager.channelManager == null) {
			ChannelManager.channelManager = new ChannelManager(new ArrayList<Channel>());
		}
	}

	private ChannelManager(List<Channel> channelList) {
		this.channelList = channelList;
	}
	
	public static void addChannel(Channel channel) {
		if (channel != null) {
			if (!ChannelManager.channelManager.channelList.contains(channel)) {
				ChannelManager.channelManager.channelList.add(channel);
				subscribeToChannel(channel);
			}
		}
	}
	
	public static void addChannel(SimpleFieldSet sfs) {
		Channel channel = null;
		try {
			channel = new Channel(sfs.getString("insertURI"));
		} catch (FSParseException e) {
			// TODO Log this
			e.printStackTrace();
			return;
		}
		addChannel(channel);
	}
	
	public static boolean removeChannel(Channel channel) {
		if (channel != null && ChannelManager.channelManager.channelList.contains(channel)) {
			ChannelManager.channelManager.channelList.remove(channel);
			return true;
		}
		return false;
	}
	
	private static void subscribeToChannel(Channel channel) {
		//TODO Subscribe to channel's USK
	}


}
