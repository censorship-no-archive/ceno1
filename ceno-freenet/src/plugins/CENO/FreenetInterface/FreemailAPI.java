package plugins.CENO.FreenetInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

import org.apache.commons.compress.utils.IOUtils;

import com.sun.mail.smtp.SMTPTransport;

public class FreemailAPI {
	private static final String localHost = "127.0.0.1";
	private static final int smtpPort = 4025;
	private static final int imapPort = 4143;

	/**
	 * Inner class that extends javax mail Authenticator.
	 * Handles authentication with Freemail's SMTPHandler during the
	 * creation of the SMTPTransport session.
	 */
	private static class SMTPAuthenticator extends Authenticator {
		String freemail, password;

		/**
		 * SMTPAuthenticator constructor
		 * @param freemail
		 * @param password
		 */
		public SMTPAuthenticator(String freemail, String password) {
			this.freemail = freemail;
			this.password = password;
		}

		/**
		 * If freemail and password are set and not empty, return a PasswordAuthentication instance.
		 * Unless, return null.
		 * 
		 * In case of null, Freemail SMTPHandler throws an AuthenticationFailedException
		 */
		@Override
		public PasswordAuthentication getPasswordAuthentication() {
			if (freemail != null && !freemail.isEmpty()) {
				if (password != null && !password.isEmpty()) {
					return new PasswordAuthentication(freemail, password);
				}
			}
			return null;
		}
	}

	/**
	 * Static method that sends a freemail over SMTP
	 * 
	 * @param freemailFrom the sender freemail
	 * @param freemailTo the list of recipients' freemails
	 * @param subject the subject of the freemail
	 * @param content the content (plain text body) of the freemail
	 * @param password the password to use for authentication with freemail's SMTP handler
	 * @return true, if the freemail was sent successfully
	 */
	public static synchronized boolean sendFreemail(String freemailFrom, String freemailTo[], String subject, String content, String password) {
		Session smtpSession = prepareSMTPSession(freemailFrom, password);
		if (smtpSession == null) {
			return false;
		}
		SMTPTransport smtpTransport = doConnectSMTP(smtpSession, freemailFrom, password);
		if (smtpTransport != null) {
			Message msg = prepareMessage(smtpSession, freemailFrom, freemailTo, subject, content);
			if (msg != null) {
				try {
					Transport.send(msg, msg.getAllRecipients());
					smtpTransport.close();
				} catch (MessagingException e) {
					//TODO Handle 550 bridge freemail not found because bridge's WOT has no trust value
					// by sending fcp message > add trust value 75 to the WOT identity of the freemail
					e.printStackTrace();
					return false;
				}
				return true;
			}
		}
		return false;
	}


