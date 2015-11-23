package Legacy.plugins.CENO;

import java.util.EnumSet;

import plugins.CENO.FreenetInterface.FreemailAPI.Freemail;

public class CENORequest extends Freemail {

	public enum CENOReqFlags {
		X_Ceno_Rewritten
	}

	private EnumSet<CENOReqFlags> reqFlags;

	public CENORequest(Freemail freemail) {
		super(freemail.getFreemailFrom(), freemail.getFreemailTo(), freemail.getSubject(), freemail.getBody());
	}

	public CENORequest(Freemail freemail, EnumSet<CENOReqFlags> reqFlags) {
		this(freemail);
		this.reqFlags = reqFlags;
	}

	public CENORequest(String freemailFrom, String[] freemailTo, String subject, String body) {
		super(freemailFrom, freemailTo, subject, body);
	}

	public CENORequest(String freemailFrom, String[] freemailTo, String subject, String body, EnumSet<CENOReqFlags> reqFlags) {
		this(freemailFrom, freemailTo, subject, body);
		this.reqFlags = reqFlags;
	}

	public EnumSet<CENOReqFlags> getReqFlags() {
		return reqFlags;
	}
}
