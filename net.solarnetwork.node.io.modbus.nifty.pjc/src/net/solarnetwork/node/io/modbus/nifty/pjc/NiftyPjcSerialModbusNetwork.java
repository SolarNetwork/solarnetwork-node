/* ==================================================================
 * NiftyPjcSerialModbusNetwork.java - 20/12/2023 6:37:01 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.nifty.pjc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import net.solarnetwork.io.modbus.ModbusClient;
import net.solarnetwork.io.modbus.rtu.netty.NettyRtuModbusClientConfig;
import net.solarnetwork.io.modbus.rtu.netty.RtuNettyModbusClient;
import net.solarnetwork.io.modbus.rtu.pjc.PjcSerialPortProvider;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.nifty.AbstractNiftyModbusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Nifty Modbus implementation of {@link ModbusNetwork} using a serial RTU
 * connection.
 * 
 * @author matt
 * @version 1.0
 */
public class NiftyPjcSerialModbusNetwork extends AbstractNiftyModbusNetwork<NettyRtuModbusClientConfig> {

	/**
	 * Constructor.
	 */
	public NiftyPjcSerialModbusNetwork() {
		super(new NettyRtuModbusClientConfig());
		config.setAutoReconnect(false);
		config.setSerialParameters(new SerialParameters(config));
		setDisplayName("Modbus RTU");
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
		RtuNettyModbusClient controller = new RtuNettyModbusClient(config, g,
				new PjcSerialPortProvider());
		controller.setWireLogging(isWireLogging());
		controller.setReplyTimeout(getReplyTimeout());
		return controller;
	}

	@SuppressWarnings("deprecation")
	private class InternalEventLoopGroup extends io.netty.channel.oio.OioEventLoopGroup {

		private volatile boolean shutDown;

		private InternalEventLoopGroup() {
			super(getEventLoopGroupMaxThreadCount(), NiftyPjcSerialModbusNetwork.this);
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
				String.valueOf(SerialParameters.DEFAULT_BAUD_RATE)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.databits",
				String.valueOf(SerialParameters.DEFAULT_DATA_BITS)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.stopbits",
				String.valueOf(SerialParameters.DEFAULT_STOP_BITS.getCode())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.parityString",
				SerialParameters.DEFAULT_PARITY.name()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.receiveTimeout",
				String.valueOf(SerialParameters.DEFAULT_READ_TIMEOUT)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlInString", "none"));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlOutString", "none"));

		results.addAll(baseNiftyModbusNetworkSettings(DEFAULT_KEEP_OPEN_SECONDS));

		return results;
	}

	// Accessors

	/**
	 * Get the serial parameters.
	 * 
	 * @return the parameters
	 */
	public SerialParameters getSerialParams() {
		return (SerialParameters) config.getSerialParameters();
	}
}
