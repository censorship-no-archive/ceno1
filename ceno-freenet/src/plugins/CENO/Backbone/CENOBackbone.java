package plugins.CENO.Backbone;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import plugins.CENO.Version;
import plugins.CENO.FreenetInterface.NodeInterface;

import freenet.clients.http.ConnectionsToadlet.PeerAdditionReturnCodes;
import freenet.io.comm.PeerParseException;
import freenet.io.comm.ReferenceSignatureVerificationException;
import freenet.node.DarknetPeerNode;
import freenet.node.DarknetPeerNode.FRIEND_TRUST;
import freenet.node.DarknetPeerNode.FRIEND_VISIBILITY;
import freenet.node.FSParseException;
import freenet.node.Node;
import freenet.node.PeerNode;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginRealVersioned;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.Logger;
import freenet.support.SimpleFieldSet;

/**
 * CENOBackbone plugin for Freenet
 * 
 * Its main purpose is to help route CENOBrdige packets
 * faster to the area they should arrive to, based on the
 * phenomenon of Small World Routing.
 *
 * Once the plugin is loaded, it will add as friend the
 * bridge node (using the node reference in the resources)
 * and send a Freemail to the bridge with its own reference,
 * so that the bridge can add it back as a friend.
 * Trust is set to HIGH and other friends of the Backbone
 * node won't be able to see either the name or the node
 * reference of the Bridge node.
 * 
 * It is recommended that the Backbone nodes stay connected
 * for as long as possible and are given a high
 * bandwidth limit. The most straightforward way to spin
 * a backbone router is to use the CENOBackboneBox.
 * This plugin has a strong dependency on the WebOfTrust
 * and Freemail official Freenet plugins.
 */
public class CENOBackbone implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned, FredPluginThreadless {

	private static final Version version = new Version(Version.PluginType.BACKBONE);

	public static final String bridgeIdentityRequestURI = "USK@QfqLw7-BJpGGMnhnJQ3~KkCiciMAsoihBCtSqy6nNbY,-lG83h70XIJ03r4ckdNnsY4zIQ-J8qTqwzSBeIG5q3s,AQACAAE/WebOfTrust/0";
	public static final String bridgeFreemail = "DEFLECTBridge@ih5ixq57yetjdbrspbtskdp6fjake4rdacziriiefnjkwlvhgw3a.freemail";

	public static final String backboneIdentityInsertURI = "";
	public static final String backboneFreemail = "deflectbackbone@gpksc2qu27zvrp3md3g7fomwyghewkfbb56plifb5qgszwilgjua.freemail";

	public static Node node;
	public static NodeInterface nodeInterface;

	private NodeRefHelper nodeRefHelper;

	ScheduledExecutorService scheduledExecutorService;
	ScheduledFuture<?> scheduleSend;

	public void runPlugin(PluginRespirator pr) {
		node = pr.getNode();
		nodeRefHelper = new NodeRefHelper(node);

		// Add the bridge node reference in the resources as a friend
		PeerAdditionReturnCodes addBridgeResult = addFriendBridge();
		if (addBridgeResult == PeerAdditionReturnCodes.ALREADY_IN_REFERENCE || addBridgeResult == PeerAdditionReturnCodes.OK) {
			Logger.normal(this, "Successfully added the node in bridgeref.txt resource file as friend.");
		} else {
			// Bridge node could not be added as a friend, the plugin will terminate and unload
			Logger.error(this, "Error while adding Bridge node as a friend, will terminate Backbone plugin...");
			terminate();
		}

		nodeInterface = new NodeInterface(pr.getNode(), pr);

		/* Set a random next message number in order to avoid dropping freemails at the bridge,
		 * because of their message number being processed before. This is obligatory since
		 * we are using the same Freemail address with multiple backbone nodes, for reaching
		 * the bridge.
		 */
		if (!nodeInterface.setRandomNextMsgNumber(backboneFreemail, bridgeFreemail)) {
			Logger.error(this, "Could not set a random nextMessageNumber. Freemails will most probably be dropped at the bridge");
			terminate();
		}

		/* Schedule a thread in order to Send a Freemail to the bridge node with the own node reference.
		 * First attempt will be in a minute from plugin initialization, and if it fails, there will be
		 * other attempts every 2 minutes till the Freemail is sent. For every failed attempt, we keep
		 * an error-level entry in the log.
		 */
		scheduledExecutorService = Executors.newScheduledThreadPool(1);
		scheduleSend = scheduledExecutorService.scheduleWithFixedDelay(new RefSender(), 2, 1, TimeUnit.MINUTES);
	}

	/**
	 * Adds the node reference in the resources
	 * as a friend to the node this plugin is loaded.
	 * 
	 * @return the corresponding PeerAdditionReturnCode
	 * indicating whether the bridge was added successfully
	 * as a friend
	 */
	private PeerAdditionReturnCodes addFriendBridge() {
		SimpleFieldSet bridgeNodeFS;
		try {
			bridgeNodeFS = nodeRefHelper.getBridgeNodeRefFS();
		} catch (IOException e) {
			Logger.error(this, "IO Exception while parsing bridge reference resource file");
			return PeerAdditionReturnCodes.INTERNAL_ERROR;
		}
		PeerNode pn;
		try {
			pn = node.createNewDarknetNode(bridgeNodeFS, FRIEND_TRUST.HIGH, FRIEND_VISIBILITY.NO);
			((DarknetPeerNode)pn).setPrivateDarknetCommentNote("Master Bridge");
		} catch (FSParseException e) {
			return PeerAdditionReturnCodes.CANT_PARSE;
		} catch (PeerParseException e) {
			return PeerAdditionReturnCodes.CANT_PARSE;
		} catch (ReferenceSignatureVerificationException e){
			return PeerAdditionReturnCodes.INVALID_SIGNATURE;
		} catch (Throwable t) {
			Logger.error(this, "Internal error adding reference :" + t.getMessage(), t);
			return PeerAdditionReturnCodes.INTERNAL_ERROR;
		}
		if(Arrays.equals(pn.getPubKeyHash(), node.getDarknetPubKeyHash())) {
			Logger.warning(this, "The bridge  node reference file belongs to this node.");
			return PeerAdditionReturnCodes.TRY_TO_ADD_SELF;
		}
		if(!node.addPeerConnection(pn)) {
			return PeerAdditionReturnCodes.ALREADY_IN_REFERENCE;
		}
		return PeerAdditionReturnCodes.OK;
	}

	public String getVersion() {
		return version.getVersion();
	}

	public long getRealVersion() {
		return version.getRealVersion();
	}

	public void terminate() {
		if (scheduledExecutorService != null) {
			scheduledExecutorService.shutdownNow();
		}
	}


	private class RefSender implements Runnable {

		public void run() {
			if (nodeInterface.sendFreemail(CENOBackbone.backboneFreemail, new String[]{bridgeFreemail}, "addFriend", nodeRefHelper.getNodeRef(), "CENO")) {
				scheduleSend.isDone();
				scheduledExecutorService.shutdown();
				Logger.normal(RefSender.class, "Sent Freemail to the bridge with own node reference");
			} else {
				Logger.error(RefSender.class, "Failed to send an email with the own node reference to the bridge");
			}
		}

	}

}
