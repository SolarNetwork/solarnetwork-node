/* ==================================================================
 * ModbusServer.java - 12/01/2026 6:02:04 PM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.rtu;

import static net.solarnetwork.service.OptionalService.requiredService;
import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.io.modbus.ModbusMessage;
import net.solarnetwork.io.modbus.ModbusValidationException;
import net.solarnetwork.io.modbus.rtu.netty.NettyRtuModbusServer;
import net.solarnetwork.node.io.modbus.nifty.rtu.SerialConnectionProvider;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.solarnetwork.node.io.modbus.server.impl.BaseModbusServer;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalServiceNotAvailableException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.ObjectUtils;

/**
 * Modbus RTU server service.
 *
 * @author matt
 * @version 1.1
 * @since 5.3
 */
public class ModbusServer extends BaseModbusServer<NettyRtuModbusServer> {

	/**
	 * The setting UID used by this service.
	 *
	 */
	public static final String SETTING_UID = "net.solarnetwork.node.io.modbus.server.rtu";

	private final OptionalService<SerialConnectionProvider> connectionProvider;

	/**
	 * Constructor.
	 *
	 * @param executor
	 *        the executor to handle client connections with
	 * @param connectionProvider
	 *        the connection provider
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusServer(Executor executor,
			OptionalService<SerialConnectionProvider> connectionProvider) {
		this(executor, connectionProvider, new ConcurrentHashMap<>(2, 0.9f, 2));
	}

	/**
	 * Constructor.
	 *
	 * @param executor
	 *        the executor to handle client connections with
	 * @param connectionProvider
	 *        the connection provider
	 * @param registers
	 *        the register data map to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusServer(Executor executor, OptionalService<SerialConnectionProvider> connectionProvider,
			ConcurrentMap<Integer, ModbusRegisterData> registers) {
		super(executor, registers);
		this.connectionProvider = ObjectUtils.requireNonNullArgument(connectionProvider,
				"connectionProvider");
	}

	@Override
	protected String description() {
		final String uid = getUid();
		final SerialConnectionProvider provider = service(this.connectionProvider);
		final String serialPort = (provider != null ? provider.serialPortName() : null);
		final boolean hasUid = (uid != null && !uid.isEmpty());
		StringBuilder buf = new StringBuilder();
		if ( hasUid ) {
			buf.append(uid);
		}
		if ( serialPort != null && !serialPort.isEmpty() ) {
			if ( hasUid ) {
				buf.append(" (");
			}
			buf.append("port ").append(serialPort);
			if ( hasUid ) {
				buf.append(")");
			}
		}
		return buf.toString();
	}

	private SerialConnectionProvider provider() {
		final SerialConnectionProvider provider = requiredService(this.connectionProvider, "");
		if ( provider == null ) {
			throw new OptionalServiceNotAvailableException(
					"The serial connection provider is not available.");
		}
		return provider;
	}

	@Override
	protected NettyRtuModbusServer startServer() throws IOException {
		final SerialConnectionProvider provider = provider();
		NettyRtuModbusServer server = new NettyRtuModbusServer(provider.serialPortName(),
				provider.serialParameters(), provider.serialPortProvider());
		server.setMessageHandler(handler);
		server.setWireLogging(isWireLogging());
		server.start();
		return server;
	}

	@Override
	protected void stopServer(NettyRtuModbusServer server) {
		server.stop();
	}

	@Override
	protected void handleException(Throwable t, Optional<ModbusMessage> msg) {
		super.handleException(t, msg);
		if ( t instanceof ModbusValidationException ) {
			// restart server after validation exception
			log.warn("Restarting Modbus Server [{}] after message validation exception.", description());
			final TaskScheduler scheduler = getTaskScheduler();
			if ( scheduler != null ) {
				scheduler.schedule(this::restartServer, Instant.now().plusMillis(100));
			} else {
				restartServer();
			}
		}
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	protected List<SettingSpecifier> getExtendedSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(1);

		// note we use bindAddress as the setting for compatibility with ModbusServerConfig
		result.add(new BasicTextFieldSettingSpecifier("bindAddress", null, false,
				"(objectClass=net.solarnetwork.node.io.modbus.nifty.rtu.SerialConnectionProvider)"));

		return result;
	}

	/**
	 * Get the connection provider.
	 *
	 * @return the provider
	 */
	public OptionalService<SerialConnectionProvider> getConnectionProvider() {
		return connectionProvider;
	}

	/**
	 * Get the SerialConnectionProvider service UID filter value.
	 *
	 * @return the SerialConnectionProvider UID filter value, if
	 *         {@code connectionProvider} also implements
	 *         {@link FilterableService}
	 */
	public String getSerialConnectionProviderUid() {
		String uid = FilterableService.filterPropValue(connectionProvider, "uid");
		if ( uid != null && uid.trim().isEmpty() ) {
			uid = null;
		}
		return uid;
	}

	/**
	 * Set the SerialConnectionProvider service UID filter value.
	 *
	 * @param uid
	 *        the SerialConnectionProvider UID filter value to set, if
	 *        {@code baconnectionProvidercnetNetwork} also implements
	 *        {@link FilterableService}
	 */
	public void setSerialConnectionProviderUid(String uid) {
		FilterableService.setFilterProp(connectionProvider, "uid", uid);
	}

	/**
	 * Get the address to bind to.
	 *
	 * <p>
	 * This method is provided for compatibility with
	 * {@code ModbusServerConfig}.
	 * </p>
	 *
	 * @return the {@link SerialConnectionProvider} UID
	 */
	public String getBindAddress() {
		return getSerialConnectionProviderUid();
	}

	/**
	 * Set the address to bind to.
	 *
	 * <p>
	 * This method is provided for compatibility with
	 * {@code ModbusServerConfig}.
	 * </p>
	 *
	 * @param bindAddress
	 *        the address to set, which will be treated as the
	 *        {@link SerialConnectionProvider} UID
	 */
	public void setBindAddress(String bindAddress) {
		setSerialConnectionProviderUid(bindAddress);
	}

	/**
	 * Get the server listen port.
	 *
	 * <p>
	 * This method is provided for compatibility with
	 * {@code ModbusServerConfig}.
	 * </p>
	 *
	 * @return {@code null}
	 */
	public Integer getPort() {
		return null;
	}

	/**
	 * Set the server listen port.
	 *
	 * <p>
	 * This method is provided for compatibility with
	 * {@code ModbusServerConfig}.
	 * </p>
	 *
	 * @param port
	 *        will be ignored
	 */
	public void setPort(Integer port) {
		// ignored
	}

}
