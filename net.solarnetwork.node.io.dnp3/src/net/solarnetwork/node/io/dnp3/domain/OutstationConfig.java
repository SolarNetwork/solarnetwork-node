/* ==================================================================
 * OutstationConfig.java - 13/05/2019 7:24:35 am
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

package net.solarnetwork.node.io.dnp3.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Extension of {@link com.automatak.dnp3.OutstationConfig} to provide JavaBean
 * accessors to make it configurable via settings.
 *
 * @author matt
 * @version 2.0
 * @since 1.1
 */
public class OutstationConfig extends com.automatak.dnp3.OutstationConfig {

	/**
	 * Constructor.
	 */
	public OutstationConfig() {
		super();
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the configuration to copy
	 * @since 2.0
	 */
	public OutstationConfig(com.automatak.dnp3.OutstationConfig other) {
		super();
		copySettings(other, this);
	}

	/**
	 * Copy the link outstation configuration from one object to another.
	 *
	 * @param from
	 *        the settings to copy
	 * @param to
	 *        the destination to copy the settings to
	 * @since 2.0
	 */
	public static void copySettings(com.automatak.dnp3.OutstationConfig from,
			com.automatak.dnp3.OutstationConfig to) {
		to.allowUnsolicited = from.allowUnsolicited;
		to.maxControlsPerRequest = from.maxControlsPerRequest;
		to.maxRxFragSize = from.maxRxFragSize;
		to.maxTxFragSize = from.maxTxFragSize;
		to.selectTimeout = from.selectTimeout;
		to.solConfirmTimeout = from.solConfirmTimeout;
		to.noDefferedReadDuringUnsolicitedNullResponse = from.noDefferedReadDuringUnsolicitedNullResponse;
		to.numUnsolRetries = from.numUnsolRetries;
		to.unsolConfirmTimeout = from.unsolConfirmTimeout;
		to.typesAllowedInClass0 = from.typesAllowedInClass0;
	}

	/**
	 * Get settings suitable for configuring an instance of
	 * {@link OutstationConfig}.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @param defaults
	 *        the default settings
	 * @return the settings, never {@literal null}
	 * @since 2.0
	 */
	public static List<SettingSpecifier> outstationSettings(String prefix, OutstationConfig defaults) {
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxControlsPerRequest",
				String.valueOf(defaults.maxControlsPerRequest)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxRxFragSize",
				String.valueOf(defaults.maxRxFragSize)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxTxFragSize",
				String.valueOf(defaults.maxTxFragSize)));
		return results;
	}

	/**
	 * Get the maximum controls per request count.
	 *
	 * @return the count
	 */
	public long getMaxControlsPerRequest() {
		return maxControlsPerRequest;
	}

	/**
	 * Set the maximum controls per request count.
	 *
	 * @param maxControlsPerRequest
	 *        the count to set
	 */
	public void setMaxControlsPerRequest(long maxControlsPerRequest) {
		this.maxControlsPerRequest = maxControlsPerRequest;
	}

	/**
	 * Get the select timeout.
	 *
	 * @return the timeout
	 */
	public Duration getSelectTimeout() {
		return selectTimeout;
	}

	/**
	 * Set the select timeout.
	 *
	 * @param selectTimeout
	 *        the timeout to set
	 */
	public void setSelectTimeout(Duration selectTimeout) {
		this.selectTimeout = selectTimeout;
	}

	/**
	 * Get the select timeout, in seconds.
	 *
	 * @return the timeout, in seconds
	 */
	public int getSelectTimeoutSecs() {
		Duration d = getSelectTimeout();
		return (d != null ? (int) (d.toMillis() / 1000L) : 0);
	}

	/**
	 * Set the select timeout, in seconds.
	 *
	 * @param secs
	 *        the seconds to set
	 */
	public void setSelectTimeoutSecs(int secs) {
		setSelectTimeout(Duration.ofSeconds(secs));
	}

	/**
	 * Get the solicited confirmation timeout.
	 *
	 * @return the timeout
	 */
	public Duration getSolConfirmTimeout() {
		return solConfirmTimeout;
	}

	/**
	 * Set the solicited confirmation timeout.
	 *
	 * @param solConfirmTimeout
	 *        the timeout to set
	 */
	public void setSolConfirmTimeout(Duration solConfirmTimeout) {
		this.solConfirmTimeout = solConfirmTimeout;
	}

	/**
	 * Get the solicited confirmation timeout, in seconds.
	 *
	 * @return the timeout, in seconds
	 */
	public int getSolConfirmTimeoutSecs() {
		Duration d = getSolConfirmTimeout();
		return (d != null ? (int) (d.toMillis() / 1000L) : 0);
	}

	/**
	 * Set the solicited confirmation timeout, in seconds.
	 *
	 * @param secs
	 *        the seconds to set
	 */
	public void setSolConfirmTimeoutSecs(int secs) {
		setSolConfirmTimeout(Duration.ofSeconds(secs));
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

	/**
	 * Get the allow unsolicited mode.
	 *
	 * @return {@literal true} to allow unsolicited mode
	 */
	public boolean isAllowUnsolicited() {
		return allowUnsolicited;
	}

	/**
	 * Set the allow unsolicited mode.
	 *
	 * @param allowUnsolicited
	 *        {@literal true} to allow unsolicited mode
	 */
	public void setAllowUnsolicited(boolean allowUnsolicited) {
		this.allowUnsolicited = allowUnsolicited;
	}

}
