/* ==================================================================
 * SDM630Data.java - 23/01/2016 5:34:07 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.deson.meter;

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDeviceSupport;

/**
 * Encapsulates raw Modbus register data from SDM 360 meters.
 * 
 * @author matt
 * @version 1.2
 */
public class SDM630Data extends BaseSDMData {

	public static final String INFO_KEY_DEVICE_WIRING_TYPE = "Wiring Type";

	// voltage (Float32)
	public static final int ADDR_DATA_V_L1_NEUTRAL = 0;
	public static final int ADDR_DATA_V_L2_NEUTRAL = 2;
	public static final int ADDR_DATA_V_L3_NEUTRAL = 4;
	public static final int ADDR_DATA_V_NEUTRAL_AVERAGE = 42;
	public static final int ADDR_DATA_V_L1_L2 = 200;
	public static final int ADDR_DATA_V_L2_L3 = 202;
	public static final int ADDR_DATA_V_L3_L1 = 204;
	public static final int ADDR_DATA_V_L_L_AVERAGE = 206;

	// current (Float32)
	public static final int ADDR_DATA_I1 = 6;
	public static final int ADDR_DATA_I2 = 8;
	public static final int ADDR_DATA_I3 = 10;
	public static final int ADDR_DATA_I_AVERAGE = 46;
	public static final int ADDR_DATA_I_NEUTRAL = 224;

	// power (Float32)
	public static final int ADDR_DATA_ACTIVE_POWER_P1 = 12;
	public static final int ADDR_DATA_ACTIVE_POWER_P2 = 14;
	public static final int ADDR_DATA_ACTIVE_POWER_P3 = 16;
	public static final int ADDR_DATA_ACTIVE_POWER_TOTAL = 52;
	public static final int ADDR_DATA_APPARENT_POWER_P1 = 18;
	public static final int ADDR_DATA_APPARENT_POWER_P2 = 20;
	public static final int ADDR_DATA_APPARENT_POWER_P3 = 22;
	public static final int ADDR_DATA_APPARENT_POWER_TOTAL = 56;
	public static final int ADDR_DATA_REACTIVE_POWER_P1 = 24;
	public static final int ADDR_DATA_REACTIVE_POWER_P2 = 26;
	public static final int ADDR_DATA_REACTIVE_POWER_P3 = 28;
	public static final int ADDR_DATA_REACTIVE_POWER_TOTAL = 60;

	// power factor (Float32)
	public static final int ADDR_DATA_POWER_FACTOR_P1 = 30;
	public static final int ADDR_DATA_POWER_FACTOR_P2 = 32;
	public static final int ADDR_DATA_POWER_FACTOR_P3 = 34;
	public static final int ADDR_DATA_POWER_FACTOR_TOTAL = 62;

	// frequency (Float32)
	public static final int ADDR_DATA_FREQUENCY = 70;

	// total energy (Float32, k)
	public static final int ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL = 72;
	public static final int ADDR_DATA_ACTIVE_ENERGY_EXPORT_TOTAL = 74;
	public static final int ADDR_DATA_REACTIVE_ENERGY_IMPORT_TOTAL = 76;
	public static final int ADDR_DATA_REACTIVE_ENERGY_EXPORT_TOTAL = 78;

	// control info
	public static final int ADDR_SYSTEM_WIRING_TYPE = 10;
	public static final int ADDR_SYSTEM_SERIAL_NUMBER = 42;

	/**
	 * Default constructor.
	 */
	public SDM630Data() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public SDM630Data(SDM630Data other) {
		super(other);
	}

	/**
	 * Construct with backwards setting.
	 * 
	 * @since 1.2
	 */
	public SDM630Data(boolean backwards) {
		super(backwards);
	}

	@Override
	public String toString() {
		return "SDM630Data{V=" + getVoltage(ADDR_DATA_V_NEUTRAL_AVERAGE) + ",A="
				+ getCurrent(ADDR_DATA_I_AVERAGE) + ",PF=" + getPowerFactor(ADDR_DATA_POWER_FACTOR_TOTAL)
				+ ",Hz=" + getFrequency(ADDR_DATA_FREQUENCY) + ",W="
				+ getPower(ADDR_DATA_ACTIVE_POWER_TOTAL) + ",var="
				+ getPower(ADDR_DATA_REACTIVE_POWER_TOTAL) + ",VA="
				+ getPower(ADDR_DATA_APPARENT_POWER_TOTAL) + ",Wh-I="
				+ getEnergy(ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL) + ",varh-I="
				+ getEnergy(ADDR_DATA_REACTIVE_ENERGY_IMPORT_TOTAL) + ",Wh-E="
				+ getEnergy(ADDR_DATA_ACTIVE_ENERGY_EXPORT_TOTAL) + ",varh-E="
				+ getEnergy(ADDR_DATA_REACTIVE_ENERGY_EXPORT_TOTAL) + "}";
	}

	@Override
	public SDMData getSnapshot() {
		return new SDM630Data(this);
	}

	@Override
	public String dataDebugString() {
		final SDM630Data snapshot = new SDM630Data(this);
		return dataDebugString(snapshot);
	}

	public SDMWiringMode getWiringMode() {
		final Float wiringType = getControlFloat32(ADDR_SYSTEM_WIRING_TYPE);
		if ( wiringType == null ) {
			return null;
		}
		final int type = wiringType.intValue();
		return SDMWiringMode.valueOf(type);
	}

