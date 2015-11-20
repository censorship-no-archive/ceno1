package plugins.CENO.Client.Signaling;

import java.util.EnumSet;

public enum ChannelStatus {
		starting,
		fatal,
		failedToGetSignalSSK,
		gotSignalSSK,
		failedToParseSignalSFS,
		failedToSolvePuzzle,
		puzzleSolved,
		failedToEncrypt,
		failedToPublishKSK,
		publishedKSK,
		waitingForSyn,
		syn,
		failedToParseSyn;
		
	static EnumSet<ChannelStatus> fatalChannelStatus = EnumSet.of(fatal, failedToGetSignalSSK, failedToSolvePuzzle, failedToEncrypt, failedToPublishKSK);
	static EnumSet<ChannelStatus> canSend = EnumSet.of(syn);
	static EnumSet<ChannelStatus> sentPrivUSK = EnumSet.of(publishedKSK, waitingForSyn, syn);
	
	public static boolean isFatalStatus(ChannelStatus status) {
		return fatalChannelStatus.contains(status);
	}
	
	public static boolean canSend(ChannelStatus status) {
		return canSend.contains(status);
	}
	
	public static boolean sentPrivUSK(ChannelStatus status) {
		return sentPrivUSK.contains(status);
	}
	
}
