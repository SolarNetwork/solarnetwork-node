/* ==================================================================
 * SunspecModbusReference.java - 8/10/2018 12:13:04 PM
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

package net.solarnetwork.node.hw.sunspec;

import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Extension of {@link ModbusReference} to add additional SunSpec data type
 * support.
 *
 * @author matt
 * @version 1.1
 */
public interface SunspecModbusReference extends ModbusReference {

	/**
	 * Return the classification of this modbus reference.
	 *
	 * @return the classification, or {@code null} if none
	 */
	default @Nullable DataClassification getClassification() {
		return null;
	}

	/**
	 * Get the access level of this modbus reference.
	 *
	 * @return the access level, never {@code null}; this implementation returns
	 *         {@link PointAccess#ReadOnly}
	 * @since 1.1
	 */
	default PointAccess getAccess() {
		return PointAccess.ReadOnly;
	}

}
