/* ==================================================================
 * AbstractModbusConnection.java - 24/03/2018 9:11:50 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus;

/**
 * Base class for Modbus connections.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractModbusConnection {

	private final int unitId;
	private final boolean headless;
	private int retries = 3;
	private long retryDelayMs = 0;

	public AbstractModbusConnection(int unitId, boolean headless) {
		super();
		this.unitId = unitId;
		this.headless = headless;
	}

	/**
	 * Get the unit ID.
	 * 
	 * @return the unit ID
	 */
	public int getUnitId() {
		return unitId;
	}

	/**
	 * Get the headless mode.
	 * 
	 * @return {@literal true} if in headless mode
	 */
	public boolean isHeadless() {
		return headless;
	}

	/**
	 * Get the number of "retries" to perform on each transaction in the event
	 * of errors.
	 * 
	 * @return the number of retries; defaults to {@literal 3}
	 */
	public int getRetries() {
		return retries;
	}

	/**
	 * Set the number of "retries" to perform on each transaction in the event
	 * of errors.
	 * 
	 * @param retries
	 *        the number of retries
	 */
	public void setRetries(int retries) {
		this.retries = retries;
	}

	/**
	 * Get a retry delay, in milliseconds.
	 * 
	 * @return the retry delay
	 */
	public long getRetryDelayMs() {
		return retryDelayMs;
	}

	/**
	 * Set a retry delay, in milliseconds.
	 * 
	 * @param retryDelayMs
	 *        the retry delay to set
	 */
	public void setRetryDelayMs(long retryDelayMs) {
		this.retryDelayMs = retryDelayMs;
	}

}
