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

import net.solarnetwork.node.domain.BaseEnergyDatum;

/**
 * Domain object for energy consumption related data.
 * 
 * <p>
 * The {@code sourceId} value is used to differentiate between multiple
 * consumption monitors within the same system.
 * </p>
 * 
 * @author matt
 * @version 1.3
 */
public class ConsumptionDatum extends BaseEnergyDatum {

	private Long locationId = null; // price location ID

	// these are for backwards compatibility only
	private Float amps = null;
	private Float volts = null;

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

	/**
	 * Construct with a watt value.
	 * 
	 * @param sourceId
	 *        the source ID
	 * @param watts
	 *        the watts
	 */
	public ConsumptionDatum(String sourceId, Integer watts) {
		super();
		setSourceId(sourceId);
		setWatts(watts);
	}

	@Deprecated
	public Float getAmps() {
		return amps;
	}

	@Deprecated
	public void setAmps(Float amps) {
		this.amps = amps;
	}

	@Deprecated
	public Float getVolts() {
		return volts;
	}

	@Deprecated
	public void setVolts(Float volts) {
		this.volts = volts;
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	/**
	 * Get the watts.
	 * 
	 * <p>
	 * This will return the {@code watts} value if available, or fall back to
	 * {@code amps * volts}.
	 * </p>
	 * 
	 * @return watts, or <em>null</em> if watts not available and either amps or
	 *         volts are null
	 */
	@Override
	public Integer getWatts() {
		Integer watts = super.getWatts();
		if ( watts != null ) {
			return watts;
		}
		if ( amps == null || volts == null ) {
			return null;
		}
		return Integer.valueOf((int) Math.round(amps.doubleValue() * volts.doubleValue()));
	}

}
