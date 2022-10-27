/* ==================================================================
 * ModbusTransactionUtils.java - 3/02/2018 8:53:29 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.j2mod;

import static net.solarnetwork.node.io.modbus.ModbusDataUtils.shortArray;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteCoilResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.BitVector;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;

/**
 * Utility methods for Modbus actions.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusTransactionUtils {

	private static final Logger LOG = LoggerFactory.getLogger(ModbusTransactionUtils.class);

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@literal 1} request.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register addresses to start reading from
	 * @param count
	 *        the count of 16-bit registers to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @return BitSet, with indexes set from {@literal 0} to a {@code count - 1}
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static BitSet readDiscreteValues(final ModbusTransaction trans, final int address,
			final int count, final int unitId, final boolean headless) throws IOException {
		BitSet result = new BitSet(count);
		ReadCoilsRequest req = new ReadCoilsRequest(address, count);
		req.setUnitID(unitId);
		req.setHeadless();
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new IOException(e);
		}
		ReadCoilsResponse res = (ReadCoilsResponse) trans.getResponse();
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Got Modbus read coil {} response [{}]", address, res.getCoils());
		}
		for ( int i = 0; i < res.getBitCount(); i++ ) {
			result.set(i, res.getCoilStatus(i));
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read {} Modbus coil {} values: {}", count, address, result);
		}
		return result;
	}

	/**
	 * Get the values of a set of "coil" type registers, as a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@literal 1} request. The returned set
	 * will have a size equal to {@code addresses.length * count}.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param addresses
	 *        the Modbus register addresses to read
	 * @param count
	 *        the count of registers to read with each address
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @return BitSet, with each {@code count} indexes for each index in the
	 *         {@code addresses} parameter
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static BitSet readDiscreetValues(final ModbusTransaction trans, final int[] addresses,
			final int count, final int unitId, final boolean headless) throws IOException {
		BitSet result = new BitSet(addresses.length);
		for ( int i = 0, w = 0; i < addresses.length; i++ ) {
			BitSet set = readDiscreteValues(trans, addresses[i], count, unitId, true);
			for ( int j = 0; j < count; j++ ) {
				// map individual bitset index to overall output bitset index
				result.set(w++, set.get(j));
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus coil {} x {} values: {}", addresses, count, result);
		}
		return result;
	}

	/**
	 * Write values of a set of "coil" type registers, via a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@literal 5} request, once for each
	 * address in {@code addresses}. Each address at index <em>i</em>
	 * corresponds to the value of bit at index <em>i</em>. Thus bits
	 * {@literal 0} to {@code addresses.length - 1} are used.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param addresses
	 *        the Modbus register addresses to start writing to
	 * @param bits
	 *        the bits to write, each index corresponding to an index in
	 *        {@code addresses}
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static void writeDiscreetValues(final ModbusTransaction trans, final int[] addresses,
			final BitSet bits, final int unitId, final boolean headless) throws IOException {
		for ( int i = 0; i < addresses.length; i++ ) {
			WriteCoilRequest req = new WriteCoilRequest(addresses[i], bits.get(i));
			req.setUnitID(unitId);
			if ( headless ) {
				req.setHeadless();
			}
			trans.setRequest(req);
			try {
				trans.execute();
			} catch ( ModbusException e ) {
				throw new IOException(e);
			}
			WriteCoilResponse res = (WriteCoilResponse) trans.getResponse();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace("Got write {} response [{}]", addresses[i], res.getCoil());
			}
		}
	}

	/**
	 * Get the values of a set of "input discrete" type registers, as a BitSet.
	 * 
	 * <p>
	 * This uses a Modbus function code {@literal 2} request. The returned
	 * bitset will have {@code count} values set, from {@literal 0} to
	 * {@code count - 1}.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param address
	 *        the Modbus register addresses to start reading from
	 * @param count
	 *        the count of registers to read
	 * @param unitId
	 *        the Modbus unit ID to use in the read request
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @return BitSet, with each {@literal 0} to {@code count} indexes
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static BitSet readInputDiscreteValues(final ModbusTransaction trans, final int address,
			final int count, final int unitId, final boolean headless) throws IOException {
		BitSet result = new BitSet(count);
		ReadInputDiscretesRequest req = new ReadInputDiscretesRequest(address, count);
		req.setUnitID(unitId);
		req.setHeadless();
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new IOException(e);
		}
		ReadInputDiscretesResponse res = (ReadInputDiscretesResponse) trans.getResponse();
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Got Modbus read input discretes {} response [{}]", address, res.getDiscretes());
		}
		for ( int i = 0; i < res.getBitCount(); i++ ) {
			result.set(i, res.getDiscreteStatus(i));
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read {} Modbus input discrete {} values: {}", count, address, result);
		}
		return result;
	}

	/**
	 * Create a new {@link ModbusRequest} instance appropriate for a given
	 * function, unit ID, address, and count.
	 * 
	 * @param function
	 *        the function to use
	 * @param unitId
	 *        the unit ID
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param address
	 *        the register address to start reading from
	 * @param count
	 *        the count of registers to read
	 * @return a newly created request instance
	 * @throws UnsupportedOperationException
	 *         if the function is not supported
	 */
	public static ModbusRequest modbusReadRequest(final ModbusReadFunction function, final int unitId,
			final boolean headless, final int address, final int count) {
		ModbusRequest req;
		switch (function) {
			case ReadCoil:
				req = new ReadCoilsRequest(address, count);
				break;

			case ReadDiscreteInput:
				req = new ReadInputDiscretesRequest(address, count);
				break;

			case ReadHoldingRegister:
				req = new ReadMultipleRegistersRequest(address, count);
				break;

			case ReadInputRegister:
				req = new ReadInputRegistersRequest(address, count);
				break;

			default:
				throw new UnsupportedOperationException("Function " + function + " is not supported");

		}
		if ( headless ) {
			req.setHeadless();
		}
		req.setUnitID(unitId);
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Modbus {} {} @ {} x {}", unitId, function, address, count);
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
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param address
	 *        the address to start writing to
	 * @param count
	 *        the number of Modbus 16-bit registers to read
	 * @return a newly created request instance
	 * @throws UnsupportedOperationException
	 *         if the function is not supported
	 */
	public static ModbusRequest modbusWriteRequest(final ModbusWriteFunction function, final int unitId,
			final boolean headless, final int address, final int count) {
		ModbusRequest req;
		switch (function) {
			case WriteHoldingRegister:
				req = new WriteSingleRegisterRequest(address, null);
				break;

			case WriteMultipleHoldingRegisters:
				req = new WriteMultipleRegistersRequest(address, null);
				break;

			default:
				throw new UnsupportedOperationException("Function " + function + " is not supported");

		}
		if ( headless ) {
			req.setHeadless();
		}
		req.setUnitID(unitId);
		if ( LOG.isTraceEnabled() ) {
			LOG.trace("Modbus {} {} @ {} x {}", unitId, function, address, count);
		}
		return req;
	}

	/**
	 * Get the values of specific 16-bit Modbus registers as an array of 16-bit
	 * words.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param unitId
	 *        the unit ID
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param function
	 *        the function to use
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus 16-bit registers to read
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static short[] readWords(final ModbusTransaction trans, final int unitId,
			final boolean headless, final ModbusReadFunction function, final int address,
			final int count) throws IOException {
		ModbusRequest req = modbusReadRequest(function, unitId, headless, address, count);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new IOException(e);
		}
		ModbusResponse response = trans.getResponse();
		short[] result = new short[count];
		if ( response instanceof ReadMultipleRegistersResponse ) {
			ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) response;
			for ( int w = 0, len = res.getWordCount(); w < count && w < len; w += 1 ) {
				result[w] = res.getRegister(w).toShort();
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + w, result[w]);
				}
			}
		} else if ( response instanceof ReadInputRegistersResponse ) {
			ReadInputRegistersResponse res = (ReadInputRegistersResponse) response;
			for ( int w = 0, len = res.getWordCount(); w < count && w < len; w += 1 ) {
				result[w] = res.getRegister(w).toShort();
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + w, result[w]);
				}
			}
		} else if ( response instanceof ReadInputDiscretesResponse ) {
			ReadInputDiscretesResponse res = (ReadInputDiscretesResponse) response;
			BitVector bv = res.getDiscretes();
			for ( int w = 0; w < count; w += 1 ) {
				result[w] = bv.getBit(w) ? (short) 1 : (short) 0;
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + w, result[w]);
				}
			}
		} else if ( response instanceof ReadCoilsResponse ) {
			ReadCoilsResponse res = (ReadCoilsResponse) response;
			BitVector bv = res.getCoils();
			for ( int w = 0; w < count; w += 1 ) {
				result[w] = bv.getBit(w) ? (short) 1 : (short) 0;
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + w, result[w]);
				}
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus register {} count {} values: {}",
					new Object[] { address, count, result });
		}
		return result;
	}

	/**
	 * Write 16-bit word values to 16-bit Modbus registers.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param unitId
	 *        the unit ID
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param function
	 *        the function to use
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param values
	 *        the signed 16-bit values to write
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static void writeWords(final ModbusTransaction trans, final int unitId,
			final boolean headless, final ModbusWriteFunction function, final int address,
			final short[] values) throws IOException {
		ModbusRequest request = modbusWriteRequest(function, unitId, headless, address,
				(values != null ? values.length : 0));
		if ( request instanceof WriteMultipleRegistersRequest ) {
			WriteMultipleRegistersRequest req = (WriteMultipleRegistersRequest) request;
			int len = values.length;
			Register[] regs = new Register[len];
			for ( int i = 0; i < len; i += 1 ) {
				regs[i] = new SimpleRegister(values[i]);
			}
			req.setRegisters(regs);
		} else if ( request instanceof WriteSingleRegisterRequest ) {
			WriteSingleRegisterRequest req = (WriteSingleRegisterRequest) request;
			Register reg = new SimpleRegister(values[0]);
			req.setRegister(reg);
		} else {
			throw new UnsupportedOperationException("Funciton " + function + " not supported");
		}

		trans.setRequest(request);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new IOException(e);
		}

		if ( LOG.isTraceEnabled() ) {
			ModbusResponse response = trans.getResponse();
			if ( response instanceof WriteMultipleRegistersResponse ) {
				WriteMultipleRegistersResponse res = (WriteMultipleRegistersResponse) response;
				LOG.trace("Got write {} response count {}", address, res.getWordCount());
			} else if ( response instanceof WriteSingleRegisterResponse ) {
				WriteSingleRegisterResponse res = (WriteSingleRegisterResponse) response;
				LOG.trace("Got write {} response [{}]", address, res.getRegisterValue());
			}
		}
	}

	/**
	 * Write unsigned 16-bit word values to 16-bit Modbus registers.
	 * 
	 * <p>
	 * All the elements in {@code values} will be truncated to 16-bits and then
	 * stored in Modbus registers.
	 * </p>
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param unitId
	 *        the Modbus unit ID to direct the request to
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start writing to
	 * @param values
	 *        the unsigned 16-bit values to write
	 * @see #writeWords(ModbusTransaction, int, boolean, ModbusWriteFunction,
	 *      int, short[])
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static void writeWords(final ModbusTransaction trans, final int unitId,
			final boolean headless, final ModbusWriteFunction function, final int address,
			final int[] values) throws IOException {
		writeWords(trans, unitId, headless, function, address, shortArray(values));
	}

	/**
	 * Get the values of specific 16-bit Modbus registers as an array of
	 * unsigned 16-bit words.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param unitId
	 *        the Modbus unit ID to direct the request to
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of 16-bit Modbus registers to read
	 * @return array of register values; the result will have a length equal to
	 *         {@code count}
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static int[] readWordsUnsigned(final ModbusTransaction trans, final int unitId,
			final boolean headless, final ModbusReadFunction function, final int address,
			final int count) throws IOException {
		ModbusRequest req = modbusReadRequest(function, unitId, headless, address, count);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new IOException(e);
		}
		ModbusResponse response = trans.getResponse();
		int[] result = new int[count];
		if ( response instanceof ReadMultipleRegistersResponse ) {
			ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) response;
			for ( int w = 0, len = res.getWordCount(); w < count && w < len; w += 1 ) {
				result[w] = res.getRegisterValue(w);
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + w, result[w]);
				}
			}
		} else if ( response instanceof ReadInputRegistersResponse ) {
			ReadInputRegistersResponse res = (ReadInputRegistersResponse) response;
			for ( int w = 0, len = res.getWordCount(); w < count && w < len; w += 1 ) {
				result[w] = res.getRegisterValue(w);
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + w, result[w]);
				}
			}
		} else if ( response instanceof ReadInputDiscretesResponse ) {
			ReadInputDiscretesResponse res = (ReadInputDiscretesResponse) response;
			BitVector bv = res.getDiscretes();
			for ( int w = 0; w < count; w += 1 ) {
				result[w] = bv.getBit(w) ? 1 : 0;
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + w, result[w]);
				}
			}
		} else if ( response instanceof ReadCoilsResponse ) {
			ReadCoilsResponse res = (ReadCoilsResponse) response;
			BitVector bv = res.getCoils();
			for ( int w = 0; w < count; w += 1 ) {
				result[w] = bv.getBit(w) ? 1 : 0;
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + w, result[w]);
				}
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus register {} count {} values: {}",
					new Object[] { address, count, result });
		}
		return result;
	}

	/**
	 * Get the raw bytes of specific registers.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param unitId
	 *        the Modbus unit ID to direct the request to
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus 16-bit registers to read
	 * @return array of register bytes; the result will have a length equal to
	 *         {@code count * 2}
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static byte[] readBytes(final ModbusTransaction trans, final int unitId,
			final boolean headless, final ModbusReadFunction function, final int address,
			final int count) throws IOException {
		byte[] result = new byte[count * 2];
		ModbusRequest req = modbusReadRequest(function, unitId, headless, address, count);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch ( ModbusException e ) {
			throw new IOException(e);
		}
		ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
		InputRegister[] registers = res.getRegisters();
		if ( registers != null ) {

			for ( int i = 0; i < registers.length; i++ ) {
				if ( LOG.isTraceEnabled() ) {
					LOG.trace("Got Modbus read {} response {}", address + i, res.getRegisterValue(i));
				}
				byte[] b = registers[i].toBytes();
				result[i * 2] = b[0];
				result[i * 2 + 1] = b[1];
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus register {} count {} bytes: {}",
					new Object[] { address, count, result });
		}
		return result;
	}

	/**
	 * Write raw byte values to registers.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param unitId
	 *        the Modbus unit ID to direct the request to
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start writing to
	 * @param values
	 *        the byte values to write
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static void writeBytes(final ModbusTransaction trans, final int unitId,
			final boolean headless, final ModbusWriteFunction function, final int address,
			final byte[] values) throws IOException {
		int[] unsigned = new int[(int) Math.ceil(values.length / 2.0)];
		for ( int i = 0; i < values.length; i += 2 ) {
			int v = ((values[i] & 0xFF) << 8);
			if ( i + 1 < values.length ) {
				v |= (values[i + 1] & 0xFF);
			}
			unsigned[i / 2] = v;
		}
		writeWords(trans, unitId, headless, function, address, unsigned);
	}

	/**
	 * Read a set of registers as bytes and interpret as a string.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param unitId
	 *        the Modbus unit ID to direct the request to
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start reading from
	 * @param count
	 *        the number of Modbus 16-bit registers to read
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @param charset
	 *        the character set to interpret the bytes as
	 * @return String from interpreting raw bytes as a string
	 * @see #readBytes(ModbusTransaction, int, boolean, ModbusReadFunction, int,
	 *      int)
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static String readString(final ModbusTransaction trans, final int unitId,
			final boolean headless, final ModbusReadFunction function, final int address,
			final int count, final boolean trim, final Charset charset) throws IOException {
		final byte[] bytes = readBytes(trans, unitId, headless, function, address, count);
		String result = null;
		if ( bytes != null ) {
			result = new String(bytes, charset);
			if ( trim ) {
				result = result.trim();
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("Read Modbus input register {} count {} string: {}",
					new Object[] { address, count, result });
		}
		return result;
	}

	/**
	 * Write a string as raw byte values to registers.
	 * 
	 * @param trans
	 *        the Modbus transaction to use
	 * @param unitId
	 *        the Modbus unit ID to direct the request to
	 * @param headless
	 *        {@literal true} for headless (serial) mode
	 * @param function
	 *        the Modbus function code to use
	 * @param address
	 *        the 0-based Modbus register address to start writing to
	 * @param value
	 *        the string value to write
	 * @param charset
	 *        the character set to interpret the bytes as
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static void writeString(final ModbusTransaction trans, final int unitId,
			final boolean headless, final ModbusWriteFunction function, final int address,
			final String value, final Charset charset) throws IOException {
		byte[] bytes = value.getBytes(charset);
		writeBytes(trans, unitId, headless, function, address, bytes);
	}
}
