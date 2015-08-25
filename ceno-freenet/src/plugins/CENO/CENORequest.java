package plugins.CENO;

import plugins.CENO.FreenetInterface.FreemailAPI.Freemail;

public class CENORequest extends Freemail {

	private CENOReqFlags[] reqFlags;

	public CENORequest(Freemail freemail) {
		super(freemail.getFreemailFrom(), freemail.getFreemailTo(), freemail.getSubject(), freemail.getBody());
	}

	public CENORequest(Freemail freemail, CENOReqFlags[] reqFlags) {
		this(freemail);
		this.reqFlags = reqFlags;
	}

	public CENORequest(String freemailFrom, String[] freemailTo, String subject, String body) {
		super(freemailFrom, freemailTo, subject, body);
	}

	public CENORequest(String freemailFrom, String[] freemailTo, String subject, String body, CENOReqFlags[] reqFlags) {
		this(freemailFrom, freemailTo, subject, body);
		this.reqFlags = reqFlags;
	}

	public CENOReqFlags[] getReqFlags() {
		return reqFlags;
	}
	
	public enum CENOReqFlags {
		X_Ceno_Rewritten
	}
}
