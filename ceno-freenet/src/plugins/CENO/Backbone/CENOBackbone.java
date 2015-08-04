package plugins.CENO.Backbone;

import java.io.IOException;
import java.util.Arrays;

import plugins.CENO.Version;
import plugins.CENO.Client.CENOClient;
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
import freenet.support.Fields;
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
 * refernece of the Bridge node.
 * 
 * It is recommended that the Backbone nodes stay connected
 * for as long as possible and are given a high
 * bandwidth limit. The most straightforward way to spin
 * a backbone router is to use the CENOBackboneBox.
 * This plugin has a strong dependency on the WebOfTrust
 * and Freemail official Freenet plugins.
 *
 */
public class CENOBackbone implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned, FredPluginThreadless {

	private static final Version version = new Version(Version.PluginType.BACKBONE);

	public static final String bridgeIdentityRequestURI = "USK@QfqLw7-BJpGGMnhnJQ3~KkCiciMAsoihBCtSqy6nNbY,-lG83h70XIJ03r4ckdNnsY4zIQ-J8qTqwzSBeIG5q3s,AQACAAE/WebOfTrust/0";
	public static final String bridgeFreemail = "DEFLECTBridge@ih5ixq57yetjdbrspbtskdp6fjake4rdacziriiefnjkwlvhgw3a.freemail";

	public static final String backboneIdentityInsertURI = "";
	public static final String backboneFreemail = "";
	
	public static Node node;
	private NodeRefHelper nodeRefHelper;
	public static NodeInterface nodeInterface;
	
	@Override
	public void runPlugin(PluginRespirator pr) {
		node = pr.getNode();
		nodeRefHelper = new NodeRefHelper(node);
		PeerAdditionReturnCodes addBridgeResult = addFriendBridge();
		if (addBridgeResult == PeerAdditionReturnCodes.ALREADY_IN_REFERENCE || addBridgeResult == PeerAdditionReturnCodes.OK) {
			Logger.normal(this, "Successfully added the node in bridgeref.txt resource file as friend.");
		} else {
			Logger.error(this, "Error while adding Bridge node as a friend, will terminate Backbone plugin...");
			terminate();
		}
		nodeInterface = new NodeInterface(pr.getNode(), pr);
		nodeInterface.sendFreemail(CENOClient.clientFreemail, new String[]{bridgeFreemail}, "addFriend", nodeRefHelper.getNodeRef(), "CENO");
	}
	
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
			if(node.isOpennetEnabled()) {
				pn = node.createNewOpennetNode(bridgeNodeFS);
			} else {
				pn = node.createNewDarknetNode(bridgeNodeFS, FRIEND_TRUST.HIGH, FRIEND_VISIBILITY.NO);
				((DarknetPeerNode)pn).setPrivateDarknetCommentNote("Bridge");
			}
		} catch (FSParseException e1) {
			return PeerAdditionReturnCodes.CANT_PARSE;
		} catch (PeerParseException e1) {
			return PeerAdditionReturnCodes.CANT_PARSE;
		} catch (ReferenceSignatureVerificationException e1){
			return PeerAdditionReturnCodes.INVALID_SIGNATURE;
		} catch (Throwable t) {
            Logger.error(this, "Internal error adding reference :" + t.getMessage(), t);
			return PeerAdditionReturnCodes.INTERNAL_ERROR;
		}
		if(Arrays.equals(pn.getPubKeyHash(), node.getDarknetPubKeyHash())) {
			Logger.warning(this, "The bridge  node reference file belongs to this node.");
			return PeerAdditionReturnCodes.TRY_TO_ADD_SELF;
		}
		if(!this.node.addPeerConnection(pn)) {
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

	@Override
	public void terminate() {
		// TODO Auto-generated method stub

	}

}
