/* ==================================================================
 * AEInverterType.java - 27/07/2018 2:59:58 PM
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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Enumeration of inverter model types.
 * 
 * @author matt
 * @version 1.0
 */
public enum AEInverterType {

	PVP30kW(
			unmodifiableSet(
					new LinkedHashSet<String>(asList("0272", "0273", "0274", "0288", "0289", "0290"))),
			"PVP30kW"),

	AE35TX(
			unmodifiableSet(new LinkedHashSet<String>(asList("0300", "0301", "0302", "0303"))),
			"AE 35TX"),

	AE50TX(
			unmodifiableSet(new LinkedHashSet<String>(asList("0304", "0305", "0306", "0307"))),
			"AE 50TX"),

	AE75TX(
			unmodifiableSet(new LinkedHashSet<String>(asList("0276", "0277", "0278", "0279"))),
			"AE 75TX"),

	AE100TX(
			unmodifiableSet(new LinkedHashSet<String>(asList("0280", "0281", "0282", "0283"))),
			"AE 100TX"),

	AE250TX(
			unmodifiableSet(new LinkedHashSet<String>(
					asList("0312", "0313", "0314", "0315", "0316", "0317", "0318", "0319"))),
			"AE 250TX"),

	// the documentation lists the same model code values for AE260TX as AE250TX!
	AE260TX(
			unmodifiableSet(new LinkedHashSet<String>(
					asList("0312", "0313", "0314", "0315", "0316", "0317", "0318", "0319"))),
			"AE 260TX"),

	AE500TX(unmodifiableSet(new LinkedHashSet<String>(asList("0386", "0387"))), "AE 500TX");

	private final Set<String> modelCodes;
	private final String description;

	private AEInverterType(Set<String> modelCodes, String description) {
		this.modelCodes = modelCodes;
		this.description = description;
	}

	/**
	 * Get the type value encoding.
	 * 
	 * @return the code
	 */
	public Set<String> getModelCodes() {
		return modelCodes;
	}

	/**
	 * Get a description of the type.
	 * 
	 * @return a description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get an enumeration for a given inverter ID value.
	 * 
	 * <p>
	 * The inverter ID contains a model code in characters 3-6.
	 * </p>
	 * 
	 * @param inverterId
	 *        the inverterId to get the enum value from
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code inverterId} is not supported
	 */
	public static AEInverterType forInverterId(String inverterId) {
		if ( inverterId != null && inverterId.length() > 5 ) {
			String modelCode = inverterId.substring(2, 6);
			return forModelCode(modelCode);
		}
		throw new IllegalArgumentException("Unsupported inverter ID value: " + inverterId);
	}

	/**
	 * Get an enumeration for a given code value.
	 * 
	 * @param modelCode
	 *        the code to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static AEInverterType forModelCode(String modelCode) {
		for ( AEInverterType s : values() ) {
			if ( s.modelCodes.contains(modelCode) ) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unsupported model type value: " + modelCode);
	}

}
