package plugins.CENO.Bridge.Signaling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.RSAKeyParameters;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Bridge.CENOBridge;
import plugins.CENO.Common.GetSyncCallback;
import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.client.InsertException;
import freenet.client.async.PersistenceDisabledException;
import freenet.keys.FreenetURI;
import freenet.support.Logger;
import freenet.support.SimpleFieldSet;

public class ChannelMaker {
	private String bridgeInsertURI;
	private AsymmetricCipherKeyPair asymKeyPair;
	ChannelMakerListener channelListener;

	static final long KSK_POLLING_PAUSE = TimeUnit.MINUTES.toMillis(5);

	public ChannelMaker(String insertURI, AsymmetricCipherKeyPair asymKeyPair) throws CENOException {
		this.bridgeInsertURI = insertURI;
		this.asymKeyPair = asymKeyPair;

		Puzzle puzzle = new Puzzle();
		try {
			ChannelMakerAnnouncer channelAnnouncer = new ChannelMakerAnnouncer(bridgeInsertURI, (RSAKeyParameters) asymKeyPair.getPublic(), puzzle.getQuestion());
			channelAnnouncer.doAnnounce();
		} catch (IOException e) {
			throw new CENOException(CENOErrCode.RR, "IOException in channel announcer page creation");
		} catch (InsertException e) {
			throw new CENOException(CENOErrCode.RR, "Could not insert channel announcer page");
		}

		try {
			channelListener = new ChannelMakerListener(puzzle.getAnswer());
			Thread listenerThread = new Thread(channelListener);
			listenerThread.setName("ChannelListener");
			listenerThread.start();
		} catch (MalformedURLException e) {
			throw new CENOException(CENOErrCode.RR, "Could not start Channel Maker Listener thread.");
		}
	}

	private class ChannelMakerAnnouncer {
		private String bridgeInsertURI;
		private RSAKeyParameters pubAsymKey;
		private String puzzleQuestion;


		public ChannelMakerAnnouncer(String bridgeInsertURI, RSAKeyParameters pubAsymKey, String puzzleQuestion) {
			this.bridgeInsertURI = bridgeInsertURI;
			this.pubAsymKey = pubAsymKey;
			this.puzzleQuestion = puzzleQuestion;
		}

		public void doAnnounce() throws IOException, InsertException {
			SimpleFieldSet sfs = new SimpleFieldSet(false, true);
			sfs.putOverwrite("asymkey.modulus", pubAsymKey.getModulus().toString(32));
			sfs.putOverwrite("asymkey.pubexponent", pubAsymKey.getExponent().toString(32));
			sfs.putOverwrite("question", puzzleQuestion);

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

	public void stopListener() {
		if (channelListener != null) {
			channelListener.continueLoop = false;
		}
	}

	private class ChannelMakerListener implements Runnable {
		private String puzzleAnswer;
		private FreenetURI channelMakingKSK;
		private int lastHandledReq = 0;

		private volatile boolean continueLoop;

		public ChannelMakerListener(String puzzleAnswer) throws MalformedURLException {
			this.puzzleAnswer = puzzleAnswer;
			channelMakingKSK = new FreenetURI("KSK@" + puzzleAnswer);
			continueLoop = true;
		}

		@Override
		public void run() {
			try {
				int window = 0;
				while(continueLoop) {
					// TODO Poll "KSK@puzzleAnswer" and discover insertion SSKs by clients
					String kskContent = null;
					GetSyncCallback getSyncCallback = new GetSyncCallback(CENOBridge.nodeInterface.getRequestClient());
					try {
						CENOBridge.nodeInterface.distFetchURI(channelMakingKSK, getSyncCallback);
						kskContent = getSyncCallback.getResult(10 * KSK_POLLING_PAUSE, TimeUnit.MILLISECONDS);
					} catch (FetchException e) {
						// TODO Fine-grain log messages according to FetchException codes
						if(e.mode == FetchExceptionMode.RECENTLY_FAILED) {
							window++;
						}
						Logger.minor(ChannelMakerListener.class, "Exception while fetching KSK clients use for making channels: " + e.getShortMessage());
					}

					if (kskContent != null) {
						window--;
						Logger.minor(this, "Congestion window for fetching KSKs without getting Recently Failed exceptions set to: " + window + " minutes");
						/*
						try {
							Crypto.decryptMessage(kskContent.asByteArray(), (RSAKeyParameters) asymKeyPair.getPrivate());
						} catch (InvalidCipherTextException e) {
							Logger.warning(ChannelMakerListener.class, "Could not get byte array from users' KSK response");
						} catch (Exception e) {
							Logger.warning(ChannelMakerListener.class, "Error while decrypting users' KSK response: " + e.getMessage());
						}
						 */
						SimpleFieldSet sfs = new SimpleFieldSet(kskContent, false, true, true);
						int reqID = sfs.getInt("id", -1);
						if (reqID > 0 && reqID != lastHandledReq) {
							Logger.normal(ChannelMakerListener.class, "A client has posted information for establishing a signaling channel with ID: " + reqID);
							ChannelManager.getInstance().addChannel(sfs);
							lastHandledReq = reqID;
						}
					}
					// Pause the looping thread
					Thread.sleep(KSK_POLLING_PAUSE + TimeUnit.MINUTES.toMillis(window));
				}
			} catch (InterruptedException e) {
				continueLoop = false;
			} catch (IOException e) {
				// TODO Log this
			}
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
