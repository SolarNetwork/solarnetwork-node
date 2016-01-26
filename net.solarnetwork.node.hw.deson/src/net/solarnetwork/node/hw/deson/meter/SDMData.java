/* ==================================================================
 * SDMData.java - 25/01/2016 5:42:42 pm
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

import java.util.Map;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Common API for SDM meter data.
 * 
 * @author matt
 * @version 1.0
 */
public interface SDMData {

	/**
	 * Get the system time meter data was read from the actual device. If never
	 * read, then return {@code 0}.
	 * 
	 * @return the data timestamp
	 */
	long getMeterDataTimestamp();

	/**
	 * Get the system time control data was read from the actual device. If
	 * never read, then return {@code 0}.
	 * 
	 * @return the control data timestamp
	 */
	long getControlDataTimestamp();

	/**
	 * Read data from the meter and store it internally. If data is populated
	 * successfully, {@link #getMeterDataTimestamp()} will be updated to reflect
	 * the current system time.
	 * 
	 * @param conn
	 *        the Modbus connection
	 */
	void readMeterData(final ModbusConnection conn);

	/**
	 * Read control (holding) data from the meter and store it internally. If
	 * data is populated successfully, {@link #getControlDataTimestamp()} will
	 * be updated to reflect the current system time.
	 * 
	 * @param conn
	 *        the Modbuss connection
	 */
	void readControlData(final ModbusConnection conn);

	/**
	 * Get a copy of the data.
	 * 
	 * @return Get a new instance with a copy of the data.
	 */
	SDMData getSnapshot();

	/**
	 * Get a string representation of the meter data, for debugging purposes.
	 * 
	 * The generated string will contain prefix and suffix lines, with any
	 * number of lines between that contain a register address followed by two
	 * register values per line, printed as hexidecimal integers. For example:
	 * 
	 * <pre>
	 * SDMData{
	 *      30001: 0x4141, 0x727E
	 *      30007: 0xFFC0, 0x0000
	 *      ...
	 *      30345: 0x0000, 0x0000
	 * }
	 * </pre>
	 * 
	 * @return A debug string.
	 */
	String dataDebugString();

	/**
	 * Get information about the meter, such as the model number, manufacture
	 * date, etc.
	 * 
	 * @return Meter information, or <em>null</em> if none available.
	 */
	Map<String, Object> getDeviceInfo();

	/**
	 * Get a brief information message about the operational status of the
	 * meter, such as the overall power being used, etc.
	 * 
	 * @return A brief status message, or <em>null</em> if none available.
	 */
	String getOperationStatusMessage();

	/**
	 * Populate an {@code ACEnergyDatum} from sample data for a specific phase.
	 * 
	 * @param phase
	 *        The phase to populate data for.
	 * @param datum
	 *        The datum to populate data into.
	 */
	void populateMeasurements(ACPhase phase, GeneralNodeACEnergyDatum datum);

	/**
	 * Get an effective voltage value in V. Only valid after
	 * {@link #readMeterData(ModbusConnection)} called at least once.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a voltage value
	 */
	Float getVoltage(int addr);

	/**
	 * Get an effective current value in A. Only valid after
	 * {@link #readMeterData(ModbusConnection)} called at least once.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a current value
	 */
	Float getCurrent(int addr);

	/**
	 * Get an effective frequency value in Hz. Only valid after
	 * {@link #readMeterData(ModbusConnection)} called at least once.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a frequency value
	 */
	Float getFrequency(int addr);

	/**
	 * Get an effective power factor value. Only valid after
	 * {@link #readMeterData(ModbusConnection)} called at least once.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a power factor value
	 */
	Float getPowerFactor(int addr);

	/**
	 * Get an effective power value in W (active), Var (reactive) or VA
	 * (apparent). These are rounded from their native floating point
	 * representations. Only valid after
	 * {@link #readMeterData(ModbusConnection)} called at least once.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a power value
	 */
	Integer getPower(int addr);

	/**
	 * Get an effective energy value in Wh (real), Varh (reactive). Only valid
	 * after {@link #readMeterData(ModbusConnection)} called at least once.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as an energy value
	 */
	Long getEnergy(int addr);
}
