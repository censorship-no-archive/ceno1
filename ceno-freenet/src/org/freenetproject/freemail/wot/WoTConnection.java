/*
 * WoTConnection.java
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

import java.util.List;
import java.util.Set;

import freenet.pluginmanager.PluginNotFoundException;

public interface WoTConnection {
	/**
	 * Returns all the OwnIdentities fetched from WoT, or {@code null} if an error occurs. If there
	 * are no OwnIdentities the returned list will be empty.
	 * @return all the OwnIdentities fetched from WoT
	 * @throws PluginNotFoundException If the WoT plugin isn't loaded
	 */
	public List<OwnIdentity> getAllOwnIdentities() throws PluginNotFoundException;

	/**
	 * Returns all the identities that are trusted by the given identity, or {@code null} if an
	 * error occurs. Trusted identities are those with a score of 0 or higher.
	 * @param trusterId the identity id of the truster
	 * @return all the identities that are trusted by the given identity
	 * @throws NullPointerException if trusterId is {@code null}
	 * @throws PluginNotFoundException If the WoT plugin isn't loaded
	 */
	public Set<Identity> getAllTrustedIdentities(String trusterId) throws PluginNotFoundException;

	/**
	 * Returns all the identities that are not trusted by the given identity, or {@code null} if an
	 * error occurs.
	 * @param trusterId the identity id of the truster
	 * @return all the identities that are not trusted by the given identity
	 * @throws NullPointerException if trusterId is {@code null}
	 * @throws PluginNotFoundException If the WoT plugin isn't loaded
	 */
	public Set<Identity> getAllUntrustedIdentities(String trusterId) throws PluginNotFoundException;

	/**
	 * Returns the Identity with the given identity string. The truster parameter is used to fetch
	 * the trust and score of the identity and must be a valid OwnIdentity.
	 * @param identity the id of the Identity that should be fetched
	 * @param truster the OwnIdentity to use for fetching the trust and score
	 * @return the Identity with the given identity string
	 * @throws NullPointerException if any of the parameters are {@code null}
	 * @throws PluginNotFoundException If the WoT plugin isn't loaded
	 */
	public Identity getIdentity(String identity, String truster) throws PluginNotFoundException;

	/**
	 * Sets the property {@code key} to {@code value} for the given identity, returning {@code true}
	 * if the property was successfully set and {@code false} otherwise.
	 * @param identity the identity whose parameter should be set
	 * @param key the name of the parameter
	 * @param value the new value of the parameter
	 * @return {@code true} if the parameter was successfully set
	 * @throws NullPointerException if any of the parameters are {@code null}
	 * @throws PluginNotFoundException If the WoT plugin isn't loaded
	 */
	public boolean setProperty(String identity, String key, String value) throws PluginNotFoundException;

	/**
	 * Returns the value of the named parameter from the given identity. {@code null} is returned if
	 * the parameter doesn't exist, or if an error occurs.
	 * @param identity the identity whose parameter should be fetched
	 * @param key the name of the parameter to be fetched
	 * @return the value of the parameter, or {@code null}
	 * @throws NullPointerException if any of the parameters are {@code null}
	 * @throws PluginNotFoundException If the WoT plugin isn't loaded
	 */
	public String getProperty(String identity, String key) throws PluginNotFoundException;

	/**
	 * Sets the context {@code context} for the given identity, returning {@code true} if the
	 * context was successfully set and {@code false} otherwise.
	 * @param identity the identity whose context should be set
	 * @param context the context that should be set
	 */
	public boolean setContext(String identity, String context) throws PluginNotFoundException;
}
