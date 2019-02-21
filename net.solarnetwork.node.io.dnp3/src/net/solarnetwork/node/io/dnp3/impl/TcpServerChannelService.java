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
import com.automatak.dnp3.ChannelListener;
import com.automatak.dnp3.ChannelRetry;
import com.automatak.dnp3.DNP3Exception;
import com.automatak.dnp3.DNP3Manager;
import net.solarnetwork.dnp3.util.Slf4jChannelListener;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * TCP based server (outstation) channel service.
 * 
 * @author matt
 * @version 1.0
 */
public class TcpServerChannelService extends AbstractChannelService<TcpServerChannelConfiguration>
		implements SettingSpecifierProvider {

	/** The default uid value. */
	public static final String DEFAULT_UID = "DNP3 server TCP";

	/**
	 * Constructor.
	 * 
	 * @param manager
	 *        the manager
	 */
	public TcpServerChannelService(DNP3Manager manager) {
		super(manager, new TcpServerChannelConfiguration());
		setUid(DEFAULT_UID);
	}

	@Override
	public Channel createChannel(TcpServerChannelConfiguration configuration) throws DNP3Exception {
		ChannelRetry retryConfig = new ChannelRetry(configuration.getMinRetryDelay(),
				configuration.getMaxRetryDelay());
		ChannelListener listener = new Slf4jChannelListener(getUid());
		return getManager().addTCPServer(getUid(), configuration.getLogLevels(), retryConfig,
				configuration.getBindAddress(), configuration.getPort(), listener);
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTextFieldSettingSpecifier("uid", DEFAULT_UID));
		result.add(new BasicTextFieldSettingSpecifier("groupUID", ""));

		result.addAll(TcpServerChannelConfiguration.settings("config."));

		return result;
	}

	@Override
	public String getDisplayName() {
		return DEFAULT_UID;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.dnp3.tcp";
	}

}
