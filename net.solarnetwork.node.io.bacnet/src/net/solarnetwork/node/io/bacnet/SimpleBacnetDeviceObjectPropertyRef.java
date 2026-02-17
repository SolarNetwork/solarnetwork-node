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

import static net.solarnetwork.domain.CodedValue.forCodeValue;
import java.io.Serializable;
import java.util.Objects;

/**
 * Simple implementation of {@link BacnetDeviceObjectPropertyRef}.
 *
 * @author matt
 * @version 1.2
 */
public class SimpleBacnetDeviceObjectPropertyRef implements BacnetDeviceObjectPropertyRef, Serializable,
		Comparable<SimpleBacnetDeviceObjectPropertyRef> {

	private static final long serialVersionUID = 5528795423604252975L;

	/** The device ID. */
	private final int deviceId;

	/** The object type ID. */
	private final int objectType;

	/** The object instance number. */
	private final int objectNumber;

	/** The property identifier. */
	private final int propertyId;

	/** The property index, for list properties. */
	private final int propertyIndex;

	/** The priority, e.g. for write operations. */
	private final int priority;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link BacnetDeviceObjectPropertyRef#NOT_INDEXED} and
	 * {@link BacnetDeviceObjectPropertyRef#NO_PRIORITY} values will be used.
	 * </p>
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
	 * <p>
	 * The {@link BacnetDeviceObjectPropertyRef#NO_PRIORITY} will be used.
	 * </p>
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
		this(deviceId, objectType, objectNumber, propertyId, propertyIndex, NO_PRIORITY);
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
	 * @param priority
	 *        the priority
	 * @since 1.1
	 */
	public SimpleBacnetDeviceObjectPropertyRef(int deviceId, int objectType, int objectNumber,
			int propertyId, int propertyIndex, int priority) {
		super();
		this.deviceId = deviceId;
		this.objectType = objectType;
		this.objectNumber = objectNumber;
		this.propertyId = propertyId;
		this.propertyIndex = propertyIndex;
		this.priority = priority;
	}

	@Override
	public int compareTo(SimpleBacnetDeviceObjectPropertyRef o) {
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
		c = Integer.compare(propertyIndex, o.getPropertyIndex());
		if ( c != 0 ) {
			return c;
		}
		return Integer.compare(priority, o.getPriority());
	}

	@Override
	public int hashCode() {
		return Objects.hash(deviceId, objectNumber, objectType, propertyId, propertyIndex, priority);
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
				&& propertyIndex == other.propertyIndex && priority == other.priority;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BACnetRef{device=");
		builder.append(deviceId);
		builder.append(", objectType=");
		try {
			BacnetObjectType objType = forCodeValue(objectType, BacnetObjectType.class, null);
			if ( objType != null ) {
				builder.append(objType);
			} else {
				builder.append(objectType);
			}
		} catch ( IllegalArgumentException e ) {
			builder.append(objectType);
		}
		builder.append(", objectNumber=");
		builder.append(objectNumber);
		builder.append(", propertyType=");
		try {
			BacnetPropertyType propType = forCodeValue(propertyId, BacnetPropertyType.class, null);
			if ( propType != null ) {
				builder.append(propType);
			} else {
				builder.append(propertyId);
			}
		} catch ( IllegalArgumentException e ) {
			builder.append(propertyId);
		}
		if ( hasPropertyIndex() ) {
			builder.append(", propertyIndex=");
			builder.append(propertyIndex);
		}
		if ( hasPriority() ) {
			builder.append(", priority=");
			builder.append(priority);
		}
		builder.append("}");
		return builder.toString();
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

	@Override
	public int getPriority() {
		return priority;
	}

}
