package plugins.CENO.Bridge.Signaling;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Iterator;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import plugins.CENO.CENOException;
import freenet.client.InsertException;
import freenet.support.Logger;

public class ChannelMaker {

	static final int MAX_POLLING_PUZZLES = 3;

	private String bridgeInsertURI;
	private KeyPair asymKeyPair;

	private static volatile ChannelMaker instance;

	public static ChannelMaker getInstance() {
		if (instance == null) {
			synchronized(ChannelMaker.class) {
				instance = new ChannelMaker();
			}
		}
		return instance;
	}

	private static CircularFifoQueue<PollingPuzzle> puzzleQueue = new CircularFifoQueue<PollingPuzzle>(MAX_POLLING_PUZZLES);

	public void config(String insertURI, KeyPair asymKeyPair) {
		this.bridgeInsertURI = insertURI;
		this.asymKeyPair = asymKeyPair;
	}

	public void publishNewPuzzle() throws CENOException, IOException, GeneralSecurityException {
		PollingPuzzle pollingPuzzle = new PollingPuzzle(bridgeInsertURI, asymKeyPair);
		try {
			pollingPuzzle.startPolling();
		} catch (InsertException e) {
			Logger.error(this, "InsertException while inserting signal announcement, will try again with a new puzzle: " + e.getMessage());
			publishNewPuzzle();
			return;
		}
		if (puzzleQueue.isAtFullCapacity()) {
			puzzleQueue.peek().stopListeners();
		}
		puzzleQueue.offer(pollingPuzzle);
	}

	public void stopPuzzleListeners() {
		if (puzzleQueue == null) {
			return;
		}
		Iterator<PollingPuzzle> puzzleQueueIterator = puzzleQueue.iterator();
		while (puzzleQueueIterator.hasNext()) {
			puzzleQueueIterator.next().stopListeners();
		}
		puzzleQueue.clear();
	}

}
