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

package net.solarnetwork.node.power.enasolar.ws;

import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link PowerDatum} to map EnaSolar data appropriately.
 * 
 * @author matt
 * @version 1.2
 */
public class EnaSolarPowerDatum extends GeneralNodePVEnergyDatum {

	private static final Logger log = LoggerFactory.getLogger(EnaSolarPowerDatum.class);

	private boolean usingDailyResettingTotal = false;

	/**
	 * Set the deca-watt hour total reading, as a hexidecimal string.
	 * 
	 * @param decaWattHoursTotal
	 *        the hexidecimal string
	 */
	public void setDecaWattHoursTotal(String decaWattHoursTotal) {
		if ( decaWattHoursTotal == null ) {
			return;
		}
		try {
			Long wattHoursTotal = Long.parseLong(decaWattHoursTotal, 16) * 10;
			setWattHourReading(wattHoursTotal);
		} catch ( NumberFormatException e ) {
			log.warn("Unable to parse decaWattHoursTotal number value {}: {}", decaWattHoursTotal,
					e.getMessage());
		}
	}

	/**
	 * Set the {@code wattHourReading} as an offset in kWh.
	 * 
	 * <p>
	 * This method will multiply the value by 1000, set that on
	 * {@code wattHourReading}.
	 * </p>
	 * 
	 * @param kWattHoursToday
	 *        the kWh reading to set
	 * @deprecated use {@link #setDecaWattHoursTotal(String)}
	 */
	@Deprecated
	public void setKWattHoursToday(Double kWattHoursToday) {
		usingDailyResettingTotal = true;
		if ( kWattHoursToday != null ) {
			setWattHourReading(Math.round(kWattHoursToday.doubleValue() * 1000));
		} else {
			setWattHourReading(null);
		}
	}

	/**
	 * Set the PV power, as a kW reading. This calls {@link #setWatts(Integer)}
	 * which should be the AC output power but this is here for backwards
	 * compatibility.
	 * 
	 * @param power
	 *        the kW power reading
	 * @deprecated use {@link #setOutputPower(Float)}
	 */
	@Deprecated
	public void setPvPower(Float power) {
		Integer watts = (power == null ? null : (int) Math.round(power.floatValue() * 1000F));
		setWatts(watts);
	}

	/**
	 * Set the AC output power, as a kW reading. This is an alias for
	 * {@link #setOutputPower(Float)}.
	 * 
	 * @param power
	 *        the kW power reading
	 */
	public void setAcPower(Float power) {
		setOutputPower(power);
	}

	/**
	 * Set the AC output power, in kW. This will set the {@code watts} value.
	 * 
	 * @param power
	 *        the output power, in kW
	 */
	public void setOutputPower(Float power) {
		Integer w = (power == null ? null : (int) Math.round(power.floatValue() * 1000F));
		setWatts(w);
	}

	/**
	 * Get the AC output power, in kW.
	 * 
	 * @return the AC output power, in kW
	 */
	public Float getOutputPower() {
		Integer w = getWatts();
		return (w == null ? null : w.floatValue() / 1000F);
	}

	/**
	 * Set the DC input power, in kW. This will set the {@code DCPower} value.
	 * 
	 * @param power
	 *        the input power, in kW
	 */
	public void setInputPower(Float power) {
		Integer w = (power == null ? null : (int) Math.round(power.floatValue() * 1000F));
		setDCPower(w);
	}

	/**
	 * Get the DC input power, in kW.
	 * 
	 * @return the DC input power, in kW
	 */
	public Float getInputPower() {
		Integer w = getDCPower();
		return (w == null ? null : w.floatValue() / 1000F);
	}

	/**
	 * Set the DC input voltage, in volts. This is an alias for
	 * {@code #setDCVoltage(Float)}.
	 * 
	 * @param power
	 *        the input voltage, in volts
	 */
	public void setInputVoltage(Float value) {
		setDCVoltage(value);
	}

	/**
	 * Get the DC input voltage, in volts. This is an alias for
	 * {@link #getDCVoltage()}.
	 * 
	 * @return the DC input voltage, in volts
	 */
	public Float getInputVoltage() {
		return getDCVoltage();
	}

	/**
	 * Set the overall lifetime energy produced, as a deca-watt hour hexidecimal
	 * string value. This is an alias for {@link #setDecaWattHoursTotal(String)}
	 * .
	 * 
	 * @param energyLifetime
	 *        the lifetime energy value
	 * @see #setDecaWattHoursTotal(String)
	 */
	public void setEnergyLifetime(String energyLifetime) {
		setDecaWattHoursTotal(energyLifetime);
	}

	/**
	 * Set the AC output voltage, in volts. This is an alias for
	 * {@link #setVoltage(Float)}.
	 * 
	 * @param power
	 *        the output voltage, in volts
	 */
	public void setOutputVoltage(Float power) {
		setVoltage(power);
	}

	/**
	 * Get the AC output voltage, in volts.
	 * 
	 * @return the AC output voltage
	 */
	public Float getOutputVoltage() {
		return getVoltage();
	}

	/**
	 * Return <em>true</em> if {@link #setKWattHoursToday(Double)} was called.
	 * 
	 * @return boolean
	 */
	public boolean isUsingDailyResettingTotal() {
		return usingDailyResettingTotal;
	}

}
