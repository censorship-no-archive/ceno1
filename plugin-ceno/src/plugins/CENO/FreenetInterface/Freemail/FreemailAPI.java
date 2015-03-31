package plugins.CENO.FreenetInterface.Freemail;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import plugins.CENO.Client.CENOClient;

import com.sun.mail.smtp.SMTPTransport;

public class FreemailAPI {
	private static final String smtpHost = "127.0.0.1";
	private static final int smtpPort = 4025;

	public static boolean sendFreemail(String freemailFrom, String freemailTo, String subject, String content) {
		Session smtpSession = prepareSMTPSession(freemailFrom);
		SMTPTransport smtpTransport = doConnectSMTP(smtpSession, freemailFrom);
		if (smtpTransport != null) {
			Message msg = prepareMessage(smtpSession, freemailFrom, freemailTo, subject, content);
			if (msg != null) {
				try {
					Transport.send(msg, msg.getAllRecipients());
					smtpTransport.close();
				} catch (MessagingException e) {
					return false;
				}
				return true;
			}
		}
		return false;
	}

	private static Session prepareSMTPSession(String smtpUser) {
		Properties props = System.getProperties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", smtpPort);
		props.put("mail.smtp.auth","true");
		props.put("mail.smtp.user", smtpUser);
		props.put("mail.smtp.timeout", "5000");

		return Session.getInstance(props, null);
	}

	private static Message prepareMessage(Session smtpSession, String freemailFrom, String freemailTo, String subect, String content) {
		Message msg = new MimeMessage(smtpSession);
		try {
			msg.setFrom(new InternetAddress(freemailFrom));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(freemailTo));
			msg.setSubject(subect);
			msg.setText(content);
		} catch (MessagingException e) {
			return null;
		}
		return msg;
	}

	private static SMTPTransport doConnectSMTP(Session session, String smtpUser) {
		SMTPTransport smtpTransport = null;
		try {
			smtpTransport = (SMTPTransport)session.getTransport("smtp");
		} catch (NoSuchProviderException e) {
			return null;
		}

		try {
			smtpTransport.connect(smtpUser, "CENO");
		} catch (MessagingException e) {
			e.printStackTrace();
			return null;
		}
		return smtpTransport;
	}

}