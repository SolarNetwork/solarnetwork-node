/* ==================================================================
 * ADAM411xData.java - 21/11/2018 7:11:37 PM
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Data object for the ADAM 411x series devices.
 * 
 * @author matt
 * @version 1.0
 */
public class ADAM411xData extends ModbusData implements ADAM411xDataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Constructor.
	 */
	public ADAM411xData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public ADAM411xData(ModbusData other) {
		super(other);
	}

	@Override
	public ModbusData copy() {
		return new ADAM411xData(this);
	}

	/**
	 * Get a snapshot copy of the data.
	 * 
	 * @return a copy of the data
	 * @see #copy()
	 */
	public ADAM411xData getSnapshot() {
		return (ADAM411xData) copy();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		ADAM411xDataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(4);
		String modelName = data.getModelName();
		if ( modelName != null ) {
			String version = data.getFirmwareRevision();
			if ( version != null ) {
				result.put(INFO_KEY_DEVICE_MODEL,
						String.format("%s (firmware revision %s)", modelName, version));
			} else {
				result.put(INFO_KEY_DEVICE_MODEL, modelName);
			}
		}
		Set<Integer> enabledChannels = data.getEnabledChannelNumbers();
		if ( enabledChannels != null && !enabledChannels.isEmpty() ) {
			for ( final int channel : enabledChannels ) {
				InputRangeType type = data.getChannelType(channel);
				result.put("Channel " + channel, type.toStringDescription());
			}
		}
		return result;
	}

	/**
	 * Read the configuration and information registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readConfigurationData(final ModbusConnection conn) {
		// we actually read ALL registers here, so our snapshot timestamp includes everything
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				ADAM411xRegister.getRegisterAddressSet(), MAX_RESULTS);
	}

	/**
	 * Read the channel registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readDeviceData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				ADAM411xRegister.getChannelRegisterAddressSet(), MAX_RESULTS);
	}

	@Override
	public String getModelName() {
		Number n = getNumber(ADAM411xRegister.InfoModelName1);
		return (n != null ? Integer.toString(n.intValue(), 16) : null);
	}

	@Override
	public String getFirmwareRevision() {
		Number n = getNumber(ADAM411xRegister.InfoVersion1);
		return (n != null ? Integer.toString(n.intValue(), 16).toUpperCase() : null);
	}

	@Override
	public Set<Integer> getEnabledChannelNumbers() {
		Number n = getNumber(ADAM411xRegister.ConfigChannelEnable);
		int bitmask = (n != null ? n.intValue() : 0);
		Set<Integer> enabled = new LinkedHashSet<>(8);
		for ( int i = 0; i < 8; i++ ) {
			if ( ((bitmask >> i) & 1) == 1 ) {
				enabled.add(i);
			}
		}
		return enabled;
	}

	@Override
	public InputRangeType getChannelType(int channelNumber) {
		final ADAM411xRegister reg;
		switch (channelNumber) {
			case 0:
				reg = ADAM411xRegister.ConfigChannel0InputType;
				break;

			case 1:
				reg = ADAM411xRegister.ConfigChannel1InputType;
				break;

			case 2:
				reg = ADAM411xRegister.ConfigChannel2InputType;
				break;

			case 3:
				reg = ADAM411xRegister.ConfigChannel3InputType;
				break;

			case 4:
				reg = ADAM411xRegister.ConfigChannel4InputType;
				break;

			case 5:
				reg = ADAM411xRegister.ConfigChannel5InputType;
				break;

			case 6:
				reg = ADAM411xRegister.ConfigChannel6InputType;
				break;

			case 7:
				reg = ADAM411xRegister.ConfigChannel7InputType;
				break;

			default:
				throw new IllegalArgumentException("Channel " + channelNumber + " not supported");

		}
		Number n = getNumber(reg);
		if ( n != null ) {
			try {
				return InputRangeType.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return InputRangeType.Unknown;
	}

	@Override
	public BigDecimal getChannelValue(int channelNumber) {
		final ADAM411xRegister reg;
		switch (channelNumber) {
			case 0:
				reg = ADAM411xRegister.Channel0;
				break;

			case 1:
				reg = ADAM411xRegister.Channel1;
				break;

			case 2:
				reg = ADAM411xRegister.Channel2;
				break;

			case 3:
				reg = ADAM411xRegister.Channel3;
				break;

			case 4:
				reg = ADAM411xRegister.Channel4;
				break;

			case 5:
				reg = ADAM411xRegister.Channel5;
				break;

			case 6:
				reg = ADAM411xRegister.Channel6;
				break;

			case 7:
				reg = ADAM411xRegister.Channel7;
				break;

			default:
				throw new IllegalArgumentException("Channel " + channelNumber + " not supported");

		}
		Number n = getNumber(reg);
		InputRangeType type = getChannelType(channelNumber);
		return type.normalizedDataValue(n.intValue());
	}

}
