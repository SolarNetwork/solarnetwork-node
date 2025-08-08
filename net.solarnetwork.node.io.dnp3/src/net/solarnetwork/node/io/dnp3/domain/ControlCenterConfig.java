/* ==================================================================
 * ControlCenterConfig.java - 7/08/2025 2:58:30 pm
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

package net.solarnetwork.node.io.dnp3.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.automatak.dnp3.MasterConfig;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Extension of {@link com.automatak.dnp3.MasterConfig} to provide JavaBean
 * accessors to make it configurable via settings.
 *
 * @author matt
 * @version 1.0
 */
public class ControlCenterConfig extends MasterConfig {

	/**
	 * Constructor.
	 */
	public ControlCenterConfig() {
		super();
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the configuration to copy
	 */
	public ControlCenterConfig(MasterConfig other) {
		super();
		copySettings(other, this);
	}

	/**
	 * Copy the configuration from one object to another.
	 *
	 * @param from
	 *        the settings to copy
	 * @param to
	 *        the destination to copy the settings to
	 */
	public static void copySettings(com.automatak.dnp3.MasterConfig from,
			com.automatak.dnp3.MasterConfig to) {
		to.responseTimeout = from.responseTimeout;
		to.timeSyncMode = from.timeSyncMode;
		to.disableUnsolOnStartup = from.disableUnsolOnStartup;
		to.ignoreRestartIIN = from.ignoreRestartIIN;
		to.unsolClassMask = from.unsolClassMask;
		to.startupIntegrityClassMask = from.startupIntegrityClassMask;
		to.integrityOnEventOverflowIIN = from.integrityOnEventOverflowIIN;
		to.eventScanOnEventsAvailableClassMask = from.eventScanOnEventsAvailableClassMask;
		to.taskRetryPeriod = from.taskRetryPeriod;
		to.taskStartTimeout = from.taskStartTimeout;
		to.maxRxFragSize = from.maxRxFragSize;
		to.maxTxFragSize = from.maxTxFragSize;
		to.controlQualifierMode = from.controlQualifierMode;
	}

	/**
	 * Get settings suitable for configuring an instance of
	 * {@link ControlCenterConfig}.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @param defaults
	 *        the default settings
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> controlCenterSettings(String prefix,
			ControlCenterConfig defaults) {
		List<SettingSpecifier> results = new ArrayList<>(8);
		ControlCenterConfig config = new ControlCenterConfig();
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxRxFragSize",
				String.valueOf(config.maxRxFragSize)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxTxFragSize",
				String.valueOf(config.maxTxFragSize)));
		return results;
	}

	/**
	 * Get the response timeout.
	 *
	 * @return the timeout
	 */
	public Duration getResponseTimeout() {
		return responseTimeout;
	}

	/**
	 * Set the response timeout.
	 *
	 * @param responseTimeout
	 *        the timeout to set; if {@code null} then a default of 5 seconds
	 *        will be used
	 */
	public void setResponseTimeout(Duration responseTimeout) {
		this.responseTimeout = responseTimeout != null ? responseTimeout : Duration.ofSeconds(5);
	}

	/**
	 * Get the maximum transmit fragment size.
	 *
	 * @return the size
	 */
	public int getMaxTxFragSize() {
		return maxTxFragSize;
	}

	/**
	 * Set the maximum transmit fragment size.
	 *
	 * @param maxTxFragSize
	 *        the size to set
	 */
	public void setMaxTxFragSize(int maxTxFragSize) {
		this.maxTxFragSize = maxTxFragSize;
	}

	/**
	 * Get the maximum receive fragment size.
	 *
	 * @return the size
	 */
	public int getMaxRxFragSize() {
		return maxRxFragSize;
	}

	/**
	 * Set the maximum receive fragment size.
	 *
	 * @param maxRxFragSize
	 *        the size to set
	 */
	public void setMaxRxFragSize(int maxRxFragSize) {
		this.maxRxFragSize = maxRxFragSize;
	}

}
