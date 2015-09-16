package plugins.CENO.Bridge.Signaling;

import java.util.ArrayList;
import java.util.List;

public class ChannelManager {
	
	List<Channel> channelList;
	
	public ChannelManager() {
		channelList = new ArrayList<Channel>();
	}
	
	public ChannelManager(List<Channel> channelList) {
		this.channelList = channelList;
	}
	
	public void addChannel(Channel channel) {
		if (channel != null) {
			if (!this.channelList.contains(channel)) {
				this.channelList.add(channel);
				subscribeToChannel(channel);
			}
		}
	}
	
	public boolean removeChannel(Channel channel) {
		if (channel != null && this.channelList.contains(channel)) {
			this.channelList.remove(channel);
			return true;
		}
		return false;
	}
	
	private void subscribeToChannel(Channel channel) {
		//TODO Subscribe to channel's USK
	}


}
