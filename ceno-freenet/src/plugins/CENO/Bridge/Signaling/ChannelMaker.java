package plugins.CENO.Bridge.Signaling;

import java.net.MalformedURLException;

import plugins.CENO.Bridge.CENOBridge;
import freenet.keys.FreenetURI;
import freenet.support.SimpleFieldSet;

public class ChannelMaker {
	private String bridgeInsertURI;
	private String privateRSA;
	private String publicRSA;
	
	public ChannelMaker(String insertURI, String[] RSA) throws MalformedURLException {
		this.bridgeInsertURI = insertURI;
		this.privateRSA = RSA[0];
		this.publicRSA = RSA[1];
		
		ChannelAnnouncer channelAnnouncer = new ChannelAnnouncer(bridgeInsertURI, publicRSA, generatePuzzle().getQuestion());
		channelAnnouncer.doAnnounce();
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
		
		public void doAnnounce() throws MalformedURLException {
			SimpleFieldSet sfs = new SimpleFieldSet(false, true);
			sfs.putOverwrite("publicRSA", publicRSA);
			sfs.putOverwrite("question", puzzleQuestion);
			
			FreenetURI insertURIconfig = new FreenetURI(bridgeInsertURI);
			FreenetURI announcementURI = new FreenetURI("USK", CENOBridge.announcerPath, insertURIconfig.getRoutingKey(), insertURIconfig.getCryptoKey(), insertURIconfig.getExtra());
			//TODO publish channel announcement freesite
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
}
