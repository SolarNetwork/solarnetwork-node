/* ==================================================================
 * BacnetDeviceObjectPropertyRef.java - 4/11/2022 3:05:20 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.bacnet;

import net.solarnetwork.domain.CodedValue;

/**
 * A reference to a device object property.
 * 
 * @author matt
 * @version 1.0
 */
public interface BacnetDeviceObjectPropertyRef {

	/** Represents a "not indexed" property index value. */
	int NOT_INDEXED = -1;

	/**
	 * Get the device ID.
	 * 
	 * @return the device ID
	 */
	int getDeviceId();

	/**
	 * Get the object type.
	 * 
	 * @return the object type
	 */
	int getObjectType();

	/**
	 * Get the object type as an enumerated type.
	 * 
	 * @return the object type enumeration, or {@literal null} if unsupported
	 */
	default BacnetObjectType objectType() {
		return CodedValue.forCodeValue(getObjectType(), BacnetObjectType.class, null);
	}

	/**
	 * Get the object instance number.
	 * 
	 * @return the object instance number
	 */
	int getObjectNumber();

	/**
	 * Get the property ID.
	 * 
	 * @return the property ID
	 */
	int getPropertyId();

	/**
	 * Get the property array index.
	 * 
	 * @return the array index, or {@literal -1} for non-indexed properties or
	 *         "all values" for indexed properties
	 */
	default int getPropertyIndex() {
		return NOT_INDEXED;
	}

	/**
	 * Test if a property array index is present.
	 * 
	 * @return {@literal true} if a property array index is present
	 */
	default boolean hasPropertyIndex() {
		return getPropertyIndex() >= 0;
	}

	/**
	 * Get the property ID as an enumerated type.
	 * 
	 * @return the property type enumeration, or {@literal null} if unsupported
	 */
	default BacnetPropertyType propertyType() {
		return CodedValue.forCodeValue(getPropertyId(), BacnetPropertyType.class, null);
	}

}
