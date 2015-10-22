package plugins.CENO.Bridge.Signaling;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

public class ChannelManager {
	private List<Channel> channelList;
	private static volatile ChannelManager instance;

	private ChannelManager() {
		channelList = new ArrayList<Channel>();
	}

	public static ChannelManager getInstance() {
		if (instance == null) {
			synchronized(ChannelManager.class) {
				instance = new ChannelManager();
			}
		}
		return instance;
	}

	public void addChannel(Channel channel) throws MalformedURLException {
		if (channel != null) {
			if (!channelList.contains(channel)) {
				channelList.add(channel);
				subscribeToChannel(channel);
			}
		}
	}

	public void addChannels(List<Channel> extraChannelsList) {
		if (extraChannelsList != null) {
			for (Channel channel : extraChannelsList) {
				extraChannelsList.add(channel);
			}
		}
	}

	public void addChannel(SimpleFieldSet sfs) throws MalformedURLException {
		Channel channel = null;
		try {
			channel = new Channel(sfs.getString("insertURI"));
		} catch (FSParseException e) {
			// TODO Log this
			e.printStackTrace();
			return;
		} catch (MalformedURLException e) {
			// TODO Log this
			e.printStackTrace();
		}
		addChannel(channel);
	}

	public boolean removeChannel(Channel channel) {
		if (channel != null && channelList.contains(channel)) {
			channelList.remove(channel);
			return true;
		}
		return false;
	}

	private void subscribeToChannel(Channel channel) throws MalformedURLException {
		channel.publishSyn();
		channel.subscribeToChannelUpdates();
	}


}
