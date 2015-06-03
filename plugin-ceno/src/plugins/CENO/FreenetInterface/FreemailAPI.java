package plugins.CENO.FreenetInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

import org.apache.commons.compress.utils.IOUtils;

import com.sun.mail.smtp.SMTPTransport;

import plugins.CENO.Client.CENOClient;
import freenet.node.NodeStarter;

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
	public static synchronized String[] getUnreadMailsSubject(String freemail, String password, String inboxFolder, boolean shouldDelete) {
		Message[] unreadMessages = getMessages(freemail, password, inboxFolder, shouldDelete, Flags.Flag.SEEN, false);
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

	private static Message[] getMessages(String freemail, String password, String inboxFolder, boolean shouldDelete, Flag flag, boolean flagBool) {
		Store store = null;
		Folder folder = null;
		try {
			Session session = prepareIMAPSession(freemail, password);
			if (session == null) {
				return null;
			}
			store = session.getStore("imap");
			store.connect(localHost, freemail, password);

			folder = store.getFolder(inboxFolder);
			if (folder == null || !folder.exists()) {
				return null;
			}
			folder.open(Folder.READ_WRITE);

			Message[] unreadMessages;
			if (flag != null) {
				unreadMessages = folder.search(new FlagTerm(new Flags(flag), flagBool));
			} else {
				unreadMessages = folder.getMessages();
			}

			if (shouldDelete) {
				for (Message message : unreadMessages) {
					message.setFlag(Flag.DELETED, true);
				}
			}

			// Once the folder is closed, messages cannot be read. Therefore we need to
			// return a copy of them in a new array.
			Message[] unreadMessagesCopy = new Message[unreadMessages.length];
			for (int i=0; i<unreadMessages.length; i++) {
				unreadMessagesCopy[i] = new MimeMessage((MimeMessage) unreadMessages[i]);
			}

			// Close the folder and expunge (remove) all mails with the DELETED flag
			folder.close(true);
			store.close();

			return unreadMessagesCopy;
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

	public static boolean startIMAPMonitor(String freemail, String password, String idleFolder) {
		Store store = null;
		try {
			Session session = prepareIMAPSession(freemail, password);
			store = session.getStore("imap");
			store.connect(localHost, freemail, password);

			Folder folder = store.getFolder(idleFolder);
			if (folder == null || !folder.exists()) {
				return false;
			}
			folder.open(Folder.READ_ONLY);

			folder.addMessageCountListener(new MessageCountAdapter() {
				public void messagesAdded(MessageCountEvent ev) {
					// Get new freemails
					Message[] msgs = ev.getMessages();
					for (Message message : msgs) {
						try {
							if (message.getFrom().equals(CENOClient.bridgeFreemail)) {
								System.out.println("Bundle requested for URL: " + message.getSubject());
							}
						} catch (MessagingException mex) {
							mex.printStackTrace();
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
			if (store!=null && store.isConnected()) {
				try {
					store.close();
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
		return true;
	}

	private static String getShortFreemailAddr(String freemailAddress) {
		return freemailAddress.split("@|\\.")[1];
	}

	private static Path getAccountPath(String freemailAccount) throws UnsupportedEncodingException {
		String runningJarPath = NodeStarter.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		return Paths.get(URLDecoder.decode(runningJarPath, "UTF-8"), "freemail-wot/data", getShortFreemailAddr(freemailAccount) + "/");
	}

	public static boolean copyAccprops(String freemailAccount) {
		// Check if an account for the CENO client identity already exists
		// If it doesn't exist, create the corresponding directory
		Path accpropsPath = null;
		try {
			accpropsPath = getAccountPath(freemailAccount);
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
		InputStream accpropsIn = FreemailAPI.class.getResourceAsStream("Freemail/Resources/accprops");
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

	public static boolean clearOutboxLogs(String freemailAccount, String freemailFrom) {
		Path outboxPath;
		try {
			outboxPath = Paths.get(getAccountPath(freemailAccount).toString(), "outbox", getShortFreemailAddr(freemailFrom));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		File outboxFiles = new File(outboxPath.toString());
		for (File outboxFile : outboxFiles.listFiles()) {
			if (!outboxFile.isDirectory()) {
				outboxFile.delete();
			}
		}

		new File(outboxPath + "logs");
		return true;
	}

	public static boolean clearOutboxMessages(String freemailAccount, String freemailTo) {
		Path outboxPath;
		try {
			outboxPath = Paths.get(getAccountPath(freemailAccount).toString(), "outbox", getShortFreemailAddr(freemailTo));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		File outboxFiles;
		try {
			outboxFiles = new File(outboxPath.toString());
		} catch (NullPointerException e) {
			return false;
		}

		for (File outboxFile : outboxFiles.listFiles()) {
			if (!outboxFile.isDirectory()) {
				outboxFile.delete();
			}
		}

		PrintWriter pwIndex;
		try {
			pwIndex = new PrintWriter(outboxPath + "index");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		pwIndex.print("nextMessageNumber=");
		pwIndex.print((int) (Math.random()*1000 + 10));
		pwIndex.flush();
		pwIndex.close();
		return true;
	}

}