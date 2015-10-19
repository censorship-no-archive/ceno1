package plugins.CENO.Bridge.Signaling;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.RSAKeyParameters;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Bridge.CENOBridge;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.support.Logger;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;
import freenet.support.io.ResumeFailedException;

public class ChannelMaker {
	private String bridgeInsertURI;
	private AsymmetricCipherKeyPair asymKeyPair;
	ChannelMakerListener channelListener;

	static final long KSK_POLLING_PAUSE = TimeUnit.MINUTES.toMillis(12);

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
			FreenetURI announcementURI = new FreenetURI("USK", CENOBridge.announcerPath, insertURIconfig.getRoutingKey(), insertURIconfig.getCryptoKey(), insertURIconfig.getExtra());

			Logger.normal(ChannelMaker.class, "Inserting announcement freesite with USK: " + announcementURI.toASCIIString());
			CENOBridge.nodeInterface.insertFreesite(announcementURI, "default.html", sfs.toOrderedString(), new AnnouncementInsertionCB());
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
		private long lastHandledReq = 0L;

		private volatile boolean continueLoop;

		public ChannelMakerListener(String puzzleAnswer) throws MalformedURLException {
			this.puzzleAnswer = puzzleAnswer;
			channelMakingKSK = new FreenetURI("KSK@" + puzzleAnswer);
			continueLoop = true;
		}

		@Override
		public void run() {
			try {
				while(continueLoop) {
					// TODO Poll "KSK@puzzleAnswer" and discover insertion SSKs by clients
					FetchResult kskContent = null;
					try {
						kskContent = CENOBridge.nodeInterface.fetchURI(channelMakingKSK);
					} catch (FetchException e) {
						// TODO Fine-grain log messages according to FetchException codes
						Logger.warning(ChannelMakerListener.class, "Exception while fetching KSK clients use for making channels");
					}

					if (kskContent != null && kskContent.getMimeType() == "text/html") {
						try {
							Crypto.decryptMessage(kskContent.asByteArray(), (RSAKeyParameters) asymKeyPair.getPrivate());
						} catch (InvalidCipherTextException e) {
							Logger.warning(ChannelMakerListener.class, "Could not get byte array from users' KSK response");
						} catch (Exception e) {
							Logger.warning(ChannelMakerListener.class, "Error while decrypting users' KSK response: " + e.getMessage());
						}
						SimpleFieldSet sfs = new SimpleFieldSet(kskContent.toString(), false, true, true);
						Long reqID = sfs.getLong("id", -1L);
						if (reqID > 0 && reqID != lastHandledReq) {
							Logger.normal(ChannelMakerListener.class, "A client has posted information for establishing a signaling channel with ID: " + reqID);
							ChannelManager.getInstance().addChannel(sfs);
							lastHandledReq = reqID;
						}
					}
					// Pause the looping thread
					Thread.sleep(KSK_POLLING_PAUSE);
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

	class AnnouncementInsertionCB implements ClientPutCallback {
		
		private FreenetURI uri;

		public AnnouncementInsertionCB() {
		}

		@Override
		public void onResume(ClientContext context) throws ResumeFailedException {
		}

		@Override
		public RequestClient getRequestClient() {
			return CENOBridge.nodeInterface.getRequestClient();
		}

		@Override
		public void onGeneratedURI(FreenetURI uri, BaseClientPutter state) {
			this.uri = uri;
		}

		@Override
		public void onGeneratedMetadata(Bucket metadata, BaseClientPutter state) {
		}

		@Override
		public void onFetchable(BaseClientPutter state) {
		}

		@Override
		public void onSuccess(BaseClientPutter state) {
			Logger.normal(ChannelMaker.class, "Successfully inserted Channel Maker Announcer page with URI: " + uri);
		}

		@Override
		public void onFailure(InsertException e, BaseClientPutter state) {			
		}

	}
}
