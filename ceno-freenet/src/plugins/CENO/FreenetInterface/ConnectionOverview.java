package plugins.CENO.FreenetInterface;

import freenet.node.DarknetPeerNode;
import freenet.node.Node;
import freenet.node.PeerManager;

public class ConnectionOverview {

	private Node node;
	private PeerManager peerManager;

	public class NodeConnections {
		private int numberOfCurrentConnections;
		private int numberOfMaximumConnections;

		public NodeConnections(int numberOfCurrentConnections, int numberOfMaximumConnections) {
			this.numberOfCurrentConnections = numberOfCurrentConnections;
			this.numberOfMaximumConnections = numberOfMaximumConnections;
		}

		public int getCurrent() {
			return numberOfCurrentConnections;
		}

		public int getMaximum() {
			return numberOfMaximumConnections;
		}
	}

	public ConnectionOverview(Node node) {
		this.node = node;
		this.peerManager = node.peers;
	}

	public NodeConnections getConnections() {
		int numberOfCurrentConnections = getNumberOfCurrentConnections();
		int numberOfMaximumConnections = getNumberOfMaximumConnections();
		return new NodeConnections(numberOfCurrentConnections, numberOfMaximumConnections);
	}

	private int getNumberOfCurrentConnections() {
		return peerManager.countConnectedPeers();
	}

	private int getNumberOfMaximumConnections() {
		if (node.isOpennetEnabled()) {
			return node.getOpennet().getNumberOfConnectedPeersToAimIncludingDarknet();
		}

		int enabledPeers = 0;
		for (DarknetPeerNode darknetPeer : peerManager.getDarknetPeers()) {
			if (darknetPeer != null && !darknetPeer.isDisabled()) {
				enabledPeers++;
			}
		}
		return enabledPeers;
	}

}