/* ==================================================================
 * SmaScNnnUDeviceRegister.java - 12/09/2020 12:25:06 PM
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

import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import static net.solarnetwork.node.io.modbus.ModbusReference.createAddressSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for SMA SC nnnU series devices.
 * 
 * <p>
 * This covers device ID {@literal 155}, or {@literal 87} profile 1.30 or later.
 * These devices all support the {@link SmaCommonDeviceRegister} values.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum SmaScNnnURegister implements ModbusReference {

	/** Time until grid connection attempt (TmsRmg), in seconds. */
	GridConnectTimeRemaining(30199, UInt32),

	/** Recommended action enumeration (Prio). */
	RecommendedAction(30211, UInt32),

	/** Grid contactor status enumeration (GdCtcStt). */
	GridContactorStatus(30217, UInt32),

	/** Operating state enumeration (mode). */
	OperatingState(30241, UInt32),

	/** Error state enumeration (Error). */
	ErrorState(30243, UInt32),

	/** Current event number for manufacturer (ErrNoSma). */
	SmaErrorId(30247, UInt32),

	/** DC switch in cabinet state enumeration (DcSwStt). */
	DcSwitchState(30257, UInt32),

	/** AC switch 1 in cabinet state enumeration (AcSwStt). */
	AcSwitchState(30261, UInt32),

	/** AC switch-disconnector in cabinet state enumeration (DInErrAcScir). */
	AcSwitchDisconnectState(30265, UInt32),

	/** Grid current line 1 (IacL1), in mA. */
	GridCurrentLine1(30797, UInt32),

	/** Grid current line 2 (IacL2), in mA. */
	GridCurrentLine2(30799, UInt32),

	/** Grid current line 3 (IacL3), in mA. */
	GridCurrentLine(30801, UInt32),

	/** Operating mode of active power limitation enumeration (P-WMod). */
	ActivePowerLimitationOperatingMode(30835, UInt32),

	/** Active power target value (PWNom), in percentage. */
	ActivePowerTarget(30839, UInt32),

	/** AC voltages average of all string voltages (Vac), in cV. */
	AcVoltageTotal(30841, UInt32),

	/** Operating time interior fan 2 (CntFanCab2), in seconds. */
	Fan2OperatingTime(34101, UInt64),

	/** OPerating time heat sink fan (CntFanHs), in seconds. */
	HeatSinkFanOperatingTime(34105, UInt64),

	/** Interior temperature 2 (TmpCab2), in dCel. */
	InteriorTemperature2(34115, Int32),

	/** Transformer temperature (TmpTrf), in dCel. */
	TransformerTemperature(34115, Int32),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	/** A register address set for general device information. */
	public static final IntRangeSet INFO_REGISTER_ADDRESS_SET = SmaCommonDeviceRegister.INFO_REGISTER_ADDRESS_SET;

	/** A register address set for device data. */
	public static final IntRangeSet DATA_REGISTER_ADDRESS_SET;
	static {
		IntRangeSet set = new IntRangeSet();
		for ( IntRange r : SmaCommonDeviceRegister.DATA_REGISTER_ADDRESS_SET.ranges() ) {
			set.addRange(r);
		}
		for ( IntRange r : createAddressSet(SmaScNnnURegister.class, null).ranges() ) {
			set.addRange(r);
		}
		DATA_REGISTER_ADDRESS_SET = set.immutableCopy();
	}

	private SmaScNnnURegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private SmaScNnnURegister(int address, ModbusDataType dataType, int wordLength) {
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
