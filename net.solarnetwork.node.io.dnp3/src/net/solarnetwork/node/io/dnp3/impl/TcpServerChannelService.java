/* ==================================================================
 * TcpServerChannelService.java - 21/02/2019 5:51:37 pm
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

import java.util.ArrayList;
import java.util.List;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.DNP3Exception;
import com.automatak.dnp3.DNP3Manager;
import com.automatak.dnp3.IPEndpoint;
import com.automatak.dnp3.enums.ServerAcceptMode;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * TCP based server (outstation) channel service.
 *
 * @author matt
 * @version 2.0
 */
public class TcpServerChannelService extends AbstractChannelService<TcpServerChannelConfiguration>
		implements SettingSpecifierProvider {

	/**
	 * Constructor.
	 *
	 * @param manager
	 *        the manager
	 */
	public TcpServerChannelService(DNP3Manager manager) {
		super(manager, new TcpServerChannelConfiguration());
		setDisplayName("DNP3 server TCP");
	}

	@Override
	public Channel createChannel(TcpServerChannelConfiguration configuration) throws DNP3Exception {
		final String uid = getUid();
		if ( uid == null || uid.isBlank() ) {
			log.warn("Missing UID: can not start DPN3 server channel.");
			return null;
		}
		return getManager().addTCPServer(getUid(), configuration.getLogLevels(),
				ServerAcceptMode.CloseNew,
				new IPEndpoint(configuration.getBindAddress(), configuration.getPort()), this);
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTitleSettingSpecifier("status", getChannelStatusMessage(), true));

		result.addAll(basicIdentifiableSettings("", "", null));

		result.addAll(TcpServerChannelConfiguration.settings("config."));

		return result;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.dnp3.tcp";
	}

}