	private static Session prepareSMTPSession(String smtpUser, String smtpPassword) {
		Properties props = System.getProperties();
		props.put("mail.smtp.host", localHost);
		props.put("mail.smtp.port", smtpPort);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.user", smtpUser);
		props.put("mail.smtp.connectiontimeout", "5000");

		Session session = null;
		try {
			session = Session.getInstance(props, new SMTPAuthenticator(smtpUser, smtpPassword));
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		return session;
	}

	private static Session prepareIMAPSession(String imapUser, String imapPassword) {
		Properties props = System.getProperties();
		props.put("mail.imap.host", localHost);
		props.put("mail.imap.port", imapPort);
		props.put("mail.imap.connectiontimeout", "5000");

		Session session = null;
		try {
			session = Session.getInstance(props);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		return session;
	}

	private static Message prepareMessage(Session smtpSession, String freemailFrom, String freemailTo[], String subect, String content) {
		Message msg = new MimeMessage(smtpSession);
		try {
			msg.setFrom(new InternetAddress(freemailFrom));
			for (String recipient : freemailTo) {
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
			}
			msg.setSubject(subect);
			msg.setText(content);
		} catch (MessagingException e) {
			return null;
		}
		return msg;
	}

	private static SMTPTransport doConnectSMTP(Session session, String smtpUser, String smtpPassword) {
		SMTPTransport smtpTransport = null;
		try {
			smtpTransport = (SMTPTransport)session.getTransport("smtp");
		} catch (NoSuchProviderException e) {
			return null;
		}

		try {
			smtpTransport.connect(smtpUser, smtpPassword);
		} catch (MessagingException e) {
			e.printStackTrace();
			return null;
		}
		return smtpTransport;
	}

	/**
	 * Get an array of the unread freemails of a folder via IMAP
	 * 
	 * @param freemail the freemail user
	 * @param password freemail user's password
	 * @param inboxFolder the folder to search for undread freemails
	 * @param shouldDelete if {@code true}, delete unread messages after retrieval
	 * @return a Message array of the unread freemails, an empty array if there are no unread 
	 * freemails and {@code null} if there was an error
	 */
	public static synchronized String[] getUnreadMailsSubject(String freemail, String password, String mailFolder, boolean shouldDelete) {
		Message[] unreadMessages = getMessages(freemail, password, mailFolder, shouldDelete, Flags.Flag.SEEN, false);
		if (unreadMessages == null) {
			return null;
		}
		String[] mailsSubject = new String[unreadMessages.length];
		for (int i=0; i<unreadMessages.length; i++) {
			try {
				mailsSubject[i] = unreadMessages[i].getSubject();
			} catch (MessagingException e) {
				mailsSubject[i] = "Invalid";
			}
		}
		return mailsSubject;
	}

	public static synchronized String[] getMailsContentFrom(String freemail, String freemailFrom, String password, String mailFolder) {
		Message[] messages = getMailsFrom(freemail, freemailFrom, password, mailFolder);
		ArrayList<String> mailsBody = new ArrayList<String>();
		ByteArrayOutputStream contentStr = new ByteArrayOutputStream();

		for (Message message : messages) {
			try {
				if (!message.getContentType().startsWith("text/plain")) {
					continue;
				}
				message.writeTo(contentStr);
				mailsBody.add(contentStr.toString());
			} catch (MessagingException | IOException e) {
				continue;
			}
		}
		return (String[]) mailsBody.toArray();
	}

	public static synchronized Message[] getMailsFrom(String freemail, String freemailFrom, String password, String mailFolder) {
		Message[] allMessages = getMessages(freemail, password, mailFolder, false, null, false);
		ArrayList<Message> mailsFrom = new ArrayList<>();
		for (Message message : allMessages) {
			try {
				if (message.getFrom()[0].equals(freemailFrom)) {
					mailsFrom.add(message);
				}
			} catch (MessagingException e) {
				continue;
			}
		}
		return (Message[]) mailsFrom.toArray();
	}

	private static Message[] getMessages(String freemail, String password, String mailFolder, boolean shouldDelete, Flag flag, boolean flagBool) {
		Store store = null;
		Folder folder = null;
		try {
			Session session = prepareIMAPSession(freemail, password);
			if (session == null) {
				return null;
			}
			store = session.getStore("imap");
			store.connect(localHost, freemail, password);

			folder = store.getFolder(mailFolder);
			if (folder == null || !folder.exists()) {
				return null;
			}
			folder.open(Folder.READ_WRITE);

			Message[] messages;
			if (flag != null) {
				messages = folder.search(new FlagTerm(new Flags(flag), flagBool));
			} else {
				messages = folder.getMessages();
			}

			if (shouldDelete) {
				for (Message message : messages) {
					message.setFlag(Flag.DELETED, true);
				}
			}

			// Once the folder is closed, messages cannot be read. Therefore we need to
			// return a clone of them in a new array.
			Message[] messagesCopy = messages.clone();

			// Close the folder and expunge (remove) all mails with the DELETED flag
			folder.close(true);
			store.close();

			return messagesCopy;
		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				if (store != null) {
					store.close();
				}
				if (folder != null) {
					// Close the folder without expunging the mails
					folder.close(false);
				}
			} catch (MessagingException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	private static String getShortFreemailAddr(String freemailAddress) {
		return freemailAddress.split("@|\\.")[1];
	}

	private static String getShortIdentityURI(String identityRequestURI) {
		return identityRequestURI.split("@|,")[1];
	}

	private static Path getAccountDir(String freemailAccount) throws UnsupportedEncodingException {
		return Paths.get("freemail-wot/data", getShortFreemailAddr(freemailAccount) + "/");
	}

	public static boolean copyAccprops(String freemailAccount) {
		// Check if an account for the CENO client identity already exists
		// If it doesn't exist, create the corresponding directory
		Path accpropsPath = null;
		try {
			accpropsPath = getAccountDir(freemailAccount);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
		if (Files.isDirectory(accpropsPath)) {
			return true;
		}
		try {
			Files.createDirectories(accpropsPath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		// Copy the accprops to the directory created
		InputStream accpropsIn = FreemailAPI.class.getResourceAsStream("Freemail/resources/accprops");
		if (accpropsIn == null) {
			return false;
		}
		FileOutputStream accpropsOut = null;
		try {
			accpropsOut = new FileOutputStream(accpropsPath + "/accprops");
		} catch (FileNotFoundException e) {
			return false;
		}
		try {
			IOUtils.copy(accpropsIn, accpropsOut);
			accpropsIn.close();
			accpropsOut.flush();
			accpropsOut.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static boolean setRandomNextMsgNumber(String freemailAccount, String freemailTo) {
		Path outboxPath;
		try {
			outboxPath = Paths.get(getAccountDir(freemailAccount).toString(), "outbox", getShortFreemailAddr(freemailTo));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		try {
			PrintWriter pwIndex = new PrintWriter(outboxPath + "/index");
			pwIndex.print("nextMessageNumber=");
			pwIndex.println((int) (Math.random() * (Integer.MAX_VALUE * 0.8)));
			pwIndex.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static boolean clearOutboxLog(String freemailAccount, String identityFrom) {
		Path outboxPath;
		try {
			outboxPath = Paths.get(getAccountDir(freemailAccount).toString(), "outbox", getShortIdentityURI(identityFrom));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		File outboxFiles = new File(outboxPath.toString());
		if (!outboxFiles.isDirectory()) {
			return false;
		}

		for (File outboxFile : outboxFiles.listFiles()) {
			if (!outboxFile.isDirectory()) {
				outboxFile.delete();
			}
		}

		return true;
	}

	public static boolean clearOutboxMessages(String freemailAccount, String freemailTo) {
		Path outboxPath;
		try {
			outboxPath = Paths.get(getAccountDir(freemailAccount).toString(), "outbox", getShortFreemailAddr(freemailTo));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		File outboxFiles = new File(outboxPath.toString());
		if (!outboxFiles.isDirectory()) {
			return false;
		}

		for (File outboxFile : outboxFiles.listFiles()) {
			if (!outboxFile.isDirectory()) {
				outboxFile.delete();
			}
		}

		return true;
	}

	public static abstract class Freemail {
		private String freemailFrom;
		private String[] freemailTo;
		private String subject;
		private String body;
		
		public Freemail(String freemailFrom, String[] freemailTo, String subject, String body) {
			this.setFreemailFrom(freemailFrom);
			this.setFreemailTo(freemailTo);
			this.setSubject(subject);
			this.setBody(body);
		}

		public String getFreemailFrom() {
			return freemailFrom;
		}

		public void setFreemailFrom(String freemailFrom) {
			this.freemailFrom = freemailFrom;
		}

		public String[] getFreemailTo() {
			return freemailTo;
		}

		public void setFreemailTo(String[] freemailTo) {
			this.freemailTo = freemailTo;
		}

		public String getSubject() {
			return subject;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}
	}

}