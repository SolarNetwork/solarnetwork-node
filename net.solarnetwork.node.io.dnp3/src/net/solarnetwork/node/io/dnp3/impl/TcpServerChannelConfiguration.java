/* ==================================================================
 * TcpServerChannelConfiguration.java - 22/02/2019 5:40:06 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.io.dnp3.impl;

import java.util.List;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A set of configuration options for a TCP server (outstation) based DNP3
 * channel.
 *
 * @author matt
 * @version 2.0
 */
public class TcpServerChannelConfiguration extends BaseChannelConfiguration {

	/** The default bind address. */
	public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";

	/** The default port. */
	public static final int DEFAULT_PORT = 20000;

	private String bindAddress = DEFAULT_BIND_ADDRESS;
	private int port = DEFAULT_PORT;

	/**
	 * Constructor.
	 */
	public TcpServerChannelConfiguration() {
		super();
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = BaseChannelConfiguration.settings(prefix);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "bindAddress", DEFAULT_BIND_ADDRESS));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "port", String.valueOf(DEFAULT_PORT)));

		return results;
	}

	@Override
	public List<SettingSpecifier> defaultSettings(String prefix) {
		return settings(prefix);
	}

	/**
	 * Get the bind address.
	 *
	 * @return the address
	 */
	public String getBindAddress() {
		return bindAddress;
	}

	/**
	 * Set the bind address.
	 *
	 * @param bindAddress
	 *        the address to set
	 */
	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

	/**
	 * Get the listen port.
	 *
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the listen port.
	 *
	 * @param port
	 *        the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

}
