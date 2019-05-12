/* ==================================================================
 * LinkLayerConfig.java - 13/05/2019 7:26:35 am
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
 * Extension of {@link com.automatak.dnp3.LinkLayerConfig} to provide JavaBean
 * accessors to make it configurable via settings.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class LinkLayerConfig extends com.automatak.dnp3.LinkLayerConfig {

	/**
	 * Constructor.
	 * 
	 * @param isMaster
	 *        the master flag
	 */
	public LinkLayerConfig(boolean isMaster) {
		super(isMaster);
	}

	public boolean isMaster() {
		return isMaster;
	}

	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}

	public boolean isUseConfirms() {
		return useConfirms;
	}

	public void setUseConfirms(boolean useConfirms) {
		this.useConfirms = useConfirms;
	}

	public int getNumRetry() {
		return numRetry;
	}

	public void setNumRetry(int numRetry) {
		this.numRetry = numRetry;
	}

	public int getLocalAddr() {
		return localAddr;
	}

	public void setLocalAddr(int localAddr) {
		this.localAddr = localAddr;
	}

	public int getRemoteAddr() {
		return remoteAddr;
	}

	public void setRemoteAddr(int remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public Duration getResponseTimeout() {
		return responseTimeout;
	}

	public void setResponseTimeout(Duration responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

	/**
	 * Get the response timeout, in seconds.
	 * 
	 * @return the response timeout, in seconds
	 */
	public int getResponseTimeoutSecs() {
		Duration d = getResponseTimeout();
		return (d != null ? (int) (d.toMillis() / 1000L) : 0);
	}

	/**
	 * Set the response timeout, in seconds.
	 * 
	 * @param secs
	 *        the seconds to set
	 */
	public void setResponseTimeoutSecs(int secs) {
		setResponseTimeout(Duration.ofSeconds(secs));
	}

	public Duration getKeepAliveTimeout() {
		return keepAliveTimeout;
	}

	public void setKeepAliveTimeout(Duration keepAliveTimeout) {
		this.keepAliveTimeout = keepAliveTimeout;
	}

	/**
	 * Get the keep-alive timeout, in seconds.
	 * 
	 * @return the keep-alive timeout, in seconds
	 */
	public int getKeepAliveTimeoutSecs() {
		Duration d = getKeepAliveTimeout();
		return (d != null ? (int) (d.toMillis() / 1000L) : 0);
	}

	/**
	 * Set the keep-alive timeout, in seconds.
	 * 
	 * @param secs
	 *        the seconds to set
	 */
	public void setKeepAliveTimeoutSecs(int secs) {
		setKeepAliveTimeout(Duration.ofSeconds(secs));
	}

}
