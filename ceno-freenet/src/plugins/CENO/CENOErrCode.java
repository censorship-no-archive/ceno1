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
	LCS_HANDLER_URL(2110, CENODocCode.LCS_MALFORMED_URL, ErrAction.LOG),
	LCS_HANDLER_URL_MALFORMED(2111, CENODocCode.LCS_MALFORMED_URL, ErrAction.LOG),
	LCS_HANDLER_URL_DECODE(2112, CENODocCode.LCS_URL_DECODE_ERR, ErrAction.LOG),
	LCS_HANDLER_URL_INVALID(2113, CENODocCode.LCS_WILL_NOT_SERVE, ErrAction.LOG),
	LCS_HANDLER_URL_TO_USK(2114, CENODocCode.LCS_MALFORMED_URL, ErrAction.LOG),
	LCS_HANDLER_URL_WONT_SERVE(2115, CENODocCode.LCS_WILL_NOT_SERVE, ErrAction.LOG),

	LCS_HANDLER_STATIC(2120, CENODocCode.LCS_INTERNAL, ErrAction.LOG),
	LCS_HANDLER_STATIC_NOT_FOUND(2121, CENODocCode.LCS_INTERNAL, ErrAction.LOG),
	LCS_HANDLER_STATIC_IO(2122, CENODocCode.LCS_INTERNAL, ErrAction.LOG),

	LCS_LOOKUP_LOCAL(2130, CENODocCode.LCS_LOOKUP_FAILURE, ErrAction.LOG),
	LCS_LOOKUP_ULPR(2140, CENODocCode.LCS_LOOKUP_FAILURE, ErrAction.LOG),
	LCS_LOOKUP_ULPR_INIT(2141, CENODocCode.LCS_LOOKUP_FAILURE, ErrAction.RETRY),
	LCS_LOOKUP_ULPR_FAILED(2142, CENODocCode.LCS_LOOKUP_FAILURE, ErrAction.LOG),

	// LCS inter-agent errors
	LCS_CC(2210, CENODocCode.HANDLE_INTERNALLY, ErrAction.LOG),
	LCS_CC_COMPOSE(2211, CENODocCode.LCS_INTERNAL, ErrAction.RETRY),
	LCS_CC_REACH(2212, CENODocCode.HANDLE_INTERNALLY, ErrAction.RETRY),
	LCS_CC_RESPOND(2213, CENODocCode.HANDLE_INTERNALLY, ErrAction.LOG),

	// Freenet node errors
	LCS_NODE(2300, CENODocCode.LCS_FREENET_NODE_NOT_READY, ErrAction.LOG),
	LCS_NODE_INITIALIZING(2301, CENODocCode.LCS_FREENET_NODE_NOT_READY, ErrAction.WAIT),
	LCS_NODE_NOT_ENOUGH_PEERS(2302, CENODocCode.LCS_FREENET_COULD_NOT_CONNECT_TO_PEERS, ErrAction.WAIT),

	/////////////////////
	// RS Agent Errors //
	/////////////////////

	// RS Agent internal errors
	RS_URL(3100, CENODocCode.RS_MALFORMED_URL, ErrAction.LOG),
	RS_URL_MALFORMED(3111, CENODocCode.RS_MALFORMED_URL, ErrAction.LOG),
	RS_URL_DECODE(3112, CENODocCode.RS_URL_DECODE_ERR, ErrAction.LOG),
	RS_HANDLER_URL_INVALID(3113, CENODocCode.RS_WILL_NOT_SERVE, ErrAction.LOG),
	RS_URL_TO_USK(3114, CENODocCode.RS_MALFORMED_URL, ErrAction.LOG),
	RS_URL_WONT_SERVE(3115, CENODocCode.RS_WILL_NOT_SERVE, ErrAction.LOG),

	// RS WoT errors
	RS_WOT(3310, CENODocCode.RS_WOT_ERR, ErrAction.LOG),
	RS_WOT_NOT_LOADED(3311, CENODocCode.RS_WOT_ERR, ErrAction.WAIT),
	RS_WOT_NOT_RESPONDING(3312, CENODocCode.RS_WOT_ERR, ErrAction.RETRY),
	RS_WOT_DOWNLOADING(3313, CENODocCode.RS_WOT_ERR, ErrAction.WAIT),
	RS_WOT_INIT(3314, CENODocCode.RS_WOT_ERR, ErrAction.WAIT),

	RS_WOT_IDENT(3320, CENODocCode.RS_WOT_ERR, ErrAction.LOG),
	RS_WOT_IDENT_NOT_AVAIL(3321, CENODocCode.RS_WOT_ERR, ErrAction.LOG),
	RS_WOT_IDENT_DOWNLOADING(3322, CENODocCode.RS_WOT_ERR, ErrAction.WAIT),
	RS_WOT_IDENT_INSERTION_FAILED(3323, CENODocCode.RS_WOT_ERR, ErrAction.RETRY),

	// RS Freemail errors
	RS_FREEMAIL(3330, CENODocCode.RS_FREEMAIL_ERR, ErrAction.LOG),
	RS_FREEMAIL_NOT_LOADED(3331, CENODocCode.RS_FREEMAIL_ERR, ErrAction.WAIT),
	RS_FREEMAIL_NOT_RESPONDING(3332, CENODocCode.RS_FREEMAIL_ERR, ErrAction.RETRY),
	RS_FREEMAIL_DOWNLOADING(3333, CENODocCode.RS_FREEMAIL_ERR, ErrAction.WAIT),
	RS_FREEMAIL_INIT(3334, CENODocCode.RS_FREEMAIL_ERR, ErrAction.WAIT),
	RS_FREEMAIL_NOT_WOT(3335, CENODocCode.RS_FREEMAIL_ERR, ErrAction.LOG),

	RS_FREEMAIL_ACC(3340, CENODocCode.RS_FREEMAIL_ERR, ErrAction.LOG),
	RS_FREEMAIL_ACC_SETUP(3341, CENODocCode.RS_FREEMAIL_ERR, ErrAction.RETRY),
	RS_FREEMAIL_ACC_CONNECT(3342, CENODocCode.RS_FREEMAIL_ERR, ErrAction.RETRY),
	RS_FREEMAIL_ACC_CONNECT_NOPROPS(3343, CENODocCode.RS_FREEMAIL_ERR, ErrAction.LOG),

	// RS SMTP/IMAP errors
	RS_SMTP_ERR(3410, CENODocCode.RS_FREEMAIL_SMTP, ErrAction.RETRY),
	RS_IMAP_ERR(3420, CENODocCode.HANDLE_INTERNALLY, ErrAction.RETRY),

	/////////////////////
	// RR Agent Errors //
	/////////////////////
	RR(4000, CENODocCode.HANDLE_INTERNALLY, ErrAction.LOG),

	/////////////////////
	// BI Agent Errors //
	/////////////////////
	BI(6000, CENODocCode.HANDLE_INTERNALLY, ErrAction.LOG);

	private final int errCode;
	private final CENODocCode docCode;
	private final ErrAction errAction;

	private CENOErrCode(int errCode, CENODocCode docCode, ErrAction errAction) {
		this.errCode = errCode;
		this.docCode = docCode;
		this.errAction = errAction;
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
		return CENOL10n.get(name());
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
