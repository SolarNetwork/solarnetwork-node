/* ==================================================================
 * ModbusServer.java - 17/09/2020 4:54:54 PM
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

import static java.lang.String.format;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.server.domain.MeasurementConfig;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.solarnetwork.node.io.modbus.server.domain.RegisterBlockConfig;
import net.solarnetwork.node.io.modbus.server.domain.RegisterBlockType;
import net.solarnetwork.node.io.modbus.server.domain.UnitConfig;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.StringUtils;
import net.wimpi.modbus.io.ModbusTCPTransport;

/**
 * Modbus TCP server service.
 * 
 * @author matt
 * @version 2.4
 */
public class ModbusServer extends BaseIdentifiable
		implements SettingSpecifierProvider, SettingsChangeObserver, EventHandler, ThreadFactory {

	/** The default listen port. */
	public static final int DEFAULT_PORT = 502;

	/** The default startup delay, in seconds. */
	public static final int DEFAULT_STARTUP_DELAY_SECS = 15;

	/** The default {@code maxBacklog} property value. */
	public static final int DEFAULT_BACKLOG = 5;

	/** The default {@code bindAddress} property value. */
	public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";

	/** The default {@code requestThrottle} property value. */
	public static final long DEFAULT_REQUEST_THROTTLE = 100;

	/**
	 * The setting UID used by this service.
	 * 
	 * @since 2.3
	 */
	public static final String SETTING_UID = "net.solarnetwork.node.io.modbus.server";

	private static final Logger log = LoggerFactory.getLogger(ModbusServer.class);

	private final Executor executor;
	private final Executor serverExecutor;
	private final ConcurrentMap<Integer, ModbusRegisterData> registers;
	private final AtomicInteger clientThreadCount = new AtomicInteger(0);
	private int port = DEFAULT_PORT;
	private String bindAddress = DEFAULT_BIND_ADDRESS;
	private int backlog = DEFAULT_BACKLOG;
	private int startupDelay = DEFAULT_STARTUP_DELAY_SECS;
	private long requestThrottle = DEFAULT_REQUEST_THROTTLE;
	private TaskScheduler taskScheduler;
	private UnitConfig[] unitConfigs;
	private boolean allowWrites;

	private ScheduledFuture<?> startupFuture;
	private ServerThread serverThread;

	/**
	 * Constructor.
	 * 
	 * @param executor
	 *        the executor to handle client connections with
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusServer(Executor executor) {
		this(executor, new ConcurrentHashMap<>(2, 0.9f, 2));
	}

	/**
	 * Constructor.
	 * 
	 * @param executor
	 *        the executor to handle client connections with
	 * @param registers
	 *        the register data map to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusServer(Executor executor, ConcurrentMap<Integer, ModbusRegisterData> registers) {
		super();
		if ( executor == null ) {
			throw new IllegalArgumentException("The executor argument must not be null.");
		}
		this.executor = executor;
		this.serverExecutor = Executors.newCachedThreadPool(this);
		if ( registers == null ) {
			throw new IllegalArgumentException("The registers argument must not be null.");
		}
		this.registers = registers;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread thread = new Thread(null, r,
				format("ModbusServer-%d-Client-%d", getPort(), clientThreadCount.incrementAndGet()));
		thread.setDaemon(true);
		return thread;
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		restartServer();
	}

	/**
	 * Start up the server.
	 */
	public void startup() {
		restartServer();
	}

	/**
	 * Shut down the server.
	 */
	public void shutdown() {
		if ( startupFuture != null && !startupFuture.isDone() ) {
			startupFuture.cancel(true);
			startupFuture = null;
		}
		if ( serverThread != null ) {
			serverThread.finish();
			serverThread = null;
		}
	}

	private synchronized void restartServer() {
		shutdown();
		Runnable startupTask = new Runnable() {

			@Override
			public void run() {
				synchronized ( ModbusServer.this ) {
					startupFuture = null;
					if ( serverThread != null ) {
						try {
							serverThread.finish();
						} finally {
							serverThread = null;
						}
					}
					try {
						serverThread = new ServerThread(bindAddress, port, backlog);
						serverThread.start();
					} catch ( IOException e ) {
						if ( serverThread != null ) {
							serverThread.finish();
							serverThread = null;
						}
						log.error("Error binding Modbus server {} to {}:{}: {}", ModbusServer.this, port,
								bindAddress, e.toString());
						if ( taskScheduler != null ) {
							log.info("Will start Modbus server on port {} in {} seconds", port,
									startupDelay);
							startupFuture = taskScheduler.schedule(this,
									new Date(System.currentTimeMillis() + startupDelay * 1000L));
						}
					}
				}
			}
		};
		if ( taskScheduler != null ) {
			log.info("Will start Modbus server on port {} in {} seconds", port, startupDelay);
			startupFuture = taskScheduler.schedule(startupTask,
					new Date(System.currentTimeMillis() + startupDelay * 1000L));
		} else {
			startupTask.run();
		}
	}

	private final class ServerThread extends Thread {

		private final InetAddress addr;
		private final int port;
		private final int backlog;
		private ServerSocket socket;
		private boolean listening;

		private final Set<ModbusConnectionHandler> clients = new CopyOnWriteArraySet<>();

		private ServerThread(String bindAddress, int port, int backlog) throws IOException {
			super();
			this.addr = InetAddress.getByName(bindAddress);
			this.port = port;
			this.backlog = backlog;
			bind();
		}

		private synchronized void bind() throws IOException {
			socket = new ServerSocket();
			socket.setReuseAddress(true);
			socket.setSoTimeout(60000);
			socket.bind(new InetSocketAddress(this.addr, port), backlog);
			this.listening = true;
			log.info("Modbus server listening on {}:{}", addr, port);
		}

		private synchronized void finish() {
			this.listening = false;
			close();
		}

		private synchronized void close() {
			try {
				if ( socket != null && !socket.isClosed() ) {
					for ( ModbusConnectionHandler handler : clients ) {
						try {
							handler.close();
						} catch ( IOException e ) {
							log.debug("Error closing Modbus TCP client connection: " + e.toString());
						}
					}
					clients.clear();
					try {
						socket.close();
					} catch ( IOException e ) {
						log.warn("Error closing Modbus server {}:{}: {}", addr, port, e.toString());
					}
				}
			} finally {
				socket = null;
			}
		}

		@Override
		public void run() {
			try {
				while ( true ) {
					synchronized ( this ) {
						if ( !listening ) {
							return;
						}
					}
					try {
						if ( socket == null ) {
							bind();
						}
						Socket in = socket.accept();
						log.debug("Modbus server {}:{} connection created: {}", addr, port, in);
						ModbusConnectionHandler handler = new ModbusConnectionHandler(
								new ModbusTCPTransport(in), registers,
								String.format("TCP %s:%d %d", addr, port, in.getLocalPort()), in,
								requestThrottle, allowWrites);
						clients.add(handler);
						serverExecutor.execute(handler);
					} catch ( SocketTimeoutException e ) {
						//  just try to accept again
						log.debug("Socket timeout exception in Modbus server {}:{}: {}", addr, port,
								e.toString());
					} catch ( SocketException e ) {
						// just try to accept again
						log.debug("Socket exception in Modbus server {}:{}: {}", addr, port,
								e.toString());
					} catch ( IOException e ) {
						// close and try again
						close();
					}
				}
			} finally {
				close();
				log.info("Modbus server {}:{} finished.", addr, port);
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		String topic = (event != null ? event.getTopic() : null);
		if ( DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED.equals(topic)
				|| NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED.equals(topic)
				|| NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED.equals(topic) ) {
			handleDatumCapturedEvent(event);
		}
	}

	private void handleDatumCapturedEvent(Event event) {
		Object d = event.getProperty(DatumEvents.DATUM_PROPERTY);
		if ( !(d instanceof NodeDatum && ((NodeDatum) d).getSourceId() != null) ) {
			return;
		}
		UnitConfig[] unitConfigs = getUnitConfigs();
		if ( unitConfigs == null || unitConfigs.length < 1 ) {
			return;
		}

		NodeDatum datum = (NodeDatum) d;
		final DatumSamplesOperations ops = datum.asSampleOperations();
		final String sourceId = datum.getSourceId();

		log.trace("Inspecting {} event datum {} ", event.getTopic(), datum);
		List<MeasurementUpdate> updates = null;
		for ( UnitConfig unitConfig : unitConfigs ) {
			RegisterBlockConfig[] blockConfigs = unitConfig.getRegisterBlockConfigs();
			if ( blockConfigs == null || blockConfigs.length < 1 ) {
				continue;
			}
			for ( RegisterBlockConfig blockConfig : blockConfigs ) {
				MeasurementConfig[] measConfigs = blockConfig.getMeasurementConfigs();
				if ( measConfigs == null || measConfigs.length < 1 ) {
					continue;
				}
				int address = blockConfig.getStartAddress();
				for ( MeasurementConfig measConfig : measConfigs ) {
					if ( sourceId.equals(measConfig.getSourceId())
							&& measConfig.getPropertyName() != null
							&& ops.hasSampleValue(measConfig.getPropertyName()) ) {
						MeasurementUpdate up = new MeasurementUpdate(unitConfig, blockConfig, measConfig,
								ops.findSampleValue(measConfig.getPropertyName()), address);
						if ( updates == null ) {
							updates = new ArrayList<>(4);
						}
						updates.add(up);
					}
					address += measConfig.getSize();
				}
			}
		}
		if ( updates != null ) {
			log.trace("Queuing [{}] updates: {}", sourceId, updates.stream().map(Object::toString)
					.collect(Collectors.joining(",\n\t", "[\n\t", "\n]")));
			final List<MeasurementUpdate> finalUpdates = updates;
			executor.execute(new Runnable() {

				@Override
				public void run() {
					applyDatumCapturedUpdates(finalUpdates);
				}
			});
		}
	}

	private static class MeasurementUpdate {

		private final UnitConfig unitConfig;
		private final RegisterBlockConfig blockConfig;
		private final MeasurementConfig measConfig;
		private final Object propertyValue;
		private final int address;

		public MeasurementUpdate(UnitConfig unitConfig, RegisterBlockConfig blockConfig,
				MeasurementConfig measConfig, Object propertyValue, int address) {
			super();
			this.unitConfig = unitConfig;
			this.blockConfig = blockConfig;
			this.measConfig = measConfig;
			this.propertyValue = propertyValue;
			this.address = address;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MeasurementUpdate{");
			if ( unitConfig != null ) {
				builder.append("unit=");
				builder.append(unitConfig.getUnitId());
				builder.append(", ");
			}
			if ( blockConfig != null ) {
				builder.append("blockType=");
				builder.append(blockConfig.getBlockType());
				builder.append(", ");
			}
			if ( measConfig != null ) {
				builder.append("dataType=");
				builder.append(measConfig.getDataType());
				builder.append(", ");
			}
			if ( propertyValue != null ) {
				builder.append("address=");
				builder.append(address);
				builder.append(", ");
			}
			builder.append("value=");
			builder.append(propertyValue);
			builder.append("}");
			return builder.toString();
		}

	}

	private void applyDatumCapturedUpdates(final Collection<MeasurementUpdate> updates) {
		for ( MeasurementUpdate update : updates ) {
			Integer unitId = update.unitConfig.getUnitId();
			RegisterBlockType blockType = update.blockConfig.getBlockType();
			ModbusDataType dataType = update.measConfig.getDataType();
			log.trace("Updating measurement [{}.{}] unit {} {} {} @ {}: {}",
					update.measConfig.getSourceId(), update.measConfig.getPropertyName(), unitId,
					blockType, dataType, update.address, update.propertyValue);
			if ( update.propertyValue == null ) {
				continue;
			}
			ModbusRegisterData regData = registers.computeIfAbsent(unitId, k -> {
				return new ModbusRegisterData();
			});
			switch (blockType) {
				case Coil:
				case Discrete:
					boolean bitVal = booleanPropertyValue(update.propertyValue);
					regData.writeBit(blockType, update.address, bitVal);
					break;

				case Holding:
				case Input:
					Object xVal = update.measConfig.applyTransforms(update.propertyValue);
					regData.writeValue(blockType, dataType, update.address, update.measConfig.getSize(),
							xVal);
					break;

			}
		}
	}

	private boolean booleanPropertyValue(Object propVal) {
		if ( propVal instanceof Boolean ) {
			return ((Boolean) propVal).booleanValue();
		} else if ( propVal instanceof Number ) {
			return ((Number) propVal).intValue() == 0 ? false : true;
		} else {
			return StringUtils.parseBoolean(propVal.toString());
		}
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);

		for ( Map.Entry<Integer, ModbusRegisterData> me : registers.entrySet() ) {
			result.add(new BasicTitleSettingSpecifier("info", registersInfo(me.getKey(), me.getValue()),
					true));
		}

		result.addAll(baseIdentifiableSettings(null));
		result.add(new BasicTextFieldSettingSpecifier("bindAddress", DEFAULT_BIND_ADDRESS));
		result.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(DEFAULT_PORT)));
		result.add(new BasicTextFieldSettingSpecifier("requestThrottle",
				String.valueOf(DEFAULT_REQUEST_THROTTLE)));
		result.add(new BasicToggleSettingSpecifier("allowWrites", false));

		UnitConfig[] blockConfs = getUnitConfigs();
		List<UnitConfig> blockConfsList = (blockConfs != null ? Arrays.asList(blockConfs)
				: Collections.emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("unitConfigs", blockConfsList,
				new SettingUtils.KeyedListCallback<UnitConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(UnitConfig value, int index,
							String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								value.settings(key + ".", getMessageSource()));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return result;
	}

	private String registersInfo(Integer unitId, ModbusRegisterData regData) {
		StringBuilder buf = new StringBuilder("Unit ID: ");
		buf.append(unitId);
		String info = coilsInfoValue(regData);
		if ( info != null ) {
			buf.append("\nCoils: ").append(info);
		}
		info = discretesInfoValue(regData);
		if ( info != null ) {
			buf.append("\nDiscrete Inputs: ").append(info);
		}
		info = holdingsInfoValue(regData);
		if ( info != null ) {
			buf.append("\nHolding Registers:\n").append(info);
		}
		info = inputsInfoValue(regData);
		if ( info != null ) {
			buf.append("\nInput Registers:\n").append(info);
		}
		return buf.toString();
	}

	private String coilsInfoValue(ModbusRegisterData registers) {
		return bitSetString(registers.getCoils());
	}

	private String discretesInfoValue(ModbusRegisterData registers) {
		return bitSetString(registers.getDiscretes());
	}

	private String holdingsInfoValue(ModbusRegisterData registers) {
		return dataString(registers.getHoldings());
	}

	private String inputsInfoValue(ModbusRegisterData registers) {
		return dataString(registers.getInputs());
	}

	private static String bitSetString(BitSet set) {
		if ( set.isEmpty() ) {
			return null;
		}
		return StringUtils.commaDelimitedStringFromCollection(
				set.stream().mapToObj(Integer::valueOf).collect(Collectors.toList()));
	}

	private static String dataString(ModbusData data) {
		if ( data == null ) {
			return null;
		}
		String s = data.dataDebugString();
		int line1 = s.indexOf('\n');
		int lineN = s.lastIndexOf('\n');
		if ( line1 > 0 && lineN > line1 ) {
			s = s.substring(line1 + 1, lineN);
		} else {
			return null;
		}
		return s.isEmpty() ? null : s;
	}

	/**
	 * Get the task scheduler.
	 * 
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 * 
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the startup delay.
	 * 
	 * @return the startup delay, in seconds
	 */
	public int getStartupDelay() {
		return startupDelay;
	}

	/**
	 * Get the address to bind to.
	 * 
	 * @return the address; defaults to {@link #DEFAULT_BIND_ADDRESS}
	 */
	public String getBindAddress() {
		return bindAddress;
	}

	/**
	 * Set the address to bind to.
	 * 
	 * @param bindAddress
	 *        the address to set
	 */
	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

	/**
	 * Set the startup delay.
	 * 
	 * @param startupDelay
	 *        the delay to set, in seconds
	 */
	public void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * Get the server socket backlog setting.
	 * 
	 * @return the backlog; defaults to {@link #DEFAULT_BACKLOG}
	 */
	public int getBacklog() {
		return backlog;
	}

	/**
	 * Set the server socket backlog setting.
	 * 
	 * @param backlog
	 *        the backlog to set
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * Get the server listen port.
	 * 
	 * @return the port; defaults to {@link #DEFAULT_PORT}
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the server listen port.
	 * 
	 * @param port
	 *        the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Get the block configurations.
	 * 
	 * @return the block configurations
	 */
	public UnitConfig[] getUnitConfigs() {
		return unitConfigs;
	}

	/**
	 * Set the block configurations to use.
	 * 
	 * @param unitConfigs
	 *        the configurations to use
	 */
	public void setUnitConfigs(UnitConfig[] unitConfigs) {
		this.unitConfigs = unitConfigs;
	}

	/**
	 * Get the number of configured {@code unitConfigs} elements.
	 * 
	 * @return the number of {@code unitConfigs} elements
	 */
	public int getUnitConfigsCount() {
		UnitConfig[] confs = this.unitConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code UnitConfig} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new {@link UnitConfig}
	 * instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code unitConfigs} elements.
	 */
	public void setUnitConfigsCount(int count) {
		this.unitConfigs = ArrayUtils.arrayWithLength(this.unitConfigs, count, UnitConfig.class, null);
	}

	/**
	 * Get the request throttle.
	 * 
	 * @return the minimum time in milliseconds allowed between client requests,
	 *         or {@code 0} for no minimum
	 * @since 2.2
	 */
	public long getRequestThrottle() {
		return requestThrottle;
	}

	/**
	 * Set the request throttle.
	 * 
	 * @param requestThrottle
	 *        the minimum time in milliseconds allowed between client requests,
	 *        or {@code 0} to disable
	 * @since 2.2
	 */
	public void setRequestThrottle(long requestThrottle) {
		this.requestThrottle = requestThrottle;
	}

	/**
	 * Get the toggle value to allow Modbus writes.
	 * 
	 * @return {@literal true} to allow Modbus clients to write to holding/coil
	 *         registers
	 * @since 2.4
	 */
	public boolean isAllowWrites() {
		return allowWrites;
	}

	/**
	 * Toggle allowing Modbus writes.
	 * 
	 * @param allowWrites
	 *        {@literal true} to allow Modbus clients to write to holding/coil
	 *        registers
	 * @since 2.4
	 */
	public void setAllowWrites(boolean allowWrites) {
		this.allowWrites = allowWrites;
	}

}
