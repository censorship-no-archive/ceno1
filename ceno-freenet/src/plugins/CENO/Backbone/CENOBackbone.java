package plugins.CENO.Backbone;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import plugins.CENO.Configuration;
import plugins.CENO.Version;
import plugins.CENO.FreenetInterface.NodeInterface;
import freenet.client.InsertException;
import freenet.client.async.PersistenceDisabledException;
import freenet.clients.http.ConnectionsToadlet.PeerAdditionReturnCodes;
import freenet.io.comm.PeerParseException;
import freenet.io.comm.ReferenceSignatureVerificationException;
import freenet.keys.FreenetURI;
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
 * Also, you are encouraged to allocate enough space for the
 * client cache, because it will contribute to the longevity
 * and faster access of the bundles inserted by the CENOBridge.
 * 
 * This plugin has a strong dependency on the WebOfTrust
 * and Freemail official Freenet plugins.
 */
public class CENOBackbone implements FredPlugin, FredPluginVersioned, FredPluginRealVersioned, FredPluginThreadless {

	private static final Version VERSION = new Version(Version.PluginType.BACKBONE);

	//public static final String BRIDGE_KEY = "SSK@mlfLfkZmWIYVpKbsGSzOU~-XuPp~ItUhD8GlESxv8l4,tcB-IHa9c4wpFudoSm0k-iTaiE~INdeQXvcYP2M1Nec,AQACAAE/"; what's up?

	public static Node node;
	public static NodeInterface nodeInterface;

	private NodeRefHelper nodeRefHelper;

	public static Configuration initConfig;
	private static final String CONFIGPATH = ".CENO/bridge.properties";

	public static final String ANNOUNCER_PATH = "CENO-Backbone";

	ScheduledExecutorService scheduledExecutorService;
	ScheduledFuture<?> scheduleSend;

	public void runPlugin(PluginRespirator pr) {
		node = pr.getNode();
		nodeRefHelper = new NodeRefHelper(node);

		// Read properties of the configuration file
		initConfig = new Configuration(CONFIGPATH);
		initConfig.readProperties();

		// Add the bridge node reference in the resources as a friend
		PeerAdditionReturnCodes addBridgeResult = addFriendBridges();
		if (addBridgeResult == PeerAdditionReturnCodes.ALREADY_IN_REFERENCE || addBridgeResult == PeerAdditionReturnCodes.OK) {
			Logger.normal(this, "Successfully added the node in bridgeref.txt resource file as friend.");
		} else {
			// Bridge node could not be added as a friend, the plugin will terminate and unload
			Logger.error(this, "Error while adding Bridge node as a friend, will terminate Backbone plugin...");
			terminate();
		}

		nodeInterface = new NodeInterface(pr.getNode(), pr);

		if (initConfig.getProperty("backboneAnnounceURI") == null || initConfig.getProperty("backboneAnnounceURI").isEmpty()) { //we are not told where to announce our existance
			Logger.warning(this, "backboneAnnounceURI is needed in order to inform the master bridge about our existance");
		} else { //announce the descriptor in the uri

			//vmon: I don't think we need this we just can simply insert our descriptor in the given URI.
			/* Schedule a thread in order to Send a Freemail to the bridge node with the own node reference.
			 * First attempt will be in a minute from plugin initialization, and if it fails, there will be
			 * other attempts every 2 minutes till the Freemail is sent. For every failed attempt, we keep
			 * an error-level entry in the log.
			 */
			//scheduledExecutorService = Executors.newScheduledThreadPool(1);
			//scheduleSend = scheduledExecutorService.scheduleWithFixedDelay(new RefSender(), 2, 1, TimeUnit.MINUTES);
			try {
				announceDescriptor(initConfig.getProperty("backboneAnnounceURI"));
				Logger.normal(this, "Successfully annouced our node reference to the bridges.");
			} catch(InsertException e) {
				Logger.warning(this, "failed to announce our descriptor to the CENO Bridge(s): "+ e.getMessage());
			}

		}
		//store the node descriptor for later use
		try {
			nodeRefHelper.writeOwnRef();
		} catch (IOException e) {
			Logger.error(this, "IO Exception while storing own reference resource file");
		}

	}

	/**
	 * Adds the node references in the resources
	 * as friends to the node this plugin is loaded.
	 * 
	 * @return the corresponding PeerAdditionReturnCode
	 * indicating whether the bridges were added successfully
	 * as friends
	 */
	private PeerAdditionReturnCodes addFriendBridges() {
		List<SimpleFieldSet> bridgeNodeFSList;
		try {
			bridgeNodeFSList = nodeRefHelper.readBridgeRefs();
		} catch (IOException e) {
			Logger.error(this, "IO Exception while parsing bridge reference resource file");
			return PeerAdditionReturnCodes.INTERNAL_ERROR;
		}
		//For now we are panincing if even we fail to add one of the bridges
		PeerNode pn;
		for(SimpleFieldSet bridgeNodeFS :  bridgeNodeFSList) {
			try {
				pn = node.createNewDarknetNode(bridgeNodeFS, FRIEND_TRUST.HIGH, FRIEND_VISIBILITY.NO);
				((DarknetPeerNode)pn).setPrivateDarknetCommentNote("CeNo Bridge");
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

			//if(Arrays.equals(pn.getPubKeyHash(), node.getDarknetPubKeyHash())) {
			if(Arrays.equals(pn.getPubKeyHash(), node.getDarknetPubKeyHash())) { //fred-next version
				Logger.warning(this, "The bridge  node reference file belongs to this node.");
				//return PeerAdditionReturnCodes.TRY_TO_ADD_SELF;
			}
			if(!node.addPeerConnection(pn)) {
				Logger.warning(this, "The bridge node is already be friended.");
				//return PeerAdditionReturnCodes.ALREADY_IN_REFERENCE;
			}
		}
		return PeerAdditionReturnCodes.OK;
	}

	public String getVersion() {
		return VERSION.getVersion();
	}

	public long getRealVersion() {
		return VERSION.getRealVersion();
	}

	public void terminate() {
		if (scheduledExecutorService != null) {
			scheduledExecutorService.shutdownNow();
		}
	}

	/**
	 * inserts our node descriptor into the given URI
	 *
	 * @param backboneAnnounceURI the URI we are supposed to announce our descriptor in
	 *
	 * @throws InsertException in case insertion fails
	 */
	public void announceDescriptor(String backboneAnnounceURI) throws InsertException {
		//we read the ssk we are supposed to insert our node in from the Bridge.properties
		//Now Inserting our node descriptor into the SSK, shouldn't this be encrypted?
		//Maybe not as we are not disclosing the insertion URI
		try {
			FreenetURI insertURIconfig = new FreenetURI(backboneAnnounceURI);
			FreenetURI announcementURI = new FreenetURI("USK", ANNOUNCER_PATH, insertURIconfig.getRoutingKey(), insertURIconfig.getCryptoKey(), insertURIconfig.getExtra());

			Logger.normal(this, "Inserting announcement freesite with USK: " + announcementURI.toString());
			nodeInterface.insertSingleChunk(announcementURI, nodeRefHelper.getNodeRef(), nodeInterface.getVoidPutCallback(
					"Successfully inserted our node descriptor with URI: " + announcementURI, ""));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (PersistenceDisabledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
