/* ==================================================================
 * ACEnergyDatum.java - Apr 2, 2014 7:08:15 AM
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

/**
 * Standardized API for alternating current related energy datum to implement.
 * This API represents a single phase, either a direct phase measurement or an
 * average or total measurement.
 * 
 * @author matt
 * @version 1.2
 */
public interface ACEnergyDatum extends EnergyDatum {

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} status sample
	 * key for {@link #getPhase()} values.
	 */
	public static final String PHASE_KEY = "phase";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getRealPower()} values.
	 */
	public static final String REAL_POWER_KEY = "realPower";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getApparentPower()} values.
	 */
	public static final String APPARENT_POWER_KEY = "apparentPower";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getReactivePower()} values.
	 */
	public static final String REACTIVE_POWER_KEY = "reactivePower";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getPowerFactor()} values.
	 */
	public static final String POWER_FACTOR_KEY = "powerFactor";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getEffectivePowerFactor()} values.
	 */
	public static final String EFFECTIVE_POWER_FACTOR_KEY = "effectivePowerFactor";

	/**
	 * The{@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getFrequency()} values.
	 */
	public static final String FREQUENCY_KEY = "frequency";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getVoltage()} values.
	 */
	public static final String VOLTAGE_KEY = "voltage";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getCurrent()} values.
	 */
	public static final String CURRENT_KEY = "current";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getPhaseVoltage()} values.
	 */
	public static final String PHASE_VOLTAGE_KEY = "phaseVoltage";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getLineVoltage()} values.
	 * 
	 * @since 1.2
	 */
	public static final String LINE_VOLTAGE_KEY = "lineVoltage";

	/**
	 * Get the phase measured by this datum.
	 * 
	 * @return the phase, should never be <em>null</em>
	 */
	ACPhase getPhase();

	/**
	 * Get the instantaneous real power, in watts (W). This should return the
	 * same value as {@link EnergyDatum#getWatts()} but has this method to be
	 * explicit.
	 * 
	 * @return the real power in watts, or <em>null</em> if not available
	 */
	Integer getRealPower();

	/**
	 * Get the instantaneous apparent power, in volt-amperes (VA).
	 * 
	 * @return the apparent power in volt-amperes, or <em>null</em> if not
	 *         available
	 */
	Integer getApparentPower();

	/**
	 * Get the instantaneous reactive power, in reactive volt-amperes (var).
	 * 
	 * @return the reactive power in reactive volt-amperes, or <em>null</em> if
	 *         not available
	 */
	Integer getReactivePower();

	/**
	 * Get the effective instantaneous power factor, as a value between
	 * {@code -1} and {@code 1}. If the phase angle is positive (current leads
	 * voltage) this method returns a positive value. If the phase angle is
	 * negative (current lags voltage) this method returns a negative value.
	 * 
	 * @return the effective power factor
	 */
	Float getEffectivePowerFactor();

	/**
	 * Get the instantaneous frequency, in hertz (Hz).
	 * 
	 * @return the frequency, or <em>null</em> if not known
	 */
	Float getFrequency();

	/**
	 * Get the instantaneous neutral voltage.
	 * 
	 * @return the volts, or <em>null</em> if not known
	 */
	Float getVoltage();

	/**
	 * Get the instantaneous current, in amps.
	 * 
	 * <p>
	 * This metnod is equivalent to calling
	 * {@code datum.getCurrent(datum.getPhase())}.
	 * </p>
	 * 
	 * @return the amps, or <em>null</em> if not known
	 */
	Float getCurrent();

	/**
	 * Get the instantaneous current, in amps, for a specific phase.
	 * 
	 * @param phase
	 *        the phase
	 * @return the phase
	 * @sicne 1.2
	 */
	Float getCurrent(ACPhase phase);

	/**
	 * Get the instantaneous phase-to-neutral line voltage.
	 * 
	 * <p>
	 * This metnod is equivalent to calling
	 * {@code datum.getPhaseVoltage(datum.getPhase())}.
	 * </p>
	 * 
	 * @return the volts, or {@literal null} if not known
	 */
	Float getPhaseVoltage();

	/**
	 * Get the instantaneous phase-to-neutral line voltage for a specific phase.
	 * 
	 * @param phase
	 *        the phase
	 * @return the volts, or {@literal null} if not known
	 * @since 1.2
	 */
	Float getPhaseVoltage(ACPhase phase);

	/**
	 * Get the instantaneous phase-to-phase line voltage.
	 * 
	 * <p>
	 * For the {@link #getPhase()}, this value represents the difference between
	 * this phase and the <i>next</i> phase, in {@literal a}, {@literal b},
	 * {@literal c} order, with {@code PhaseC} wrapping around back to
	 * {@code PhaseA}. Thus the possible values represent:
	 * </p>
	 * 
	 * <dl>
	 * <dt>{@code PhaseA}</dt>
	 * <dd>Vab</dd>
	 * <dt>{@code PhaseB}</dt>
	 * <dd>Vbc</dd>
	 * <dt>{@code PhaseC}</dt>
	 * <dd>Vca</dd>
	 * </dl>
	 * 
	 * <p>
	 * This metnod is equivalent to calling
	 * {@code datum.getLineVoltage(datum.getPhase())}.
	 * </p>
	 * 
	 * @return the line voltage
	 * @since 1.2
	 * @see #getLineVoltage(ACPhase)
	 */
	Float getLineVoltage();

	/**
	 * Get the instantaneous phase-to-phase line voltage for a specific phase.
	 * 
	 * @param phase
	 *        the phase (first)
	 * @return the line voltage
	 * @since 1.2
	 */
	Float getLineVoltage(ACPhase phase);

	/**
	 * Get the instantaneous power factor.
	 * 
	 * @return the power factor, or <em>null</em> if not known
	 */
	Float getPowerFactor();
}
