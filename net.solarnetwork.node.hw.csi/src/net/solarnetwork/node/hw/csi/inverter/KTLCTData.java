/* ==================================================================
 * KTLCTData.java - 22 Nov 2017 12:28:46
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.csi.inverter;

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation for accessing SI-60KTL-CT data.
 * 
 * @author maxieduncan
 * @version 1.2
 */
public class KTLCTData extends ModbusData implements KTLCTDataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Default constructor.
	 */
	public KTLCTData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public KTLCTData(ModbusData other) {
		super(other);
	}

	public KTLCTData getSnapshot() {
		return new KTLCTData(this);
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
	 */
	public final void readConfigurationData(final ModbusConnection conn) {
		// we actually read ALL registers here, so our snapshot timestamp includes everything
		refreshData(conn, ModbusReadFunction.ReadInputRegister, KTLCTRegister.getRegisterAddressSet(),
				MAX_RESULTS);
	}

	/**
	 * Read the inverter registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readInverterData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadInputRegister,
				KTLCTRegister.getInverterRegisterAddressSet(), MAX_RESULTS);
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		KTLCTDataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(4);
		String model = data.getModelName();
		if ( model != null ) {
			KTLCTInverterType type = data.getInverterType();
			if ( type != null ) {
				result.put(INFO_KEY_DEVICE_MODEL,
						String.format("%s (%s)", model, type.getDescription()));
			} else {
				result.put(INFO_KEY_DEVICE_MODEL, model);
			}
		}
		String s = data.getSerialNumber();
		if ( s != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, s);
		}
		return result;
	}

	private Float getCentiValueAsFloat(ModbusReference ref) {
		Number n = getNumber(ref);
		if ( n == null ) {
			return null;
		}
		return n.floatValue() / 10;
	}

	private Integer getHectoValueAsInteger(ModbusReference ref) {
		Number n = getNumber(ref);
		if ( n == null ) {
			return null;
		}
		return n.intValue() * 100;

	}

	@Override
	public Integer getActivePower() {
		return getHectoValueAsInteger(KTLCTRegister.InverterActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getHectoValueAsInteger(KTLCTRegister.InverterApparentPowerTotal);
	}

	@Override
	public Float getFrequency() {
		return getCentiValueAsFloat(KTLCTRegister.InverterFrequency);
	}

	@Override
	public Float getPv1Voltage() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv1Voltage);
	}

	@Override
	public Float getPv1Current() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv1Current);
	}

	@Override
	public Float getPv2Voltage() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv2Voltage);
	}

	@Override
	public Float getPv2Current() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv2Current);
	}

	@Override
	public Float getPv3Voltage() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv3Voltage);
	}

	@Override
	public Float getPv3Current() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv1Current);
	}

	@Override
	public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ACEnergyDataAccessor reversed() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Float getCurrent() {
		Number a = getNumber(KTLCTRegister.InverterCurrentPhaseA);
		Number b = getNumber(KTLCTRegister.InverterCurrentPhaseB);
		Number c = getNumber(KTLCTRegister.InverterCurrentPhaseC);
		return (a != null && b != null && c != null
				? (a.floatValue() + b.floatValue() + c.floatValue()) / 10.0f
				: null);
	}

	@Override
	public Float getNeutralCurrent() {
		return null;
	}

	@Override
	public Float getVoltage() {
		return getLineVoltage();
	}

	@Override
	public Float getLineVoltage() {
		Number a = getNumber(KTLCTRegister.InverterVoltageLineLinePhaseAPhaseB);
		Number b = getNumber(KTLCTRegister.InverterVoltageLineLinePhaseBPhaseC);
		Number c = getNumber(KTLCTRegister.InverterVoltageLineLinePhaseCPhaseA);
		return (a != null && b != null && c != null
				? (a.floatValue() + b.floatValue() + c.floatValue()) / 30.0f
				: null);
	}

	@Override
	public Float getPowerFactor() {
		Number n = getNumber(KTLCTRegister.InverterPowerFactor);
		return (n != null ? n.floatValue() / 1000.0f : null);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		Number n = getNumber(KTLCTRegister.InverterActiveEnergyDelivered);
		return (n != null ? n.longValue() * 1000L : null);
	}

	@Override
	public Long getActiveEnergyDeliveredToday() {
		Number n = getNumber(KTLCTRegister.InverterActiveEnergyDeliveredToday);
		return (n != null ? n.longValue() * 100L : null);
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
	public Integer getReactivePower() {
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
	public Float getDCVoltage() {
		return getPv1Voltage();
	}

	@Override
	public Integer getDCPower() {
		return Math.round(getPv1Voltage() * getPv1Current());
	}

	@Override
	public KTLCTInverterType getInverterType() {
		Number n = getNumber(KTLCTRegister.InfoInverterModel);
		if ( n == null ) {
			return null;
		}
		try {
			return KTLCTInverterType.forCode(n.intValue());
		} catch ( IllegalArgumentException e ) {
			return null;
		}
	}

	private String getNullTerminatedString(String s) {
		if ( s == null || s.isEmpty() ) {
			return s;
		}
		char[] c = s.toCharArray();
		for ( int i = 0; i < c.length; i++ ) {
			if ( c[i] == 0 ) {
				return s.substring(0, i);
			}
		}
		return s;
	}

	@Override
	public String getModelName() {
		return getNullTerminatedString(getAsciiString(KTLCTRegister.InfoInverterModelName, true));
	}

	@Override
	public String getSerialNumber() {
		Number n = getNumber(KTLCTRegister.InfoSerialNumber);
		return (n != null ? n.toString() : null);
	}

	@Override
	public Float getModuleTemperature() {
		return getCentiValueAsFloat(KTLCTRegister.InverterModuleTemperature);
	}

	@Override
	public Float getInternalTemperature() {
		return getCentiValueAsFloat(KTLCTRegister.InverterInternalTemperature);
	}

	@Override
	public Float getTransformerTemperature() {
		return getCentiValueAsFloat(KTLCTRegister.InverterTransformerTemperature);
	}

}
