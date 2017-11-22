package net.solarnetwork.node.hw.csi.inverter;

import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Common API for SDM meter data.
 * 
 * @author Max Duncan
 */
public interface CSIData {
	
	/**
	 * Read data from the inverter and store it internally.
	 * 
	 * @param connection the Modbus connection
	 */
	void readInverterData(ModbusConnection connection);
	
	long getInverterDataTimestamp();

	void populateMeasurements(GeneralNodeACEnergyDatum datum);
	
	CSIData getSnapshot();

	String dataDebugString();
	
}
