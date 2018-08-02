/* ==================================================================
 * AEInverterConfiguration.java - 27/07/2018 2:17:27 PM
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

package net.solarnetwork.node.hw.ae.inverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Inverter configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class AEInverterConfiguration {

	private final InverterVoltageType voltageType;
	private final TransformerTapType tapType;
	private final TransformerWiringType wiringType;
	private final boolean meterInstalled;

	// not being concerned with threading here if we re-create a few instances
	private static final Map<Integer, AEInverterConfiguration> cache = new HashMap<>(2);

	/**
	 * Constructor.
	 * 
	 * @param voltageType
	 *        the voltage type
	 * @param tapType
	 *        the tap type
	 * @param wiringType
	 *        the wiring type
	 * @param meterInstalled
	 *        {@literal true} if the utility meter is installed
	 */
	public AEInverterConfiguration(InverterVoltageType voltageType, TransformerTapType tapType,
			TransformerWiringType wiringType, boolean meterInstalled) {
		super();
		this.voltageType = voltageType;
		this.tapType = tapType;
		this.wiringType = wiringType;
		this.meterInstalled = meterInstalled;
	}

	/**
	 * Get a configuration instance from a raw register value.
	 * 
	 * <p>
	 * This data is packed into a single 16-bit register value where the bits
	 * are treated as a mask.
	 * </p>
	 * 
	 * 
	 * @param word
	 *        the Modbus register word with the configuration information
	 * @return the configuration instance
	 * @throws IllegalArgumentException
	 *         if {@code word} does not contain a valid value
	 */
	public static AEInverterConfiguration forRegisterValue(int word) {
		return cache.computeIfAbsent(word, w -> {
			InverterVoltageType vType = InverterVoltageType.forRegisterValue(w);
			TransformerTapType tType = TransformerTapType.forRegisterValue(w);
			TransformerWiringType wType = TransformerWiringType.forRegisterValue(w);
			boolean meterInstalled = (0x0100 & w) == 0x0100 ? true : false;
			return new AEInverterConfiguration(vType, tType, wType, meterInstalled);
		});
	}

	@Override
	public String toString() {
		return "AEInverterConfiguration{voltageType=" + voltageType + ",tapType=" + tapType
				+ ",wiringType=" + wiringType + ",meterInstalled=" + meterInstalled + "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (meterInstalled ? 1231 : 1237);
		result = prime * result + ((tapType == null) ? 0 : tapType.hashCode());
		result = prime * result + ((voltageType == null) ? 0 : voltageType.hashCode());
		result = prime * result + ((wiringType == null) ? 0 : wiringType.hashCode());
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
		if ( !(obj instanceof AEInverterConfiguration) ) {
			return false;
		}
		AEInverterConfiguration other = (AEInverterConfiguration) obj;
		if ( meterInstalled != other.meterInstalled ) {
			return false;
		}
		if ( tapType != other.tapType ) {
			return false;
		}
		if ( voltageType != other.voltageType ) {
			return false;
		}
		if ( wiringType != other.wiringType ) {
			return false;
		}
		return true;
	}

	/**
	 * Get the voltage type.
	 * 
	 * @return the voltage type
	 */
	public InverterVoltageType getVoltageType() {
		return voltageType;
	}

	/**
	 * Get the transformer tap type.
	 * 
	 * @return the tap type
	 */
	public TransformerTapType getTapType() {
		return tapType;
	}

	/**
	 * Get the transformer wiring type.
	 * 
	 * @return the wiring type
	 */
	public TransformerWiringType getWiringType() {
		return wiringType;
	}

	/**
	 * Get utility meter installed flag.
	 * 
	 * @return {@literal true} if the utility meter is installed
	 */
	public boolean isMeterInstalled() {
		return meterInstalled;
	}

}
