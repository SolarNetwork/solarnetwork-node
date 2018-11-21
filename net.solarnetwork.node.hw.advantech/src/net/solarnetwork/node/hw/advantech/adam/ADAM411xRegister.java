/* ==================================================================
 * ADAM411xRegister.java - 20/11/2018 4:21:24 PM
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

package net.solarnetwork.node.hw.advantech.adam;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Enumeration of Modbus register mappings for the ADAM 411x series input
 * modules.
 * 
 * @author matt
 * @version 1.0
 */
public enum ADAM411xRegister implements ModbusReference {

	CoilChannel0BurnOut(0, ModbusDataType.UInt16, ModbusReadFunction.ReadCoil),

	CoilChannel1BurnOut(1, ModbusDataType.UInt16, ModbusReadFunction.ReadCoil),

	CoilChannel2BurnOut(2, ModbusDataType.UInt16, ModbusReadFunction.ReadCoil),

	CoilChannel3BurnOut(3, ModbusDataType.UInt16, ModbusReadFunction.ReadCoil),

	CoilChannel4BurnOut(4, ModbusDataType.UInt16, ModbusReadFunction.ReadCoil),

	CoilChannel5BurnOut(5, ModbusDataType.UInt16, ModbusReadFunction.ReadCoil),

	CoilChannel6BurnOut(6, ModbusDataType.UInt16, ModbusReadFunction.ReadCoil),

	CoilChannel7BurnOut(7, ModbusDataType.UInt16, ModbusReadFunction.ReadCoil),

	Channel0(0, ModbusDataType.Int16),

	Channel1(1, ModbusDataType.Int16),

	Channel2(2, ModbusDataType.Int16),

	Channel3(3, ModbusDataType.Int16),

	Channel4(4, ModbusDataType.Int16),

	Channel5(5, ModbusDataType.Int16),

	Channel6(6, ModbusDataType.Int16),

	Channel7(7, ModbusDataType.Int16),

	ConfigChannel0InputType(200, ModbusDataType.UInt16),

	ConfigChannel1InputType(201, ModbusDataType.UInt16),

	ConfigChannel2InputType(202, ModbusDataType.UInt16),

	ConfigChannel3InputType(203, ModbusDataType.UInt16),

	ConfigChannel4InputType(204, ModbusDataType.UInt16),

	ConfigChannel5InputType(205, ModbusDataType.UInt16),

	ConfigChannel6InputType(206, ModbusDataType.UInt16),

	ConfigChannel7InputType(207, ModbusDataType.UInt16),

	InfoModelName1(210, ModbusDataType.UInt16),

	InfoModelName2(211, ModbusDataType.UInt16),

	InfoVersion1(212, ModbusDataType.UInt16),

	InfoVersion2(213, ModbusDataType.UInt16),

	ConfigChannelEnable(220, ModbusDataType.UInt16);

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createConfigRegisterAddressSet();
	private static final IntRangeSet CHANNEL_REGISTER_ADDRESS_SET = createChannelRegisterAddressSet();

	private final int address;
	private final int length;
	private final ModbusDataType dataType;
	private final ModbusReadFunction function;

	private ADAM411xRegister(int address, ModbusDataType dataType) {
		this(address, 0, dataType, ModbusReadFunction.ReadHoldingRegister);
	}

	private ADAM411xRegister(int address, ModbusDataType dataType, ModbusReadFunction function) {
		this(address, 0, dataType, function);
	}

	private ADAM411xRegister(int address, int length, ModbusDataType dataType,
			ModbusReadFunction function) {
		this.address = address;
		this.length = length;
		this.dataType = dataType;
		this.function = function;
	}

	private static IntRangeSet createRegisterAddressSet(Set<String> prefixes) {
		IntRangeSet set = new IntRangeSet();
		for ( ADAM411xRegister r : ADAM411xRegister.values() ) {
			for ( String prefix : prefixes ) {
				if ( r.name().startsWith(prefix) ) {
					int len = r.getWordLength();
					if ( len > 0 ) {
						set.addAll(r.getAddress(), r.getAddress() + len - 1);
					}
					break;
				}
			}
		}
		return set;
	}

	private static IntRangeSet createChannelRegisterAddressSet() {
		return createRegisterAddressSet(Collections.singleton("Channel"));
	}

	private static IntRangeSet createConfigRegisterAddressSet() {
		return createRegisterAddressSet(new HashSet<>(Arrays.asList("Info", "Config")));
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
		return function;
	}

	@Override
	public int getWordLength() {
		return (this.length > 0 ? this.length : dataType.getWordLength());
	}

	/**
	 * Get an address range set that covers all the non-coil registers defined
	 * in this enumeration.
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getRegisterAddressSet() {
		IntRangeSet s = new IntRangeSet(CONFIG_REGISTER_ADDRESS_SET);
		s.addAll(CHANNEL_REGISTER_ADDRESS_SET);
		return s;
	}

	/**
	 * Get an address range set that covers all the configuration and info
	 * registers defined in this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getConfigRegisterAddressSet() {
		return (IntRangeSet) CONFIG_REGISTER_ADDRESS_SET.clone();
	}

	/**
	 * Get an address range set that covers all the channel registers defined in
	 * this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getChannelRegisterAddressSet() {
		return (IntRangeSet) CHANNEL_REGISTER_ADDRESS_SET.clone();
	}

}
