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
