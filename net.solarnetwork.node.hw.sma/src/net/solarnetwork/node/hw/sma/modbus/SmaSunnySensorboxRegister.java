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
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
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
 * This covers device ID {@literal 81}. These devices <b>do not</b> support the
 * {@link SmaCommonDeviceRegister} values.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaSunnySensorboxRegister implements ModbusReference {

	/** Device class enumeration (MainModel). */
	MainModel(30051, UInt32),

	/** Serial number of the device. */
	SerialNumber(30057, UInt32),

	/** Operating time (SMA h-On), in seconds. */
	OperatingTime(30521, UInt64),

	/** Environment temperature (TmpAmb C), dCel. */
	AmbientTemperature(34609, Int32),

	/** Total irradiation on sensor surface (IntSolIrr), in W/m2. */
	Irradiance(34613, UInt32),

	/** Wind speed (WindVel m/s), in m/s. */
	WindSpeed(34615, UInt32),

	/** PV module temperature (TmpMdul C), in dCel. */
	ModuleTemperature(34621, Int32),

	/**
	 * Total irradiation on external sensor/pyranometer (ExlSolIrr), in W/m2.
	 */
	ExternalIrradiance(34623, UInt32),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	/** A register address set for general device information. */
	public static final IntRangeSet INFO_REGISTER_ADDRESS_SET = createAddressSet(
			SmaSunnySensorboxRegister.class,
			new HashSet<>(asList(MainModel.name(), SerialNumber.name()))).immutableCopy();

	/** A register address set for device data. */
	public static final IntRangeSet DATA_REGISTER_ADDRESS_SET;
	static {
		IntRangeSet set = new IntRangeSet();
		for ( IntRange r : createAddressSet(SmaSunnySensorboxRegister.class, null).ranges() ) {
			if ( !INFO_REGISTER_ADDRESS_SET.contains(r.getMin()) ) {
				set.addRange(r);
			}
		}
		DATA_REGISTER_ADDRESS_SET = set.immutableCopy();
	}

	private SmaSunnySensorboxRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private SmaSunnySensorboxRegister(int address, ModbusDataType dataType, int wordLength) {
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
