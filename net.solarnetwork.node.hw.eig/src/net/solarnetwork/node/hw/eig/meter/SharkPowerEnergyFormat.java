/* ==================================================================
 * SharkPowerEnergyFormat.java - 26/07/2018 11:45:40 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.eig.meter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.util.NumberUtils;

/**
 * Represents the power and energy scale configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class SharkPowerEnergyFormat {

	private final SharkScale powerScale;
	private final int numEnergyDigits;
	private final SharkScale energyScale;
	private final int energyDigitsAfterDecimal;

	// not being concerned with threading here if we re-create a few instances
	private static final Map<Integer, SharkPowerEnergyFormat> cache = new HashMap<>(2);

	/**
	 * Constructor.
	 * 
	 * @param powerScale
	 *        the power scale
	 * @param numEnergyDigits
	 *        the number of energy digits
	 * @param energyScale
	 *        the energy scale
	 * @param energyDigitsAfterDecimal
	 *        the number of energy digits after decimal point
	 */
	public SharkPowerEnergyFormat(SharkScale powerScale, int numEnergyDigits, SharkScale energyScale,
			int energyDigitsAfterDecimal) {
		super();
		this.powerScale = powerScale;
		this.numEnergyDigits = numEnergyDigits;
		this.energyScale = energyScale;
		this.energyDigitsAfterDecimal = energyDigitsAfterDecimal;
	}

	/**
	 * Get a power/energy format instance from a raw register value.
	 * 
	 * <p>
	 * This data is packed into a single 16-bit register value, where the bits
	 * are in the form {@literal pppp--nn-eee-ddd} where {@literal pppp}
	 * represents the power scale, {@literal nn} the number of energy digits,
	 * {@literal eee} the energy scale, and {@literal ddd} the number of energy
	 * digits after the decimal point.
	 * </p>
	 * 
	 * <p>
	 * The {@literal pppp} bits (13-16) represent the power scale enumeration,
	 * using:
	 * </p>
	 * 
	 * <dl>
	 * <dt>0</dt>
	 * <dd>unit (no scaling)</dd>
	 * <dt>3</dt>
	 * <dd>kilo (1000)</dd>
	 * <dt>6</dt>
	 * <dd>mega (1000000)</dd>
	 * <dt>8</dt>
	 * <dd>auto</dd>
	 * </dl>
	 * 
	 * <p>
	 * The {@literal ddd} bits (5-7) represent the energy scale enumeration,
	 * using:
	 * </p>
	 * 
	 * <dl>
	 * <dt>0</dt>
	 * <dt>3</dt>
	 * <dd>kilo (1000)</dd>
	 * <dt>6</dt>
	 * <dd>mega (1000000)</dd>
	 * </dl>
	 * 
	 * @param word
	 *        the Modbus register word with the power/energy format
	 * @return the energy format instance
	 * @throws IllegalArgumentException
	 *         if {@code word} does not contain a valid value
	 */
	public static SharkPowerEnergyFormat forRegisterValue(int word) {
		return cache.computeIfAbsent(word, w -> {
			SharkScale powerScale = SharkScale.forPowerRegisterValue(w);
			SharkScale energyScale = SharkScale.forEnergyRegisterValue(w);
			int numEnergyDigits = (w >> 8) & 3;
			int energyDigitsAfterDecimal = w & 7;
			return new SharkPowerEnergyFormat(powerScale, numEnergyDigits, energyScale,
					energyDigitsAfterDecimal);
		});
	}

	/**
	 * Get a power value based on this power format.
	 * 
	 * <p>
	 * This will apply the power scale to the given value.
	 * </p>
	 * 
	 * @param v
	 *        the number to scale
	 * @return the scaled number
	 */
	public Number powerValue(Number v) {
		if ( v == null ) {
			return v;
		}
		int scaleFactor = (powerScale != null ? powerScale.getScaleFactor() : 1);
		if ( scaleFactor == 1 ) {
			return v;
		}
		BigDecimal d = NumberUtils.bigDecimalForNumber(v);
		return d.multiply(BigDecimal.valueOf(scaleFactor));
	}

	/**
	 * Get an energy value based on this energy format.
	 * 
	 * <p>
	 * This will apply the energy scale and decimal shift to the given value.
	 * </p>
	 * 
	 * @param v
	 *        the number to scale
	 * @return the scaled number
	 */
	public Number energyValue(Number v) {
		if ( v == null ) {
			return v;
		}
		int scaleFactor = (energyScale != null ? energyScale.getScaleFactor() : 1);
		if ( scaleFactor == 1 && energyDigitsAfterDecimal == 0 ) {
			return v;
		}
		BigDecimal d = NumberUtils.bigDecimalForNumber(v);
		return d.multiply(new BigDecimal(BigInteger.ONE, energyDigitsAfterDecimal))
				.multiply(BigDecimal.valueOf(scaleFactor));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + energyDigitsAfterDecimal;
		result = prime * result + ((energyScale == null) ? 0 : energyScale.hashCode());
		result = prime * result + numEnergyDigits;
		result = prime * result + ((powerScale == null) ? 0 : powerScale.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof SharkPowerEnergyFormat) ) {
			return false;
		}
		SharkPowerEnergyFormat other = (SharkPowerEnergyFormat) obj;
		if ( energyDigitsAfterDecimal != other.energyDigitsAfterDecimal ) {
			return false;
		}
		if ( energyScale != other.energyScale ) {
			return false;
		}
		if ( numEnergyDigits != other.numEnergyDigits ) {
			return false;
		}
		if ( powerScale != other.powerScale ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SharkPowerEnergyFormat{powerScale=" + powerScale + ",numEnergyDigits=" + numEnergyDigits
				+ ",energyScale=" + energyScale + ",energyDigitsAfterDecimal=" + energyDigitsAfterDecimal
				+ "}";
	}

	/**
	 * Get the power scale.
	 * 
	 * @return the power scale
	 */
	public SharkScale getPowerScale() {
		return powerScale;
	}

	/**
	 * Get the number of energy digits.
	 * 
	 * @return the number of energy digits.
	 */
	public int getNumEnergyDigits() {
		return numEnergyDigits;
	}

	/**
	 * Get the energy scale.
	 * 
	 * @return the energy scale
	 */
	public SharkScale getEnergyScale() {
		return energyScale;
	}

	/**
	 * Get the number of energy digits after the decimal point.
	 * 
	 * @return the number of digits
	 */
	public int getEnergyDigitsAfterDecimal() {
		return energyDigitsAfterDecimal;
	}

}
