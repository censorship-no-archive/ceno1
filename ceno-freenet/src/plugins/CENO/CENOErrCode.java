package plugins.CENO;

/**
 * CENO specific error codes.
 * They are defined in errorConditions under doc and is the way
 * to exchange error messages with the other agents.
 */
public enum CENOErrCode {
	//////////////////////
	// LCS Agent Errors //
	//////////////////////

	// LCS Agent internal errors
	LCS_HANDLER_URL(2110, CENODocCode.LCS_MALFORMED_URL, ErrAction.LOG, "Error while processing the URL"),
	LCS_HANDLER_URL_MALFORMED(2111, CENODocCode.LCS_MALFORMED_URL, ErrAction.LOG, "Malformed request URL"),
	LCS_HANDLER_URL_DECODE(2112, CENODocCode.LCS_URL_DECODE_ERR, ErrAction.LOG, "Could not decode URL value"),
	LCS_HANDLER_URL_INVALID(2113, CENODocCode.LCS_WILL_NOT_SERVE, ErrAction.LOG, "Requested URL is invalid"),
	LCS_HANDLER_URL_TO_USK(2114, CENODocCode.LCS_MALFORMED_URL, ErrAction.LOG, "Could not translate URL to a Freenet key"),
	LCS_HANDLER_URL_WONT_SERVE(2115, CENODocCode.LCS_WILL_NOT_SERVE, ErrAction.LOG, "Requested URL is pointing to a resource LCS won't serve"),

	LCS_HANDLER_STATIC(2120, CENODocCode.LCS_INTERNAL, ErrAction.LOG, "Error while serving static file"),
	LCS_HANDLER_STATIC_NOT_FOUND(2121, CENODocCode.LCS_INTERNAL, ErrAction.LOG, "Static file to serve was not found"),
	LCS_HANDLER_STATIC_IO(2122, CENODocCode.LCS_INTERNAL, ErrAction.LOG, "Could not read static file to serve"),

	LCS_LOOKUP_LOCAL(2130, CENODocCode.LCS_LOOKUP_FAILURE, ErrAction.LOG, "Lookup in the local cache failed"),
	LCS_LOOKUP_ULPR(2140, CENODocCode.LCS_LOOKUP_FAILURE, ErrAction.LOG, "Lookup in the distributed cache failed"),
	LCS_LOOKUP_ULPR_INIT(2141, CENODocCode.LCS_LOOKUP_FAILURE, ErrAction.RETRY, "Could not initiate a Passive Request in the distributed cache"),
	LCS_LOOKUP_ULPR_FAILED(2142, CENODocCode.LCS_LOOKUP_FAILURE, ErrAction.LOG, "Passive request in the distributed cache failed"),

	// LCS inter-agent errors
	LCS_CC(2210, CENODocCode.HANDLE_INTERNALLY, ErrAction.LOG, "Error in communication with the CC agent"),
	LCS_CC_COMPOSE(2211, CENODocCode.LCS_INTERNAL, ErrAction.RETRY, "Could not compose response for CC"),
	LCS_CC_REACH(2212, CENODocCode.HANDLE_INTERNALLY, ErrAction.RETRY, "Could not reach the CC agent"),
	LCS_CC_RESPOND(2213, CENODocCode.HANDLE_INTERNALLY, ErrAction.LOG, "Could not respond to CC"),

	// Freenet node errors
	LCS_NODE(2300, CENODocCode.LCS_FREENET_NODE_NOT_READY, ErrAction.LOG, "Freenet node error"),
	LCS_NODE_INITIALIZING(2301, CENODocCode.LCS_FREENET_NODE_NOT_READY, ErrAction.WAIT, "Freenet node Initializing"),
	LCS_NODE_NOT_ENOUGH_PEERS(2302, CENODocCode.LCS_FREENET_COULD_NOT_CONNECT_TO_PEERS, ErrAction.WAIT, "Freenet node not connected to enough peers"),

	/////////////////////
	// RS Agent Errors //
	/////////////////////

	// RS Agent internal errors
	RS_URL(3100, CENODocCode.RS_MALFORMED_URL, ErrAction.LOG, "Error while processing the URL"),
	RS_URL_MALFORMED(3111, CENODocCode.RS_MALFORMED_URL, ErrAction.LOG, "Malformed request URL"),
	RS_URL_DECODE(3112, CENODocCode.RS_URL_DECODE_ERR, ErrAction.LOG, "Could not decode URL value"),
	RS_HANDLER_URL_INVALID(3113, CENODocCode.RS_WILL_NOT_SERVE, ErrAction.LOG, "Requested URL is invalid"),
	RS_URL_TO_USK(3114, CENODocCode.RS_MALFORMED_URL, ErrAction.LOG, "Could not translate URL to a Freenet key"),
	RS_URL_WONT_SERVE(3115, CENODocCode.RS_WILL_NOT_SERVE, ErrAction.LOG, "Requested URL is pointing to a resource RS won't request"),

