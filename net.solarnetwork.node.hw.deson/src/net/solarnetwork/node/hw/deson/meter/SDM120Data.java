/* ==================================================================
 * SDM120Data.java - 23/01/2016 5:33:22 pm
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
 * Encapsulates raw Modbus register data from SDM 120 meters.
 * 
 * @author matt
 * @version 1.2
 */
public class SDM120Data extends BaseSDMData {

	// voltage (Float32)
	public static final int ADDR_DATA_V_NEUTRAL = 0;

	// current (Float32)
	public static final int ADDR_DATA_I = 6;

	// power (Float32)
	public static final int ADDR_DATA_ACTIVE_POWER = 12;
	public static final int ADDR_DATA_APPARENT_POWER = 18;
	public static final int ADDR_DATA_REACTIVE_POWER = 24;

	// power factor (Float32)
	public static final int ADDR_DATA_POWER_FACTOR = 30;

	// frequency (Float32)
	public static final int ADDR_DATA_FREQUENCY = 70;

	// total energy (Float32, k)
	public static final int ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL = 72;
	public static final int ADDR_DATA_ACTIVE_ENERGY_EXPORT_TOTAL = 74;
	public static final int ADDR_DATA_REACTIVE_ENERGY_IMPORT_TOTAL = 76;
	public static final int ADDR_DATA_REACTIVE_ENERGY_EXPORT_TOTAL = 78;

	/**
	 * Default constructor.
	 */
	public SDM120Data() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public SDM120Data(SDM120Data other) {
		super(other);
	}

	/**
	 * Construct with backwards setting.
	 * 
	 * @since 1.2
	 */
	public SDM120Data(boolean backwards) {
		super(backwards);
	}

	@Override
	public String toString() {
		return "SDM120Data{V=" + getVoltage(ADDR_DATA_V_NEUTRAL) + ",A=" + getCurrent(ADDR_DATA_I)
				+ ",PF=" + getPowerFactor(ADDR_DATA_POWER_FACTOR) + ",Hz="
				+ getFrequency(ADDR_DATA_FREQUENCY) + ",W=" + getPower(ADDR_DATA_ACTIVE_POWER) + ",var="
				+ getPower(ADDR_DATA_REACTIVE_POWER) + ",VA=" + getPower(ADDR_DATA_APPARENT_POWER)
				+ ",Wh-I=" + getEnergy(ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL) + ",varh-I="
				+ getEnergy(ADDR_DATA_REACTIVE_ENERGY_IMPORT_TOTAL) + ",Wh-E="
				+ getEnergy(ADDR_DATA_ACTIVE_ENERGY_EXPORT_TOTAL) + ",varh-E="
				+ getEnergy(ADDR_DATA_REACTIVE_ENERGY_EXPORT_TOTAL) + "}";
	}

	@Override
	public SDMData getSnapshot() {
		return new SDM120Data(this);
	}

	@Override
	public String dataDebugString() {
		final SDM120Data snapshot = new SDM120Data(this);
		return dataDebugString(snapshot);
	}

	@Override
	public boolean supportsPhase(ACPhase phase) {
		return (phase == ACPhase.Total);
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		Map<String, Object> result = new LinkedHashMap<String, Object>(4);
		result.put(ModbusDeviceSupport.INFO_KEY_DEVICE_MODEL, "SDM-120");
		return result;
	}

	@Override
	public String getOperationStatusMessage() {
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(getPower(ADDR_DATA_ACTIVE_POWER));
		buf.append(", VA = ").append(getPower(ADDR_DATA_APPARENT_POWER));
		buf.append(", Wh = ").append(getEnergy(ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL));
		buf.append(", PF = ").append(getPowerFactor(ADDR_DATA_POWER_FACTOR));
		return buf.toString();
	}

	@Override
	public boolean readMeterDataInternal(final ModbusConnection conn) {
		readInputData(conn, ADDR_DATA_V_NEUTRAL, ADDR_DATA_V_NEUTRAL + 79);
		return true;
	}

	@Override
	protected boolean readControlDataInternal(ModbusConnection conn) {
		return true;
	}

	@Override
	public void populateMeasurements(final ACPhase phase, final GeneralNodeACEnergyDatum datum) {
		if ( !ACPhase.Total.equals(phase) ) {
			return;
		}
		SDM120Data sample = new SDM120Data(this);
		populateTotalMeasurements(sample, datum);
	}

	private void populateTotalMeasurements(SDMData sample, GeneralNodeACEnergyDatum datum) {
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

		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_NEUTRAL));
		datum.setWatts((isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER));
	}

}
