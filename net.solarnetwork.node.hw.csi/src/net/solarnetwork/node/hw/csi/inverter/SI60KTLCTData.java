/* ==================================================================
 * SI60KTLCTData.java - 22 Nov 2017 12:28:46
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

import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Implementation for accessing SI-60KTL-CT data.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class SI60KTLCTData extends BaseKTLData {

	public static final int MODEL_1 = 16433;
	public static final int MODEL_2 = 16434;
	public static final int MODEL_3 = 16435;

	public static final int ADDR_START = 0;
	public static final int ADDR_LENGTH = 59;

	public static final int ADDR_DEVICE_MODEL = 0;

	/** AC Active Power 0x001D */
	public static final int ADDR_ACTIVE_POWER = 29;

	/** AC Apparent Power 0x001E */
	public static final int ADDR_APPARENT_POWER = 30;

	/**
	 * Default constructor.
	 */
	public SI60KTLCTData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public SI60KTLCTData(ModbusData other) {
		super(other);
	}

	@Override
	public void populateMeasurements(GeneralNodeACEnergyDatum datum) {
		// TODO populate the datum with values
	}

	@Override
	public KTLData getSnapshot() {
		return new SI60KTLCTData(this);
	}

	@Override
	protected boolean readInverterDataInternal(ModbusConnection conn, MutableModbusData mutableData) {
		int[] data = conn.readUnsignedShorts(ModbusReadFunction.ReadInputRegister, ADDR_START,
				ADDR_LENGTH);
		mutableData.saveDataArray(data, ADDR_START);
		return true;
	}

	public Integer getDeviceModel() {
		return getInt16(ADDR_DEVICE_MODEL);
	}

	public Integer getActivePower() {
		return getInt16(ADDR_ACTIVE_POWER);
	}

	public Integer getApparentPower() {
		return getInt16(ADDR_APPARENT_POWER);
	}

	// TODO add accessors to get other data.

}
