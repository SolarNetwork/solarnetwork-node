/* ==================================================================
 * NiftyModbusConnection.java - 19/12/2022 10:18:38 am
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

package net.solarnetwork.node.io.modbus.nifty;

import static java.lang.String.format;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readCoilsRequest;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readDiscretesRequest;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.writeCoilRequest;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.writeCoilsRequest;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readHoldingsRequest;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readInputsRequest;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.writeHoldingRequest;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.writeHoldingsRequest;
import static net.solarnetwork.node.io.modbus.ModbusDataUtils.shortArray;
import static net.solarnetwork.node.io.modbus.ModbusDataUtils.unsignedIntArray;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.io.modbus.BitsModbusMessage;
import net.solarnetwork.io.modbus.ModbusClient;
import net.solarnetwork.io.modbus.ModbusMessage;
import net.solarnetwork.io.modbus.RegistersModbusMessage;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.AbstractModbusConnection;
import net.solarnetwork.node.service.LockTimeoutException;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * Nifty Modbus implementation of {@link ModbusConnection}.
 * 
 * @author matt
 * @version 1.0
 */
public class NiftyModbusConnection extends AbstractModbusConnection implements ModbusConnection {

	/** The {@code connectTimeout} property default value. */
	public static final long DEFAULT_CONNECT_TIMEOUT = 10_000L;

	private static final Logger log = LoggerFactory.getLogger(NiftyModbusConnection.class);

	/** The Modbus client. */
	protected final ModbusClient controller;

	/** Function that describes this network connection. */
	protected final Supplier<String> describer;

	/** The connection timeout, in milliseconds. */
	private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;

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
	public NiftyModbusConnection(int unitId, boolean headless, ModbusClient controller,
			Supplier<String> describer) {
		super(unitId, headless);
		this.controller = ObjectUtils.requireNonNullArgument(controller, "controller");
		this.describer = ObjectUtils.requireNonNullArgument(describer, "describer");
	}

	@Override
	public void open() throws IOException, LockTimeoutException {
		if ( !controller.isStarted() ) {
			try {
				Future<?> f = controller.start();
				f.get(connectTimeout, TimeUnit.MILLISECONDS);
			} catch ( TimeoutException | InterruptedException e ) {
				throw new IOException(format("Timeout opening Modbus connection to %s unit %d: %s",
						describer.get(), getUnitId(), e.toString()), e);
			} catch ( ExecutionException e ) {
				Throwable cause = e.getCause();
				throw new IOException(format("Error opening Modbus connection to %s unit %d: %s",
						describer.get(), getUnitId(), cause.toString()), cause);
			}
		}
	}

	@Override
	public void close() {
		controller.stop();
	}

