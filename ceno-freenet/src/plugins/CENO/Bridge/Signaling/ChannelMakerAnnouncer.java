package plugins.CENO.Bridge.Signaling;

import java.io.IOException;

import plugins.CENO.Bridge.CENOBridge;
import plugins.CENO.Bridge.Signaling.PollingPuzzle.Puzzle;
import freenet.client.InsertException;
import freenet.client.async.PersistenceDisabledException;
import freenet.keys.FreenetURI;
import freenet.support.Logger;
import freenet.support.SimpleFieldSet;

public class ChannelMakerAnnouncer {
	private String bridgeInsertURI;
	private String pubAsymKey;
	private Puzzle puzzle;

	public ChannelMakerAnnouncer(String bridgeInsertURI, String pubAsymKey, Puzzle puzzle) {
		this.bridgeInsertURI = bridgeInsertURI;
		this.pubAsymKey = pubAsymKey;
		this.puzzle = puzzle;
	}

	public void doAnnounce() throws IOException, InsertException {
		SimpleFieldSet sfs = new SimpleFieldSet(false, true);
		sfs.putOverwrite("pubkey", pubAsymKey);
		sfs.putOverwrite("question", puzzle.getQuestion());

		FreenetURI insertURIconfig = new FreenetURI(bridgeInsertURI);
		FreenetURI announcementURI = new FreenetURI("USK", CENOBridge.ANNOUNCER_PATH, insertURIconfig.getRoutingKey(), insertURIconfig.getCryptoKey(), insertURIconfig.getExtra());

		Logger.normal(ChannelMaker.class, "Inserting announcement freesite with USK: " + announcementURI.toString());
		try {
			CENOBridge.nodeInterface.insertSingleChunk(announcementURI, sfs.toOrderedString(), CENOBridge.nodeInterface.getVoidPutCallback(
					"Successfully inserted Channel Maker Announcer page with URI: " + announcementURI, ""));
		} catch (PersistenceDisabledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}