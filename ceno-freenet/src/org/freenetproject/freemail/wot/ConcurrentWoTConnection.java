/*
 * ConcurrentWoTConnection.java
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
import freenet.pluginmanager.PluginRespirator;

public class ConcurrentWoTConnection implements WoTConnection {
	private final PluginRespirator pluginRespirator;

	public ConcurrentWoTConnection(PluginRespirator pluginRespirator) {
		this.pluginRespirator = pluginRespirator;
	}

	public List<OwnIdentity> getAllOwnIdentities() throws PluginNotFoundException {
		WoTConnectionImpl wotConnection = new WoTConnectionImpl(pluginRespirator);
		return wotConnection.getAllOwnIdentities();
	}

	public Set<Identity> getAllTrustedIdentities(String trusterId) throws PluginNotFoundException {
		WoTConnectionImpl wotConnection = new WoTConnectionImpl(pluginRespirator);
		return wotConnection.getAllTrustedIdentities(trusterId);
	}

	public Set<Identity> getAllUntrustedIdentities(String trusterId) throws PluginNotFoundException {
		WoTConnectionImpl wotConnection = new WoTConnectionImpl(pluginRespirator);
		return wotConnection.getAllUntrustedIdentities(trusterId);
	}

	public Identity getIdentity(String identity, String truster) throws PluginNotFoundException {
		WoTConnectionImpl wotConnection = new WoTConnectionImpl(pluginRespirator);
		return wotConnection.getIdentity(identity, truster);
	}

	public boolean setProperty(String identity, String key, String value) throws PluginNotFoundException {
		WoTConnectionImpl wotConnection = new WoTConnectionImpl(pluginRespirator);
		return wotConnection.setProperty(identity, key, value);
	}

	public String getProperty(String identity, String key) throws PluginNotFoundException {
		WoTConnectionImpl wotConnection = new WoTConnectionImpl(pluginRespirator);
		return wotConnection.getProperty(identity, key);
	}

	public boolean setContext(String identity, String context) throws PluginNotFoundException {
		WoTConnectionImpl wotConnection = new WoTConnectionImpl(pluginRespirator);
		return wotConnection.setContext(identity, context);
	}
}
