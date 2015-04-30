/*
 * IdentityMatcher.java
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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import freenet.pluginmanager.PluginNotFoundException;

public class IdentityMatcher {
	private final WoTConnection wotConnection;

	public IdentityMatcher(WoTConnection wotConnection) {
		this.wotConnection = wotConnection;
	}

	public Map<String, List<Identity>> matchIdentities(Set<String> recipients, String wotOwnIdentity, EnumSet<MatchMethod> methods) throws PluginNotFoundException {
		Set<Identity> trustedIds = wotConnection.getAllTrustedIdentities(wotOwnIdentity);
		Set<Identity> untrustedIds = wotConnection.getAllUntrustedIdentities(wotOwnIdentity);
		List<OwnIdentity> ownIds = wotConnection.getAllOwnIdentities();

		Map<String, List<Identity>> allMatches = new HashMap<String, List<Identity>>(recipients.size());
		for(String recipient : recipients) {
			allMatches.put(recipient, new LinkedList<Identity>());
		}

		if(trustedIds == null || untrustedIds == null || ownIds == null) {
			return allMatches;
		}

		Set<Identity> wotIdentities = new HashSet<Identity>();
		wotIdentities.addAll(trustedIds);
		wotIdentities.addAll(untrustedIds);
		wotIdentities.addAll(ownIds);

		for(Identity wotIdentity : wotIdentities) {
			for(String recipient : recipients) {
				if(matchIdentity(recipient, wotIdentity, methods)) {
					allMatches.get(recipient).add(wotIdentity);
				}
			}
		}

		return allMatches;
	}

	private boolean matchIdentity(String recipient, Identity wotIdentity, EnumSet<MatchMethod> methods) {
		for(MatchMethod method : methods) {
			switch(method) {
			case PARTIAL_BASE32:
				if(matchBase32Address(recipient, wotIdentity)) {
					return true;
				}
				break;
			case PARTIAL_BASE64:
				if(matchBase64Address(recipient, wotIdentity)) {
					return true;
				}
				break;
			case FULL_BASE32:
				if(matchFullAddress(recipient, wotIdentity.getBase32IdentityID())) {
					return true;
				}
				break;
			case FULL_BASE64:
				if(matchFullAddress(recipient, wotIdentity.getIdentityID())) {
					return true;
				}
				break;
			default:
				throw new AssertionError();
			}
		}

		return false;
	}

	private boolean matchBase64Address(String recipient, Identity identity) {
		String identityAddress = identity.getNickname() + "@" + identity.getIdentityID() + ".freemail";
		return identityAddress.startsWith(recipient);
	}

	private boolean matchBase32Address(String recipient, Identity identity) {
		String base32Id = identity.getBase32IdentityID();
		String identityAddress = identity.getNickname() + "@" + base32Id + ".freemail";

		//Change the recipient address to lower case, but leave the nickname in the original case
		if(recipient.contains("@")) {
			String recipientNickname = recipient.substring(0, recipient.indexOf("@"));
			String recipientDomain = recipient.substring(recipient.indexOf("@") + 1);
			recipient = recipientNickname + "@" + recipientDomain.toLowerCase(Locale.ROOT);
		}

		return identityAddress.startsWith(recipient);
	}

	/**
	 * Matches addresses of the format [local part@]&lt;identityId&gt;[.freemail]
	 * @param recipient the address to check
	 * @param identityId the identity to check against
	 * @return {@code true} if recipient matches identity
	 */
	private boolean matchFullAddress(String recipient, String identityId) {
		//Remove the optional local part
		if(recipient.contains("@")) {
			recipient = recipient.substring(recipient.indexOf("@") + 1);
		}

		//Remove the optional ".freemail"
		if(recipient.endsWith(".freemail")) {
			recipient = recipient.substring(0, recipient.length() - ".freemail".length());
		}

		return recipient.equals(identityId);
	}

	public enum MatchMethod {
		PARTIAL_BASE32,
		PARTIAL_BASE64,
		FULL_BASE32,
		FULL_BASE64,
	}
}
