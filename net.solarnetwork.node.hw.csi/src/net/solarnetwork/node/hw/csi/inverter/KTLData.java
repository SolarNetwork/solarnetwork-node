/* ==================================================================
 * KTLData.java - 22 Nov 2017 12:28:46
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

import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Common API for KTL inverter data.
 * 
 * @author Max Duncan
 */
public interface KTLData {

	/**
	 * Read data from the inverter and store it internally.
	 * 
	 * @param connection
	 *        the Modbus connection
	 */
	void readInverterData(ModbusConnection connection);

	/**
	 * Gets the time stamp of the inverter.
	 * 
	 * @return the inverter time stamp
	 */
	long getInverterDataTimestamp();

	/**
	 * Populates the supplied GeneralNodeACEnergyDatum with data.
	 * 
	 * @param datum
	 *        The Datum to populate.
	 */
	void populateMeasurements(GeneralNodeACEnergyDatum datum);

	/**
	 * Gets an instance with the current readings.
	 * 
	 * @return
	 */
	KTLData getSnapshot();

	/**
	 * A String that contains debug info.
	 * 
	 * @return a String that contains debug info.
	 */
	String dataDebugString();

}
