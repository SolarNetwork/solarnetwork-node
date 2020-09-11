/* ==================================================================
 * WebBoxPlantRegister.java - 11/09/2020 3:54:36 PM
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

package net.solarnetwork.node.hw.sma.modbus.webbox;

import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Enumeration of common Modbus register mappings for the SMA WebBox devices
 * (unit ID 3-247).
 * 
 * @author matt
 * @version 1.0
 */
public enum WebBoxCommonDeviceRegister implements ModbusReference {

	/** Serial number of the WebBox. */
	SerialNumber(30057, UInt32),

	/** The plant time (Unix epoch UTC). */
	Date(30193, UInt32),

	/** Event ID for current event (ErrNo). */
	Event(30197, UInt32),

	/** Maximum possible continuous active power (Plimit), in W. */
	MaximumActivePower(30231, UInt32),

	/** Permanent active power limitation (Pmax), in W. */
	MaximumActivePowerLimit(30233, UInt32),

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

	/** DC power input (Ppv), in W. */
	DcPowerInput(30773, Int32),

	/** The total active power (Pac), in W. */
	ActivePowerTotal(30775, Int32),

	/** Grid voltage phase AB (VacL12), in cV. */
	GridVoltageLine1Line2(30789, UInt32),

	/** Grid voltage phase BC (VacL23), in cV. */
	GridVoltageLine2Line3(30791, UInt32),

	/** Grid voltage phase CA (VacL31), in cV. */
	GridVoltageLine3Line1(30793, UInt32),

	/** Grid current (Iac), in mA. */
	GridCurrent(30795, UInt32),

	/** Power frequency (Fac), in cHz. */
	Frequency(30803, UInt32),

	/** Reactive power (Qac), in cVAR. */
	ReactivePower(30805, Int32),

	/** Apparent power (Sac), in VA. */
	ApparentPower(30813, Int32),

	/** Active power target (P-WSpt), in W. */
	ActivePowerTarget(30837, UInt32),

	/** Heat sink temperature 1 (TmpHs), in dCel. */
	HeatSinkTemperature(34109, Int32),

	/** Interior temperature (TmpCab1), in dCel. */
	InteriorTemperature(34113, Int32),

	/** External temperature (TmpExl1), in dCel. */
	ExternalTemperature1(34125, Int32),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	/** The starting Modbus address for the connected device list. */
	public static final int DEVICE_UNIT_IDS_STARTING_ADDRESS = 42109;

	private WebBoxCommonDeviceRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private WebBoxCommonDeviceRegister(int address, ModbusDataType dataType, int wordLength) {
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
