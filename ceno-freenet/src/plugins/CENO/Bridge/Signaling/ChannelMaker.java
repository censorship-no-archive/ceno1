package plugins.CENO.Bridge.Signaling;

import java.io.IOException;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Bridge.CENOBridge;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.keys.FreenetURI;
import freenet.node.RequestClient;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;
import freenet.support.io.ResumeFailedException;

public class ChannelMaker {
	private String bridgeInsertURI;
	private String privateRSA;
	private String publicRSA;
	
	public ChannelMaker(String insertURI, String[] RSA) throws CENOException {
		this.bridgeInsertURI = insertURI;
		this.privateRSA = RSA[0];
		this.publicRSA = RSA[1];
		
		try {
		ChannelAnnouncer channelAnnouncer = new ChannelAnnouncer(bridgeInsertURI, publicRSA, generatePuzzle().getQuestion());
		channelAnnouncer.doAnnounce();
		} catch (IOException e) {
			throw new CENOException(CENOErrCode.RR, "IOException in announcement channel creation");
		} catch (InsertException e) {
			throw new CENOException(CENOErrCode.RR, "Could not insert announcement channel");
		}
	}
	
	private Puzzle generatePuzzle() {
		Puzzle puzzle = new Puzzle();
		return puzzle;
	}
	
	private class ChannelAnnouncer {
		private String bridgeInsertURI;
		private String publicRSA;
		private String puzzleQuestion;


		public ChannelAnnouncer(String bridgeInsertURI, String publicRSA, String puzzleQuestion) {
			this.bridgeInsertURI = bridgeInsertURI;
			this.publicRSA = publicRSA;
			this.puzzleQuestion = puzzleQuestion;
		}
		
		public void doAnnounce() throws IOException, InsertException {
			SimpleFieldSet sfs = new SimpleFieldSet(false, true);
			sfs.putOverwrite("publicRSA", publicRSA);
			sfs.putOverwrite("question", puzzleQuestion);
			
			FreenetURI insertURIconfig = new FreenetURI(bridgeInsertURI);
			FreenetURI announcementURI = new FreenetURI("USK", CENOBridge.announcerPath, insertURIconfig.getRoutingKey(), insertURIconfig.getCryptoKey(), insertURIconfig.getExtra());
			
			CENOBridge.nodeInterface.insertFreesite(announcementURI, "default.html", sfs.toOrderedString(), new AnnouncementInsertionCB());
		}
	}
	
	private class ChannelListener {
		private String puzzleAnswer;
		
		public ChannelListener(String puzzleAnswer) {
			// TODO Subscribe to "KSK@puzzleAnswer" and discover insertion SSKs by clients
		}
		
	}
	
	private class Puzzle {
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
	
	private class AnnouncementInsertionCB implements ClientPutCallback {

		@Override
		public void onResume(ClientContext context) throws ResumeFailedException {
		}

		@Override
		public RequestClient getRequestClient() {
			return CENOBridge.nodeInterface.getRequestClient();
		}

		@Override
		public void onGeneratedURI(FreenetURI uri, BaseClientPutter state) {
		}

		@Override
		public void onGeneratedMetadata(Bucket metadata, BaseClientPutter state) {
		}

		@Override
		public void onFetchable(BaseClientPutter state) {
		}

		@Override
		public void onSuccess(BaseClientPutter state) {
		}

		@Override
		public void onFailure(InsertException e, BaseClientPutter state) {			
		}
		
	}
}
