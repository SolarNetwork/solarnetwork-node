/* ==================================================================
 * PVITLData.java - 21/08/2018 7:49:24 AM
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

package net.solarnetwork.node.hw.yaskawa.mb.inverter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Implementation for accessing PVI-14TL series data.
 * 
 * @author matt
 * @version 2.1
 */
public class PVITLData extends ModbusData implements PVITLDataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Default constructor.
	 */
	public PVITLData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public PVITLData(ModbusData other) {
		super(other);
	}

	/**
	 * Get a snapshot (copy).
	 * 
	 * @return the copy
	 */
	public PVITLData getSnapshot() {
		return new PVITLData(this);
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
		refreshData(conn, ModbusReadFunction.ReadInputRegister, PVITLRegister.getRegisterAddressSet(),
				MAX_RESULTS);
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
		refreshData(conn, ModbusReadFunction.ReadInputRegister,
				PVITLRegister.getInverterRegisterAddressSet(), MAX_RESULTS);
	}

	@Override
	public String getDspFirmwareVersion() {
		Number n = getNumber(PVITLRegister.InfoFirmwareVersions);
		String result = null;
		if ( n != null ) {
			int v = (n.intValue() >> 2) & 0xFF;
			result = String.format("%.2f", v * 0.01);
		}
		return result;
	}

	@Override
	public String getLcdFirmwareVersion() {
		Number n = getNumber(PVITLRegister.InfoFirmwareVersions);
		String result = null;
		if ( n != null ) {
			int v = n.intValue() & 0xFF;
			result = String.format("%.2f", v * 0.01);
		}
		return result;
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		PVITLDataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(4);
		String model = data.getModelName();
		if ( model != null ) {
			result.put(DataAccessor.INFO_KEY_DEVICE_MODEL, model);
		}
		String dspFirmwareVersion = getDspFirmwareVersion();
		String lcdFirmwareVersion = getLcdFirmwareVersion();
		if ( dspFirmwareVersion != null && lcdFirmwareVersion != null ) {
			result.put(DataAccessor.INFO_KEY_DEVICE_VERSION,
					String.format("DSP = %s, LCD = %s", dspFirmwareVersion, lcdFirmwareVersion));
		}
		String s = data.getSerialNumber();
		if ( s != null ) {
			result.put(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, s);
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

	private Float getMilliValueAsFloat(ModbusReference ref) {
		Number n = getNumber(ref);
		if ( n == null ) {
			return null;
		}
		return n.floatValue() / 100;
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
		return getHectoValueAsInteger(PVITLRegister.InverterActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getHectoValueAsInteger(PVITLRegister.InverterApparentPowerTotal);
	}

	@Override
	public Float getFrequency() {
		return getCentiValueAsFloat(PVITLRegister.InverterFrequency);
	}

	@Override
	public Float getPv1Current() {
		return getCentiValueAsFloat(PVITLRegister.InverterPv1Current);
	}

	@Override
	public Float getPv1Voltage() {
		return getCentiValueAsFloat(PVITLRegister.InverterPv1Voltage);
	}

	@Override
	public Integer getPv1Power() {
		Float a = getCentiValueAsFloat(PVITLRegister.InverterPv1Current);
		Float v = getPv1Voltage();
		return (a != null && v != null ? Math.round(a * v) : null);

	}

	@Override
	public Float getPv2Current() {
		return getCentiValueAsFloat(PVITLRegister.InverterPv2Current);
	}

	@Override
	public Float getPv2Voltage() {
		return getCentiValueAsFloat(PVITLRegister.InverterPv2Voltage);
	}

	@Override
	public Integer getPv2Power() {
		Float a = getCentiValueAsFloat(PVITLRegister.InverterPv2Current);
		Float v = getPv2Voltage();
		return (a != null && v != null ? Math.round(a * v) : null);
	}

	@Override
	public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
		return new PhaseDataAccessor(phase);
	}

	@Override
	public AcEnergyDataAccessor reversed() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Float getCurrent() {
		Number a = getNumber(PVITLRegister.InverterCurrentPhaseA);
		Number b = getNumber(PVITLRegister.InverterCurrentPhaseB);
		Number c = getNumber(PVITLRegister.InverterCurrentPhaseC);
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
		Number a = getNumber(PVITLRegister.InverterVoltageLineLinePhaseAPhaseB);
		Number b = getNumber(PVITLRegister.InverterVoltageLineLinePhaseBPhaseC);
		Number c = getNumber(PVITLRegister.InverterVoltageLineLinePhaseCPhaseA);
		return (a != null && b != null && c != null
				? (a.floatValue() + b.floatValue() + c.floatValue()) / 30.0f
				: null);
	}

	@Override
	public Float getLineVoltage() {
		return getVoltage();
	}

	@Override
	public Float getPowerFactor() {
		return getMilliValueAsFloat(PVITLRegister.InverterPowerFactor);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		Number n = getNumber(PVITLRegister.InverterActiveEnergyDelivered);
		return (n != null ? n.longValue() * 1000L : null);
	}

	@Override
	public Long getActiveEnergyDeliveredToday() {
		Number n = getNumber(PVITLRegister.InverterActiveEnergyDeliveredToday);
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
	public Float getDcVoltage() {
		Float v1 = getPv1Voltage();
		Float v2 = getPv2Voltage();
		if ( v1 == null ) {
			return null;
		}
		float f = v1;
		if ( v2 != null ) {
			f = (v1 + v2) / 2.0f;
		}
		return f;
	}

	@Override
	public Float getDcCurrent() {
		Float f1 = getPv1Current();
		if ( f1 == null ) {
			return null;
		}
		Float f2 = getPv2Current();
		float f = f1;
		if ( f2 != null ) {
			f += f2;
		}
		return f;
	}

	@Override
	public Integer getDcPower() {
		Integer w1 = getPv1Power();
		Integer w2 = getPv2Power();
		if ( w1 == null ) {
			return null;
		}
		int w = w1;
		if ( w2 != null ) {
			w += w2;
		}
		return w;
	}

	@Override
	public PVITLInverterState getOperatingState() {
		Number n = getNumber(PVITLRegister.StatusMode);
		if ( n == null ) {
			return null;
		}
		try {
			return PVITLInverterState.forCode(n.intValue());
		} catch ( IllegalArgumentException e ) {
			return null;
		}
	}

	@Override
	public PVITLInverterType getInverterType() {
		String s = getModelName();
		try {
			return PVITLInverterType.forModelName(s);
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
		return getNullTerminatedString(getAsciiString(PVITLRegister.InfoInverterModelName, true));
	}

	@Override
	public String getSerialNumber() {
		Number n = getNumber(PVITLRegister.InfoSerialNumber);
		long s = n.longValue();
		return (n != null ? Long.toHexString(s) : null);
	}

	@Override
	public Float getModuleTemperature() {
		return getCentiValueAsFloat(PVITLRegister.InverterModuleTemperature);
	}

	@Override
	public Float getInternalTemperature() {
		return getCentiValueAsFloat(PVITLRegister.InverterInternalTemperature);
	}

	private class PhaseDataAccessor implements PVITLDataAccessor {

		private final AcPhase phase;

		private PhaseDataAccessor(AcPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Instant getDataTimestamp() {
			return PVITLData.this.getDataTimestamp();
		}

		@Override
		public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
			return PVITLData.this.accessorForPhase(phase);
		}

		@Override
		public Float getFrequency() {
			return PVITLData.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			switch (phase) {
				case PhaseA:
					return getCentiValueAsFloat(PVITLRegister.InverterCurrentPhaseA);

				case PhaseB:
					return getCentiValueAsFloat(PVITLRegister.InverterCurrentPhaseB);

				case PhaseC:
					return getCentiValueAsFloat(PVITLRegister.InverterCurrentPhaseC);

				default:
					return PVITLData.this.getCurrent();
			}
		}

		@Override
		public Float getVoltage() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getVoltage();
			}
		}

		@Override
		public Float getPowerFactor() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getPowerFactor();
			}
		}

		@Override
		public Integer getActivePower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getActivePower();
			}
		}

		@Override
		public Integer getApparentPower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getApparentPower();
			}
		}

		@Override
		public Integer getReactivePower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getReactivePower();
			}
		}

		@Override
		public Float getDcCurrent() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getDcCurrent();
			}
		}

		@Override
		public Float getDcVoltage() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getDcVoltage();
			}
		}

		@Override
		public Integer getDcPower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getDcPower();
			}
		}

		@Override
		public Float getNeutralCurrent() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;
				default:
					return PVITLData.this.getNeutralCurrent();
			}
		}

		@Override
		public Float getLineVoltage() {
			switch (phase) {
				case PhaseA:
					return getCentiValueAsFloat(PVITLRegister.InverterVoltageLineLinePhaseAPhaseB);

				case PhaseB:
					return getCentiValueAsFloat(PVITLRegister.InverterVoltageLineLinePhaseBPhaseC);

				case PhaseC:
					return getCentiValueAsFloat(PVITLRegister.InverterVoltageLineLinePhaseCPhaseA);

				default:
					return PVITLData.this.getLineVoltage();
			}
		}

		@Override
		public Long getActiveEnergyDelivered() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getActiveEnergyDelivered();
			}
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
		public Map<String, Object> getDeviceInfo() {
			return PVITLData.this.getDeviceInfo();
		}

		@Override
		public AcEnergyDataAccessor reversed() {
			return PVITLData.this.reversed();
		}

		@Override
		public PVITLInverterType getInverterType() {
			return PVITLData.this.getInverterType();
		}

		@Override
		public String getModelName() {
			return PVITLData.this.getModelName();
		}

		@Override
		public String getSerialNumber() {
			return PVITLData.this.getSerialNumber();
		}

		@Override
		public Float getModuleTemperature() {
			return PVITLData.this.getModuleTemperature();
		}

		@Override
		public Float getInternalTemperature() {
			return PVITLData.this.getInternalTemperature();
		}

		@Override
		public Long getActiveEnergyDeliveredToday() {
			return PVITLData.this.getActiveEnergyDeliveredToday();
		}

		@Override
		public Float getPv1Voltage() {
			return PVITLData.this.getPv1Voltage();
		}

		@Override
		public Float getPv1Current() {
			return PVITLData.this.getPv1Current();
		}

		@Override
		public Float getPv2Voltage() {
			return PVITLData.this.getPv2Voltage();
		}

		@Override
		public Float getPv2Current() {
			return PVITLData.this.getPv2Current();
		}

		@Override
		public PVITLInverterState getOperatingState() {
			return PVITLData.this.getOperatingState();
		}

		@Override
		public String getDspFirmwareVersion() {
			return PVITLData.this.getDspFirmwareVersion();
		}

		@Override
		public String getLcdFirmwareVersion() {
			return PVITLData.this.getLcdFirmwareVersion();
		}

		@Override
		public Integer getPv1Power() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getPv1Power();
			}
		}

		@Override
		public Integer getPv2Power() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return PVITLData.this.getPv2Power();
			}
		}

	}

}
