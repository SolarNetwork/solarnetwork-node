/* ==================================================================
 * ModbusReference.java - 15/05/2018 11:04:04 AM
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

package net.solarnetwork.node.io.modbus;

import java.util.Set;
import net.solarnetwork.util.IntRangeSet;

/**
 * A reference to a Modbus register (or registers).
 * 
 * @author matt
 * @version 2.0
 * @since 2.8
 */
public interface ModbusReference {

	/**
	 * Get the register address.
	 * 
	 * @return the address
	 */
	int getAddress();

	/**
	 * Get the data type.
	 * 
	 * @return the data type
	 */
	ModbusDataType getDataType();

	/**
	 * Get the read function for accessing the register.
	 * 
	 * @return the read function
	 */
	ModbusReadFunction getFunction();

	/**
	 * Get the number of Modbus words to include.
	 * 
	 * @return the word length
	 */
	int getWordLength();

	/**
	 * Create a Modbus register address set from enum {@link ModbusReference}
	 * values.
	 * 
	 * @param <T>
	 *        the enum type that also implements {@link ModbusReference}
	 * @param clazz
	 *        the enum class to extract the register set from
	 * @param prefixes
	 *        an optional set of enum prefixes to restrict the result to; if not
	 *        provided then all enum values will be included
	 * @return the range set, never {@literal null}
	 * @see createRegisterAddressSet
	 * @since 2.0
	 */
	static <T extends Enum<?> & ModbusReference> IntRangeSet createAddressSet(Class<T> clazz,
			Set<String> prefixes) {
		return createAddressSet(clazz.getEnumConstants(), prefixes);
	}

	/**
	 * Create a Modbus register address set from an array of
	 * {@link ModbusReference} values.
	 * 
	 * @param refs
	 *        the list of references to extract addresses from
	 * @param prefixes
	 *        an optional set of enum prefixes to restrict the result to, which
	 *        are compared to the {@link Object#toString()} value of each
	 *        {@link ModbusReference} in {@code refs}; if not provided then all
	 *        values will be included
	 * @return the range set, never {@literal null}
	 * @since 2.0
	 */
	static IntRangeSet createAddressSet(ModbusReference[] refs, Set<String> prefixes) {
		IntRangeSet set = new IntRangeSet();
		for ( ModbusReference r : refs ) {
			if ( prefixes != null ) {
				String name = r.toString();
				boolean found = false;
				for ( String prefix : prefixes ) {
					if ( name.startsWith(prefix) ) {
						found = true;
						break;
					}
				}
				if ( !found ) {
					continue;
				}
			}

			int len = r.getWordLength();
			if ( len > 0 ) {
				set.addRange(r.getAddress(), r.getAddress() + len - 1);
			}
		}
		return set;
	}

}
