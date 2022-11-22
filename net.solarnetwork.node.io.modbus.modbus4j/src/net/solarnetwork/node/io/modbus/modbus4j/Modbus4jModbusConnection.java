/* ==================================================================
 * Modbus4jModbusConnection.java - 22/11/2022 2:00:57 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.modbus4j;

import static java.lang.String.format;
import static net.solarnetwork.node.io.modbus.ModbusDataUtils.shortArray;
import static net.solarnetwork.node.io.modbus.ModbusDataUtils.unsignedIntArray;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.msg.ReadCoilsRequest;
import com.serotonin.modbus4j.msg.ReadCoilsResponse;
import com.serotonin.modbus4j.msg.ReadDiscreteInputsRequest;
import com.serotonin.modbus4j.msg.ReadDiscreteInputsResponse;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadInputRegistersRequest;
import com.serotonin.modbus4j.msg.ReadResponse;
import com.serotonin.modbus4j.msg.WriteCoilRequest;
import com.serotonin.modbus4j.msg.WriteCoilResponse;
import com.serotonin.modbus4j.msg.WriteCoilsRequest;
import com.serotonin.modbus4j.msg.WriteRegisterRequest;
import com.serotonin.modbus4j.msg.WriteRegistersRequest;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.AbstractModbusConnection;
import net.solarnetwork.node.service.LockTimeoutException;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * Modbus4j implementation of {@link ModbusConnection}.
 * 
 * @author matt
 * @version 1.0
 */
public class Modbus4jModbusConnection extends AbstractModbusConnection implements ModbusConnection {

	private static final Logger log = LoggerFactory.getLogger(Modbus4jModbusConnection.class);

	protected final ModbusMaster controller;
	protected final Supplier<String> describer;

	/**
	 * Constructor.
	 * 
	 * @param unitId
	 *        the unit ID
	 * @param headless
	 *        the headless flag
	 * @param controller
	 *        the controller
	 * @param describer
	 *        a function that returns a description of the connection
	 */
	public Modbus4jModbusConnection(int unitId, boolean headless, ModbusMaster controller,
			Supplier<String> describer) {
		super(unitId, headless);
		this.controller = ObjectUtils.requireNonNullArgument(controller, "controller");
		this.describer = ObjectUtils.requireNonNullArgument(describer, "describer");
	}

	@Override
	public void open() throws IOException, LockTimeoutException {
		synchronized ( controller ) {
			if ( !controller.isInitialized() ) {
				try {
					controller.init();
				} catch ( ModbusInitException e ) {
					throw new IOException(format("Error opening Modbus connection to %s unit %d: %s",
							describer.get(), getUnitId(), e.toString()), e);
				}
			}
		}
	}

	@Override
	public void close() {
		synchronized ( controller ) {
			controller.destroy();
		}
	}

