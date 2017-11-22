/**
 * 
 */
package net.solarnetwork.node.hw.csi.inverter;

import java.util.Map;
import gnu.trove.map.hash.TIntIntHashMap;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusHelper;

/**
 * @author maxieduncan
 *
 */
public abstract class BaseCSIData implements CSIData {
	private long inverterDataTimestamp = 0;
	private final TIntIntHashMap dataRegisters = new TIntIntHashMap(64);

	/**
	 * @param
	 */
	@Override
	public final synchronized void readInverterData(ModbusConnection conn) {
		if (readInverterDataInternal(conn)) {
			this.inverterDataTimestamp = System.currentTimeMillis();
		}
	}

	protected abstract boolean readInverterDataInternal(ModbusConnection conn);

	@Override
	public long getInverterDataTimestamp() {
		return inverterDataTimestamp;
	}
	
	/**
	 * Read Modbus input registers in an address range.
	 * 
	 * @param conn
	 *        The Modbus connection.
	 * @param startAddr
	 *        The starting Modbus register address.
	 * @param endAddr
	 *        The ending Modbus register address.
	 */
	protected void readInputData(final ModbusConnection conn, final int startAddr, final int endAddr) {
		Map<Integer, Integer> data = conn.readInputValues(new Integer[] { startAddr },
				(endAddr - startAddr + 1));
		dataRegisters.putAll(data);
	}
	
	/**
	 * Internally store an array of 16-bit integer register data values,
	 * starting at a given address.
	 * 
	 * @param data
	 *        the data array to save
	 * @param addr
	 *        the starting address of the data
	 */
	protected void saveDataArray(final int[] data, int addr) {
		if ( data == null || data.length < 1 ) {
			return;
		}
		for ( int v : data ) {
			dataRegisters.put(addr, v);
			addr++;
		}
	}
	
	/**
	 * Construct a Float from a saved data register address. This method can
	 * only be called after data register data has been populated using:
	 * {@link #readInputData(ModbusConnection, int, int)}.
	 * 
	 * @param addr
	 *        The address of the saved data register to read.
	 * @return The parsed value, or <em>null</em> if not available.
	 */
	protected Float getFloat32(final int addr) {
		return ModbusHelper.parseFloat32(dataRegisters.get(addr), dataRegisters.get(addr + 1));
	}
	
	protected Integer getInteger(final int addr) {
		return dataRegisters.get(addr);
	}

}
