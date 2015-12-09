package plugins.CENO.Bridge.Signaling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Common.Crypto;
import freenet.client.InsertException;

public class ChannelMaker {
	static final int MAX_POLLING_PUZZLES = 3;
	static final int MAX_KSK_SLOTS = 20;

	private String bridgeInsertURI;
	private KeyPair asymKeyPair;
	private ChannelMakerListener[] channelListeners = new ChannelMakerListener[MAX_KSK_SLOTS];

	public ChannelMaker(String insertURI, KeyPair asymKeyPair) throws CENOException, IOException, InsertException, GeneralSecurityException {
		this.bridgeInsertURI = insertURI;
		this.asymKeyPair = asymKeyPair;
	}

	public void publishNewPuzzle() throws IOException, InsertException, CENOException, GeneralSecurityException {
		Puzzle puzzle = new Puzzle();

		ChannelMakerAnnouncer channelMakerAnnouncer = new ChannelMakerAnnouncer(bridgeInsertURI, Crypto.savePublicKey(asymKeyPair.getPublic()), puzzle);
		channelMakerAnnouncer.doAnnounce();

		startSlotListeners(puzzle.getAnswer());
	}

	private void startSlotListeners(String puzzleAnswer) throws CENOException {
		try {
			for (int i = 0; i < MAX_KSK_SLOTS; i++) {
				channelListeners[i] = new ChannelMakerListener(puzzleAnswer, i, asymKeyPair.getPrivate());
				Thread listenerThread = new Thread(channelListeners[i]);
				listenerThread.setName("ChannelListener-" + i);
				listenerThread.start();
			}
		} catch (MalformedURLException e) {
			throw new CENOException(CENOErrCode.RR, "Could not start Channel Maker Listener thread.");
		}
	}

	public void stopListeners() {
		for (ChannelMakerListener channelLister : channelListeners) {
			channelLister.stopListener();
		}
	} 

	class Puzzle {
		private String question;
		private String answer;

		public Puzzle() {
			this.question = Long.toHexString(Double.doubleToLongBits(Math.random()));
			this.answer = this.question;
		}

		public String getQuestion() {
			return question;
		}

		public String getAnswer() {
			return answer;
		}
	}

}
