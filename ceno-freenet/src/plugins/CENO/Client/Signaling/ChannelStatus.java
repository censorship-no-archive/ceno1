package plugins.CENO.Client.Signaling;

import java.util.EnumSet;

public enum ChannelStatus {
		starting,
		fatal,
		gotSignalSSK,
		failedToSolvePuzzle,
		puzzleSolved,
		failedToEncrypt,
		failedToPublishKSK,
		publishedKSK,
		waitingForSyn,
		syn;
		
	static EnumSet<ChannelStatus> fatalChannelStatus = EnumSet.of(fatal, failedToSolvePuzzle, failedToEncrypt, failedToPublishKSK);
	
	public static boolean isFatalStatus(ChannelStatus status) {
		return fatalChannelStatus.contains(status);
	}
}
