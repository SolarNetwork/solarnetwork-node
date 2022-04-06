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

import java.io.Closeable;
import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.io.ModbusTransport;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;

/**
 * Handler for a Modbus server connection.
 * 
 * @author matt
 * @version 1.1
 */
public class ModbusConnectionHandler implements Runnable, Closeable {

	private static final Logger log = LoggerFactory.getLogger(ModbusConnectionHandler.class);

	private final String description;
	private final ConcurrentMap<Integer, ModbusRegisterData> registers;
	private final ModbusTransport transport;
	private final Closeable closeable;
	private final long requestThrottle;

	/**
	 * Constructor.
	 * 
	 * @param transport
	 *        the transport to use
	 * @param registers
	 *        the register data
	 * @param description
	 *        a description for the connection
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusConnectionHandler(ModbusTransport transport,
			ConcurrentMap<Integer, ModbusRegisterData> registers, String description) {
		this(transport, registers, description, null, 0);
	}

	/**
	 * Constructor.
	 * 
	 * @param transport
	 *        the transport to use
	 * @param registers
	 *        the register data
	 * @param description
	 *        a description for the connection
	 * @param closeable
	 *        if provided, something to close when the handler is finished
	 * @param requestThrottle
	 *        if greater than {@literal 0} then a throttle in milliseconds to
	 *        handling requests
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code closeable} is {@literal null}
	 */
	public ModbusConnectionHandler(ModbusTransport transport,
			ConcurrentMap<Integer, ModbusRegisterData> registers, String description,
			Closeable closeable, long requestThrottle) {
		super();
		if ( transport == null ) {
			throw new IllegalArgumentException("The transport argument must not be null.");
		}
		this.transport = transport;
		if ( registers == null ) {
			throw new IllegalArgumentException("The registers argument must not be null.");
		}
		this.registers = registers;
		if ( description == null ) {
			throw new IllegalArgumentException("The description argument must not be null.");
		}
		this.description = description;
		this.closeable = closeable;
		this.requestThrottle = requestThrottle;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ModbusConnectionHandler{");
		builder.append(description);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public void close() throws IOException {
		transport.close();
	}

	@Override
	public void run() {
		long lastRequestTime = 0;
		try {
			do {
				ModbusRequest req = transport.readRequest();
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
					log.trace("Modbus [{}] request: {}", description, req.getHexMessage());
				}
				ModbusResponse res = null;
				if ( req instanceof ReadCoilsRequest ) {
					res = readCoils((ReadCoilsRequest) req);
				} else if ( req instanceof ReadInputDiscretesRequest ) {
					res = readDiscretes((ReadInputDiscretesRequest) req);
				} else if ( req instanceof ReadMultipleRegistersRequest ) {
					res = readHoldings((ReadMultipleRegistersRequest) req);
				} else if ( req instanceof ReadInputRegistersRequest ) {
					res = readInputs((ReadInputRegistersRequest) req);
				}
				if ( res == null ) {
					res = req.createExceptionResponse(Modbus.ILLEGAL_FUNCTION_EXCEPTION);
				}
				if ( log.isTraceEnabled() ) {
					log.trace("Modbus [{}] request {} response: {}", description, req.getHexMessage(),
							res.getHexMessage());
				}
				transport.writeMessage(res);
			} while ( true );
		} catch ( ModbusIOException e ) {
			if ( !e.isEOF() ) {
				log.warn("Modbus [{}] communication error: {}", description, e.toString());
			}
		} finally {
			try {
				transport.close();
			} catch ( IOException e ) {
				//ignore
			} finally {
				if ( closeable != null ) {
					try {
						closeable.close();
					} catch ( IOException e ) {
						// ignore
					}
				}
			}
		}
	}

	private ModbusRegisterData registerData(ModbusRequest req) {
		final Integer unitId = req.getUnitID();
		return registers.computeIfAbsent(unitId, k -> {
			return new ModbusRegisterData();
		});
	}

	private ReadCoilsResponse readCoils(ReadCoilsRequest req) {
		BitSet bits = registerData(req).readCoils(req.getReference(), req.getBitCount());
		ReadCoilsResponse res = new ReadCoilsResponse(req);
		for ( int i = 0, len = req.getBitCount(); i < len; i++ ) {
			res.setCoilStatus(i, bits.get(i));
		}
		return res;
	}

	private ReadInputDiscretesResponse readDiscretes(ReadInputDiscretesRequest req) {
		BitSet bits = registerData(req).readDiscretes(req.getReference(), req.getBitCount());
		ReadInputDiscretesResponse res = new ReadInputDiscretesResponse(req);
		for ( int i = 0, len = req.getBitCount(); i < len; i++ ) {
			res.setDiscreteStatus(i, bits.get(i));
		}
		return res;
	}

	private ReadMultipleRegistersResponse readHoldings(ReadMultipleRegistersRequest req) {
		byte[] data = registerData(req).readHoldings(req.getReference(), req.getWordCount());
		Register[] regs = new Register[req.getWordCount()];
		for ( int i = 0, a = 0, len = req.getWordCount(); i < len; i++, a += 2 ) {
			regs[i] = new SimpleRegister(data[a], data[a + 1]);
		}
		return new ReadMultipleRegistersResponse(req, regs);
	}

	private ReadInputRegistersResponse readInputs(ReadInputRegistersRequest req) {
		byte[] data = registerData(req).readInputs(req.getReference(), req.getWordCount());
		InputRegister[] regs = new InputRegister[req.getWordCount()];
		for ( int i = 0, a = 0, len = req.getWordCount(); i < len; i++, a += 2 ) {
			regs[i] = new SimpleRegister(data[a], data[a + 1]);
		}
		return new ReadInputRegistersResponse(req, regs);
	}

}
