/* ==================================================================
 * SimpleChargeConfiguration.java - 25/03/2017 11:55:16 AM
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

package net.solarnetwork.node.ocpp.support;

import net.solarnetwork.node.ocpp.ChargeConfiguration;

/**
 * Basic implementation of {@link ChargeConfiguration}.
 * 
 * @author matt
 * @version 1.0
 * @since 0.6
 */
public class SimpleChargeConfiguration implements ChargeConfiguration {

	private int heartBeatInterval;
	private int meterValueSampleInterval;

	/**
	 * Default constructor.
	 */
	public SimpleChargeConfiguration() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param config
	 *        The config to copy properties from.
	 */
	public SimpleChargeConfiguration(ChargeConfiguration config) {
		super();
		this.heartBeatInterval = config.getHeartBeatInterval();
		this.meterValueSampleInterval = config.getHeartBeatInterval();
	}

	@Override
	public int getHeartBeatInterval() {
		return heartBeatInterval;
	}

	@Override
	public int getMeterValueSampleInterval() {
		return meterValueSampleInterval;
	}

	/**
	 * Set the heart beat interval.
	 * 
	 * @param heartBeatInterval
	 *        the interval to set
	 */
	public void setHeartBeatInterval(int heartBeatInterval) {
		this.heartBeatInterval = heartBeatInterval;
	}

	/**
	 * Set the meter value sample interval.
	 * 
	 * @param meterValueSampleInterval
	 *        the interval to set
	 */
	public void setMeterValueSampleInterval(int meterValueSampleInterval) {
		this.meterValueSampleInterval = meterValueSampleInterval;
	}

	@Override
	public boolean differsFrom(ChargeConfiguration config) {
		return !equals(config);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + heartBeatInterval;
		result = prime * result + meterValueSampleInterval;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof SimpleChargeConfiguration) ) {
			return false;
		}
		SimpleChargeConfiguration other = (SimpleChargeConfiguration) obj;
		if ( heartBeatInterval != other.heartBeatInterval ) {
			return false;
		}
		if ( meterValueSampleInterval != other.meterValueSampleInterval ) {
			return false;
		}
		return true;
	}

}
