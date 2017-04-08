/* ==================================================================
 * ChargeConfiguration.java - 25/03/2017 11:43:12 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp;

/**
 * Configuration properties supported by SolarNode.
 * 
 * @author matt
 * @version 1.0
 * @since 0.6
 */
public interface ChargeConfiguration {

	/**
	 * Get the heart beat interval, in seconds. A value of {@code 0} indicates
	 * no heart beat should be used.
	 * 
	 * @return the heart beat interval
	 */
	int getHeartBeatInterval();

	/**
	 * Get the interval at which to sample meter values during a charge session,
	 * in seconds. A value of {@code 0} indicates no samples should be taken.
	 * 
	 * @return the meter value sample interval
	 */
	int getMeterValueSampleInterval();

	/**
	 * Test if this configuration differs in any way from another instance.
	 * 
	 * @param config
	 *        The other configuration to compare to.
	 * @return {@code true} if any properties differ between the two
	 *         configurations
	 */
	boolean differsFrom(ChargeConfiguration config);

}