	@Override
	public BitSet readDiscreetValues(int address, int count) throws IOException {
		if ( !controller.isInitialized() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			ModbusRequest req = new ReadCoilsRequest(getUnitId(), address, count);
			ModbusResponse res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(
						String.format("Modbus exception %d reading %d coil values from %d @ %s",
								res.getExceptionCode(), count, address, describer.get()));
			}
			if ( res instanceof ReadCoilsResponse ) {
				ReadCoilsResponse r = (ReadCoilsResponse) res;
				boolean[] data = r.getBooleanData();
				int len = data.length;
				BitSet result = new BitSet(len);
				for ( int i = 0; i < len && i < count; i++ ) {
					result.set(i, data[i]);
				}
				log.debug("Read {} Modbus coil values from {} @ {}: {}", count, address, describer.get(),
						result);
				return result;
			}
			return new BitSet();
		} catch ( ModbusTransportException e ) {
			throw new IOException(String.format("Error reading %d coil values from %d @ %s", count,
					address, describer.get()));
		}
	}

	@Override
	public BitSet readDiscreetValues(int[] addresses, int count) throws IOException {
		if ( !controller.isInitialized() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		BitSet result = new BitSet(addresses.length);
		for ( int i = 0, w = 0; i < addresses.length; i++ ) {
			BitSet set = readDiscreetValues(addresses[i], count);
			for ( int j = 0; j < count; j++ ) {
				// map individual bitset index to overall output bitset index
				result.set(w++, set.get(j));
			}
		}
		return result;
	}

	@Override
	public void writeDiscreetValues(int[] addresses, BitSet bits) throws IOException {
		if ( !controller.isInitialized() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		for ( int i = 0; i < addresses.length; i++ ) {
			try {
				WriteCoilRequest req = new WriteCoilRequest(getUnitId(), addresses[i], bits.get(i));
				ModbusResponse res = controller.send(req);
				if ( res.isException() ) {
					throw new IOException(
							String.format("Modbus exception %d writing %d coil value to %d @ %s",
									res.getExceptionCode(), 1, addresses[i], describer.get()));
				}
				if ( log.isTraceEnabled() && res instanceof WriteCoilResponse ) {
					log.trace("Got write {} response [{}] from {} @ {}", addresses[i],
							((WriteCoilResponse) res).isWriteValue(), addresses[i], describer.get());
				}
			} catch ( ModbusTransportException e ) {
				throw new IOException(String.format("Error writing %d coil values from %d @ %s", 1,
						addresses[i], describer.get()));
			}
		}
	}

	private static BitSet bitSetForData(boolean[] data) {
		int len = data.length;
		BitSet result = new BitSet(len);
		for ( int i = 0; i < len; i++ ) {
			result.set(i, data[i]);
		}
		return result;
	}

	@Override
	public BitSet readInputDiscreteValues(int address, int count) throws IOException {
		if ( !controller.isInitialized() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			ModbusRequest req = new ReadDiscreteInputsRequest(getUnitId(), address, count);
			ModbusResponse res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(String.format(
						"Modbus exception %d reading %d discrete input values from %d @ %s",
						res.getExceptionCode(), count, address, describer.get()));
			}
			if ( res instanceof ReadDiscreteInputsResponse ) {
				ReadDiscreteInputsResponse r = (ReadDiscreteInputsResponse) res;
				boolean[] data = r.getBooleanData();
				BitSet result = bitSetForData(data);
				log.debug("Read {} Modbus discrete input values from {} @ {}: {}", count, address,
						describer.get(), result);
				return result;
			}
			return new BitSet();
		} catch ( ModbusTransportException e ) {
			throw new IOException(String.format("Error reading %d discrete input values from %d @ %s",
					count, address, describer.get()));
		}
	}

	/**
	 * Create a new {@link ModbusRequest} instance appropriate for a given
	 * function, unit ID, address, and count.
	 * 
	 * @param function
	 *        the function to use
	 * @param unitId
	 *        the unit ID
	 * @param address
	 *        the register address to start reading from
	 * @param count
	 *        the count of registers to read
	 * @return a newly created request instance
	 * @throws UnsupportedOperationException
	 *         if the function is not supported
	 * @throws ModbusTransportException
	 *         if a transport error occurs
	 */
	public static ModbusRequest modbusReadRequest(final ModbusReadFunction function, final int unitId,
			final int address, final int count) throws ModbusTransportException {
		ModbusRequest req = null;
		switch (function) {
			case ReadCoil:
				req = new ReadCoilsRequest(unitId, address, count);
				break;

			case ReadDiscreteInput:
				req = new ReadDiscreteInputsRequest(unitId, address, count);
				break;

			case ReadHoldingRegister:
				req = new ReadHoldingRegistersRequest(unitId, address, count);
				break;

			case ReadInputRegister:
				req = new ReadInputRegistersRequest(unitId, address, count);
				break;

			default:
				throw new UnsupportedOperationException("Function " + function + " is not supported");

		}
		if ( log.isTraceEnabled() ) {
			log.trace("Modbus {} {} @ {} x {}", unitId, function, address, count);
		}
		return req;
	}

	/**
	 * Create a new {@link ModbusRequest} suitable for writing to non-discrete
	 * registers.
	 * 
	 * @param function
	 *        the function to use
	 * @param unitId
	 *        the unit ID
	 * @param address
	 *        the address to start writing to
	 * @param count
	 *        the number of Modbus 16-bit registers to read
	 * @return a newly created request instance
	 * @throws UnsupportedOperationException
	 *         if the function is not supported
	 * @throws ModbusTransportException
	 *         if a transport error occurs
	 */
	public static ModbusRequest modbusWriteRequest(final ModbusWriteFunction function, final int unitId,
			final int address, final int count, final short[] values) throws ModbusTransportException {
		ModbusRequest req = null;
		switch (function) {
			case WriteHoldingRegister:
				req = new WriteRegisterRequest(unitId, address, values[0]);
				break;

			case WriteMultipleHoldingRegisters:
				req = new WriteRegistersRequest(unitId, address, values);
				break;

			default:
				throw new UnsupportedOperationException("Function " + function + " is not supported");

		}
		if ( log.isTraceEnabled() ) {
			log.trace("Modbus {} {} @ {} x {}", unitId, function, address, count);
		}
		return req;
	}

	@Override
	public short[] readWords(ModbusReadFunction function, int address, int count) throws IOException {
		if ( !controller.isInitialized() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			ModbusRequest req = modbusReadRequest(function, getUnitId(), address, count);
			ModbusResponse res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(String.format(
						"Modbus exception %d reading %d %s values from %d @ %s", res.getExceptionCode(),
						count, function.blockType(), address, describer.get()));
			}
			if ( res instanceof ReadResponse ) {
				if ( function.blockType() == ModbusRegisterBlockType.Coil
						|| function.blockType() == ModbusRegisterBlockType.Discrete ) {
					boolean[] data = ((ReadResponse) res).getBooleanData();
					int len = data.length;
					short[] result = new short[len];
					for ( int i = 0; i < len; i++ ) {
						result[i] = data[i] ? (short) 1 : (short) 0;
					}
					return result;
				}
				return ((ReadResponse) res).getShortData();
			}
			return null;
		} catch ( ModbusTransportException e ) {
			throw new IOException(String.format("Error reading %d %s values from %d @ %s", count,
					function.blockType(), address, describer.get()));
		}
	}

	@Override
	public int[] readWordsUnsigned(ModbusReadFunction function, int address, int count)
			throws IOException {
		short[] result = readWords(function, address, count);
		return unsignedIntArray(result);
	}

	private static boolean[] toBooleans(short[] values) {
		int len = values.length;
		boolean[] result = new boolean[len];
		for ( int i = 0; i < len; i++ ) {
			result[i] = values[i] == (short) 0 ? false : true;
		}
		return result;
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, short[] values)
			throws IOException {
		if ( !controller.isInitialized() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			ModbusRequest req = null;
			switch (function) {
				case WriteCoil:
					req = new WriteCoilRequest(getUnitId(), address,
							values[0] == (short) 0 ? false : true);
					break;

				case WriteMultipleCoils:
					req = new WriteCoilsRequest(getUnitId(), address, toBooleans(values));
					break;

				case WriteHoldingRegister:
					req = new WriteRegisterRequest(getUnitId(), address, Short.toUnsignedInt(values[0]));
					break;

				case WriteMultipleHoldingRegisters:
					req = new WriteRegistersRequest(getUnitId(), address, values);
					break;

				default:
					throw new UnsupportedOperationException(
							"Function " + function + " is not supported");
			}
			ModbusResponse res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(String.format(
						"Modbus exception %d writing %d %s values to %d @ %s", res.getExceptionCode(),
						values.length, function.blockType(), address, describer.get()));
			}
			if ( log.isTraceEnabled() ) {
				byte[] data = ModbusDataUtils.parseBytes(values, 0);
				log.trace("Wrote {} {} values to {} @ {}: {}", values.length, function.blockType(),
						address, describer.get(), ByteUtils.encodeHexString(data, 0, data.length, true));
			}
		} catch ( ModbusTransportException e ) {
			throw new IOException(String.format("Error writing %d %s values from %d @ %s", values.length,
					function.blockType(), address, describer.get()));
		}
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, int[] values) throws IOException {
		writeWords(function, address, shortArray(values));
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, int address, int count) throws IOException {
		if ( !controller.isInitialized() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			ModbusRequest req = modbusReadRequest(function, getUnitId(), address, count);
			ModbusResponse res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(String.format(
						"Modbus exception %d reading %d %s values from %d @ %s", res.getExceptionCode(),
						count, function.blockType(), address, describer.get()));
			}
			if ( res instanceof ReadResponse ) {
				return ((ReadResponse) res).getData();
			}
			return null;
		} catch ( ModbusTransportException e ) {
			throw new IOException(String.format("Error reading %d %s values from %d @ %s", count,
					function.blockType(), address, describer.get()));
		}
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, int address, byte[] values) throws IOException {
		int[] unsigned = new int[(int) Math.ceil(values.length / 2.0)];
		for ( int i = 0; i < values.length; i += 2 ) {
			int v = ((values[i] & 0xFF) << 8);
			if ( i + 1 < values.length ) {
				v |= (values[i + 1] & 0xFF);
			}
			unsigned[i / 2] = v;
		}
		writeWords(function, address, unsigned);
	}

	@Override
	public String readString(ModbusReadFunction function, int address, int count, boolean trim,
			Charset charset) throws IOException {
		final byte[] bytes = readBytes(function, address, count);
		String result = null;
		if ( bytes != null ) {
			result = new String(bytes, charset);
			if ( trim ) {
				result = result.trim();
			}
		}
		log.debug("Read {} {} values from {} @ {} as string: {}", count, function.blockType(), address,
				describer.get(), result);
		return result;
	}

	@Override
	public void writeString(ModbusWriteFunction function, int address, String value, Charset charset)
			throws IOException {
		byte[] bytes = value.getBytes(charset);
		writeBytes(function, address, bytes);
	}

}
