/* ==================================================================
 * SimpleBacnetDeviceObjectPropertyRef.java - 5/11/2022 1:27:48 pm
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

import java.io.Serializable;
import java.util.Objects;

/**
 * Simple implementation of {@link BacnetDeviceObjectPropertyRef}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleBacnetDeviceObjectPropertyRef implements BacnetDeviceObjectPropertyRef, Serializable,
		Comparable<BacnetDeviceObjectPropertyRef> {

	private static final long serialVersionUID = -2337176762098262203L;

	private final int deviceId;
	private final int objectType;
	private final int objectNumber;
	private final int propertyId;
	private final int propertyIndex;

	/**
	 * Constructor.
	 * 
	 * @param deviceId
	 *        the device ID
	 * @param objectType
	 *        the object type
	 * @param objectNumber
	 *        the object number
	 * @param propertyId
	 *        the property ID
	 */
	public SimpleBacnetDeviceObjectPropertyRef(int deviceId, int objectType, int objectNumber,
			int propertyId) {
		this(deviceId, objectType, objectNumber, propertyId, NOT_INDEXED);
	}

	/**
	 * Constructor.
	 * 
	 * @param deviceId
	 *        the device ID
	 * @param objectType
	 *        the object type
	 * @param objectNumber
	 *        the object number
	 * @param propertyId
	 *        the property ID
	 * @param propertyIndex
	 *        the property index
	 */
	public SimpleBacnetDeviceObjectPropertyRef(int deviceId, int objectType, int objectNumber,
			int propertyId, int propertyIndex) {
		super();
		this.deviceId = deviceId;
		this.objectType = objectType;
		this.objectNumber = objectNumber;
		this.propertyId = propertyId;
		this.propertyIndex = propertyIndex;
	}

	@Override
	public int compareTo(BacnetDeviceObjectPropertyRef o) {
		int c = Integer.compare(deviceId, o.getDeviceId());
		if ( c != 0 ) {
			return c;
		}
		c = Integer.compare(objectType, o.getObjectType());
		if ( c != 0 ) {
			return c;
		}
		c = Integer.compare(objectNumber, o.getObjectNumber());
		if ( c != 0 ) {
			return c;
		}
		c = Integer.compare(propertyId, o.getPropertyId());
		if ( c != 0 ) {
			return c;
		}
		return Integer.compare(propertyIndex, o.getPropertyIndex());
	}

	@Override
	public int hashCode() {
		return Objects.hash(deviceId, objectNumber, objectType, propertyId, propertyIndex);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof SimpleBacnetDeviceObjectPropertyRef) ) {
			return false;
		}
		SimpleBacnetDeviceObjectPropertyRef other = (SimpleBacnetDeviceObjectPropertyRef) obj;
		return deviceId == other.deviceId && objectNumber == other.objectNumber
				&& objectType == other.objectType && propertyId == other.propertyId
				&& propertyIndex == other.propertyIndex;
	}

	@Override
	public int getDeviceId() {
		return deviceId;
	}

	@Override
	public int getObjectType() {
		return objectType;
	}

	@Override
	public int getObjectNumber() {
		return objectNumber;
	}

	@Override
	public int getPropertyId() {
		return propertyId;
	}

	@Override
	public int getPropertyIndex() {
		return propertyIndex;
	}

}
