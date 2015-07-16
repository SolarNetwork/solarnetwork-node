/* ==================================================================
 * GeneralNodejava - Aug 26, 2014 10:29:21 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import net.solarnetwork.util.SerializeIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * GeneralNodeDatum that also implements {@link ACEnergyDatum}.
 * 
 * @author matt
 * @version 1.1
 */
public class GeneralNodeACEnergyDatum extends GeneralNodeEnergyDatum implements ACEnergyDatum {

	@Override
	@JsonIgnore
	@SerializeIgnore
	public ACPhase getPhase() {
		String p = getStatusSampleString(PHASE_KEY);
		return (p == null ? null : ACPhase.valueOf(p));
	}

	public void setPhase(ACPhase phase) {
		putStatusSampleValue(PHASE_KEY, phase == null ? null : phase.toString());
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Integer getRealPower() {
		return getInstantaneousSampleInteger(REAL_POWER_KEY);
	}

	public void setRealPower(Integer realPower) {
		putInstantaneousSampleValue(REAL_POWER_KEY, realPower);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Integer getApparentPower() {
		return getInstantaneousSampleInteger(APPARENT_POWER_KEY);
	}

	public void setApparentPower(Integer apparentPower) {
		putInstantaneousSampleValue(APPARENT_POWER_KEY, apparentPower);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Integer getReactivePower() {
		return getInstantaneousSampleInteger(REACTIVE_POWER_KEY);
	}

	public void setReactivePower(Integer reactivePower) {
		putInstantaneousSampleValue(REACTIVE_POWER_KEY, reactivePower);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getEffectivePowerFactor() {
		return getInstantaneousSampleFloat(EFFECTIVE_POWER_FACTOR_KEY);
	}

	public void setEffectivePowerFactor(Float effectivePowerFactor) {
		putInstantaneousSampleValue(EFFECTIVE_POWER_FACTOR_KEY, effectivePowerFactor);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getFrequency() {
		return getInstantaneousSampleFloat(FREQUENCY_KEY);
	}

	public void setFrequency(Float frequency) {
		putInstantaneousSampleValue(FREQUENCY_KEY, frequency);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getVoltage() {
		return getInstantaneousSampleFloat(VOLTAGE_KEY);
	}

	public void setVoltage(Float voltage) {
		putInstantaneousSampleValue(VOLTAGE_KEY, voltage);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getCurrent() {
		return getInstantaneousSampleFloat(CURRENT_KEY);
	}

	public void setCurrent(Float current) {
		putInstantaneousSampleValue(CURRENT_KEY, current);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getPhaseVoltage() {
		return getInstantaneousSampleFloat(PHASE_VOLTAGE_KEY);
	}

	public void setPhaseVoltage(Float phaseVoltage) {
		putInstantaneousSampleValue(PHASE_VOLTAGE_KEY, phaseVoltage);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getPowerFactor() {
		return getInstantaneousSampleFloat(POWER_FACTOR_KEY);
	}

	public void setPowerFactor(Float powerFactor) {
		putInstantaneousSampleValue(POWER_FACTOR_KEY, powerFactor);
	}

	/**
	 * Get an export energy value.
	 * 
	 * @return An energy value, in watts, or <em>null</em> if none available.
	 * @since 1.1
	 */
	@JsonIgnore
	@SerializeIgnore
	public Long getReverseWattHourReading() {
		return getAccumulatingSampleLong(WATT_HOUR_READING_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY);
	}

	/**
	 * Set an export energy value.
	 * 
	 * @param wattHourReading
	 *        An energy value, in watts, or <em>null</em> if none available.
	 * @since 1.1
	 */
	public void setReverseWattHourReading(Long wattHourReading) {
		putAccumulatingSampleValue(WATT_HOUR_READING_KEY + REVERSE_ACCUMULATING_SUFFIX_KEY,
				wattHourReading);
	}

}
