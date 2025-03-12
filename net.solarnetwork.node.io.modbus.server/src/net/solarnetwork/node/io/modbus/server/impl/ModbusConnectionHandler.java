/* ==================================================================
 * ModbusConnectionHandler.java - 18/09/2020 6:55:36 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.impl;

import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readCoilsResponse;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readDiscretesResponse;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.writeCoilResponse;
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.writeCoilsResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readHoldingsResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.readInputsResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.writeHoldingResponse;
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.writeHoldingsResponse;
import static net.solarnetwork.node.io.modbus.ModbusRegisterBlockType.Holding;
import static net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterEntity.newRegisterEntity;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.util.BitSet;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.io.modbus.BitsModbusMessage;
import net.solarnetwork.io.modbus.ModbusErrorCode;
import net.solarnetwork.io.modbus.ModbusFunctionCode;
import net.solarnetwork.io.modbus.ModbusMessage;
import net.solarnetwork.io.modbus.RegistersModbusMessage;
import net.solarnetwork.io.modbus.netty.msg.BaseModbusMessage;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterDao;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.solarnetwork.util.NumberUtils;

/**
 * Handler for a Modbus server connection.
 *
 * @author matt
 * @version 2.1
 */
public class ModbusConnectionHandler implements BiConsumer<ModbusMessage, Consumer<ModbusMessage>> {

	/** The default {@code requestThrottle} property value. */
	public static final long DEFAULT_REQUEST_THROTTLE = 100;

	private static final Logger log = LoggerFactory.getLogger(ModbusConnectionHandler.class);

	private final Clock clock;
	private final Supplier<String> descriptor;
	private final ConcurrentMap<Integer, ModbusRegisterData> registers;
	private final Supplier<String> serverIdProvider;
	private final Supplier<ModbusRegisterDao> daoProvider;
	private long requestThrottle = DEFAULT_REQUEST_THROTTLE;
	private boolean allowWrites;

	private long lastRequestTime;

	/**
	 * Constructor.
	 *
	 * @param registers
	 *        the register data
	 * @param descriptor
	 *        a descriptor.get() for the connection
	 * @param serverIdProvider
	 *        and optional provider of the server ID to use, when
	 *        {@code daoProvider} is also configured
	 * @param daoProvider
	 *        and optional provider of a register DAO to persist updates with;
	 *        requires the {@code serverIdProvider} to be configured as well
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusConnectionHandler(ConcurrentMap<Integer, ModbusRegisterData> registers,
			Supplier<String> descriptor) {
		this(registers, descriptor, 0);
	}

	/**
	 * Constructor.
	 *
	 * @param registers
	 *        the register data
	 * @param descriptor
	 *        a descriptor.get() for the connection
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @since 2.1
	 */
	public ModbusConnectionHandler(ConcurrentMap<Integer, ModbusRegisterData> registers,
			Supplier<String> descriptor, Supplier<String> serverIdProvider,
			Supplier<ModbusRegisterDao> daoProvider) {
		this(Clock.systemUTC(), registers, descriptor, serverIdProvider, daoProvider, 0, false);
	}

	/**
	 * Constructor.
	 *
	 * @param registers
	 *        the register data
	 * @param descriptor
	 *        a descriptor.get() for the connection
	 * @param requestThrottle
	 *        if greater than {@literal 0} then a throttle in milliseconds to
	 *        handling requests
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code closeable} is {@literal null}
	 */
	public ModbusConnectionHandler(ConcurrentMap<Integer, ModbusRegisterData> registers,
			Supplier<String> descriptor, long requestThrottle) {
		this(registers, descriptor, requestThrottle, false);
	}

