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
import static net.solarnetwork.node.io.modbus.nifty.AbstractNiftyModbusNetwork.PUBLISH_MODBUS_CLI_COMMANDS_TOPIC;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import net.solarnetwork.io.modbus.AddressedModbusMessage;
import net.solarnetwork.io.modbus.BitsModbusMessage;
import net.solarnetwork.io.modbus.ModbusClient;
import net.solarnetwork.io.modbus.ModbusClientConfig;
import net.solarnetwork.io.modbus.ModbusMessage;
import net.solarnetwork.io.modbus.RegistersModbusMessage;
import net.solarnetwork.io.modbus.rtu.RtuModbusClientConfig;
import net.solarnetwork.io.modbus.serial.SerialParity;
import net.solarnetwork.io.modbus.serial.SerialStopBits;
import net.solarnetwork.io.modbus.tcp.TcpModbusClientConfig;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.AbstractModbusConnection;
import net.solarnetwork.node.service.LockTimeoutException;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Nifty Modbus implementation of {@link ModbusConnection}.
 *
 * <p>
 * This class has support for generating {@code mbpoll} commands from Modbus
 * messages. The {@code publishCliCommandMessages} property must be enabled for
 * this to occur. Then {@code mbpoll} commands will be logged at the
 * {@code DEBUG} level on the {@code net.solarnetwork.node.cli.modbus} logger.
 * If the {@code messageSendingOps} property is configured as well, then those
 * commands will be published to the
 * {@link AbstractNiftyModbusNetwork#PUBLISH_MODBUS_CLI_COMMANDS_TOPIC} topic.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public class NiftyModbusConnection extends AbstractModbusConnection implements ModbusConnection {

	/** The {@code connectTimeout} property default value. */
	public static final long DEFAULT_CONNECT_TIMEOUT = 10_000L;

	private static final Logger log = LoggerFactory.getLogger(NiftyModbusConnection.class);

	private static final Logger logCli = LoggerFactory.getLogger("net.solarnetwork.node.cli.modbus");

	/** The Modbus client. */
	protected final ModbusClient controller;

	/** Function that describes this network connection. */
	protected final Supplier<String> describer;

	/** The connection timeout, in milliseconds. */
	private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;

	/** Toggle the publishing of CLI command messages. */
	private boolean publishCliCommandMessages;

	/**
	 * The optional message sending operations, for publishing CLI command
	 * messages.
	 */
	private OptionalService<SimpMessageSendingOperations> messageSendingOps;

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

			if ( publishCliCommandMessages ) {
				publishCliCommand(req);
			}

			ModbusMessage res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(
						String.format("Modbus exception %s reading %d coil values from %d @ %s",
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

	private void publishCliCommand(ModbusMessage req) {
		final SimpMessageSendingOperations ops = OptionalService.service(this.messageSendingOps);
		if ( logCli.isDebugEnabled() || ops != null ) {
			List<String> cmd = mbpollCommand(req);
			if ( cmd != null ) {
				if ( logCli.isDebugEnabled() ) {
					logCli.debug(StringUtils.delimitedStringFromCollection(cmd, " "));
				}
				if ( ops != null ) {
					try {
						ops.convertAndSend(PUBLISH_MODBUS_CLI_COMMANDS_TOPIC, cmd);
					} catch ( MessagingException e ) {
						log.warn("Unable to post CLI command: {}", e);
					}
				}
			}
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
							String.format("Modbus exception %s writing %d coil value to %d @ %s",
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

			if ( publishCliCommandMessages ) {
				publishCliCommand(req);
			}

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

			if ( publishCliCommandMessages ) {
				publishCliCommand(req);
			}

			ModbusMessage res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(
						String.format("Modbus exception %s reading %d %s values from %d @ %s",
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

	/**
	 * Generate a CLI command for a {@link ModbusMessage} using {@code mbpoll}.
	 *
	 * @param req
	 *        the Modbus message to translate into a {@code mbpoll} command
	 * @return the command, or {@literal null} if one cannot be generated
	 * @see <a href="https://github.com/epsilonrt/mbpoll">epsilonrt/mbpoll</a>
	 */
	private List<String> mbpollCommand(ModbusMessage req) {
		final AddressedModbusMessage addReq = req.unwrap(AddressedModbusMessage.class);
		if ( addReq == null ) {
			log.warn("Unable to generate Modbus CLI command for unsupported request {}", req);
			return null;
		}
		final List<String> cmd = new ArrayList<>(32);
		final ModbusClientConfig config = controller.getClientConfig();
		if ( config == null ) {
			return null;
		}
		cmd.add("mbpoll");
		cmd.add("-q");
		cmd.add("-0");
		cmd.add("-1");
		if ( config instanceof TcpModbusClientConfig ) {
			TcpModbusClientConfig tcpConfig = (TcpModbusClientConfig) config;
			if ( tcpConfig.getPort() != TcpModbusClientConfig.DEFAULT_PORT ) {
				cmd.add("-p");
				cmd.add(Integer.toString(tcpConfig.getPort()));
			}
		} else if ( config instanceof RtuModbusClientConfig ) {
			RtuModbusClientConfig rtu = (RtuModbusClientConfig) config;
			cmd.add("-m");
			cmd.add("rtu");
			cmd.add("-b");
			cmd.add(Integer.toString(rtu.getSerialParameters().getBaudRate()));
			int v = rtu.getSerialParameters().getDataBits();
			if ( v != 8 ) {
				cmd.add("-d");
				cmd.add(Integer.toString(v));
			}
			SerialStopBits sb = rtu.getSerialParameters().getStopBits();
			if ( sb != SerialStopBits.Two ) {
				cmd.add("-s");
				cmd.add(Integer.toString(sb.getCode()));
			}
			SerialParity p = rtu.getSerialParameters().getParity();
			if ( p != SerialParity.Even ) {
				cmd.add("-P");
				if ( p == SerialParity.Odd ) {
					cmd.add("odd");
				} else {
					cmd.add("none");
				}
			}
		} else {
			log.warn("Unable to generate Modbus CLI command for unsupported ModbusClientConfig: {}",
					config.getClass());
			return null;
		}
		cmd.add("-a");
		cmd.add(Integer.toString(req.getUnitId()));
		cmd.add("-o");
		cmd.add("5");
		cmd.add("-t");
		switch (req.getFunction().blockType()) {
			case Coil:
				cmd.add("0");
				break;

			case Discrete:
				cmd.add("1");
				break;

			case Input:
				cmd.add("3:hex");
				break;

			case Holding:
				cmd.add("4:hex");
				break;

			default:
				log.warn("Unable to generate Modbus CLI command for unsupported block type {}",
						req.getFunction().blockType());
				return null;
		}
		cmd.add("-r");
		cmd.add(Integer.toString(addReq.getAddress()));
		if ( addReq.getCount() > 1 ) {
			cmd.add("-c");
			cmd.add(Integer.toString(addReq.getCount()));
		}

		if ( config instanceof TcpModbusClientConfig ) {
			TcpModbusClientConfig tcpConfig = (TcpModbusClientConfig) config;
			cmd.add(tcpConfig.getHost());
		} else if ( config instanceof RtuModbusClientConfig ) {
			RtuModbusClientConfig rtu = (RtuModbusClientConfig) config;
			cmd.add(rtu.getName());
		}

		return cmd;
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

			if ( publishCliCommandMessages ) {
				publishCliCommand(req);
			}

			ModbusMessage res = controller.send(req);
			if ( res.isException() ) {
				throw new IOException(
						String.format("Modbus exception %s reading %d %s values from %d @ %s",
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

	/**
	 * Get the "publish CLI command messages" setting.
	 *
	 * @return {@literal true} to publish CLI command messages
	 * @since 1.1
	 */
	public boolean isPublishCliCommandMessages() {
		return publishCliCommandMessages;
	}

	/**
	 * Set the "publish CLI command messages" setting.
	 *
	 * @param publishCliCommandMessages
	 *        {@literal true} to publish CLI command messages; requires the
	 *        {@link #setMessageSendingOps(OptionalService)} property also be
	 *        configured
	 * @since 1.1
	 */
	public void setPublishCliCommandMessages(boolean publishCliCommandMessages) {
		this.publishCliCommandMessages = publishCliCommandMessages;
	}

	/**
	 * Get the message sending operations.
	 *
	 * @return the message sending operations
	 * @since 1.1
	 */
	public OptionalService<SimpMessageSendingOperations> getMessageSendingOps() {
		return messageSendingOps;
	}

	/**
	 * Set the message sending operations.
	 *
	 * @param messageSendingOps
	 *        the message sending operations to set; required by the
	 *        {@link #setPublishCliCommandMessages(boolean)} setting
	 * @since 1.1
	 */
	public void setMessageSendingOps(OptionalService<SimpMessageSendingOperations> messageSendingOps) {
		this.messageSendingOps = messageSendingOps;
	}

}