	// RS WoT errors
	RS_WOT(3310, CENODocCode.RS_WOT_ERR, ErrAction.LOG, "WebOfTrust Freenet plugin error"),
	RS_WOT_NOT_LOADED(3311, CENODocCode.RS_WOT_ERR, ErrAction.WAIT, "WoT Freenet plugin is not loaded"),
	RS_WOT_NOT_RESPONDING(3312, CENODocCode.RS_WOT_ERR, ErrAction.RETRY, "WoT Freeent plugin not responding"),
	RS_WOT_DOWNLOADING(3313, CENODocCode.RS_WOT_ERR, ErrAction.WAIT, "WoT Freenet plugin is being downloaded"),
	RS_WOT_INIT(3314, CENODocCode.RS_WOT_ERR, ErrAction.WAIT, "WoT Freenet plugin is initializing"),

	RS_WOT_IDENT(3320, CENODocCode.RS_WOT_ERR, ErrAction.LOG, "Error with a WebOfTrust identity"),
	RS_WOT_IDENT_NOT_AVAIL(3321, CENODocCode.RS_WOT_ERR, ErrAction.LOG, "WoT identity is not available"),
	RS_WOT_IDENT_DOWNLOADING(3322, CENODocCode.RS_WOT_ERR, ErrAction.WAIT, "WoT identity is being inserted"),
	RS_WOT_IDENT_INSERTION_FAILED(3323, CENODocCode.RS_WOT_ERR, ErrAction.RETRY, "WoT identity insertion failed"),

	// RS Freemail errors
	RS_FREEMAIL(3330, CENODocCode.RS_FREEMAIL_ERR, ErrAction.LOG, "Freemail Freenet plugin error"),
	RS_FREEMAIL_NOT_LOADED(3331, CENODocCode.RS_FREEMAIL_ERR, ErrAction.WAIT, "Freemail plugin is not loaded"),
	RS_FREEMAIL_NOT_RESPONDING(3332, CENODocCode.RS_FREEMAIL_ERR, ErrAction.RETRY, "Freemail plugin is not responding"),
	RS_FREEMAIL_DOWNLOADING(3333, CENODocCode.RS_FREEMAIL_ERR, ErrAction.WAIT, "Freemail plugin is being downloaded"),
	RS_FREEMAIL_INIT(3334, CENODocCode.RS_FREEMAIL_ERR, ErrAction.WAIT, "Freemail plugin is initializing"),
	RS_FREEMAIL_NOT_WOT(3335, CENODocCode.RS_FREEMAIL_ERR, ErrAction.LOG, "Freemail plugin is loaded, but WoT is not loaded"),

	RS_FREEMAIL_ACC(3340, CENODocCode.RS_FREEMAIL_ERR, ErrAction.LOG, "Error with a Freemail account"),
	RS_FREEMAIL_ACC_SETUP(3341, CENODocCode.RS_FREEMAIL_ERR, ErrAction.RETRY, "Could not set up Freemail account"),
	RS_FREEMAIL_ACC_CONNECT(3342, CENODocCode.RS_FREEMAIL_ERR, ErrAction.RETRY, "Could not connect to Freemail account"),
	RS_FREEMAIL_ACC_CONNECT_NOPROPS(3343, CENODocCode.RS_FREEMAIL_ERR, ErrAction.LOG, "Acc props for Freemail account were not found"),

	// RS SMTP/IMAP errors
	RS_SMTP_ERR(3410, CENODocCode.RS_FREEMAIL_SMTP, ErrAction.RETRY, "Sending Freemail over SMTP failed"),
	RS_IMAP_ERR(3420, CENODocCode.HANDLE_INTERNALLY, ErrAction.RETRY, "Receiving Freemail over IMAP failed"),

	/////////////////////
	// RR Agent Errors //
	/////////////////////
	RR(4000, CENODocCode.HANDLE_INTERNALLY, ErrAction.LOG, "General Request Receiver agent error"),

	/////////////////////
	// BI Agent Errors //
	/////////////////////
	BI(6000, CENODocCode.HANDLE_INTERNALLY, ErrAction.LOG, "General Bundler Inserter agent error");

	private final int errCode;
	private final CENODocCode docCode;
	private final ErrAction errAction;
	private final String defErrMsg;

	private CENOErrCode(int errCode, CENODocCode docCode, ErrAction errAction, String errMsg) {
		this.errCode = errCode;
		this.docCode = docCode;
		this.errAction = errAction;
		//TODO Use l10n for reading localized messages from a file
		this.defErrMsg = errMsg;
	}

	/**
	 * Returns the error code
	 */
	public int getCode() {
		return errCode;
	}

	/**
	 * Returns the corresponding error code from the errorConditions document
	 * Use this method when you need to forward an error code to
	 * another agent.
	 */
	public int getDocCode() {
		return docCode.getCode();
	}
	/**
	 * Returns the appropriate action for the specific error code
	 */
	public ErrAction getAction() {
		return errAction;
	}

	/**
	 * Returns an explanatory message
	 */
	@Override
	public String toString() {
		return defErrMsg;
	}

	/*
	 * Default action for handling each errCode
	 */
	public enum ErrAction {
		TERMINATE,
		RETRY,
		WAIT,
		LOG,
		IGNORE,
	}

}