	/**
	 * Constructor.
	 *
	 * @param registers
	 *        the register data
	 * @param descriptor
	 *        a descriptor.get() for the connection
	 * @param requestThrottle
	 *        if greater than {@literal 0} then a throttle in milliseconds to
	 *        handling requests
	 * @param allowWrites
	 *        {@literal true} to allow Modbus write operations
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code closeable} is {@literal null}
	 * @since 2.0
	 */
	public ModbusConnectionHandler(ConcurrentMap<Integer, ModbusRegisterData> registers,
			Supplier<String> descriptor, long requestThrottle, boolean allowWrites) {
		this(Clock.systemUTC(), registers, descriptor, null, null, requestThrottle, allowWrites);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param registers
	 *        the register data
	 * @param descriptor
	 *        a descriptor.get() for the connection
	 * @param serverIdProvider
	 *        and optional provider of the server ID to use, when
	 *        {@code daoProvider} is also configured
	 * @param daoProvider
	 *        and optional provider of a register DAO to persist updates with;
	 *        requires the {@code serverIdProvider} to be configured as well
	 * @param requestThrottle
	 *        if greater than {@literal 0} then a throttle in milliseconds to
	 *        handling requests
	 * @param allowWrites
	 *        {@literal true} to allow Modbus write operations
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code closeable} is {@literal null}
	 * @since 2.1
	 */
	public ModbusConnectionHandler(Clock clock, ConcurrentMap<Integer, ModbusRegisterData> registers,
			Supplier<String> descriptor, Supplier<String> serverIdProvider,
			Supplier<ModbusRegisterDao> daoProvider, long requestThrottle, boolean allowWrites) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.registers = requireNonNullArgument(registers, "registers");
		this.descriptor = requireNonNullArgument(descriptor, "descriptor");
		this.serverIdProvider = serverIdProvider;
		this.daoProvider = daoProvider;
		this.requestThrottle = requestThrottle;
		this.allowWrites = allowWrites;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ModbusConnectionHandler{");
		builder.append(descriptor.get());
		builder.append("}");
		return builder.toString();
	}

	@Override
	public void accept(ModbusMessage req, Consumer<ModbusMessage> dest) {
		if ( requestThrottle > 0 ) {
			long now = System.currentTimeMillis();
			long elapsed = now - lastRequestTime;
			if ( elapsed < requestThrottle ) {
				try {
					long diff = requestThrottle - elapsed;
					log.trace("Request sooner than configured {}ms throttle: sleeping for {}ms",
							requestThrottle, diff);
					Thread.sleep(diff);
				} catch ( InterruptedException e ) {
					// ignore and continue
				} finally {
					lastRequestTime = System.currentTimeMillis();
				}
			} else {
				lastRequestTime = now;
			}
		}
		if ( log.isTraceEnabled() ) {
			log.trace("Modbus [{}] request: {}", descriptor.get(), req);
		}

		ModbusMessage res = null;

		final BitsModbusMessage bitReq = req.unwrap(BitsModbusMessage.class);
		if ( bitReq != null ) {
			res = handleBitsMessage(bitReq);
		} else {

			final RegistersModbusMessage regReq = req.unwrap(RegistersModbusMessage.class);
			if ( regReq != null ) {
				res = handleRegistersMessage(regReq);
			}
		}
		if ( res == null ) {
			res = new BaseModbusMessage(req.getUnitId(), req.getFunction(),
					ModbusErrorCode.IllegalFunction);
		}
		if ( log.isTraceEnabled() ) {
			log.trace("Modbus [{}] request {} response: {}", descriptor.get(), req, res);
		}
		dest.accept(res);
	}

	private ModbusMessage handleBitsMessage(BitsModbusMessage req) {
		ModbusFunctionCode fn = req.getFunction().functionCode();
		if ( fn == null ) {
			return null;
		}
		if ( fn.isReadFunction() ) {
			switch (fn) {
				case ReadCoils:
					return readCoils(req);

				case ReadDiscreteInputs:
					return readDiscretes(req);

				default:
					// not supported
			}
		} else if ( allowWrites ) {
			switch (fn) {
				case WriteCoil:
					return writeCoil(req);

				case WriteCoils:
					return writeCoils(req);

				default:
					// not supported
			}
		}
		return null;
	}

	private ModbusMessage handleRegistersMessage(RegistersModbusMessage req) {
		ModbusFunctionCode fn = req.getFunction().functionCode();
		if ( fn == null ) {
			return null;
		}
		if ( fn.isReadFunction() ) {
			switch (fn) {
				case ReadHoldingRegisters:
					return readHoldings(req);

				case ReadInputRegisters:
					return readInputs(req);

				default:
					// not supported
			}
		} else if ( allowWrites ) {
			switch (fn) {
				case WriteHoldingRegister:
					return writeRegister(req);

				case WriteHoldingRegisters:
					return writeRegisters(req);

				default:
					// not supported
			}
		}
		return null;
	}

	private ModbusRegisterData registerData(ModbusMessage req) {
		final Integer unitId = req.getUnitId();
		return registers.computeIfAbsent(unitId, k -> {
			return new ModbusRegisterData();
		});
	}

	private ModbusMessage readCoils(BitsModbusMessage req) {
		BitSet bits = registerData(req).readCoils(req.getAddress(), req.getCount());
		BigInteger data = NumberUtils.bigIntegerForBitSet(bits);
		return readCoilsResponse(req.getUnitId(), req.getAddress(), req.getCount(), data);
	}

