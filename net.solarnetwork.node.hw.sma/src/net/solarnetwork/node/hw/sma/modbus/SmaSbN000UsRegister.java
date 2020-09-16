/* ==================================================================
 * SmaSbN000UsRegister.java - 17/09/2020 9:36:00 AM
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
 * Enumeration of Modbus register mappings for SMA SB n000US series devices.
 * 
 * <p>
 * This covers device ID {@literal 268}. These devices <b>do not</b> support the
 * {@link SmaCommonDeviceRegister} values.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaSbN000UsRegister implements ModbusReference {

	/** Device class enumeration (MainModel). */
	MainModel(30051, UInt32),

	/** Serial number of the device. */
	SerialNumber(30057, UInt32),

	/** Message status enumeration (Error). */
	ErrorStatus(30213, Int32),

	/** Maximum possible continuous active power (Plimit), in W. */
	MaximumActivePower(30231, UInt32),

	/** Permanent active power limitation (Pmax), in W. */
	MaximumActivePowerLimit(30233, UInt32),

	/** Status of the backup mode status enumeration (Backup State). */
	BackupMode(30235, UInt32),

	/** Grid type enumeration (Grid Type). */
	GridType(30237, UInt32),

	/** Operating mode enumeration of the PowerBalancer (Balancer). */
	PowerBalanceOperatingMode(30239, UInt32),

	/** Operating state enumeration (mode). */
	OperatingState(30241, UInt32),

	/** The total energy (E-Total), in Wh. */
	TotalYield(30513, UInt64),

	/** The total operating time (h-On), in seconds. */
	OperatingTime(30521, UInt64),

	/** Feed-in time (h-total), in seconds. */
	FeedInTime(30525, UInt64),

	/** DC current input (Ipv), in mA. */
	DcCurrentInput(30769, Int32),

	/** DC voltage input (Vpv), in cV. */
	DcVoltageInput(30771, Int32),

	/** The total active power (Pac), in W. */
	ActivePowerTotal(30775, Int32),

	/** The grid voltage, line 1 to neutral (VacL1), in cV. */
	GridVoltageLine1Neutral(30783, Int32),

	/** The grid voltage, line 2 to neutral (VacL1), in cV. */
	GridVoltageLine2Neutral(30785, Int32),

	/** Grid current (Iac), in mA. */
	GridCurrent(30797, UInt32),

	/** Power frequency (Fac), in cHz. */
	Frequency(30803, UInt32),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	/** A register address set for general device information. */
	public static final IntRangeSet INFO_REGISTER_ADDRESS_SET = createAddressSet(
			SmaSbN000UsRegister.class, new HashSet<>(asList(MainModel.name(), SerialNumber.name())))
					.immutableCopy();

	/** A register address set for device data. */
	public static final IntRangeSet DATA_REGISTER_ADDRESS_SET;
	static {
		IntRangeSet set = new IntRangeSet();
		for ( IntRange r : createAddressSet(SmaSbN000UsRegister.class, null).ranges() ) {
			if ( !INFO_REGISTER_ADDRESS_SET.contains(r.getMin()) ) {
				set.addRange(r);
			}
		}
		DATA_REGISTER_ADDRESS_SET = set.immutableCopy();
	}

	private SmaSbN000UsRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private SmaSbN000UsRegister(int address, ModbusDataType dataType, int wordLength) {
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
