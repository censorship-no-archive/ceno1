package plugins.CENO.Bridge.Signaling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Bridge.CENOBridge;
import plugins.CENO.Bridge.BridgeDatabase;
import plugins.CENO.Bridge.SQLiteJDBC;
import plugins.CENO.Common.Crypto;
import plugins.CENO.Common.GetSyncCallback;
import freenet.client.FetchException;
import freenet.client.FetchException.FetchExceptionMode;
import freenet.client.InsertException;
import freenet.client.async.PersistenceDisabledException;
import freenet.keys.FreenetURI;
import freenet.support.IllegalBase64Exception;
import freenet.support.Logger;
import freenet.support.SimpleFieldSet;

public class ChannelMaker {
	private String bridgeInsertURI;
	private KeyPair asymKeyPair;
    BridgeDatabase bridgeDatabase;
	ChannelMakerListener channelListener;

	static final long KSK_POLLING_PAUSE = TimeUnit.MINUTES.toMillis(5);
	static final int MAX_KSK_POLLS = 20;

	public ChannelMaker(String insertURI, KeyPair asymKeyPair, BridgeDatabase bridgeDatabase) throws CENOException {
		this.bridgeInsertURI = insertURI;
		this.asymKeyPair = asymKeyPair;
        this.bridgeDatabase = bridgeDatabase;

		Puzzle puzzle = new Puzzle();
		try {
			ChannelMakerAnnouncer channelAnnouncer = new ChannelMakerAnnouncer(bridgeInsertURI, asymKeyPair.getPublic(), puzzle.getQuestion());
			channelAnnouncer.doAnnounce();
		} catch (IOException e) {
			throw new CENOException(CENOErrCode.RR, "IOException in channel announcer page creation");
		} catch (InsertException e) {
			throw new CENOException(CENOErrCode.RR, "Could not insert channel announcer page");
		}

        //For test reason only cause real test takes 20 min comment for real life
        //bridgeDatabase.storeChannel("SSK@TcKLMqVPTtqeOXhDbGBXLbdQj4wkfUN040YmAdDdzyk,5QZI7cLj4nstrpR~wYiIlKsptRX5fQG6plv5y7bPCy8,AQECAAE", null);
        //Add the channels currently stored in the database
        ChannelManager.getInstance().addChannels(bridgeDatabase.retrieveChannels());

		try {
			for (int i = 0; i < MAX_KSK_POLLS; i++) {
				channelListener = new ChannelMakerListener(puzzle.getAnswer(), i);
				Thread listenerThread = new Thread(channelListener);
				listenerThread.setName("ChannelListener-" + i);
				listenerThread.start();
			}
		} catch (MalformedURLException e) {
			throw new CENOException(CENOErrCode.RR, "Could not start Channel Maker Listener thread.");
		}
		//TODO Once >5 of slots are taken, start a new thread/do a new announcement
	}

	private class ChannelMakerAnnouncer {
		private String bridgeInsertURI;
		private PublicKey pubAsymKey;
		private String puzzleQuestion;


		public ChannelMakerAnnouncer(String bridgeInsertURI, PublicKey pubAsymKey, String puzzleQuestion) {
			this.bridgeInsertURI = bridgeInsertURI;
			this.pubAsymKey = pubAsymKey;
			this.puzzleQuestion = puzzleQuestion;
		}

		public void doAnnounce() throws IOException, InsertException {
			SimpleFieldSet sfs = new SimpleFieldSet(false, true);
			try {
				sfs.putOverwrite("pubkey", Crypto.savePublicKey(pubAsymKey));
			} catch (GeneralSecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
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
		private int subKsk;

		private volatile boolean continueLoop;

		public ChannelMakerListener(String puzzleAnswer, int subKsk) throws MalformedURLException {
			this.puzzleAnswer = puzzleAnswer;
			this.subKsk = subKsk;
			channelMakingKSK = new FreenetURI("KSK@" + this.puzzleAnswer);
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
						CENOBridge.nodeInterface.distFetchURI(new FreenetURI(channelMakingKSK.toString() + "-" + subKsk), getSyncCallback);
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
							decKskContent = new String(Crypto.decrypt(kskContent, Crypto.savePrivateKey(asymKeyPair.getPrivate())), "UTF-8");
						} catch (GeneralSecurityException e) {
							Logger.error(this, "General Security Exception while decrypting KSK reply from client");
							continue;
						} catch (IllegalBase64Exception e) {
							// Unlikely to happen
							Logger.error(this, "Could not decode base64 key");
							continue;
						}

						SimpleFieldSet sfs = new SimpleFieldSet(decKskContent, false, true, true);
						int reqID = sfs.getInt("id", -1);
						if (reqID > 0 && reqID != lastHandledReq) {
							Logger.normal(ChannelMakerListener.class, "A client has posted information for establishing a signaling channel with ID: " + reqID);
							ChannelManager.getInstance().addChannel(sfs);
							lastHandledReq = reqID;

                            try {
                                bridgeDatabase.storeChannel(decKskContent, null);
                            } catch (Exception e) {
                                Logger.error(this, "unable to store new channel in the database");
                                continue;
                                
                            }

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
