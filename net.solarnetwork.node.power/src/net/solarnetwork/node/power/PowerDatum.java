/* ===================================================================
 * PowerDatum.java
 * 
 * Created Dec 1, 2009 4:21:59 PM
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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.power;

import net.solarnetwork.node.support.BaseDatum;

/**
 * A unit of data collected from a solar-electricity generating device.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class PowerDatum extends BaseDatum {

	private Long locationId = null;			// the price location
	private Float pvVolts = null;  			// this is the volts on the PV
	private Float pvAmps = null;			// this is the current in amps from the PV
	private Float batteryVolts = null;		// this is the volts on the battery
	private Double batteryAmpHours = null;	// this is the storage level in amp hours on the battery
	private Float dcOutputVolts = null;		// this is the dc volts output on the charger/inverter
	private Float dcOutputAmps = null;		// this is the dc current in amps on the charger/inverter
	private Float acOutputVolts = null;		// this is the ac volts output on the charger/inverter
	private Float acOutputAmps = null;		// this is the ac current in amps on the charger/inverter
	private Double ampHoursToday = null; 	// this is the amp hours generated today
	private Double kWattHoursToday = null;	// this is the kilowatt hours generated today
	
	/**
	 * Default constructor.
	 */
	public PowerDatum() {
		super();
	}

	/**
	 * Construct with an ID.
	 * @param id the ID to set
	 */
	public PowerDatum(Long id) {
		super(id);
	}

	/**
	 * Construct with values.
	 * 
	 * @param batteryAmpHours the battery amp hours
	 * @param batteryVolts the battery volts
	 * @param acOutputAmps the output amps
	 * @param acOutputVolts the output volts
	 * @param dcOutputAmps the output amps
	 * @param dcOutputVolts the output volts
	 * @param pvAmps the PV amps
	 * @param pvVolts the PV volts
	 * @param ampHoursToday the amp hours collected today
	 * @param kWattHoursToday the kilowatt hours collected today
	 */
	public PowerDatum(Double batteryAmpHours, Float batteryVolts,
			Float acOutputAmps, Float acOutputVolts,
			Float dcOutputAmps, Float dcOutputVolts,
			Float pvAmps, Float pvVolts, Double ampHoursToday, Double kWattHoursToday) {
		super();
		this.batteryAmpHours = batteryAmpHours;
		this.batteryVolts = batteryVolts;
		this.acOutputAmps = acOutputAmps;
		this.acOutputVolts = acOutputVolts;
		this.dcOutputAmps = dcOutputAmps;
		this.dcOutputVolts = dcOutputVolts;
		this.pvAmps = pvAmps;
		this.pvVolts = pvVolts;
		this.ampHoursToday = ampHoursToday;
		this.kWattHoursToday = kWattHoursToday;
	}

	@Override
	public String toString() {
		return "PowerDatum{pvAmps=" +this.pvAmps
			+",pvVolts=" +this.pvVolts
			+(this.batteryVolts == null ? "" : ",batVolts=" +this.batteryVolts)
			+(this.ampHoursToday == null ? "" : ",ampHoursToday=" +this.ampHoursToday)
			+(this.kWattHoursToday == null ? "" : ",kwHoursToday=" +this.kWattHoursToday)
			+'}';
	}
	
	/**
	 * @return the pvVolts
	 */
	public Float getPvVolts() {
		return pvVolts;
	}
	
	/**
	 * @param pvVolts the pvVolts to set
	 */
	public void setPvVolts(Float pvVolts) {
		this.pvVolts = pvVolts;
	}
	
	/**
	 * @return the pvAmps
	 */
	public Float getPvAmps() {
		return pvAmps;
	}
	
	/**
	 * @param pvAmps the pvAmps to set
	 */
	public void setPvAmps(Float pvAmps) {
		this.pvAmps = pvAmps;
	}
	
	/**
	 * @return the batteryVolts
	 */
	public Float getBatteryVolts() {
		return batteryVolts;
	}
	
	/**
	 * @param batteryVolts the batteryVolts to set
	 */
	public void setBatteryVolts(Float batteryVolts) {
		this.batteryVolts = batteryVolts;
	}
	
	/**
	 * @return the batteryAmpHours
	 */
	public Double getBatteryAmpHours() {
		return batteryAmpHours;
	}
	
	/**
	 * @param batteryAmpHours the batteryAmpHours to set
	 */
	public void setBatteryAmpHours(Double batteryAmpHours) {
		this.batteryAmpHours = batteryAmpHours;
	}
	
	/**
	 * @return the dcOutputVolts
	 */
	public Float getDcOutputVolts() {
		return dcOutputVolts;
	}
	
	/**
	 * @param dcOutputVolts the dcOutputVolts to set
	 */
	public void setDcOutputVolts(Float dcOutputVolts) {
		this.dcOutputVolts = dcOutputVolts;
	}
	
	/**
	 * @return the dcOutputAmps
	 */
	public Float getDcOutputAmps() {
		return dcOutputAmps;
	}
	
	/**
	 * @param dcOutputAmps the dcOutputAmps to set
	 */
	public void setDcOutputAmps(Float dcOutputAmps) {
		this.dcOutputAmps = dcOutputAmps;
	}
	
	/**
	 * @return the acOutputVolts
	 */
	public Float getAcOutputVolts() {
		return acOutputVolts;
	}
	
	/**
	 * @param acOutputVolts the acOutputVolts to set
	 */
	public void setAcOutputVolts(Float acOutputVolts) {
		this.acOutputVolts = acOutputVolts;
	}
	
	/**
	 * @return the acOutputAmps
	 */
	public Float getAcOutputAmps() {
		return acOutputAmps;
	}
	
	/**
	 * @param acOutputAmps the acOutputAmps to set
	 */
	public void setAcOutputAmps(Float acOutputAmps) {
		this.acOutputAmps = acOutputAmps;
	}
	
	/**
	 * @return the ampHoursToday
	 */
	public Double getAmpHoursToday() {
		return ampHoursToday;
	}
	
	/**
	 * @param ampHoursToday the ampHoursToday to set
	 */
	public void setAmpHoursToday(Double ampHoursToday) {
		this.ampHoursToday = ampHoursToday;
	}
	
	/**
	 * @return the kWattHoursToday
	 */
	public Double getKWattHoursToday() {
		return kWattHoursToday;
	}
	
	/**
	 * @param kWattHoursToday the kWattHoursToday to set
	 */
	public void setKWattHoursToday(Double kWattHoursToday) {
		this.kWattHoursToday = kWattHoursToday;
	}

	/**
	 * @return the locationId
	 */
	public Long getLocationId() {
		return locationId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}
	
}
