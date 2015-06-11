package plugins.CENO;

/**
 * CENO Exceptions that include an errorCode
 * and the corresponding action for the methods
 * to handle them
 */
public class CENOException extends Exception {
	private final CENOErrCode errCode;

	/**
	 * Constructs a CENO exception with an error code
	 * from the errorConditions doc file.
	 * 
	 * @param errCode the {@link CENOErrCode} corresponding
	 * to this exception
	 */
	public CENOException(CENOErrCode errCode) {
		super(errCode.toString());
		this.errCode = errCode;
	}

	public CENOException(CENOErrCode errCode, String message) {
		super(message);
		this.errCode = errCode;
	}

	/**
	 * Getter for the CENOErrCode of the exception
	 */
	public CENOErrCode getErrCode() {
		return this.errCode;
	}
}