	public String getWiringType() {
		SDMWiringMode mode = getWiringMode();
		if ( mode == null ) {
			return "N/A";
		}
		switch (mode) {
			case OnePhaseTwoWire:
				return "1 phase, 2 wire";

			case ThreePhaseThreeWire:
				return "3 phase, 3 wire";

			case ThreePhaseFourWire:
				return "3 phase, 4 wire";

			default:
				return "Unknown";
		}
	}

	public String getSerialNumber() {
		final Float serialNumber = getControlFloat32(ADDR_SYSTEM_SERIAL_NUMBER);
		return (serialNumber == null ? "N/A" : serialNumber.toString());
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		Map<String, Object> result = new LinkedHashMap<String, Object>(4);
		result.put(ModbusDeviceSupport.INFO_KEY_DEVICE_MODEL, "SDM-360");
		result.put(ModbusDeviceSupport.INFO_KEY_DEVICE_SERIAL_NUMBER, getSerialNumber());
		result.put(INFO_KEY_DEVICE_WIRING_TYPE, getWiringType());
		return result;
	}

	@Override
	public boolean supportsPhase(ACPhase phase) {
		if ( phase == ACPhase.Total ) {
			return true;
		}
		SDMWiringMode wiringMode = getWiringMode();
		if ( wiringMode == null ) {
			return false;
		}
		if ( wiringMode == SDMWiringMode.OnePhaseTwoWire ) {
			// only the Total phase is supported for 1P2
			return false;
		}
		return true;
	}

	@Override
	public String getOperationStatusMessage() {
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(getPower(ADDR_DATA_ACTIVE_POWER_TOTAL));
		buf.append(", VA = ").append(getPower(ADDR_DATA_APPARENT_POWER_TOTAL));
		buf.append(", Wh = ").append(getEnergy(ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL));
		buf.append(", PF = ").append(getPowerFactor(ADDR_DATA_POWER_FACTOR_TOTAL));
		return buf.toString();
	}

	@Override
	protected boolean readMeterDataInternal(ModbusConnection conn) {
		readInputData(conn, ADDR_DATA_V_L1_NEUTRAL, ADDR_DATA_V_L1_NEUTRAL + 79);
		readInputData(conn, ADDR_DATA_V_L1_L2, ADDR_DATA_V_L1_L2 + 25);
		return true;
	}

	@Override
	protected boolean readControlDataInternal(ModbusConnection conn) {
		readHoldingData(conn, ADDR_SYSTEM_WIRING_TYPE, ADDR_SYSTEM_SERIAL_NUMBER + 1);
		return true;
	}

	@Override
	public void populateMeasurements(final ACPhase phase, final GeneralNodeACEnergyDatum datum) {
		SDM630Data sample = new SDM630Data(this);
		switch (phase) {
			case Total:
				populateTotalMeasurements(sample, datum);
				break;

			case PhaseA:
				populatePhaseAMeasurements(sample, datum);
				break;

			case PhaseB:
				populatePhaseBMeasurements(sample, datum);
				break;

			case PhaseC:
				populatePhaseCMeasurements(sample, datum);
				break;
		}
	}

	private void populateTotalMeasurements(final SDMData sample, final GeneralNodeACEnergyDatum datum) {
		datum.setFrequency(sample.getFrequency(ADDR_DATA_FREQUENCY));

		Long whImport = sample.getEnergy(ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL);
		Long whExport = sample.getEnergy(ADDR_DATA_ACTIVE_ENERGY_EXPORT_TOTAL);

		if ( isBackwards() ) {
			datum.setWattHourReading(whExport);
			datum.setReverseWattHourReading(whImport);
		} else {
			datum.setWattHourReading(whImport);
			datum.setReverseWattHourReading(whExport);
		}

		final SDMWiringMode wiringMode = getWiringMode();

		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_TOTAL));
		if ( wiringMode == SDMWiringMode.OnePhaseTwoWire ) {
			datum.setCurrent(sample.getCurrent(ADDR_DATA_I1));
			datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L1_NEUTRAL));
		} else {
			datum.setCurrent(sample.getCurrent(ADDR_DATA_I_AVERAGE));
			datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L_L_AVERAGE));
			datum.setVoltage(sample.getVoltage(ADDR_DATA_V_NEUTRAL_AVERAGE));
		}
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_TOTAL));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_TOTAL));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_TOTAL));
		datum.setWatts((isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_TOTAL));
	}

	private void populatePhaseAMeasurements(final SDMData sample, final GeneralNodeACEnergyDatum datum) {
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_P1));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I1));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L1_L2));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_P1));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_P1));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_P1));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L1_NEUTRAL));
		datum.setWatts((isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_P1));
	}

	private void populatePhaseBMeasurements(final SDMData sample, final GeneralNodeACEnergyDatum datum) {
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_P2));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I2));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L2_L3));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_P2));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_P2));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_P2));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L2_NEUTRAL));
		datum.setWatts((isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_P2));
	}

	private void populatePhaseCMeasurements(final SDMData sample, final GeneralNodeACEnergyDatum datum) {
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_P3));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I3));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L3_L1));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_P3));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_P3));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_P3));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L3_NEUTRAL));
		datum.setWatts((isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_P3));
	}

}
