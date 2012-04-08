/* ==================================================================
 * EnergyCapacityDatum.java - Oct 9, 2011 7:15:53 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.energy;

import net.solarnetwork.node.support.BaseDatum;

/**
 * A unit of data collected from a energy storage device.
 * 
 * @author matt
 * @version $Revision$
 */
public class EnergyCapacityDatum extends BaseDatum {

	private Float volts = null;			// this is the voltage of the energy source
	private Double ampHours = null;		// this is the storage level in amp hours of the energy source
	private Double wattHours = null;	// this is the storage level in watt hours of the energy source

	/**
	 * Default constructor.
	 */
	public EnergyCapacityDatum() {
		super();
	}

	/**
	 * Construct with primary key.
	 * @param id the primary key
	 */
	public EnergyCapacityDatum(Long id) {
		super(id);
	}

	/**
	 * Construct with values.
	 * 
	 * @param volts the volts
	 * @param ampHours the ampHours
	 */
	public EnergyCapacityDatum(Float volts, Double ampHours) {
		super();
		this.volts = volts;
		this.ampHours = ampHours;
	}

	@Override
	public String toString() {
		return "EnergyCapacityDatum{ampHours=" +this.ampHours
			+",volts=" +this.volts
			+'}';
	}
	
	public Float getVolts() {
		return volts;
	}
	public void setVolts(Float volts) {
		this.volts = volts;
	}
	public Double getAmpHours() {
		return ampHours;
	}
	public void setAmpHours(Double ampHours) {
		this.ampHours = ampHours;
	}
	public Double getWattHours() {
		return wattHours;
	}
	public void setWattHours(Double wattHours) {
		this.wattHours = wattHours;
	}

}
