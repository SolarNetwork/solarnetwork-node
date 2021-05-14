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

import java.util.EnumSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.util.SerializeIgnore;

/**
 * GeneralNodeDatum that also implements {@link ACEnergyDatum}.
 * 
 * @author matt
 * @version 1.4
 */
public class GeneralNodeACEnergyDatum extends GeneralNodeEnergyDatum implements ACEnergyDatum {

	/**
	 * Populate phase-specific property values given a data accessor.
	 * 
	 * <p>
	 * This method will populate the following values for phases A, B, and C:
	 * </p>
	 * 
	 * <ul>
	 * <li>{@link #setCurrent(Float, ACPhase)} with
	 * {@link ACEnergyDataAccessor#getCurrent()}</li>
	 * <li>{@link #setVoltage(Float, ACPhase)} with
	 * {@link ACEnergyDataAccessor#getVoltage()}</li>
	 * <li>{@link #setLineVoltage(Float, ACPhase)} with
	 * {@link ACEnergyDataAccessor#getLineVoltage()}</li>
	 * </ul>
	 * 
	 * @param data
	 *        the data accessor
	 */
	public void populatePhaseMeasurementProperties(ACEnergyDataAccessor data) {
		Set<ACPhase> phases = EnumSet.of(ACPhase.PhaseA, ACPhase.PhaseB, ACPhase.PhaseC);
		for ( ACPhase phase : phases ) {
			ACEnergyDataAccessor acc = data.accessorForPhase(phase);
			setCurrent(acc.getCurrent(), phase);
			setVoltage(acc.getVoltage(), phase);
			setLineVoltage(acc.getLineVoltage(), phase);
		}
	}

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
	public Float getCurrent(ACPhase phase) {
		return getInstantaneousSampleFloat(phase.withKey(CURRENT_KEY));
	}

	/**
	 * Set a phase voltage (to neutral) value.
	 * 
	 * <p>
	 * This sets an instantaneous value for the
	 * {@link ACEnergyDatum#CURRENT_KEY} key appended with <i>_P</i>, where
	 * {@literal P} is the phase key.
	 * </p>
	 * 
	 * @param current
	 *        the phase current
	 * @param phase
	 *        the phase
	 * @since 1.2
	 */
	public void setCurrent(Float current, ACPhase phase) {
		putInstantaneousSampleValue(phase.withKey(CURRENT_KEY), current);
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
	public Float getVoltage(ACPhase phase) {
		return getInstantaneousSampleFloat(phase.withKey(VOLTAGE_KEY));
	}

	/**
	 * Set a phase voltage (to neutral) value.
	 * 
	 * <p>
	 * This sets an instantaneous value for the
	 * {@link ACEnergyDatum#VOLTAGE_KEY} key appended with <i>_P</i>, where
	 * {@literal P} is the phase key.
	 * </p>
	 * 
	 * @param phaseVoltage
	 *        the phase voltage
	 * @param phase
	 *        the phase
	 * @since 1.2
	 */
	public void setVoltage(Float phaseVoltage, ACPhase phase) {
		putInstantaneousSampleValue(phase.withKey(VOLTAGE_KEY), phaseVoltage);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getLineVoltage() {
		return getInstantaneousSampleFloat(LINE_VOLTAGE_KEY);
	}

	public void setLineVoltage(Float lineVoltage) {
		putInstantaneousSampleValue(LINE_VOLTAGE_KEY, lineVoltage);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getLineVoltage(ACPhase phase) {
		return getInstantaneousSampleFloat(phase.withLineKey(VOLTAGE_KEY));
	}

	/**
	 * Set a line voltage value.
	 * 
	 * <p>
	 * This sets an instantaneous value for the
	 * {@link ACEnergyDatum#VOLTAGE_KEY} key appended with <i>_P</i>, where
	 * {@literal P} is the phase line key.
	 * </p>
	 * 
	 * @param lineVoltage
	 *        the line voltage
	 * @param phase
	 *        the phase
	 * @since 1.2
	 */
	public void setLineVoltage(Float lineVoltage, ACPhase phase) {
		putInstantaneousSampleValue(phase.withLineKey(VOLTAGE_KEY), lineVoltage);
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
	 * @return An energy value, in watts, or {@literal null} if none available.
	 * @since 1.1
	 */
	@JsonIgnore
	@SerializeIgnore
	public Long getReverseWattHourReading() {
		return getAccumulatingSampleLong(WATT_HOUR_READING_KEY + Datum.REVERSE_ACCUMULATING_SUFFIX_KEY);
	}

	/**
	 * Set an export energy value.
	 * 
	 * @param wattHourReading
	 *        An energy value, in watts, or {@literal null} if none available.
	 * @since 1.1
	 */
	public void setReverseWattHourReading(Long wattHourReading) {
		putAccumulatingSampleValue(WATT_HOUR_READING_KEY + Datum.REVERSE_ACCUMULATING_SUFFIX_KEY,
				wattHourReading);
	}

	/**
	 * Get a neutral current value.
	 * 
	 * @return the current value, in amperes, or {@literal null} if none
	 *         available.
	 * @since 1.3
	 */
	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getNeutralCurrent() {
		return getInstantaneousSampleFloat(NEUTRAL_CURRENT_KEY);
	}

	/**
	 * Set a neutral current value.
	 * 
	 * @param current
	 *        the current value to set, in amperes
	 * @since 1.3
	 */
	public void setNeutralCurrent(Float current) {
		putInstantaneousSampleValue(NEUTRAL_CURRENT_KEY, current);
	}
}
