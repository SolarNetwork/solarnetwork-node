/* ==================================================================
 * EnaSolarPowerDatum.java - Nov 16, 2013 6:41:18 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.egauge.ws;

import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;

/**
 * Extension of {@link PowerDatum} to map eGauge data appropriately.
 * 
 * <dl>
 * 
 * <dt>SolarPlusWatts (Watts)</dt>
 * <dd>instantaneous power in watts in terms of total output of the inverter
 * right now</dd>
 * 
 * <dt>SolarPlusWattHourReading (WattHourReading)</dt>
 * <dd>total watt-hours of energy produced by the solar inverter, read from the
 * "Solar" register</dd>
 * 
 * <dt>GridWatts (DCPower)</dt>
 * <dd>instantaneous power in watts in terms of total load represented by the
 * building as defined by the "Grid" register.</dd>
 * 
 * <dt>GridWattHourReading</dt>
 * <dd>total watt-hours of energy used by the building according to this meter,
 * read from the "Total Usage" register</dd>
 * 
 * </dl>
 * 
 * @author maxieduncan
 * @version 1.2
 */
public class EGaugePowerDatum extends GeneralNodePVEnergyDatum {

	/** Stores the "Grid" register watt hour reading. */
	private static final String GRID_WATT_HOUR_READING_KEY = "gridWattHourReading";

	/**
	 * Delegates to {@link #getWatts()}
	 * 
	 * @return the Solar+ instantaneous Watts reading
	 */
	public Integer getSolarPlusWatts() {
		return super.getWatts();
	}

	/**
	 * Delegates to {@link #setWatts(Integer)}
	 * 
	 * @param the
	 *        Solar+ instantaneous Watts reading
	 */
	public void setSolarPlusWatts(Integer watts) {
		super.setWatts(watts);
	}

	/**
	 * Delegates to {@link #getDCPower()}
	 * 
	 * @return the Grid instantaneous Watts reading
	 */
	public Integer getGridWatts() {
		return super.getDCPower();
	}

	/**
	 * Delegates to {@link #setDCPower(Integer)}
	 * 
	 * @param the
	 *        Grid instantaneous Watts reading
	 */
	public void setGridWatts(Integer watts) {
		super.setDCPower(watts);
	}

	/**
	 * Delegates to {@link #getSolarPlusWattHourReading()}
	 * 
	 * @return the Solar+ Watt-hours reading
	 */
	public Long getSolarPlusWattHourReading() {
		return super.getWattHourReading();
	}

	/**
	 * Delegates to {@link #setSolarPlusWattHourReading(Longong)}
	 * 
	 * @param the
	 *        Solar+ Watt-hours reading
	 */
	public void setSolarPlusWattHourReading(Long wattHourReading) {
		super.setWattHourReading(wattHourReading);
	}

	/**
	 * Gets the Grid Watt-hours reading.
	 * 
	 * @return the Grid Watt-hours reading
	 */
	public Long getGridWattHourReading() {
		return getAccumulatingSampleLong(GRID_WATT_HOUR_READING_KEY);
	}

	/**
	 * Sets the Grid Watt-hours reading.
	 * 
	 * @param gridWattHourReading
	 *        the Grid Watt-hours reading
	 */
	public void setGridWattHourReading(Long wattHourReading) {
		putAccumulatingSampleValue(GRID_WATT_HOUR_READING_KEY, wattHourReading);
	}

}
