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
 */

package net.solarnetwork.node.power;

import net.solarnetwork.node.support.BaseDatum;

/**
 * A unit of data collected from a solar-electricity generating device.
 * 
 * @author matt
 * @version 1.3
 */
public class PowerDatum extends BaseDatum {

	private Long locationId = null; // the price location
	private Integer watts = null; // the watts being generated
	private Float batteryVolts = null; // this is the volts on the battery
	private Double batteryAmpHours = null; // this is the storage level in amp hours on the battery
	private Float dcOutputVolts = null; // this is the dc volts output on the charger/inverter
	private Float dcOutputAmps = null; // this is the dc current in amps on the charger/inverter
	private Float acOutputVolts = null; // this is the ac volts output on the charger/inverter
	private Float acOutputAmps = null; // this is the ac current in amps on the charger/inverter
	private Double ampHourReading = null;
	private Long wattHourReading = null;

	// these are for backwards compatibility only
	private Float pvVolts = null; // this is the volts on the PV
	private Float pvAmps = null; // this is the current in amps from the PV

	/**
	 * Default constructor.
	 */
	public PowerDatum() {
		super();
	}

	/**
	 * Construct with an ID.
	 * 
	 * @param id
	 *        the ID to set
	 */
	public PowerDatum(Long id) {
		super(id);
	}

	/**
	 * Construct with values.
	 * 
	 * @param batteryAmpHours
	 *        the battery amp hours
	 * @param batteryVolts
	 *        the battery volts
	 * @param acOutputAmps
	 *        the output amps
	 * @param acOutputVolts
	 *        the output volts
	 * @param dcOutputAmps
	 *        the output amps
	 * @param dcOutputVolts
	 *        the output volts
	 * @param pvAmps
	 *        the PV amps
	 * @param pvVolts
	 *        the PV volts
	 * @param ampHourReading
	 *        the amp hours collected
	 * @param kWattHoursToday
	 *        the kilowatt hours collected today
	 */
	public PowerDatum(Double batteryAmpHours, Float batteryVolts, Float acOutputAmps,
			Float acOutputVolts, Float dcOutputAmps, Float dcOutputVolts, Integer watts,
			Double ampHourReading, Double kWattHoursToday) {
		super();
		this.batteryAmpHours = batteryAmpHours;
		this.batteryVolts = batteryVolts;
		this.acOutputAmps = acOutputAmps;
		this.acOutputVolts = acOutputVolts;
		this.dcOutputAmps = dcOutputAmps;
		this.dcOutputVolts = dcOutputVolts;
		this.watts = watts;
		this.ampHourReading = ampHourReading;
		setKWattHoursToday(kWattHoursToday);
	}

	@Override
	public String toString() {
		return "PowerDatum{watts=" + getWatts()
				+ (this.batteryVolts == null ? "" : ",batVolts=" + this.batteryVolts)
				+ (this.ampHourReading == null ? "" : ",ampHourReading=" + this.ampHourReading)
				+ (this.wattHourReading == null ? "" : ",wattHourReading=" + this.wattHourReading) + '}';
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
	public Integer getWatts() {
		if ( watts != null ) {
			return watts;
		}
		if ( pvAmps == null || pvVolts == null ) {
			return null;
		}
		return Integer.valueOf((int) Math.round(pvAmps.doubleValue() * pvVolts.doubleValue()));
	}

	public Long getWattHourReading() {
		return wattHourReading;
	}

	public void setWattHourReading(Long wattHours) {
		this.wattHourReading = wattHours;
	}

	/**
	 * Set the {@code wattHourReading} as an offset in kWh.
	 * 
	 * <p>
	 * This method will multiply the value by 1000, set that on
	 * {@code wattHourReading}.
	 * 
	 * @param kWattHoursToday
	 *        the kWh reading to set
	 */
	@Deprecated
	public void setKWattHoursToday(Double kWattHoursToday) {
		if ( kWattHoursToday != null ) {
			setWattHourReading(Math.round(kWattHoursToday.doubleValue() * 1000));
		} else {
			setWattHourReading(null);
		}
	}

	@Deprecated
	public Double getKWattHoursToday() {
		if ( wattHourReading == null ) {
			return null;
		}
		return wattHourReading.doubleValue() / 1000.0;
	}

	public void setWatts(Integer watts) {
		this.watts = watts;
	}

	@Deprecated
	public Float getPvVolts() {
		return pvVolts;
	}

	@Deprecated
	public void setPvVolts(Float pvVolts) {
		this.pvVolts = pvVolts;
	}

	@Deprecated
	public Float getPvAmps() {
		return pvAmps;
	}

	@Deprecated
	public void setPvAmps(Float pvAmps) {
		this.pvAmps = pvAmps;
	}

	@Deprecated
	public void setAmpHoursToday(Double ampHours) {
		setAmpHourReading(ampHours);
	}

	public Float getBatteryVolts() {
		return batteryVolts;
	}

	public void setBatteryVolts(Float batteryVolts) {
		this.batteryVolts = batteryVolts;
	}

	public Double getBatteryAmpHours() {
		return batteryAmpHours;
	}

	public void setBatteryAmpHours(Double batteryAmpHours) {
		this.batteryAmpHours = batteryAmpHours;
	}

	public Float getDcOutputVolts() {
		return dcOutputVolts;
	}

	public void setDcOutputVolts(Float dcOutputVolts) {
		this.dcOutputVolts = dcOutputVolts;
	}

	public Float getDcOutputAmps() {
		return dcOutputAmps;
	}

	public void setDcOutputAmps(Float dcOutputAmps) {
		this.dcOutputAmps = dcOutputAmps;
	}

	public Float getAcOutputVolts() {
		return acOutputVolts;
	}

	public void setAcOutputVolts(Float acOutputVolts) {
		this.acOutputVolts = acOutputVolts;
	}

	public Float getAcOutputAmps() {
		return acOutputAmps;
	}

	public void setAcOutputAmps(Float acOutputAmps) {
		this.acOutputAmps = acOutputAmps;
	}

	public Double getAmpHourReading() {
		return ampHourReading;
	}

	public void setAmpHourReading(Double ampHourReading) {
		this.ampHourReading = ampHourReading;
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

}
