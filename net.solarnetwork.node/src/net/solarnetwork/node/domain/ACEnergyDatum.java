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
 * @version 1.0
 */
public interface ACEnergyDatum extends EnergyDatum {

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

}
