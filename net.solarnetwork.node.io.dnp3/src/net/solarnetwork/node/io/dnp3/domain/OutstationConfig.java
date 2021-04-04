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
import com.automatak.dnp3.enums.IndexMode;

/**
 * Extension of {@link com.automatak.dnp3.OutstationConfig} to provide JavaBean
 * accessors to make it configurable via settings.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class OutstationConfig extends com.automatak.dnp3.OutstationConfig {

	public IndexMode getIndexMode() {
		return indexMode;
	}

	public void setIndexMode(IndexMode indexMode) {
		this.indexMode = indexMode;
	}

	public short getMaxControlsPerRequest() {
		return maxControlsPerRequest;
	}

	public void setMaxControlsPerRequest(short maxControlsPerRequest) {
		this.maxControlsPerRequest = maxControlsPerRequest;
	}

	public Duration getSelectTimeout() {
		return selectTimeout;
	}

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

	public Duration getSolConfirmTimeout() {
		return solConfirmTimeout;
	}

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

	public Duration getUnsolRetryTimeout() {
		return unsolRetryTimeout;
	}

	public void setUnsolRetryTimeout(Duration unsolRetryTimeout) {
		this.unsolRetryTimeout = unsolRetryTimeout;
	}

	/**
	 * Get the unsolicited retry timeout, in seconds.
	 * 
	 * @return the timeout, in seconds
	 */
	public int getUnsolRetryTimeoutSecs() {
		Duration d = getUnsolRetryTimeout();
		return (d != null ? (int) (d.toMillis() / 1000L) : 0);
	}

	/**
	 * Set the unsolicited retry timeout, in seconds.
	 * 
	 * @param secs
	 *        the seconds to set
	 */
	public void setUnsolRetryTimeoutSecs(int secs) {
		setUnsolRetryTimeout(Duration.ofSeconds(secs));
	}

	public int getMaxTxFragSize() {
		return maxTxFragSize;
	}

	public void setMaxTxFragSize(int maxTxFragSize) {
		this.maxTxFragSize = maxTxFragSize;
	}

	public int getMaxRxFragSize() {
		return maxRxFragSize;
	}

	public void setMaxRxFragSize(int maxRxFragSize) {
		this.maxRxFragSize = maxRxFragSize;
	}

	public boolean isAllowUnsolicited() {
		return allowUnsolicited;
	}

	public void setAllowUnsolicited(boolean allowUnsolicited) {
		this.allowUnsolicited = allowUnsolicited;
	}

}
