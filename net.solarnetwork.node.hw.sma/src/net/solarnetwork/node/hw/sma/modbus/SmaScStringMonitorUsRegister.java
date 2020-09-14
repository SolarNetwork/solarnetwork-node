/* ==================================================================
 * SmaScStringMonitorUsRegister.java - 12/09/2020 12:45:23 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.modbus;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusReference.createAddressSet;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for SMA SC String Monitor US series
 * devices.
 * 
 * <p>
 * This covers device ID {@literal 190}, or {@literal 97} profile 1.30 or later.
 * These devices <b>do not</b> support the {@link SmaCommonDeviceRegister}
 * values.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaScStringMonitorUsRegister implements ModbusReference {

	/** Serial number of the device. */
	SerialNumber(30057, UInt32),

	/** Operating state. */
	OperatingState(30241, UInt32),

	/** The SMU ID (SSM Identifier). */
	SmuId(30245, UInt32),

	/** String current of string 1 (IString 1), in mA. */
	CurrentString1(31793, Int32),

	/** String current of string 2 (IString 2), in mA. */
	CurrentString2(31795, Int32),

	/** String current of string 3 (IString 3), in mA. */
	CurrentString3(31797, Int32),

	/** String current of string 4 (IString 4), in mA. */
	CurrentString4(31799, Int32),

	/** String current of string 5 (IString 5), in mA. */
	CurrentString5(31801, Int32),

	/** String current of string 6 (IString 6), in mA. */
	CurrentString6(31803, Int32),

	/** String current of string 7 (IString 7), in mA. */
	CurrentString7(31805, Int32),

	/** String current of string 8 (IString 8), in mA. */
	CurrentString8(31807, Int32),

	/** Status of signal contact 1 enumeration (Signal contact 1). */
	SignalContactStatus1(32053, UInt32),

	/** Status of signal contact 2 enumeration (Signal contact 2). */
	SignalContactStatus2(32055, UInt32),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	/** A register address set for general device information. */
	public static final IntRangeSet INFO_REGISTER_ADDRESS_SET = createAddressSet(
			SmaScStringMonitorUsRegister.class, new HashSet<>(asList(SerialNumber.name(), SmuId.name())))
					.immutableCopy();

	/** A register address set for device data. */
	public static final IntRangeSet DATA_REGISTER_ADDRESS_SET;
	static {
		IntRangeSet set = new IntRangeSet();
		for ( IntRange r : SmaCommonDeviceRegister.DATA_REGISTER_ADDRESS_SET.ranges() ) {
			set.addRange(r);
		}
		for ( IntRange r : createAddressSet(SmaScNnnURegister.class, null).ranges() ) {
			if ( !INFO_REGISTER_ADDRESS_SET.contains(r.getMin()) ) {
				set.addRange(r);
			}
		}
		DATA_REGISTER_ADDRESS_SET = set.immutableCopy();
	}

	private SmaScStringMonitorUsRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private SmaScStringMonitorUsRegister(int address, ModbusDataType dataType, int wordLength) {
		this.address = address;
		this.dataType = dataType;
		this.wordLength = wordLength;
	}

	@Override
	public int getAddress() {
		return address;
	}

	@Override
	public ModbusDataType getDataType() {
		return dataType;
	}

	@Override
	public ModbusReadFunction getFunction() {
		return ModbusReadFunction.ReadHoldingRegister;
	}

	@Override
	public int getWordLength() {
		return wordLength;
	}

}
