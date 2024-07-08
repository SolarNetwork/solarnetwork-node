/* ==================================================================
 * BaseChannelConfiguration.java - 21/02/2019 8:34:15 pm
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.automatak.dnp3.LogMasks;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A basic set of configuration options for a DNP3 channel.
 *
 * @author matt
 * @version 2.0
 */
public class BaseChannelConfiguration {

	/** Default log levels. */
	public static final int DEFAULT_LOG_LEVELS = LogMasks.NORMAL;

	/** Default min retry delay. */
	public static final Duration DEFAULT_MIN_RETRY_DELAY = Duration.ofSeconds(1);

	/** Default max retry delay. */
	public static final Duration DEFAULT_MAX_RETRY_DELAY = Duration.ofMinutes(1);

	private int logLevels = DEFAULT_LOG_LEVELS;
	private Duration minRetryDelay = DEFAULT_MIN_RETRY_DELAY;
	private Duration maxRetryDelay = DEFAULT_MAX_RETRY_DELAY;

	/**
	 * Constructor.
	 */
	public BaseChannelConfiguration() {
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
		List<SettingSpecifier> results = new ArrayList<>(6);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "logLevels",
				String.valueOf(DEFAULT_LOG_LEVELS)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "minRetryDelaySecs",
				String.valueOf(DEFAULT_MIN_RETRY_DELAY.toMillis() / 1000)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxRetryDelaySecs",
				String.valueOf(DEFAULT_MAX_RETRY_DELAY.toMillis() / 1000)));

		return results;
	}

	/**
	 * Instance alias for {@link BaseChannelConfiguration#settings(String)}.
	 *
	 * @param prefix
	 *        the prefix
	 * @return the settings
	 */
	public List<SettingSpecifier> defaultSettings(String prefix) {
		return settings(prefix);
	}

	/**
	 * Get the log levels.
	 *
	 * @return the log levels
	 */
	public int getLogLevels() {
		return logLevels;
	}

	/**
	 * Set the DNP3 log levels bitmask.
	 *
	 *
	 * @param logLevels
	 *        the log levels value to set
	 * @see com.automatak.dnp3.LogLevels
	 * @see com.automatak.dnp3.LogMasks
	 */
	public void setLogLevels(int logLevels) {
		this.logLevels = logLevels;
	}

	/**
	 * Get the minimum retry delay.
	 *
	 * @return the delay
	 */
	public Duration getMinRetryDelay() {
		return minRetryDelay;
	}

	/**
	 * Set the minimum retry delay.
	 *
	 * @param minRetryDelay
	 *        the delay to set
	 */
	public void setMinRetryDelay(Duration minRetryDelay) {
		this.minRetryDelay = minRetryDelay;
	}

	/**
	 * Get the maximum retry delay.
	 *
	 * @return the delay
	 */
	public Duration getMaxRetryDelay() {
		return maxRetryDelay;
	}

	/**
	 * Set the maximum retry delay.
	 *
	 * @param maxRetryDelay
	 *        the delay to set
	 */
	public void setMaxRetryDelay(Duration maxRetryDelay) {
		this.maxRetryDelay = maxRetryDelay;
	}

	/**
	 * Get the min retry delay as a number of seconds.
	 *
	 * @return the number of seconds
	 */
	public int getMinRetryDelaySecs() {
		Duration d = getMinRetryDelay();
		return (d != null ? (int) (d.toMillis() / 1000) : 0);
	}

	/**
	 * Set the min retry delay as a number of seconds.
	 *
	 * @param minRetryDelaySecs
	 *        the number of seconds
	 */
	public void setMinRetryDelaySecs(int minRetryDelaySecs) {
		setMinRetryDelay(Duration.ofSeconds(minRetryDelaySecs));
	}

	/**
	 * Get the max retry delay as a number of seconds.
	 *
	 * @return the number of seconds
	 */
	public int getMaxRetryDelaySecs() {
		Duration d = getMaxRetryDelay();
		return (d != null ? (int) (d.toMillis() / 1000) : 0);
	}

	/**
	 * Set the max retry delay as a number of seconds.
	 *
	 * @param maxRetryDelaySecs
	 *        the number of seconds
	 */
	public void setMaxRetryDelaySecs(int maxRetryDelaySecs) {
		setMaxRetryDelay(Duration.ofSeconds(maxRetryDelaySecs));
	}

}
