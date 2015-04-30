package plugins.CENO;

/**
 * CENO specific error codes.
 * They are defined errorConditions under doc and is the way
 * to exchange error messages with the other agents.
 */
public enum CENOErrCode {
	LCS_NODE_INITIALIZING(100, ErrAction.WAIT),
	LCS_NODE_NOT_ENOUGH_PEERS(101, ErrAction.WAIT),
	BI_INVALID_URL(501, ErrAction.LOG);

	private final int errCode;
	private final ErrAction errAction;

	private CENOErrCode(int errCode, ErrAction errAction) {
		this.errCode = errCode;
		this.errAction = errAction;
	}

	/**
	 * Returns the error code
	 */
	public int getCode() {
		return errCode;
	}

	/**
	 * Returns the appropriate action for the specific error code
	 */
	public ErrAction getAction() {
		return errAction;
	}

	/**
	 * Returns the error code along with an explanatory message
	 */
	@Override
	public String toString() {
		//TODO Use a (l10n) properties file for keeping the messages
		return "message";
	}

	public enum ErrAction {
		TERMINATE,
		RETRY,
		LOG,
		IGNORE,
		WAIT
	}

}