/* ==================================================================
 * NiftyJscSerialModbusNetwork.java - 20/12/2022 2:23:56 pm
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

package net.solarnetwork.node.io.modbus.nifty.jsc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import net.solarnetwork.io.modbus.ModbusClient;
import net.solarnetwork.io.modbus.rtu.jsc.JscSerialPortProvider;
import net.solarnetwork.io.modbus.rtu.netty.NettyRtuModbusClientConfig;
import net.solarnetwork.io.modbus.rtu.netty.RtuNettyModbusClient;
import net.solarnetwork.io.modbus.serial.SerialParameters;
import net.solarnetwork.io.modbus.serial.SerialPortProvider;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.nifty.AbstractNiftyModbusNetwork;
import net.solarnetwork.node.io.modbus.nifty.rtu.SerialConnectionProvider;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Nifty Modbus implementation of {@link ModbusNetwork} using a serial RTU
 * connection.
 *
 * @author matt
 * @version 1.1
 */
public class NiftyJscSerialModbusNetwork extends AbstractNiftyModbusNetwork<NettyRtuModbusClientConfig>
		implements SerialConnectionProvider {

	private final JscSerialPortProvider serialPortProvider;

	/**
	 * Constructor.
	 */
	public NiftyJscSerialModbusNetwork() {
		this(null);
	}

	/**
	 * Constructor.
	 *
	 * @param serialPortProvider
	 *        an optional provider to use, if {@code null} a new default
	 *        instance will be created
	 */
	public NiftyJscSerialModbusNetwork(JscSerialPortProvider serialPortProvider) {
		super(new NettyRtuModbusClientConfig());
		config.setAutoReconnect(false);
		config.setSerialParameters(new NiftySerialParameters(config));
		setDisplayName("Modbus RTU");
		this.serialPortProvider = (serialPortProvider != null ? serialPortProvider
				: new JscSerialPortProvider());
	}

	@Override
	public String serialPortName() {
		return config.getName();
	}

	@Override
	public SerialParameters serialParameters() {
		return config.getSerialParameters();
	}

	@Override
	public SerialPortProvider serialPortProvider() {
		return serialPortProvider;
	}

	@Override
	protected String getNetworkDescription() {
		StringBuilder buf = new StringBuilder(config.getDescription());
		buf.append(" readTimeout=").append(config.getSerialParameters().getReadTimeout());
		return buf.toString();
	}

	@Override
	protected boolean isConfigured() {
		String portName = config.getName();
		return (portName != null && !portName.isEmpty());
	}

	@Override
	protected ModbusClient createController() {
		EventLoopGroup g = getOrCreateEventLoopGroup(() -> {
			return new InternalEventLoopGroup();
		});
		RtuNettyModbusClient controller = new RtuNettyModbusClient(config, g, serialPortProvider);
		controller.setWireLogging(isWireLogging());
		controller.setReplyTimeout(getReplyTimeout());
		return controller;
	}

	@SuppressWarnings("deprecation")
	private class InternalEventLoopGroup extends io.netty.channel.oio.OioEventLoopGroup {

		private volatile boolean shutDown;

		private InternalEventLoopGroup() {
			super(getEventLoopGroupMaxThreadCount(), NiftyJscSerialModbusNetwork.this);
		}

		@Override
		public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
			shutDown = true;
			return super.shutdownGracefully(quietPeriod, timeout, unit);
		}

		@Override
		public void shutdown() {
			shutDown = true;
			super.shutdown();
		}

		@Override
		public boolean isShuttingDown() {
			return super.isShuttingDown() && shutDown;
		}

		@Override
		public List<Runnable> shutdownNow() {
			shutDown = true;
			return super.shutdownNow();
		}

	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.modbus";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(20);
		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName", null));

		results.add(new BasicTextFieldSettingSpecifier("serialParams.baudRate",
				String.valueOf(NiftySerialParameters.DEFAULT_BAUD_RATE)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.databits",
				String.valueOf(NiftySerialParameters.DEFAULT_DATA_BITS)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.stopbits",
				String.valueOf(NiftySerialParameters.DEFAULT_STOP_BITS.getCode())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.parityString",
				NiftySerialParameters.DEFAULT_PARITY.name()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.receiveTimeout",
				String.valueOf(NiftySerialParameters.DEFAULT_READ_TIMEOUT)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlInString", "none"));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlOutString", "none"));

		results.add(new BasicToggleSettingSpecifier("serialParams.rs485ModeEnabled", Boolean.FALSE));
		results.add(new BasicToggleSettingSpecifier("serialParams.rs485RtsHighEnabled", Boolean.TRUE));
		results.add(
				new BasicToggleSettingSpecifier("serialParams.rs485TerminationEnabled", Boolean.FALSE));
		results.add(new BasicToggleSettingSpecifier("serialParams.rs485EchoEnabled", Boolean.FALSE));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.rs485BeforeSendDelay",
				String.valueOf(NiftySerialParameters.DEFAULT_RS485_BEFORE_SEND_DELAY)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.rs485AfterSendDelay",
				String.valueOf(NiftySerialParameters.DEFAULT_RS485_AFTER_SEND_DELAY)));

		results.addAll(baseNiftyModbusNetworkSettings(DEFAULT_KEEP_OPEN_SECONDS));

		return results;
	}

	// Accessors

	/**
	 * Get the serial parameters.
	 *
	 * @return the parameters
	 */
	public NiftySerialParameters getSerialParams() {
		return (NiftySerialParameters) config.getSerialParameters();
	}
}
