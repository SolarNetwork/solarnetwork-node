/* ==================================================================
 * SI60KTLCTData.java - 22 Nov 2017 12:28:46
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

import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Implementation for accessing SI-60KTL-CT data.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class SI60KTLCTData extends BaseKTLData {

	public static final int MODEL_1 = 16433;
	public static final int MODEL_2 = 16434;
	public static final int MODEL_3 = 16435;

	public static final int ADDR_START = 0;
	public static final int ADDR_LENGTH = 59;

	public static final int ADDR_DEVICE_MODEL = 0;

	/**
	 * Total accumulated energy production {@literal 0x16}, in kWh as 32-bit
	 * unsigned.
	 */
	public static final int ADDR_TOTAL_ENERGY_EXPORT = 22;

	/** AC Active Power {@literal 0x1D}, in 100 W. */
	public static final int ADDR_ACTIVE_POWER = 29;

	/** AC Apparent Power {@literal 0x1E} in 100 VA. */
	public static final int ADDR_APPARENT_POWER = 30;

	/** PV 1 voltage {@literal 0x25} in 0.1 V. */
	public static final int ADDR_PV_1_VOLTAGE = 37;

	/** PV 1 current {@literal 0x26} in 0.1 A. */
	public static final int ADDR_PV_1_CURRENT = 38;

	/** PV 2 voltage {@literal 0x27} in 0.1 V. */
	public static final int ADDR_PV_2_VOLTAGE = 39;

	/** PV 2 current {@literal 0x28} in 0.1 A. */
	public static final int ADDR_PV_2_CURRENT = 40;

	/** PV 3 voltage {@literal 0x29} in 0.1 V. */
	public static final int ADDR_PV_3_VOLTAGE = 41;

	/** PV 3 current {@literal 0x2A} in 0.1 A. */
	public static final int ADDR_PV_3_CURRENT = 42;

	/** The frequency {@literal 0x2B}, in 0.1 Hz. */
	public static final int ADDR_FREQUENCY = 43;

	/** The heatsink temperature {@literal 0x2C}, in 0.1 C. */
	public static final int ADDR_HEATSINK_TEMPERATURE = 44;

	/** The ambient temperature {@literal 0x2D}, in 0.1 C. */
	public static final int ADDR_AMBIENT_TEMPERATURE = 45;

	/**
	 * Default constructor.
	 */
	public SI60KTLCTData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public SI60KTLCTData(ModbusData other) {
		super(other);
	}

	@Override
	public void populateMeasurements(GeneralNodeACEnergyDatum datum) {
		datum.setFrequency(getFrequency());
		datum.setWatts(getActivePower());
		datum.setApparentPower(getApparentPower());
		datum.setWattHourReading(getTotalEnergyExport());

		datum.setVoltage(getPv1Voltage());
		datum.setCurrent(getPv1Current());
		datum.putInstantaneousSampleValue(ACEnergyDatum.VOLTAGE_KEY + "2", getPv2Voltage());
		datum.putInstantaneousSampleValue(ACEnergyDatum.CURRENT_KEY + "2", getPv2Current());
		datum.putInstantaneousSampleValue(ACEnergyDatum.VOLTAGE_KEY + "3", getPv3Voltage());
		datum.putInstantaneousSampleValue(ACEnergyDatum.CURRENT_KEY + "3", getPv3Current());

		datum.putInstantaneousSampleValue("temp", getHeatsinkTemperature());
		datum.putInstantaneousSampleValue("ambientTemp", getAmbientTemperature());
	}

	@Override
	public KTLData getSnapshot() {
		return new SI60KTLCTData(this);
	}

	@Override
	protected boolean readInverterDataInternal(ModbusConnection conn, MutableModbusData mutableData) {
		int[] data = conn.readUnsignedShorts(ModbusReadFunction.ReadInputRegister, ADDR_START,
				ADDR_LENGTH);
		mutableData.saveDataArray(data, ADDR_START);
		return true;
	}

	public Integer getDeviceModel() {
		return getInt16(ADDR_DEVICE_MODEL);
	}

	private Float getCentiValueAsFloat(int addr) {
		Integer centi = getInt16(addr);
		if ( centi == null ) {
			return null;
		}
		return centi.floatValue() / 100;
	}

	/**
	 * Get the active power, in watts.
	 * 
	 * @return the active power
	 */
	public Integer getActivePower() {
		Integer hundredWatts = getInt16(ADDR_ACTIVE_POWER);
		if ( hundredWatts == null ) {
			return null;
		}
		return hundredWatts * 100;
	}

	/**
	 * Get the apparent power, in watts.
	 * 
	 * @return the apparent power
	 */
	public Integer getApparentPower() {
		Integer hundredVoltAmps = getInt16(ADDR_APPARENT_POWER);
		if ( hundredVoltAmps == null ) {
			return null;
		}
		return hundredVoltAmps * 100;
	}

	/**
	 * Get the grid frequency, in hertz.
	 * 
	 * @return the frequency
	 */
	public Float getFrequency() {
		return getCentiValueAsFloat(ADDR_FREQUENCY);
	}

	/**
	 * Get the accumulating energy production, in Wh.
	 * 
	 * @return the energy export
	 */
	public Long getTotalEnergyExport() {
		Long kWh = getInt32(ADDR_TOTAL_ENERGY_EXPORT);
		if ( kWh == null ) {
			return null;
		}
		return kWh * 1000L;
	}

	public Float getPv1Voltage() {
		return getCentiValueAsFloat(ADDR_PV_1_VOLTAGE);
	}

	public Float getPv1Current() {
		return getCentiValueAsFloat(ADDR_PV_1_CURRENT);
	}

	public Float getPv2Voltage() {
		return getCentiValueAsFloat(ADDR_PV_2_VOLTAGE);
	}

	public Float getPv2Current() {
		return getCentiValueAsFloat(ADDR_PV_2_CURRENT);
	}

	public Float getPv3Voltage() {
		return getCentiValueAsFloat(ADDR_PV_3_VOLTAGE);
	}

	public Float getPv3Current() {
		return getCentiValueAsFloat(ADDR_PV_3_CURRENT);
	}

	/**
	 * Get the heatsink temperature, in degrees celsius.
	 * 
	 * @return the heatsink temperature.
	 */
	public Float getHeatsinkTemperature() {
		return getCentiValueAsFloat(ADDR_HEATSINK_TEMPERATURE);
	}

	/**
	 * Get the ambient temperature, in degrees celsius.
	 * 
	 * @return the ambient temperature
	 */
	public Float getAmbientTemperature() {
		return getCentiValueAsFloat(ADDR_AMBIENT_TEMPERATURE);
	}

}
