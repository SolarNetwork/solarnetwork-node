/* ==================================================================
 * TcpClientChannelService.java - 7/08/2025 11:53:20 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import java.util.ArrayList;
import java.util.List;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.ChannelRetry;
import com.automatak.dnp3.DNP3Exception;
import com.automatak.dnp3.DNP3Manager;
import com.automatak.dnp3.IPEndpoint;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * TCP based client (control center) channel service.
 *
 * @author matt
 * @version 1.0
 */
public class TcpClientChannelService extends AbstractChannelService<TcpClientChannelConfiguration>
		implements SettingSpecifierProvider {

	/**
	 * Constructor.
	 *
	 * @param manager
	 *        the manager
	 */
	public TcpClientChannelService(DNP3Manager manager) {
		super(manager, new TcpClientChannelConfiguration());
		setDisplayName("DNP3 client");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.dnp3.client.tcp";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTitleSettingSpecifier("status", getChannelStatusMessage(), true));

		result.addAll(basicIdentifiableSettings("", "", null));

		result.addAll(TcpClientChannelConfiguration.settings("config."));

		return result;
	}

	@Override
	protected Channel createChannel(TcpClientChannelConfiguration configuration) throws DNP3Exception {
		final String uid = getUid();
		if ( uid == null || uid.isBlank() ) {
			log.warn("Missing UID: can not start DNP3 client channel.");
			return null;
		}
		final String host = configuration.getHost();
		if ( host == null || host.isBlank() ) {
			log.warn("Missing host: can not start DNP3 client channel [{}]", uid);
			return null;
		}
		return getManager().addTCPClient(getUid(), configuration.getLogLevels(),
				ChannelRetry.getDefault(),
				List.of(new IPEndpoint(configuration.getHost(), configuration.getPort())), "0.0.0.0",
				this);
	}

}
