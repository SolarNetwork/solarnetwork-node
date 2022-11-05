/* ==================================================================
 * SimpleBacnetDeviceObjectPropertyCovRef.java - 6/11/2022 8:04:18 am
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

/**
 * Simple implementation of {@link SimpleBacnetDeviceObjectPropertyCovRef}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleBacnetDeviceObjectPropertyCovRef extends SimpleBacnetDeviceObjectPropertyRef
		implements BacnetDeviceObjectPropertyCovRef {

	private static final long serialVersionUID = -4571065509693135606L;

	private final Float covIncrement;

	/**
	 * Constructor.
	 * 
	 * @param deviceId
	 * @param objectType
	 * @param objectNumber
	 * @param propertyId
	 */
	public SimpleBacnetDeviceObjectPropertyCovRef(int deviceId, int objectType, int objectNumber,
			int propertyId, Float covIncrement) {
		super(deviceId, objectType, objectNumber, propertyId);
		this.covIncrement = covIncrement;
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
	 * @param covIncrement
	 */
	public SimpleBacnetDeviceObjectPropertyCovRef(int deviceId, int objectType, int objectNumber,
			int propertyId, int propertyIndex, Float covIncrement) {
		super(deviceId, objectType, objectNumber, propertyId, propertyIndex);
		this.covIncrement = covIncrement;
	}

	@Override
	public Float getCovIncrement() {
		return covIncrement;
	}

}
