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

import static net.solarnetwork.node.io.modbus.server.dao.BasicModbusRegisterFilter.forServerId;
import static net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterEntity.newRegisterEntity;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData.encodeValue;
import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.io.modbus.tcp.netty.NettyTcpModbusServer;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterDao;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterEntity;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterKey;
import net.solarnetwork.node.io.modbus.server.domain.MeasurementConfig;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.solarnetwork.node.io.modbus.server.domain.RegisterBlockConfig;
import net.solarnetwork.node.io.modbus.server.domain.UnitConfig;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.IntShortMap;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Modbus TCP server service.
 *
 * @author matt
 * @version 3.3
 */
public class ModbusServer extends BaseIdentifiable implements SettingSpecifierProvider,
		SettingsChangeObserver, ServiceLifecycleObserver, EventHandler, PingTest {

	/** The default listen port. */
	public static final int DEFAULT_PORT = 502;

	/** The default startup delay, in seconds. */
	public static final int DEFAULT_STARTUP_DELAY_SECS = 15;

	/**
	 * The default {@code maxBacklog} property value.
	 *
	 * @deprecated no longer used
	 */
	@Deprecated
	public static final int DEFAULT_BACKLOG = 5;

	/** The default {@code bindAddress} property value. */
	public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";

	/**
	 * The setting UID used by this service.
	 *
	 * @since 2.3
	 */
	public static final String SETTING_UID = "net.solarnetwork.node.io.modbus.server";

	/** The {@code pendingMessageTtl} property default value. */
	public static final long DEFAULT_PENDING_MESSAGE_TTL = TimeUnit.MINUTES.toMillis(2);

	private static final Logger log = LoggerFactory.getLogger(ModbusServer.class);

	private final Executor executor;
	private final ConcurrentMap<Integer, ModbusRegisterData> registers;
	private final ModbusConnectionHandler handler;
	private int port = DEFAULT_PORT;
	private String bindAddress = DEFAULT_BIND_ADDRESS;
	private int startupDelay = DEFAULT_STARTUP_DELAY_SECS;
	private TaskScheduler taskScheduler;
	private UnitConfig[] unitConfigs;
	private long pendingMessageTtl = DEFAULT_PENDING_MESSAGE_TTL;
	private boolean wireLogging;
	private OptionalService<ModbusRegisterDao> registerDao;
	private boolean daoRequired;

	private NettyTcpModbusServer server;
	private ScheduledFuture<?> startupFuture;

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
		this.executor = ObjectUtils.requireNonNullArgument(executor, "executor");
		this.registers = ObjectUtils.requireNonNullArgument(registers, "registers");
		this.handler = new ModbusConnectionHandler(registers, this::description, this::getUid,
				() -> service(registerDao));
	}

	private String description() {
		String uid = getUid();
		if ( uid != null && !uid.isEmpty() ) {
			return uid + " (port " + port + ")";
		}
		return "port " + port;
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		if ( server != null ) {
			log.info("Restarting Modbus server [{}] from configuration change", description());
		}
		restartServer();
	}

	@Override
	public void serviceDidStartup() {
		restartServer();

	}

	@Override
	public void serviceDidShutdown() {
		stop();
	}

	/**
	 * Start the server.
	 *
	 * <p>
	 * Upon return the server will be bound and ready to accept connections on
	 * the configured port.
	 * </p>
	 *
	 * @throws IOException
	 *         if an IO error occurs
	 */
	public synchronized void start() throws IOException {
		if ( server != null ) {
			return;
		}
		loadRegisterData();
		try {
			server = new NettyTcpModbusServer(bindAddress, port);
			server.setMessageHandler(handler);
			server.setWireLogging(wireLogging);
			server.setPendingMessageTtl(pendingMessageTtl);
			server.start();
			log.info("Started Modbus server [{}]", description());
		} catch ( Exception e ) {
			String msg = String.format("Error starting Modbus server [%s]", description());
			if ( e instanceof IOException ) {
				log.warn("{}: {}", msg, e.getMessage());
				throw (IOException) e;
			} else {
				log.error(msg, e);
			}
			throw new RuntimeException(msg, e);
		}
	}

	/**
	 * Shut down the server.
	 */
	public synchronized void stop() {
		if ( startupFuture != null && !startupFuture.isDone() ) {
			startupFuture.cancel(true);
			startupFuture = null;
		}
		if ( server != null ) {
			server.stop();
			log.info("Stopped Modbus server [{}]", description());
			server = null;
		}
	}

	private synchronized void restartServer() {
		stop();
		Runnable startupTask = new Runnable() {

			@Override
			public void run() {
				synchronized ( ModbusServer.this ) {
					startupFuture = null;
					try {
						start();
					} catch ( Exception e ) {
						stop();
						log.error("Error binding Modbus server [{}] to {}:{}: {}", ModbusServer.this,
								bindAddress, port, e.toString());
						if ( taskScheduler != null ) {
							log.info("Will start Modbus server [{}] in {} seconds", description(),
									startupDelay);
							startupFuture = taskScheduler.schedule(this, Instant
									.ofEpochMilli(System.currentTimeMillis()).plusSeconds(startupDelay));
						}
					}
				}
			}
		};
		if ( taskScheduler != null ) {
			log.info("Will start Modbus server [{}] in {} seconds", description(), startupDelay);
			startupFuture = taskScheduler.schedule(startupTask,
					Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(startupDelay));
		} else {
			startupTask.run();
		}
	}

	private void loadRegisterData() {
		ModbusRegisterDao dao = service(registerDao);
		if ( dao == null ) {
			return;
		}
		String serviceId = getUid();
		if ( serviceId == null || serviceId.isEmpty() ) {
			log.warn(
					"Not loading Modbus server [{}] register data from persistence store because no UID configured",
					description());
			return;
		}

		// clear any existing data to re-load from persistence store
		registers.clear();

		log.info("Loading Modbus server [{}] register data from persistence store", description());
		FilterResults<ModbusRegisterEntity, ModbusRegisterKey> data = dao
				.findFiltered(forServerId(serviceId));
		if ( data == null || data.getReturnedResultCount() < 1 ) {
			return;
		}

		// organize by block by unit for more efficient mass updates below
		Map<Integer, Map<ModbusRegisterBlockType, List<ModbusRegisterEntity>>> dataByBlockByUnit = new HashMap<>(
				2);
		for ( ModbusRegisterEntity reg : data ) {
			dataByBlockByUnit.computeIfAbsent(reg.getUnitId(), k -> new HashMap<>(4))
					.computeIfAbsent(reg.getBlockType(), k -> new ArrayList<>(8)).add(reg);
		}

		for ( Entry<Integer, Map<ModbusRegisterBlockType, List<ModbusRegisterEntity>>> unitEntry : dataByBlockByUnit
				.entrySet() ) {
			ModbusRegisterData unit = registers.computeIfAbsent(unitEntry.getKey(),
					k -> new ModbusRegisterData());
			for ( Entry<ModbusRegisterBlockType, List<ModbusRegisterEntity>> blockEntry : unitEntry
					.getValue().entrySet() ) {
				ModbusRegisterBlockType blockType = blockEntry.getKey();
				if ( blockType.isBitType() ) {
					BitSet set = (blockType == ModbusRegisterBlockType.Coil ? unit.getCoils()
							: unit.getDiscretes());
					synchronized ( set ) {
						for ( ModbusRegisterEntity reg : blockEntry.getValue() ) {
							set.set(reg.getAddress(), reg.getValue() == 0 ? false : true);
						}
					}
				} else {
					ModbusData block = (blockType == ModbusRegisterBlockType.Holding ? unit.getHoldings()
							: unit.getInputs());
					try {
						block.performUpdates((MutableModbusData m) -> {
							for ( ModbusRegisterEntity reg : blockEntry.getValue() ) {
								m.saveDataArray(new short[] { reg.getValue() }, reg.getAddress());
							}
							return true;
						});
					} catch ( IOException e ) {
						log.error("IOException loading Modbus server [{}] {} persistence data: {}",
								description(), blockType, e.toString());
					}
				}
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
		final String serverId = getUid();
		final ModbusRegisterDao dao = (serverId != null && !serverId.isEmpty() ? service(registerDao)
				: null);
		final Instant now = Instant.now();
		for ( MeasurementUpdate update : updates ) {
			Integer unitId = update.unitConfig.getUnitId();
			ModbusRegisterBlockType blockType = update.blockConfig.getBlockType();
			ModbusDataType dataType = update.measConfig.getDataType();
			log.trace("Updating measurement [{}.{}] unit {} {} {} @ {}: {}",
					update.measConfig.getSourceId(), update.measConfig.getPropertyName(), unitId,
					blockType, dataType, update.address, update.propertyValue);
			if ( update.propertyValue == null ) {
				continue;
			}
			ModbusRegisterData regData = registers.computeIfAbsent(unitId,
					k -> new ModbusRegisterData());
			switch (blockType) {
				case Coil:
				case Discrete:
					boolean bitVal = booleanPropertyValue(update.propertyValue);
					regData.writeBit(blockType, update.address, bitVal);
					if ( dao != null ) {
						dao.save(newRegisterEntity(serverId, unitId, blockType, update.address, now,
								bitVal ? (short) 1 : (short) 0));
					}
					break;

				case Holding:
				case Input:
					Object xVal = update.measConfig.applyTransforms(update.propertyValue);
					short[] vals = encodeValue(dataType, update.measConfig.getSize(), xVal);
					if ( blockType == ModbusRegisterBlockType.Holding ) {
						regData.writeHoldings(update.address, vals);
					} else {
						regData.writeInputs(update.address, vals);
					}
					if ( dao != null ) {
						for ( int i = 0, len = vals.length; i < len; i++ ) {
							dao.save(newRegisterEntity(serverId, unitId, blockType, update.address + i,
									now, vals[i]));
						}
					}
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
	public String getPingTestName() {
		return getDisplayName();
	}

	@Override
	public String getPingTestId() {
		String settingUid = getSettingUid();
		Object ident = getUid();
		if ( ident == null ) {
			ident = Integer.toUnsignedString(Objects.hashCode(this), 16);
		}
		return String.format("%s-%s", settingUid, ident);
	}

	@Override
	public Result performPingTest() throws Exception {
		boolean success = true;
		String msg = null;
		if ( daoRequired ) {
			ModbusRegisterDao dao = service(registerDao);
			if ( dao == null ) {
				success = false;
				msg = getMessageSource().getMessage("status.registerDaoMissing", null,
						"ModbusRegisterDao missing.", Locale.getDefault());
			}
		}

		return new PingTestResult(success, msg, Collections.emptyMap());
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000L;
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTitleSettingSpecifier("info",
				registerBlocksInfo(getMessageSource(), Locale.getDefault()), true, true));

		result.addAll(baseIdentifiableSettings(null));
		result.add(new BasicTextFieldSettingSpecifier("bindAddress", DEFAULT_BIND_ADDRESS));
		result.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(DEFAULT_PORT)));
		result.add(new BasicTextFieldSettingSpecifier("requestThrottle",
				String.valueOf(ModbusConnectionHandler.DEFAULT_REQUEST_THROTTLE)));
		result.add(new BasicToggleSettingSpecifier("allowWrites", false));
		result.add(new BasicToggleSettingSpecifier("daoRequired", false));

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

	private String registerBlocksInfo(MessageSource messageSource, Locale locale) {
		StringBuilder buf = new StringBuilder();
		for ( Map.Entry<Integer, ModbusRegisterData> unitEntry : registers.entrySet() ) {
			buf.append(messageSource.getMessage("serverUnitInfo.title",
					new Object[] { unitEntry.getKey() }, locale));

			BitSet bits = unitEntry.getValue().getCoils();
			if ( bits != null && !bits.isEmpty() ) {
				bitsBlockInfo(messageSource, locale, buf,
						messageSource.getMessage("serverUnitInfo.coil.label", null, locale), bits);
			}
			bits = unitEntry.getValue().getDiscretes();
			if ( bits != null && !bits.isEmpty() ) {
				bitsBlockInfo(messageSource, locale, buf,
						messageSource.getMessage("serverUnitInfo.discrete.label", null, locale), bits);
			}

			ModbusData data = unitEntry.getValue().getHoldings();
			if ( data != null && !data.isEmpty() ) {
				regsBlockInfo(messageSource, locale, buf,
						messageSource.getMessage("serverUnitInfo.holding.label", null, locale), data);
			}

			data = unitEntry.getValue().getInputs();
			if ( data != null && !data.isEmpty() ) {
				regsBlockInfo(messageSource, locale, buf,
						messageSource.getMessage("serverUnitInfo.input.label", null, locale), data);
			}
		}
		return buf.toString();
	}

	private void bitsBlockInfo(MessageSource messageSource, Locale locale, StringBuilder buf,
			String title, BitSet bits) {
		buf.append(messageSource.getMessage("serverUnitInfoBitBlock.start", new Object[] { title },
				locale));
		bits.stream().forEachOrdered(a -> {
			buf.append(messageSource.getMessage("serverUnitInfoBit.row", new Object[] { a }, locale));
		});
		buf.append(messageSource.getMessage("serverUnitInfoBitBlock.end", null, locale));
	}

	private void regsBlockInfo(MessageSource messageSource, Locale locale, StringBuilder buf,
			String title, ModbusData data) {
		buf.append(messageSource.getMessage("serverUnitInfoIntBlock.start", new Object[] { title },
				locale));
		buf.append(messageSource.getMessage("serverUnitInfoInt.start", null, locale));
		IntShortMap regs = data.dataRegisters();
		regs.forEachOrdered((a, v) -> {
			buf.append(messageSource.getMessage("serverUnitInfoInt.row",
					new Object[] { a, "0x" + Integer.toHexString(a), String.format("0x%04X", v) },
					locale));
		});
		buf.append(messageSource.getMessage("serverUnitInfoInt.end", null, locale));
		buf.append(messageSource.getMessage("serverUnitInfoIntBlock.end", null, locale));
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
	 * @deprecated only returns 0 now
	 */
	@Deprecated
	public int getBacklog() {
		return 0;
	}

	/**
	 * Set the server socket backlog setting.
	 *
	 * @param backlog
	 *        the backlog to set
	 * @deprecated does not do anything anymore
	 */
	@Deprecated
	public void setBacklog(int backlog) {
		// nothing
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
		return handler.getRequestThrottle();
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
		handler.setRequestThrottle(requestThrottle);
	}

	/**
	 * Get the toggle value to allow Modbus writes.
	 *
	 * @return {@literal true} to allow Modbus clients to write to holding/coil
	 *         registers
	 * @since 2.4
	 */
	public boolean isAllowWrites() {
		return handler.isAllowWrites();
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
		handler.setAllowWrites(allowWrites);
	}

	/**
	 * Get the "wire logging" setting.
	 *
	 * @return {@literal true} to enable wire-level logging of all messages
	 * @since 4.0
	 */
	public boolean isWireLogging() {
		return wireLogging;
	}

	/**
	 * Set the "wire logging" setting.
	 *
	 * @param wireLogging
	 *        {@literal true} to enable wire-level logging of all messages
	 * @since 4.0
	 */
	public void setWireLogging(boolean wireLogging) {
		this.wireLogging = wireLogging;
	}

	/**
	 * Get the pending Modbus message time-to-live expiration time.
	 *
	 * @return the pendingMessageTtl the pending Modbus message time-to-live, in
	 *         milliseconds; defaults to {@link #DEFAULT_PENDING_MESSAGE_TTL}
	 * @since 4.0
	 */
	public long getPendingMessageTtl() {
		return pendingMessageTtl;
	}

	/**
	 * Set the pending Modbus message time-to-live expiration time.
	 *
	 * <p>
	 * This timeout represents the minimum amount of time the client will wait
	 * for a Modbus message response, before it qualifies for removal from the
	 * pending message queue.
	 * </p>
	 *
	 * @param pendingMessageTtl
	 *        the pending Modbus message time-to-live, in milliseconds
	 * @since 4.0
	 */
	public void setPendingMessageTtl(long pendingMessageTtl) {
		this.pendingMessageTtl = pendingMessageTtl;
	}

	/**
	 * Get the register DAO.
	 *
	 * @return the DAO
	 * @since 3.1
	 */
	public final OptionalService<ModbusRegisterDao> getRegisterDao() {
		return registerDao;
	}

	/**
	 * Set the register DAO.
	 *
	 * @param registerDao
	 *        the DAO to set
	 * @since 3.1
	 */
	public final void setRegisterDao(OptionalService<ModbusRegisterDao> registerDao) {
		this.registerDao = registerDao;
	}

	/**
	 * Get the "register DAO required" mode.
	 *
	 * @return {@literal true} to treat the lack of a {@link #getRegisterDao()}
	 *         service as an error state
	 * @since 3.1
	 */
	public final boolean isDaoRequired() {
		return daoRequired;
	}

	/**
	 * Set the "register DAO required" mode.
	 *
	 * @param daoRequired
	 *        {@literal true} to treat the lack of a {@link #getRegisterDao()}
	 *        service as an error state
	 * @since 3.1
	 */
	public final void setDaoRequired(boolean daoRequired) {
		this.daoRequired = daoRequired;
	}

}
