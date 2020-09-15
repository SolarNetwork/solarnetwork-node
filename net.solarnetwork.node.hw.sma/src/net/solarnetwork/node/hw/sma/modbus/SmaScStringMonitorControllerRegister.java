/* ==================================================================
 * SmaScStringMonitorControllerRegister.java - 15/09/2020 9:38:11 AM
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
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import static net.solarnetwork.node.io.modbus.ModbusReference.createAddressSet;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for SMA SC String Monitor Controller
 * series devices.
 * 
 * <p>
 * This covers device ID {@literal 187}, or {@literal 129} profile 1.30 or
 * later. These devices <b>do not</b> support the
 * {@link SmaCommonDeviceRegister} values.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaScStringMonitorControllerRegister implements ModbusReference {

	/** Serial number of the device. */
	SerialNumber(30057, UInt32),

	/** Event ID for current event (ErrNo). */
	Event(30197, UInt32),

	/** Operating state. */
	OperatingState(30241, UInt32),

	/** Error state enumeration (Error). */
	ErrorState(30243, UInt32),

	/** Operating time (SMA h-On), in seconds. */
	OperatingTime(30521, UInt64),

	/** String current of string group 1 (MeanCurGr1), in mA. */
	CurrentGroup1(31283, Int32),

	/** String current of string group 2 (MeanCurGr2), in mA. */
	CurrentGroup2(31289, Int32),

	/** String current of string group 3 (MeanCurGr3), in mA. */
	CurrentGroup3(31295, Int32),

	/** String current of string group 4 (MeanCurGr4), in mA. */
	CurrentGroup4(31301, Int32),

	/** String current of string group 5 (MeanCurGr5), in mA. */
	CurrentGroup5(31307, Int32),

	/** String current of string group 6 (MeanCurGr6), in mA. */
	CurrentGroup6(31313, Int32),

	/** SMU warning code for string error (SSMUWrnCode). */
	SmuWarning(32051, UInt32),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	/** A register address set for general device information. */
	public static final IntRangeSet INFO_REGISTER_ADDRESS_SET = createAddressSet(
			SmaScStringMonitorControllerRegister.class, new HashSet<>(asList(SerialNumber.name())))
					.immutableCopy();

	/** A register address set for device data. */
	public static final IntRangeSet DATA_REGISTER_ADDRESS_SET;
	static {
		IntRangeSet set = new IntRangeSet();
		for ( IntRange r : createAddressSet(SmaScStringMonitorControllerRegister.class, null)
				.ranges() ) {
			if ( !INFO_REGISTER_ADDRESS_SET.contains(r.getMin()) ) {
				set.addRange(r);
			}
		}
		DATA_REGISTER_ADDRESS_SET = set.immutableCopy();
	}

	private SmaScStringMonitorControllerRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private SmaScStringMonitorControllerRegister(int address, ModbusDataType dataType, int wordLength) {
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
