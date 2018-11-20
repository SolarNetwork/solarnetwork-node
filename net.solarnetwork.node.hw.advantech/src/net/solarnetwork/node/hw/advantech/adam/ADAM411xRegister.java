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

	Channel0BurnOut(200, ModbusDataType.UInt16),

	Channel1BurnOut(201, ModbusDataType.UInt16),

	Channel2BurnOut(202, ModbusDataType.UInt16),

	Channel3BurnOut(203, ModbusDataType.UInt16),

	Channel4BurnOut(204, ModbusDataType.UInt16),

	Channel5BurnOut(205, ModbusDataType.UInt16),

	Channel6BurnOut(206, ModbusDataType.UInt16),

	Channel7BurnOut(207, ModbusDataType.UInt16),

	Channel0(40000, ModbusDataType.Int16),

	Channel1(40001, ModbusDataType.Int16),

	Channel2(40002, ModbusDataType.Int16),

	Channel3(40003, ModbusDataType.Int16),

	Channel4(40004, ModbusDataType.Int16),

	Channel5(40005, ModbusDataType.Int16),

	Channel6(40006, ModbusDataType.Int16),

	Channel7(40007, ModbusDataType.Int16),

	ConfigChannel0InputType(40200, ModbusDataType.UInt16),

	ConfigChannel1InputType(40201, ModbusDataType.UInt16),

	ConfigChannel2InputType(40202, ModbusDataType.UInt16),

	ConfigChannel3InputType(40203, ModbusDataType.UInt16),

	ConfigChannel4InputType(40204, ModbusDataType.UInt16),

	ConfigChannel5InputType(40205, ModbusDataType.UInt16),

	ConfigChannel6InputType(40206, ModbusDataType.UInt16),

	ConfigChannel7InputType(40207, ModbusDataType.UInt16),

	InfoModelName1(40210, ModbusDataType.UInt16),

	InfoModelName2(40211, ModbusDataType.UInt16),

	InfoVersion1(40212, ModbusDataType.UInt16),

	InfoVersion2(40213, ModbusDataType.UInt16),

	ConfigChannelEnable(40220, ModbusDataType.UInt16);

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createConfigRegisterAddressSet();
	private static final IntRangeSet CHANNEL_REGISTER_ADDRESS_SET = createChannelRegisterAddressSet();

	private final int address;
	private final int length;
	private final ModbusDataType dataType;

	private ADAM411xRegister(int address, ModbusDataType dataType) {
		this(address, 0, dataType);
	}

	private ADAM411xRegister(int address, int length, ModbusDataType dataType) {
		this.address = address;
		this.length = length;
		this.dataType = dataType;
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
		return ModbusReadFunction.ReadHoldingRegister;
	}

	@Override
	public int getWordLength() {
		return (this.length > 0 ? this.length : dataType.getWordLength());
	}

	/**
	 * Get an address range set that covers all the registers defined in this
	 * enumeration.
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
