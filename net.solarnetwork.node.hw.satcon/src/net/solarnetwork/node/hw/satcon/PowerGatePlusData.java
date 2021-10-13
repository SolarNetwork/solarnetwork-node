/* ==================================================================
 * PowerGatePlusData.java - 8/11/2019 11:54:46 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.satcon;

import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import static net.solarnetwork.util.NumberUtils.scaled;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.NumberUtils;

/**
 * Implementation for accessing Power Gate Plus data.
 * 
 * @author matt
 * @version 2.0
 */
public class PowerGatePlusData extends ModbusData implements PowerGateInverterDataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Default constructor.
	 */
	public PowerGatePlusData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public PowerGatePlusData(ModbusData other) {
		super(other);
	}

	public PowerGatePlusData getSnapshot() {
		return new PowerGatePlusData(this);
	}

	@Override
	public ModbusData copy() {
		return getSnapshot();
	}

	/**
	 * Read the configuration and information registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void readConfigurationData(final ModbusConnection conn) throws IOException {
		// we actually read ALL registers here, so our snapshot timestamp includes everything
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				PowerGatePlusRegister.getRegisterAddressSet(), MAX_RESULTS);
		readControlData(conn);
	}

	/**
	 * Read the inverter registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void readInverterData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				PowerGatePlusRegister.getInverterRegisterAddressSet(), MAX_RESULTS);
	}

	/**
	 * Read the control registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 1.4
	 */
	public final void readControlData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				PowerGatePlusRegister.getControlRegisterAddressSet(), MAX_RESULTS);
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		PowerGateInverterDataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(4);
		String version = data.getFirmwareVersion();
		if ( version != null ) {
			result.put("Firmware Version", version);
		}
		String s = data.getSerialNumber();
		if ( s != null ) {
			result.put(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, s);
		}
		PowerGateOperatingState opState = getOperatingState();
		if ( opState != null ) {
			result.put("Operating state", opState);
		}
		Set<? extends Fault> faults = data.getFaults();
		if ( faults != null && !faults.isEmpty() ) {
			result.put("Warnings", commaDelimitedStringFromCollection(faults));
		}
		return result;
	}

	@Override
	public PowerGateOperatingState getOperatingState() {
		Number n = getNumber(PowerGatePlusRegister.StatusOperatingState);
		PowerGateOperatingState state = null;
		if ( n != null ) {
			try {
				state = PowerGateOperatingState.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return state;
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		PowerGateOperatingState state = getOperatingState();
		return (state != null ? state.asDeviceOperatingState() : DeviceOperatingState.Unknown);
	}

	@Override
	public String getSerialNumber() {
		final int baseAddress = PowerGatePlusRegister.InfoSerialNumber.getAddress();
		int x = getUnsignedInt16(baseAddress);
		int y = getUnsignedInt16(baseAddress + 1);
		int a = getUnsignedInt16(baseAddress + 2);
		int z = getUnsignedInt16(baseAddress + 3);
		return String.format("%d%03d%c-%03d", x, y, 'A' + a - 1, z);
	}

	@Override
	public String getFirmwareVersion() {
		Number n = getNumber(PowerGatePlusRegister.InfoDpcbFirmwareVersion);
		String s = (n != null ? n.toString() : null);
		if ( s != null && s.length() > 2 ) {
			s = s.substring(0, s.length() - 2) + "." + s.substring(s.length() - 2);
		}
		return s;
	}

	@Override
	public Set<? extends Fault> getFaults() {
		Set<Fault> all = new LinkedHashSet<>();
		for ( int i = 0; i < 7; i++ ) {
			all.addAll(getFaults(i));
		}
		return all;
	}

	@Override
	public Set<? extends Fault> getFaults(int group) {
		PowerGatePlusRegister reg = null;
		switch (group) {
			case 0:
				reg = PowerGatePlusRegister.StatusFault0Bitmask;
				break;

			case 1:
				reg = PowerGatePlusRegister.StatusFault1Bitmask;
				break;

			case 2:
				reg = PowerGatePlusRegister.StatusFault2Bitmask;
				break;

			case 3:
				reg = PowerGatePlusRegister.StatusFault3Bitmask;
				break;

			case 4:
				reg = PowerGatePlusRegister.StatusFault4Bitmask;
				break;

			case 5:
				reg = PowerGatePlusRegister.StatusFault5Bitmask;
				break;

			case 6:
				reg = PowerGatePlusRegister.StatusFault6Bitmask;
				break;

			default:
				throw new IllegalArgumentException("Fault group " + group + " is not valid.");
		}

		Number n = getNumber(reg);
		if ( n == null ) {
			return null;
		}

		switch (group) {
			case 0:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault0.class);

			case 1:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault1.class);

			case 2:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault2.class);

			case 3:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault3.class);

			case 4:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault4.class);

			case 5:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault5.class);

			case 6:
				return Bitmaskable.setForBitmask(n.intValue(), PowerGateFault6.class);

			default:
				throw new IllegalArgumentException("Fault group " + group + " is not valid.");
		}
	}

	@Override
	public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AcEnergyDataAccessor reversed() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Float getFrequency() {
		Number n = getNumber(PowerGatePlusRegister.InverterFrequency);
		n = NumberUtils.scaled(n, -2);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getCurrent() {
		Number n = getNumber(PowerGatePlusRegister.InverterCurrentAverage);
		return (n != null ? n.floatValue() : 0);
	}

	@Override
	public Float getNeutralCurrent() {
		Number n = getNumber(PowerGatePlusRegister.InverterCurrentNeutral);
		n = NumberUtils.scaled(n, -1);
		return (n != null ? n.floatValue() : 0);
	}

	@Override
	public Float getVoltage() {
		return getLineVoltage();
	}

	@Override
	public Float getLineVoltage() {
		Number n = getNumber(PowerGatePlusRegister.InverterVoltageAverage);
		return (n != null ? n.floatValue() : 0);
	}

	@Override
	public Float getPowerFactor() {
		Number n = getNumber(PowerGatePlusRegister.InverterPowerFactor);
		n = scaled(n, -3);
		return (n != null ? n.floatValue() : 0);
	}

	private Integer getHectoValueAsInteger(ModbusReference ref) {
		Number n = getNumber(ref);
		n = scaled(n, 2);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public Integer getActivePower() {
		return getHectoValueAsInteger(PowerGatePlusRegister.InverterActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getHectoValueAsInteger(PowerGatePlusRegister.InverterApparentPowerTotal);
	}

	@Override
	public Integer getReactivePower() {
		return getHectoValueAsInteger(PowerGatePlusRegister.InverterReactivePowerTotal);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		Number w = getNumber(PowerGatePlusRegister.InverterActiveEnergyDelivered);
		Number kw = getNumber(PowerGatePlusRegister.InverterActiveEnergyDeliveredKilo);
		Number mw = getNumber(PowerGatePlusRegister.InverterActiveEnergyDeliveredMega);
		Number n = scaled(mw, 6).add(scaled(kw, 3)).add(bigDecimalForNumber(w));
		return (n != null ? n.longValue() : null);
	}

	@Override
	public Long getActiveEnergyDeliveredToday() {
		Number n = scaled(getNumber(PowerGatePlusRegister.InverterActiveEnergyDeliveredToday), 3);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public Long getActiveEnergyReceived() {
		return null;
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return null;
	}

	@Override
	public Long getApparentEnergyReceived() {
		return null;
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return null;
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return null;
	}

	@Override
	public Float getDcVoltage() {
		Number n = getNumber(PowerGatePlusRegister.InverterDcLinkVoltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getDcPower() {
		Number v = getNumber(PowerGatePlusRegister.InverterDcLinkVoltage);
		Number a = getNumber(PowerGatePlusRegister.InverterDcLinkCurrent);
		return (v != null && a != null
				? bigDecimalForNumber(v).multiply(bigDecimalForNumber(a)).intValue()
				: null);
	}

	@Override
	public Float getInternalTemperature() {
		Number n = getNumber(PowerGatePlusRegister.InverterInternalAirTemperature);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getInverterTemperature() {
		Number n = getNumber(PowerGatePlusRegister.InverterInverterAirTemperature);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public int getHeatsinkTemperatureCount() {
		Number n = getNumber(PowerGatePlusRegister.InfoTemperatureSensorCount);
		return (n != null ? n.intValue() : 0);
	}

	@Override
	public Float getHeatsinkTemperature(final int index) {
		if ( index < 1 || index > 6 ) {
			throw new IllegalArgumentException(
					"Heatsink temperature module number " + index + " out of range 1-6.");
		}
		int size = PowerGatePlusRegister.InverterHeatsinkTemperature1.getWordLength();
		Number n = getNumber(PowerGatePlusRegister.InverterHeatsinkTemperature1, (index - 1) * size);
		return (n != null ? n.floatValue() : null);
	}

}
