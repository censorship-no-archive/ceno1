package plugins.CENO.FreenetInterface.Freemail;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;

import plugins.CENO.Client.CENOClient;

import com.sun.mail.smtp.SMTPTransport;

public class FreemailAPI {
	private static final String smtpHost = "127.0.0.1";
	private static final int smtpPort = 4025;

	public static boolean sendFreemail(String freemailFrom, String freemailTo, String content) {
		if (!doConnect()) {
			return false;
		}
		// send freemail
		return true;
	}

	public static boolean doConnect() {
		Properties props = System.getProperties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", smtpPort);
		props.put("mail.smtp.auth","true");
		props.put("mail.smtp.user", CENOClient.clientFreemail);
		props.put("mail.smtp.timeout", "5000");

		Session session = Session.getInstance(props, null);
		SMTPTransport t;
		try {
			t = (SMTPTransport)session.getTransport("smtp");
		} catch (NoSuchProviderException e) {
			return false;
		}

		try {
			t.connect(CENOClient.clientFreemail, "CENO");
		} catch (MessagingException e) {
			e.printStackTrace();
			return false;
		}
		return t.isConnected();
	}

}