	private ModbusMessage readDiscretes(BitsModbusMessage req) {
		BitSet bits = registerData(req).readDiscretes(req.getAddress(), req.getCount());
		BigInteger data = NumberUtils.bigIntegerForBitSet(bits);
		return readDiscretesResponse(req.getUnitId(), req.getAddress(), req.getCount(), data);
	}

	private ModbusMessage readHoldings(RegistersModbusMessage req) {
		short[] data = registerData(req).readHoldings(req.getAddress(), req.getCount());
		return readHoldingsResponse(req.getUnitId(), req.getAddress(), data);
	}

	private ModbusMessage readInputs(RegistersModbusMessage req) {
		short[] data = registerData(req).readInputs(req.getAddress(), req.getCount());
		return readInputsResponse(req.getUnitId(), req.getAddress(), data);
	}

	private ModbusMessage writeCoil(BitsModbusMessage req) {
		boolean data = req.isBitEnabled(0);
		registerData(req).writeCoil(req.getAddress(), data);
		if ( daoProvider != null && serverIdProvider != null ) {
			BitSet bits = new BitSet(1);
			bits.set(0, data);
			persistCoilRegisterData(req, bits);
		}
		return writeCoilResponse(req.getUnitId(), req.getAddress(), data);
	}

	private ModbusMessage writeCoils(BitsModbusMessage req) {
		BitSet data = req.toBitSet();
		registerData(req).writeCoils(req.getAddress(), req.getCount(), data);
		persistCoilRegisterData(req, data);
		return writeCoilsResponse(req.getUnitId(), req.getAddress(), req.getCount());
	}

	private ModbusMessage writeRegister(RegistersModbusMessage req) {
		short[] data = req.dataDecode();
		if ( data != null && data.length > 0 ) {
			registerData(req).writeHolding(req.getAddress(), data[0]);
			persistHoldingRegisterData(req, data);
		}
		return writeHoldingResponse(req.getUnitId(), req.getAddress(),
				data != null && data.length > 0 ? Short.toUnsignedInt(data[0]) : 0);
	}

	private ModbusMessage writeRegisters(RegistersModbusMessage req) {
		short[] data = req.dataDecode();
		registerData(req).writeHoldings(req.getAddress(), data);
		persistHoldingRegisterData(req, data);
		return writeHoldingsResponse(req.getUnitId(), req.getAddress(), req.getCount());
	}

	private void persistCoilRegisterData(BitsModbusMessage req, BitSet data) {
		if ( data == null || data.isEmpty() ) {
			return;
		}
		final ModbusRegisterDao dao = (daoProvider != null ? daoProvider.get() : null);
		final String serverId = (dao != null ? serverIdProvider.get() : null);
		if ( dao != null && serverId != null ) {
			Instant now = clock.instant();
			for ( int i = 0, len = data.size(); i < len; i++ ) {
				dao.save(newRegisterEntity(serverId, req.getUnitId(), ModbusRegisterBlockType.Coil,
						req.getAddress() + i, now, data.get(i) ? (short) 1 : (short) 0));
			}
		}
	}

	private void persistHoldingRegisterData(RegistersModbusMessage req, short[] data) {
		if ( data == null || data.length < 1 ) {
			return;
		}
		final ModbusRegisterDao dao = (daoProvider != null ? daoProvider.get() : null);
		final String serverId = (dao != null ? serverIdProvider.get() : null);
		if ( dao != null && serverId != null ) {
			Instant now = clock.instant();
			for ( int i = 0, len = data.length; i < len; i++ ) {
				dao.save(newRegisterEntity(serverId, req.getUnitId(), Holding, req.getAddress() + i, now,
						data[i]));
			}
		}
	}

	/**
	 * Get the request throttle, in milliseconds.
	 *
	 * @return the throttle time
	 * @since 2.0
	 */
	public long getRequestThrottle() {
		return requestThrottle;
	}

	/**
	 * Set the request throttle (minimum time between requests).
	 *
	 * @param requestThrottle
	 *        the throttle, in milliseconds
	 * @since 2.0
	 */
	public void setRequestThrottle(long requestThrottle) {
		this.requestThrottle = requestThrottle;
	}

	/**
	 * Get the read-write mode.
	 *
	 * @return {@literal true} if writing is allowed
	 * @since 2.0
	 */
	public boolean isAllowWrites() {
		return allowWrites;
	}

	/**
	 * Set the read-write mode.
	 *
	 * @param allowWrites
	 *        {@literal true} if writing is allowed
	 * @since 2.0
	 */
	public void setAllowWrites(boolean allowWrites) {
		this.allowWrites = allowWrites;
	}

}
