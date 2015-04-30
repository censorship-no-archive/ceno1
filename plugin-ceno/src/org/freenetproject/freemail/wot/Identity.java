/*
 * Identity.java
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

import java.util.Locale;

import org.archive.util.Base32;

import freenet.support.Base64;
import freenet.support.IllegalBase64Exception;

public class Identity {
	private final String identityID;
	private final String requestURI;
	private final String nickname;

	public Identity(String identityID, String requestURI, String nickname) {
		this.identityID = identityID;
		this.requestURI = requestURI;
		this.nickname = nickname;
	}

	public String getIdentityID() {
		return identityID;
	}

	public String getBase32IdentityID() {
		try {
			return Base32.encode(Base64.decode(identityID)).toLowerCase(Locale.ROOT);
		} catch (IllegalBase64Exception e) {
			//Can't happen since we always get the id from WoT
			throw new AssertionError();
		}
	}

	public String getRequestURI() {
		return requestURI;
	}

	public String getNickname() {
		return nickname;
	}

	@Override
	public int hashCode() {
		return identityID.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Identity)) {
			return false;
		}

		Identity other = (Identity) obj;
		return identityID.equals(other.identityID);
	}

	@Override
	public String toString() {
		return "Identity " + identityID;
	}
}
