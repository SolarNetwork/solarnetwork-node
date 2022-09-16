/* ==================================================================
 * AcEnergyDatum.java - Apr 2, 2014 7:08:15 AM
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

package net.solarnetwork.node.domain.datum;

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import java.util.EnumSet;
import java.util.Set;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;

/**
 * Standardized API for alternating current related energy datum to implement.
 * 
 * <p>
 * This API represents a single phase, either a direct phase measurement or an
 * average or total measurement.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public interface AcEnergyDatum extends EnergyDatum, net.solarnetwork.domain.datum.AcEnergyDatum {

	/**
	 * Populate phase-specific property values given a data accessor.
	 * 
	 * <p>
	 * This method will populate the following values for phases A, B, and C:
	 * </p>
	 * 
	 * <ul>
	 * <li>{@link #setCurrent(AcPhase, Float)} with
	 * {@link AcEnergyDataAccessor#getCurrent()}</li>
	 * <li>{@link #setVoltage(AcPhase, Float)} with
	 * {@link AcEnergyDataAccessor#getVoltage()}</li>
	 * <li>{@link #setLineVoltage(AcPhase, Float)} with
	 * {@link AcEnergyDataAccessor#getLineVoltage()}</li>
	 * </ul>
	 * 
	 * @param data
	 *        the data accessor
	 */
	default void populatePhaseMeasurementProperties(AcEnergyDataAccessor data) {
		Set<AcPhase> phases = EnumSet.of(AcPhase.PhaseA, AcPhase.PhaseB, AcPhase.PhaseC);
		for ( AcPhase phase : phases ) {
			AcEnergyDataAccessor acc = data.accessorForPhase(phase);
			setCurrent(phase, acc.getCurrent());
			setVoltage(phase, acc.getVoltage());
			setLineVoltage(phase, acc.getLineVoltage());
		}
	}

	/**
	 * Set the phase measured by this datum.
	 * 
	 * @param value
	 *        the phase, if known
	 */
	default void setAcPhase(AcPhase value) {
		asMutableSampleOperations().putSampleValue(Status, PHASE_KEY, value.toString());
	}

	/**
	 * Set the instantaneous real power, in watts (W).
	 * 
	 * <p>
	 * This should return the same value as {@link EnergyDatum#getWatts()} but
	 * has this method to be explicit.
	 * </p>
	 * 
	 * @param value
	 *        the real power in watts, or {@literal null} if not available
	 */
	default void setRealPower(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, REAL_POWER_KEY, value);
	}

	/**
	 * Set the instantaneous apparent power, in volt-amperes (VA).
	 * 
	 * @param value
	 *        the apparent power in volt-amperes, or {@literal null} if not
	 *        available
	 */
	default void setApparentPower(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, APPARENT_POWER_KEY, value);

	}

	/**
	 * Set the instantaneous reactive power, in reactive volt-amperes (var).
	 * 
	 * @param value
	 *        the reactive power in reactive volt-amperes, or {@literal null} if
	 *        not available
	 */
	default void setReactivePower(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, REACTIVE_POWER_KEY, value);

	}

	/**
	 * Set the effective instantaneous power factor, as a value between
	 * {@code -1} and {@code 1}. If the phase angle is positive (current leads
	 * voltage) this method returns a positive value. If the phase angle is
	 * negative (current lags voltage) this method returns a negative value.
	 * 
	 * @param value
	 *        the effective power factor
	 */
	default void setPowerFactor(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, POWER_FACTOR_KEY, value);
	}

	/**
	 * Set the instantaneous power factor.
	 * 
	 * @param value
	 *        the effective power factor
	 */
	default void setEffectivePowerFactor(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, EFFECTIVE_POWER_FACTOR_KEY, value);
	}

	/**
	 * Set the instantaneous frequency, in hertz (Hz).
	 * 
	 * @param value
	 *        the frequency, or {@literal null} if not known
	 */
	default void setFrequency(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, FREQUENCY_KEY, value);
	}

	/**
	 * Set the instantaneous neutral voltage.
	 * 
	 * @param value
	 *        the volts, or {@literal null} if not known
	 */
	default void setVoltage(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, VOLTAGE_KEY, value);
	}

	/**
	 * Set the instantaneous phase-to-neutral line voltage for a specific phase.
	 * 
	 * @param phase
	 *        the phase
	 * @param value
	 *        the volts, or {@literal null} if not known
	 */
	default void setVoltage(AcPhase phase, Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, phase.withKey(VOLTAGE_KEY), value);
	}

	/**
	 * Set the instantaneous current, in amps.
	 * 
	 * <p>
	 * This method is equivalent to calling
	 * {@code datum.getCurrent(datum.getPhase())}.
	 * </p>
	 * 
	 * @param value
	 *        the amps, or {@literal null} if not known
	 */
	default void setCurrent(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, CURRENT_KEY, value);
	}

	/**
	 * Set the instantaneous current, in amps, for a specific phase.
	 * 
	 * @param phase
	 *        the phase
	 * @param value
	 *        the phase
	 */
	default void setCurrent(AcPhase phase, Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, phase.withKey(CURRENT_KEY), value);
	}

	/**
	 * Set the instantaneous neutral current, in amps.
	 * 
	 * @param value
	 *        the amps, or {@literal null} if not known
	 */
	default void setNeutralCurrent(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, NEUTRAL_CURRENT_KEY, value);
	}

	/**
	 * Set the instantaneous phase-to-neutral line voltage.
	 * 
	 * <p>
	 * This metnod is equivalent to calling
	 * {@code datum.getPhaseVoltage(datum.getPhase())}.
	 * </p>
	 * 
	 * @param value
	 *        the volts, or {@literal null} if not known
	 */
	default void setPhaseVoltage(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, PHASE_VOLTAGE_KEY, value);
	}

	/**
	 * Set the instantaneous phase-to-phase line voltage.
	 * 
	 * @param value
	 *        the line voltage
	 * @see #setLineVoltage(AcPhase,Float)
	 */
	default void setLineVoltage(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, LINE_VOLTAGE_KEY, value);
	}

	/**
	 * Set the instantaneous phase-to-phase line voltage for a specific phase.
	 * 
	 * @param phase
	 *        the phase
	 * @param value
	 *        the line voltage
	 */
	default void setLineVoltage(AcPhase phase, Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, phase.withLineKey(VOLTAGE_KEY), value);

	}

}
