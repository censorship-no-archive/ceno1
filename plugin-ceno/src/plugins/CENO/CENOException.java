package plugins.CENO;

public class CENOException extends Exception {
	private final CENOErrCode errCode;
	
	public CENOException(CENOErrCode errCode) {
		super(errCode.toString());
		this.errCode = errCode;
	}
}
