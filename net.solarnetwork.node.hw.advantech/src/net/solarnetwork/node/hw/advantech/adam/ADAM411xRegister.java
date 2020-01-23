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

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.IntRangeSetUtils.createRegisterAddressSet;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for the ADAM 411x series input
 * modules.
 * 
 * @author matt
 * @version 2.0
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

	Channel0(0, ModbusDataType.UInt16),

	Channel1(1, ModbusDataType.UInt16),

	Channel2(2, ModbusDataType.UInt16),

	Channel3(3, ModbusDataType.UInt16),

	Channel4(4, ModbusDataType.UInt16),

	Channel5(5, ModbusDataType.UInt16),

	Channel6(6, ModbusDataType.UInt16),

	Channel7(7, ModbusDataType.UInt16),

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

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createRegisterAddressSet(
			ADAM411xRegister.class, new HashSet<>(asList("Config", "Info"))).immutableCopy();
	private static final IntRangeSet CHANNEL_REGISTER_ADDRESS_SET = createRegisterAddressSet(
			ADAM411xRegister.class, new HashSet<>(asList("Channel"))).immutableCopy();
	private static final IntRangeSet CHANNEL_CONFIG_REGISTER_ADDRESS_SET = createChannelConfigRegisterAddressSet();
	private static final IntRangeSet COIL_REGISTER_ADDRESS_SET = createRegisterAddressSet(
			ADAM411xRegister.class, new HashSet<>(asList("Coil"))).immutableCopy();
	private static final IntRangeSet CHANNEL_WITH_CONFIG_REGISTER_ADDRESS_SET = createChannelWithConfigRegisterAddressSet();

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

	private static IntRangeSet createChannelConfigRegisterAddressSet() {
		IntRangeSet set = new IntRangeSet();
		ADAM411xRegister[] regs = new ADAM411xRegister[] { ConfigChannel0InputType,
				ConfigChannel1InputType, ConfigChannel2InputType, ConfigChannel3InputType,
				ConfigChannel4InputType, ConfigChannel5InputType, ConfigChannel6InputType,
				ConfigChannel7InputType };
		for ( ADAM411xRegister r : regs ) {
			int len = r.getWordLength();
			set.addRange(r.getAddress(), r.getAddress() + len - 1);
		}
		return set.immutableCopy();
	}

	private static IntRangeSet createChannelWithConfigRegisterAddressSet() {
		IntRangeSet s = new IntRangeSet(CHANNEL_REGISTER_ADDRESS_SET);
		s.addAll(CHANNEL_CONFIG_REGISTER_ADDRESS_SET);
		return s.immutableCopy();
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
		return CONFIG_REGISTER_ADDRESS_SET;
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
		return CHANNEL_REGISTER_ADDRESS_SET;
	}

	/**
	 * Get an address range set that covers all the channel registers defined in
	 * this enumeration, along with their configuration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getChannelWithConfigRegisterAddressSet() {
		return CHANNEL_WITH_CONFIG_REGISTER_ADDRESS_SET;
	}

	/**
	 * Get an address range set that covers all the coil registers defined in
	 * this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 * @since 1.1
	 */
	public static IntRangeSet getCoilRegisterAddressSet() {
		return COIL_REGISTER_ADDRESS_SET;
	}

}
