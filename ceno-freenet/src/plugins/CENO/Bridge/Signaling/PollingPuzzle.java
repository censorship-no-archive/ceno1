package plugins.CENO.Bridge.Signaling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Bridge.CENOBridge;
import plugins.CENO.Common.Crypto;
import plugins.CENO.Common.GetSyncCallback;
import freenet.client.FetchException;
import freenet.client.InsertException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.keys.FreenetURI;
import freenet.support.Logger;

public class PollingPuzzle {
	static final int MAX_KSK_SLOTS = 20;

	private Puzzle puzzle;
	private String bridgeInsertURI;
	private KeyPair asymKeyPair;
	private int consumedSlots = 0;

	private ChannelMakerListener[] channelListeners = new ChannelMakerListener[MAX_KSK_SLOTS];

	public PollingPuzzle( String bridgeInsertURI, KeyPair keyPair) {
		this.bridgeInsertURI = bridgeInsertURI;
		this.asymKeyPair = keyPair;
		this.puzzle = new Puzzle();
	}

	public void startPolling() throws IOException, InsertException, CENOException, GeneralSecurityException {
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

	private void shouldOfferNewPuzzle() {
		if (consumedSlots > MAX_KSK_SLOTS * 0.5) {
			try {
				ChannelMaker.getInstance().publishNewPuzzle();
			} catch (IOException e) {
				Logger.error(this, "Could not start channel listener for the given insertURI: " + e.getMessage());
				return;
			} catch (GeneralSecurityException e) {
				Logger.error(this, "The given public RSA key is invalid");
				return;
			} catch (CENOException e) {
				Logger.error(this, "Could not start decentralized signaling channel maker: " + e.getMessage());
				return;
			}
		}
	}

	private class ChannelMakerListener implements Runnable {	
		final long KSK_POLLING_PAUSE = TimeUnit.MINUTES.toMillis(5);

		private String puzzleAnswer;
		private FreenetURI channelMakingKSK;
		private PrivateKey asymPrivKey;

		private volatile boolean continueLoop;

		public ChannelMakerListener(String puzzleAnswer, int subKsk, PrivateKey asymPrivKey) throws MalformedURLException {
			this.puzzleAnswer = puzzleAnswer;
			this.asymPrivKey = asymPrivKey;
			channelMakingKSK = new FreenetURI("KSK@" + this.puzzleAnswer + "-" + subKsk);
			continueLoop = true;
		}

		@Override
		public void run() {
			try {
				int window = 0;
				while(continueLoop) {
					byte[] kskContent = null;
					GetSyncCallback getSyncCallback = new GetSyncCallback(CENOBridge.nodeInterface.getRequestClient());
					try {
						CENOBridge.nodeInterface.distFetchURI(channelMakingKSK, getSyncCallback);
						kskContent = getSyncCallback.getResult(10 * KSK_POLLING_PAUSE, TimeUnit.MILLISECONDS);
					} catch (FetchException e) {
						// TODO Fine-grain log messages according to FetchException codes
						if(e.mode == FetchExceptionMode.RECENTLY_FAILED) {
							window++;
						}
						Logger.normal(ChannelMakerListener.class, "Exception while fetching KSK clients use for making channels: " + e.getShortMessage());
					}

					if (kskContent != null) {
						if (window > 0) {
							window--;
						}
						Logger.minor(this, "Congestion window for fetching KSKs without getting Recently Failed exceptions set to: " + window + " minutes");

						String decKskContent = null;
						try {
							decKskContent = new String(Crypto.decrypt(kskContent, asymPrivKey), "UTF-8");
						} catch (GeneralSecurityException e) {
							Logger.error(this, "General Security Exception while decrypting KSK reply from client");
							continue;
						}

						Logger.normal(ChannelMakerListener.class, "A client has posted information for establishing a signaling channel");
						ChannelManager.getInstance().addChannel(decKskContent);

						consumedSlots++;
						shouldOfferNewPuzzle();

						continueLoop = false;
					}

					// Pause the looping thread
					Thread.sleep(KSK_POLLING_PAUSE + TimeUnit.MINUTES.toMillis(window));
				}
			} catch (InterruptedException e) {
				continueLoop = false;
			} catch (IOException e) {
				Logger.warning(this, "IOException while trying to create channel from KSK response");
				continueLoop = false;
			}
		}

		public void stopListener() {
			continueLoop = false;
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