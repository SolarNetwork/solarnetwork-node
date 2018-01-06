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
	 * @param connection the Modbus connection
	 */
	void readInverterData(ModbusConnection connection);
	
	/**
	 * Gets the time stamp of the inverter.
	 * @return the inverter time stamp
	 */
	long getInverterDataTimestamp();

	/**
	 * Populates the supplied GeneralNodeACEnergyDatum with data.
	 * There is a KTLDatum but currently it doesn't contain any specific fields that need to be populated so the
	 * GeneralNodeACEnergyDatum is used.
	 * 
	 * @param datum The Datum to populate.
	 */
	void populateMeasurements(GeneralNodeACEnergyDatum datum);
	
	/**
	 * Gets an instance with the current readings.
	 * @return
	 */
	KTLData getSnapshot();

	/**
	 * A String that contains debug info.
	 * @return a String that contains debug info.
	 */
	String dataDebugString();
	
}
