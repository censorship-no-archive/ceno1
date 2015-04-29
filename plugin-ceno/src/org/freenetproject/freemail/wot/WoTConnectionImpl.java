/*
 * WoTConnectionImpl.java
 * This file is part of Freemail
 * Copyright (C) 2011 Martin Nyhus
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.freenetproject.freemail.wot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.freenetproject.freemail.utils.SimpleFieldSetFactory;
import org.freenetproject.freemail.utils.Timer;

import freenet.pluginmanager.FredPluginTalker;
import freenet.pluginmanager.PluginNotFoundException;
import freenet.pluginmanager.PluginRespirator;
import freenet.pluginmanager.PluginTalker;
import freenet.support.Logger;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;

class WoTConnectionImpl implements WoTConnection {
	private static final String WOT_PLUGIN_NAME = "plugins.WebOfTrust.WebOfTrust";
	private static final String CONNECTION_IDENTIFIER = "Freemail";

	private final PluginTalker pluginTalker;

	private Message reply = null;
	private final Object replyLock = new Object();

	WoTConnectionImpl(PluginRespirator pr) throws PluginNotFoundException {
		pluginTalker = pr.getPluginTalker(new WoTConnectionTalker(), WOT_PLUGIN_NAME, CONNECTION_IDENTIFIER);
	}

	@Override
	public List<OwnIdentity> getAllOwnIdentities() {
		Message response = sendBlocking(
				new Message(
						new SimpleFieldSetFactory().put("Message", "GetOwnIdentities").create(),
						null),
				"OwnIdentities");
		if(response == null) {
			return null;
		}
		if(!"OwnIdentities".equals(response.sfs.get("Message"))) {
			return null;
		}

		final List<OwnIdentity> ownIdentities = new LinkedList<OwnIdentity>();
		for(int count = 0;; count++) {
			String identityID = response.sfs.get("Identity" + count);
			if(identityID == null) {
				//Got all the identities
				break;
			}

			String requestURI = response.sfs.get("RequestURI" + count);
			assert (requestURI != null);

			String insertURI = response.sfs.get("InsertURI" + count);
			assert (insertURI != null);

			String nickname = response.sfs.get("Nickname" + count);

			ownIdentities.add(new OwnIdentity(identityID, requestURI, insertURI, nickname));
		}

		return ownIdentities;
	}

	@Override
	public Set<Identity> getAllTrustedIdentities(String trusterId) {
		return getAllIdentities(trusterId, TrustSelection.TRUSTED);
	}

	@Override
	public Set<Identity> getAllUntrustedIdentities(String trusterId) {
		return getAllIdentities(trusterId, TrustSelection.UNTRUSTED);
	}

	private Set<Identity> getAllIdentities(String trusterId, TrustSelection selection) {
		if(trusterId == null) {
			throw new NullPointerException("Parameter trusterId must not be null");
		}
		if(selection == null) {
			throw new NullPointerException("Parameter selection must not be null");
		}

		SimpleFieldSet sfs = new SimpleFieldSetFactory().create();
		sfs.putOverwrite("Message", "GetIdentitiesByScore");
		sfs.putOverwrite("Truster", trusterId);
		sfs.putOverwrite("Selection", selection.value);
		sfs.put("WantTrustValues", false);

		/*
		 * The Freemail context wasn't added prior to 0.2.1, so for a while after 0.2.1 we need to
		 * support recipients without the Freemail context.
		 * FIXME: Restrict this to Freemail context a few months after 0.2.1 has become mandatory
		 */
		sfs.putOverwrite("Context", "Freemail");

		Message response = sendBlocking(new Message(sfs, null), "Identities");
		if(response == null) {
			return null;
		}
		if(!"Identities".equals(response.sfs.get("Message"))) {
			return null;
		}

		final Set<Identity> identities = new HashSet<Identity>();
		for(int count = 0;; count++) {
			String identityID = response.sfs.get("Identity" + count);
			if(identityID == null) {
				//Got all the identities
				break;
			}

			String requestURI = response.sfs.get("RequestURI" + count);
			assert (requestURI != null);

			String nickname = response.sfs.get("Nickname" + count);

			identities.add(new Identity(identityID, requestURI, nickname));
		}

		return identities;
	}

	@Override
	public Identity getIdentity(String identity, String trusterId) {
		if(identity == null) {
			throw new NullPointerException("Parameter identity must not be null");
		}
		if(trusterId == null) {
			throw new NullPointerException("Parameter trusterId must not be null");
		}

		SimpleFieldSet sfs = new SimpleFieldSetFactory().create();
		sfs.putOverwrite("Message", "GetIdentity");
		sfs.putOverwrite("Identity", identity);
		sfs.putOverwrite("Truster", trusterId);

		Message response = sendBlocking(new Message(sfs, null), "Identity");
		if(response == null) {
			return null;
		}
		if(!"Identity".equals(response.sfs.get("Message"))) {
			return null;
		}

		String requestURI = response.sfs.get("RequestURI");
		assert(requestURI != null);

		String nickname = response.sfs.get("Nickname");

		return new Identity(identity, requestURI, nickname);
	}

	@Override
	public boolean setProperty(String identity, String key, String value) {
		if(identity == null) {
			throw new NullPointerException("Parameter identity must not be null");
		}
		if(key == null) {
			throw new NullPointerException("Parameter key must not be null");
		}
		if(value == null) {
			throw new NullPointerException("Parameter value must not be null");
		}

		SimpleFieldSet sfs = new SimpleFieldSet(true);
		sfs.putOverwrite("Message", "SetProperty");
		sfs.putOverwrite("Identity", identity);
		sfs.putOverwrite("Property", key);
		sfs.putOverwrite("Value", value);

		Message response = sendBlocking(new Message(sfs, null), "PropertyAdded");
		if(response == null) {
			return false;
		}
		return "PropertyAdded".equals(response.sfs.get("Message"));
	}

	@Override
	public String getProperty(String identity, String key) {
		if(identity == null) {
			throw new NullPointerException("Parameter identity must not be null");
		}
		if(key == null) {
			throw new NullPointerException("Parameter key must not be null");
		}

		SimpleFieldSet sfs = new SimpleFieldSet(true);
		sfs.putOverwrite("Message", "GetProperty");
		sfs.putOverwrite("Identity", identity);
		sfs.putOverwrite("Property", key);

		Set<String> expectedTypes = new HashSet<String>();
		expectedTypes.add("PropertyValue");

		/* Also include Error since WoT returns this if the property doesn't exist for the message */
		/* FIXME: Perhaps check the description and log if it isn't what we expect? */
		expectedTypes.add("Error");

		Message response = sendBlocking(new Message(sfs, null), expectedTypes);
		if(response == null) {
			return null;
		}

		if("PropertyValue".equals(response.sfs.get("Message"))) {
			return response.sfs.get("Property");
		} else {
			return null;
		}
	}

	@Override
	public boolean setContext(String identity, String context) {
		if(identity == null) {
			throw new NullPointerException("Parameter identity must not be null");
		}
		if(context == null) {
			throw new NullPointerException("Parameter context must not be null");
		}

		SimpleFieldSet sfs = new SimpleFieldSet(true);
		sfs.putOverwrite("Message", "AddContext");
		sfs.putOverwrite("Identity", identity);
		sfs.putOverwrite("Context", context);

		Message response = sendBlocking(new Message(sfs, null), "ContextAdded");
		if(response == null) {
			return false;
		}
		return "ContextAdded".equals(response.sfs.get("Message"));
	}

	private Message sendBlocking(final Message msg, String expectedMessageType) {
		return sendBlocking(msg, Collections.singleton(expectedMessageType));
	}

	private Message sendBlocking(final Message msg, Set<String> expectedMessageTypes) {
		assert (msg != null);

		//Synchronize on this so only one message can be sent at a time
		//Log the contents of the message before sending (debug because of private keys etc)
		Iterator<String> msgContentIterator = msg.sfs.keyIterator();
		while(msgContentIterator.hasNext()) {
			String key = msgContentIterator.next();
			Logger.debug(this, key + "=" + msg.sfs.get(key));
		}

		//Synchronize on pluginTalker so only one message can be sent at a time
		final Message retValue;
		Timer requestTimer;
		synchronized(this) {
			synchronized(replyLock) {
				requestTimer = Timer.start();

				assert (reply == null) : "Reply was " + reply;
				reply = null;

				pluginTalker.send(msg.sfs, msg.data);

				while(reply == null) {
					try {
						replyLock.wait();
					} catch (InterruptedException e) {
						//Just check again
					}
				}

				retValue = reply;
				reply = null;

			}
		}
		requestTimer.log(this, "Time spent waiting for WoT request " + msg.sfs.get("Message") + " (reply was "
				+ retValue.sfs.get("Message") + ")");

		String replyType = retValue.sfs.get("Message");
		for(String expectedMessageType : expectedMessageTypes) {
			if(expectedMessageType.equals(replyType)) {
				return retValue;
			}
		}

		Logger.error(this, "Got the wrong message from WoT. Original message was "
				+ retValue.sfs.get("OriginalMessage") + ", response was "
				+ replyType);

		//Log the contents of the message, but at debug since it might contain private keys etc.
		Iterator<String> keyIterator = retValue.sfs.keyIterator();
		while(keyIterator.hasNext()) {
			String key = keyIterator.next();
			Logger.debug(this, key + "=" + retValue.sfs.get(key));
		}

		return retValue;
	}

	private static class Message {
		private final SimpleFieldSet sfs;
		private final Bucket data;

		private Message(SimpleFieldSet sfs, Bucket data) {
			this.sfs = sfs;
			this.data = data;
		}

		@Override
		public String toString() {
			return "[" + sfs + "] [" + data + "]";
		}
	}

	private class WoTConnectionTalker implements FredPluginTalker {
		@Override
		public void onReply(String pluginname, String indentifier, SimpleFieldSet params, Bucket data) {
			synchronized(replyLock) {
				assert reply == null : "Reply should be null, but was " + reply;

				reply = new Message(params, data);
				replyLock.notify();
			}
		}
	}

	private enum TrustSelection {
		TRUSTED("+"),
		ZERO("0"),
		UNTRUSTED("-");

		private final String value;
		private TrustSelection(String value) {
			this.value = value;
		}
	}
}
