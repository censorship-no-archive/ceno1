package plugins.CENO;

/**
 * Exception subclass for issues related to the CENO Freenet plugins
 */
public class CENOException extends Exception {
	private static final long serialVersionUID = 1L;
	private final CENOErrCode errCode;

	/**
	 * Constructs a CENO exception with an error code
	 * from the CENOErrCode list.
	 * 
	 * @param errCode the {@link CENOErrCode} corresponding
	 * to this exception
	 */
	public CENOException(CENOErrCode errCode) {
		super(errCode.toString());
		this.errCode = errCode;
	}

	/**
	 * Constructs a CENO exception with a custom error message.
	 * This message cannot be localized.
	 * 
	 * @param errCode the {@link CENOErrCode} corresponding
	 * to this exception
	 * @param message the custom error message to use
	 * instead of the error code's default one
	 */
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