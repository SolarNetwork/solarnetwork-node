/* ===================================================================
 * ConsumptionDatum.java
 * 
 * Created Dec 4, 2009 9:10:47 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.node.consumption;

import net.solarnetwork.node.support.BaseDatum;

/**
 * Domain object for energy consumption related data.
 * 
 * <p>
 * The {@code sourceId} value is used to differentiate between multiple
 * consumption monitors within the same system.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class ConsumptionDatum extends BaseDatum {

	private Long locationId = null; // price location ID
	private Float amps = null;
	private Float volts = null;
	private Long wattHourReading = null;

	/**
	 * Default constructor.
	 */
	public ConsumptionDatum() {
		this(null, null, null);
	}

	/**
	 * Construct with amp and volt values.
	 * 
	 * @param sourceId
	 *        the source ID
	 * @param amps
	 *        the amps
	 * @param volts
	 *        the volts
	 */
	public ConsumptionDatum(String sourceId, Float amps, Float volts) {
		super();
		setSourceId(sourceId);
		this.amps = amps;
		this.volts = volts;
	}

	@Override
	public String toString() {
		return "ConsumptionDatum{sourceId=" + getSourceId() + ",amps=" + this.amps + ",volts="
				+ this.volts + ",wattHourReading=" + this.wattHourReading + '}';
	}

	public Float getAmps() {
		return amps;
	}

	public void setAmps(Float amps) {
		this.amps = amps;
	}

	public Float getVolts() {
		return volts;
	}

	public void setVolts(Float volts) {
		this.volts = volts;
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public Long getWattHourReading() {
		return wattHourReading;
	}

	public void setWattHourReading(Long wattHourReading) {
		this.wattHourReading = wattHourReading;
	}

}