	@Override
	public BitSet readDiscreetValues(int address, int count) throws IOException {
		if ( !controller.isConnected() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			BitsModbusMessage req = readCoilsRequest(getUnitId(), address, count);
			ModbusMessage res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(
						String.format("Modbus exception %d reading %d coil values from %d @ %s",
								res.getError(), count, address, describer.get()));
			}
			BitsModbusMessage r = res.unwrap(BitsModbusMessage.class);
			if ( r != null ) {
				BitSet result = r.toBitSet();
				log.debug("Read {} Modbus coil values from {} @ {}: {}", count, address, describer.get(),
						result);
				return result;
			}
			return new BitSet();
		} catch ( Exception e ) {
			throw new IOException(String.format("Error reading %d discrete values from %d @ %s: %s",
					count, address, describer.get(), e.toString()), e);
		}
	}

	@Override
	public BitSet readDiscreetValues(int[] addresses, int count) throws IOException {
		if ( !controller.isConnected() ) {
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
		if ( !controller.isConnected() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		for ( int i = 0; i < addresses.length; i++ ) {
			try {
				BitsModbusMessage req = writeCoilsRequest(getUnitId(), addresses[i], 1,
						bits.get(i) ? BigInteger.ONE : BigInteger.ZERO);
				ModbusMessage res = controller.send(req);
				if ( res.isException() ) {
					throw new IOException(
							String.format("Modbus exception %d writing %d coil value to %d @ %s",
									res.getError(), 1, addresses[i], describer.get()));
				}
			} catch ( Exception e ) {
				throw new IOException(String.format("Error writing %d coil values from %d @ %s: %s", 1,
						addresses[i], describer.get(), e.toString()), e);
			}
		}
	}

	@Override
	public BitSet readInputDiscreteValues(int address, int count) throws IOException {
		if ( !controller.isConnected() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			BitsModbusMessage req = readDiscretesRequest(getUnitId(), address, count);
			ModbusMessage res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(String.format(
						"Modbus exception %d reading %d discrete input values from %d @ %s",
						res.getError(), count, address, describer.get()));
			}
			BitsModbusMessage r = res.unwrap(BitsModbusMessage.class);
			if ( r != null ) {
				BitSet result = r.toBitSet();
				log.debug("Read {} Modbus discrete input values from {} @ {}: {}", count, address,
						describer.get(), result);
				return result;
			}
			return new BitSet();
		} catch ( Exception e ) {
			throw new IOException(
					String.format("Error reading %d discrete input values from %d @ %s: %s", count,
							address, describer.get(), e.toString()),
					e);
		}
	}

	/**
	 * Create a new {@link ModbusMessage} instance appropriate for a given
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
	 */
	public static ModbusMessage modbusReadRequest(final ModbusReadFunction function, final int unitId,
			final int address, final int count) {
		ModbusMessage req = null;
		switch (function) {
			case ReadCoil:
				req = readCoilsRequest(unitId, address, count);
				break;

			case ReadDiscreteInput:
				req = readDiscretesRequest(unitId, address, count);
				break;

			case ReadHoldingRegister:
				req = readHoldingsRequest(unitId, address, count);
				break;

			case ReadInputRegister:
				req = readInputsRequest(unitId, address, count);
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
	 * Create a new {@link ModbusMessage} suitable for writing to non-discrete
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
	 * @param values
	 *        the register values to write
	 * @return a newly created request instance
	 * @throws UnsupportedOperationException
	 *         if the function is not supported
	 */
	public static ModbusMessage modbusWriteRequest(final ModbusWriteFunction function, final int unitId,
			final int address, final int count, final short[] values) {
		ModbusMessage req = null;
		switch (function) {
			case WriteHoldingRegister:
				req = writeHoldingRequest(unitId, address, values[0]);
				break;

			case WriteMultipleHoldingRegisters:
				req = writeHoldingsRequest(unitId, address, values);
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
		if ( !controller.isConnected() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			ModbusMessage req = modbusReadRequest(function, getUnitId(), address, count);
			ModbusMessage res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(
						String.format("Modbus exception %d reading %d %s values from %d @ %s",
								res.getError(), count, function.blockType(), address, describer.get()));
			}
			BitsModbusMessage bits = res.unwrap(BitsModbusMessage.class);
			if ( bits != null ) {
				BigInteger data = bits.getBits();
				short[] result = new short[count];
				for ( int i = 0; i < count; i++ ) {
					result[i] = data.testBit(i) ? (short) 1 : (short) 0;
				}
				return result;
			}
			RegistersModbusMessage regs = res.unwrap(RegistersModbusMessage.class);
			if ( regs != null ) {
				return regs.dataDecode();
			}
			return null;
		} catch ( Exception e ) {
			throw new IOException(String.format("Error reading %d %s values from %d @ %s: %s", count,
					function.blockType(), address, describer.get(), e.toString()), e);
		}
	}

	@Override
	public int[] readWordsUnsigned(ModbusReadFunction function, int address, int count)
			throws IOException {
		short[] result = readWords(function, address, count);
		return unsignedIntArray(result);
	}

	private static BigInteger toBooleanBits(short[] values) {
		BigInteger result = BigInteger.ZERO;
		int len = values.length;
		for ( int i = 0; i < len; i++ ) {
			if ( values[i] != (short) 0 ) {
				result = result.setBit(i);
			}
		}
		return result;
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, short[] values)
			throws IOException {
		if ( !controller.isConnected() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			ModbusMessage req = null;
			switch (function) {
				case WriteCoil:
					req = writeCoilRequest(getUnitId(), address, values[0] == (short) 0 ? false : true);
					break;

				case WriteMultipleCoils:
					req = writeCoilsRequest(getUnitId(), address, values.length, toBooleanBits(values));
					break;

				case WriteHoldingRegister:
					req = writeHoldingRequest(getUnitId(), address, Short.toUnsignedInt(values[0]));
					break;

				case WriteMultipleHoldingRegisters:
					req = writeHoldingsRequest(getUnitId(), address, values);
					break;

				default:
					throw new UnsupportedOperationException(
							"Function " + function + " is not supported");
			}
			ModbusMessage res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(String.format(
						"Modbus exception %d writing %d %s values to %d @ %s", res.getError(),
						values.length, function.blockType(), address, describer.get()));
			}
			if ( log.isTraceEnabled() ) {
				byte[] data = ModbusDataUtils.parseBytes(values, 0);
				log.trace("Wrote {} {} values to {} @ {}: {}", values.length, function.blockType(),
						address, describer.get(), ByteUtils.encodeHexString(data, 0, data.length, true));
			}
		} catch ( Exception e ) {
			throw new IOException(String.format("Error writing %d %s values from %d @ %s: %s",
					values.length, function.blockType(), address, describer.get(), e.toString()), e);
		}
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, int[] values) throws IOException {
		writeWords(function, address, shortArray(values));
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, int address, int count) throws IOException {
		if ( !controller.isConnected() ) {
			throw new IOException(String.format("Connection to %s is closed", describer.get()));
		}
		try {
			ModbusMessage req = modbusReadRequest(function, getUnitId(), address, count);
			ModbusMessage res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(
						String.format("Modbus exception %d reading %d %s values from %d @ %s",
								res.getError(), count, function.blockType(), address, describer.get()));
			}
			RegistersModbusMessage r = res.unwrap(RegistersModbusMessage.class);
			if ( r != null ) {
				return r.dataCopy();
			}
			return null;
		} catch ( Exception e ) {
			throw new IOException(String.format("Error reading %d %s values from %d @ %s: %s", count,
					function.blockType(), address, describer.get(), e.toString()), e);
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

	/**
	 * Get the connect timeout.
	 * 
	 * @return the timeout, in milliseconds
	 */
	public long getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * Set the connect timeout.
	 * 
	 * @param connectTimeout
	 *        the timeout to set, in milliseconds
	 */
	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

}
