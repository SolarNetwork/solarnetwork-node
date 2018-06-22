
/* ==================================================================
 * BaseKTLData.java - 22 Nov 2017 12:28:46
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.csi.inverter;

import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;

/**
 * Contains the common functionality for CSI KTL inverters.
 * 
 * @author maxieduncan
 */
public abstract class BaseKTLData extends ModbusData implements KTLData {

	/**
	 * Default constructor.
	 */
	public BaseKTLData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public BaseKTLData(ModbusData other) {
		super(other);
	}

	@Override
	public final synchronized void readInverterData(final ModbusConnection conn) {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				return readInverterDataInternal(conn, m);
			}
		});
	}

	protected abstract boolean readInverterDataInternal(ModbusConnection conn, MutableModbusData data);

	@Override
	public long getInverterDataTimestamp() {
		return getDataTimestamp();
	}

}
