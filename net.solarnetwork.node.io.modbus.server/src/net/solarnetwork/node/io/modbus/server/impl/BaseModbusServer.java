/* ==================================================================
 * BaseModbusServer.java - 12/01/2026 6:37:43 pm
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.io.modbus.ModbusMessage;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
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
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalServiceNotAvailableException;
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
 * Base server class for RTU and TCP Modbus server implementations.
 *
 * @param <T>
 *        the server type
 * @author matt
 * @version 1.2
 * @since 5.3
 */
public abstract class BaseModbusServer<T> extends BaseIdentifiable
		implements SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver,
		EventHandler, PingTest, NodeControlProvider, InstructionHandler {

	/** The default startup delay, in seconds. */
	public static final int DEFAULT_STARTUP_DELAY_SECS = 15;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** An executor. */
	protected final Executor executor;

	/** The register data. */
	protected final ConcurrentMap<Integer, ModbusRegisterData> registers;

	/** The server handler. */
	protected final ModbusConnectionHandler handler;

	private int startupDelay = DEFAULT_STARTUP_DELAY_SECS;
	private TaskScheduler taskScheduler;
	private UnitConfig[] unitConfigs;
	private boolean wireLogging;
	private OptionalService<EventAdmin> eventAdmin;
	private OptionalService<ModbusRegisterDao> registerDao;
	private OptionalService<OperationalModesService> opModesService;
	private String requiredOperationalMode;
	private boolean daoRequired;

	private T server;
	private ScheduledFuture<?> startupFuture;

	/**
	 * Constructor.
	 *
	 * @param executor
	 *        the executor to handle client connections with
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseModbusServer(Executor executor) {
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
	public BaseModbusServer(Executor executor, ConcurrentMap<Integer, ModbusRegisterData> registers) {
		super();
		this.executor = ObjectUtils.requireNonNullArgument(executor, "executor");
		this.registers = ObjectUtils.requireNonNullArgument(registers, "registers");
		this.handler = new ModbusConnectionHandler(registers, this::description, this::handleException,
				this::getUid, () -> service(registerDao));
	}

	/**
	 * Get a brief description of this server.
	 *
	 * <p>
	 * This will show up in log messages, so might include things like TCP port
	 * number or serial port name.
	 * </p>
	 *
	 * @return the description
	 */
	protected abstract String description();

	@Override
	public final void configurationChanged(Map<String, Object> properties) {
		if ( server != null ) {
			log.info("Restarting Modbus server [{}] from configuration change", description());
		}
		restartServer();
	}

	@Override
	public final void serviceDidStartup() {
		restartServer();

	}

	@Override
	public final void serviceDidShutdown() {
		stop();
	}

	/**
	 * Start the server.
	 *
	 * <p>
	 * Calls the {@link #startServer()} method to start the server instance.
	 * </p>
	 *
	 * @throws IOException
	 *         if an IO error occurs
	 */
	public final synchronized void start() throws IOException {
		if ( server != null ) {
			return;
		}
		loadRegisterData();

		// if restricting unit IDs, ensure register maps contains values for exactly the configured units
		if ( isRestrictUnitIds() ) {
			syncUnitIdConfiguration();
		}

		try {
			server = startServer();
			log.info("Started Modbus server [{}]", description());
		} catch ( OptionalServiceNotAvailableException e ) {
			log.info("Modbus server configuration [{}] incomplete, cannot start: {}", description(),
					e.getMessage());
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
	 * Start up the server.
	 *
	 * <p>
	 * This method will be called by the {@link #start()} method.
	 * </p>
	 *
	 * @return the started server instance
	 * @throws IOException
	 *         if an IO error occurs
	 */
	protected abstract T startServer() throws IOException;

	/**
	 * Shut down the server.
	 */
	public final synchronized void stop() {
		if ( startupFuture != null && !startupFuture.isDone() ) {
			startupFuture.cancel(true);
			startupFuture = null;
		}
		if ( server != null ) {
			stopServer(server);
			log.info("Stopped Modbus server [{}]", description());
			server = null;
		}
	}

	/**
	 * Stop the server.
	 *
	 * @param server
	 *        the server to stop
	 */
	protected abstract void stopServer(T server);

	/**
	 * Handle an exception from the connection handler.
	 *
	 * <p>
	 * This implementation simply logs a message. Extending classes may want to
	 * override this method.
	 * </p>
	 *
	 * @param t
	 *        the exception
	 * @param msg
	 *        an optional associated message
	 */
	protected void handleException(Throwable t, Optional<ModbusMessage> msg) {
		if ( msg.isPresent() ) {
			log.warn("Exception processing Modbus message [{}] in Modbus server [{}]: {}", msg.get(),
					description(), t.getMessage());
		} else {
			log.warn("Exception in Modbus server [{}]: {}", description(), t.getMessage());
		}
	}

	/**
	 * Restart the server.
	 */
	protected synchronized void restartServer() {
		stop();
		Runnable startupTask = new Runnable() {

			@Override
			public void run() {
				synchronized ( BaseModbusServer.this ) {
					startupFuture = null;
					try {
						start();
					} catch ( Exception e ) {
						stop();
						log.error("Error starting Modbus server [{}]: {}", description(), e.toString());
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
			if ( daoRequired ) {
				String msg = getMessageSource().getMessage("status.registerDaoMissing", null,
						"ModbusRegisterDao missing.", Locale.getDefault());
				throw new IllegalStateException(msg);
			}

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
					k -> handler.createRegisterData());
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

	private synchronized void syncUnitIdConfiguration() {
		final UnitConfig[] units = getUnitConfigs();
		if ( units == null || units.length < 0 ) {
			registers.clear();
			return;
		}
		Set<Integer> configuredUnitIds = new HashSet<>(units.length);
		for ( UnitConfig unit : units ) {
			Integer unitId = unit.getUnitId();
			registers.computeIfAbsent(unitId, id -> handler.createRegisterData());
			configuredUnitIds.add(unitId);
		}
		for ( Iterator<Integer> itr = registers.keySet().iterator(); itr.hasNext(); ) {
			Integer unitId = itr.next();
			if ( !configuredUnitIds.contains(unitId) ) {
				log.info(
						"Dropping data for unit ID {} because restricted unit IDs mode is enabled and that unit ID is not configured.",
						unitId);
				itr.remove();
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		final String topic = (event != null ? event.getTopic() : null);
		if ( DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED.equals(topic)
				|| NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED.equals(topic)
				|| NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED.equals(topic) ) {
			handleDatumCapturedEvent(event);
		}
	}

	/**
	 * Test if the configured required operational mode is active.
	 *
	 * <p>
	 * If {@link #getRequiredOperationalMode()} is configured but
	 * {@code #getOpModesService()} is not, this method will always return
	 * {@literal false}.
	 * </p>
	 *
	 * @return {@literal true} if an operational mode is required and that mode
	 *         is currently active
	 * @since 1.2
	 */
	private boolean operationalModeMatches() {
		final String mode = getRequiredOperationalMode();
		if ( mode == null ) {
			// no mode required, so automatically matches
			return true;
		}
		final OperationalModesService service = service(opModesService);
		if ( service == null ) {
			// service not available, so automatically does not match
			return false;
		}
		return service.isOperationalModeActive(mode);
	}

	private void handleDatumCapturedEvent(Event event) {
		Object d = event.getProperty(DatumEvents.DATUM_PROPERTY);
		if ( !(d instanceof NodeDatum datum && datum.getSourceId() != null) ) {
			return;
		}
		UnitConfig[] unitConfigs = getUnitConfigs();
		if ( unitConfigs == null || unitConfigs.length < 1 ) {
			return;
		}

		if ( !operationalModeMatches() ) {
			log.trace(
					"Modbus server [{}] required operational mode [{}] not active; ignoring datum update [{}]",
					description(), datum);
			return;
		}

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
					applyMeasurementUpdates(finalUpdates);
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

		private MeasurementUpdate(UnitConfig unitConfig, RegisterBlockConfig blockConfig,
				MeasurementConfig measConfig, Object propertyValue, int address) {
			super();
			this.unitConfig = unitConfig;
			this.blockConfig = blockConfig;
			this.measConfig = measConfig;
			this.propertyValue = propertyValue;
			this.address = address;
		}

		private MeasurementUpdate withPropertyValue(Object value) {
			return new MeasurementUpdate(unitConfig, blockConfig, measConfig, value, address);
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

	private void applyMeasurementUpdates(final Collection<MeasurementUpdate> updates) {
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
					k -> handler.createRegisterData());
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
	public final List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTitleSettingSpecifier("info",
				registerBlocksInfo(getMessageSource(), Locale.getDefault()), true, true));

		result.addAll(baseIdentifiableSettings(null));
		result.addAll(getExtendedSettingSpecifiers());
		result.add(new BasicTextFieldSettingSpecifier("requiredOperationalMode", null));
		result.add(new BasicTextFieldSettingSpecifier("requestThrottle",
				String.valueOf(ModbusConnectionHandler.DEFAULT_REQUEST_THROTTLE)));
		result.add(new BasicTextFieldSettingSpecifier("startupDelay",
				String.valueOf(DEFAULT_STARTUP_DELAY_SECS)));
		result.add(new BasicToggleSettingSpecifier("allowWrites", false));
		result.add(new BasicToggleSettingSpecifier("daoRequired", false));
		result.add(new BasicToggleSettingSpecifier("restrictUnitIds", false));
		result.add(new BasicToggleSettingSpecifier("restrictAddresses", false));
		result.add(new BasicToggleSettingSpecifier("wireLogging", false));

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

	/**
	 * Get implementation-specific server settings.
	 *
	 * <p>
	 * This is where settings like TCP port number or serial port name should be
	 * returned.
	 * </p>
	 *
	 * @return the additional server settings, never {@code null}
	 */
	protected List<SettingSpecifier> getExtendedSettingSpecifiers() {
		return Collections.emptyList();
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

	@Override
	public List<String> getAvailableControlIds() {
		Set<String> result = null;
		final UnitConfig[] unitConfigs = getUnitConfigs();
		if ( unitConfigs != null && unitConfigs.length > 0 ) {
			for ( UnitConfig unitConfig : unitConfigs ) {
				final RegisterBlockConfig[] blockConfigs = unitConfig.getRegisterBlockConfigs();
				if ( blockConfigs != null && blockConfigs.length > 0 ) {
					for ( RegisterBlockConfig blockConfig : blockConfigs ) {
						final MeasurementConfig[] measConfigs = blockConfig.getMeasurementConfigs();
						if ( measConfigs != null && measConfigs.length > 0 ) {
							for ( MeasurementConfig config : measConfigs ) {
								final String controlId = config.controlId();
								if ( controlId != null ) {
									if ( result == null ) {
										result = new LinkedHashSet<>(8);
									}
									result.add(controlId);
								}
							}
						}
					}
				}
			}
		}
		return (result != null ? new ArrayList<>(result) : Collections.emptyList());
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( !handlesTopic(instruction.getTopic()) ) {
			return null;
		}

		List<MeasurementUpdate> updates = null;

		final UnitConfig[] unitConfigs = getUnitConfigs();
		if ( unitConfigs != null && unitConfigs.length > 0 ) {
			for ( UnitConfig unitConfig : unitConfigs ) {
				final RegisterBlockConfig[] blockConfigs = unitConfig.getRegisterBlockConfigs();
				if ( blockConfigs != null && blockConfigs.length > 0 ) {
					for ( RegisterBlockConfig blockConfig : blockConfigs ) {
						final MeasurementConfig[] measConfigs = blockConfig.getMeasurementConfigs();
						if ( measConfigs != null && measConfigs.length > 0 ) {
							int address = blockConfig.getStartAddress();
							for ( MeasurementConfig measConfig : measConfigs ) {
								final String controlId = measConfig.controlId();
								if ( controlId != null ) {
									for ( String param : instruction.getParameterNames() ) {
										if ( param.equals(controlId) ) {
											MeasurementUpdate update = new MeasurementUpdate(unitConfig,
													blockConfig, measConfig,
													coerceInstructionValue(measConfig,
															instruction.getParameterValue(param)),
													address);
											if ( updates == null ) {
												updates = new ArrayList<>(4);
											}
											updates.add(update);
										}
									}
								}
								address += measConfig.getSize();
							}
						}
					}
				}
			}
		}

		if ( updates != null ) {
			log.trace("Applying instruction [{}] updates: {}", instruction.getIdentifier(),
					updates.stream().map(Object::toString)
							.collect(Collectors.joining(",\n\t", "[\n\t", "\n]")));
			applyMeasurementUpdates(updates);
			for ( MeasurementUpdate update : updates ) {
				postControlEvent(newSimpleNodeControlInfoDatum(update),
						NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
			}
			return InstructionUtils.createStatus(instruction, InstructionState.Completed);
		}

		return null;
	}

	private Object coerceInstructionValue(MeasurementConfig measConfig, String value) {
		return switch (measConfig.getDataType()) {
			case Boolean -> StringUtils.parseBoolean(value);
			case Bytes, StringAscii, StringUtf8 -> value;
			default -> StringUtils.numberValue(value);
		};
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(final String controlId) {
		final UnitConfig[] unitConfigs = getUnitConfigs();
		if ( unitConfigs != null && unitConfigs.length > 0 ) {
			for ( UnitConfig unitConfig : unitConfigs ) {
				final RegisterBlockConfig[] blockConfigs = unitConfig.getRegisterBlockConfigs();
				if ( blockConfigs != null && blockConfigs.length > 0 ) {
					for ( RegisterBlockConfig blockConfig : blockConfigs ) {
						final MeasurementConfig[] measConfigs = blockConfig.getMeasurementConfigs();
						if ( measConfigs != null && measConfigs.length > 0 ) {
							int address = blockConfig.getStartAddress();
							for ( MeasurementConfig measConfig : measConfigs ) {
								final String measControlId = measConfig.controlId();
								if ( measControlId != null && measControlId.equals(controlId) ) {
									// just using MeasurementUpdate here because convenient
									final MeasurementUpdate update = new MeasurementUpdate(unitConfig,
											blockConfig, measConfig, null, address);

									final Object value = update.measConfig
											.applyReverseTransforms(currentRawValue(update));

									final var result = newSimpleNodeControlInfoDatum(
											update.withPropertyValue(value));
									postControlEvent(result,
											NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
									return result;
								}
								address += measConfig.getSize();
							}
						}
					}
				}
			}
		}

		return null;
	}

	private Object currentRawValue(final MeasurementUpdate update) {
		final ModbusRegisterData data = registers.get(update.unitConfig.getUnitId());
		if ( data == null ) {
			return 0;
		}
		final ModbusRegisterBlockType blockType = update.blockConfig.getBlockType();
		switch (blockType) {
			case Coil: {
				BitSet bits = data.readCoils(update.address, 1);
				return bits.get(update.address);
			}

			case Discrete: {
				BitSet bits = data.readDiscretes(update.address, 1);
				return bits.get(update.address);
			}

			case Holding: {
				ModbusData d = data.getHoldings();
				return d.getValue(update.measConfig.getDataType(), update.address,
						update.measConfig.getSize());
			}

			case Input: {
				ModbusData d = data.getInputs();
				return d.getValue(update.measConfig.getDataType(), update.address,
						update.measConfig.getSize());
			}
		}
		return null;
	}

	private SimpleNodeControlInfoDatum newSimpleNodeControlInfoDatum(MeasurementUpdate update) {
		final NodeControlPropertyType controlType = switch (update.measConfig.getDataType()) {
			case Boolean -> NodeControlPropertyType.Boolean;
			case Float16, Float32, Float64 -> NodeControlPropertyType.Float;
			case Bytes, StringAscii, StringUtf8 -> NodeControlPropertyType.String;
			default -> update.measConfig.hasUnitMultiplier() ? NodeControlPropertyType.Float
					: NodeControlPropertyType.Integer;
		};

		// @formatter:off
		final NodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(resolvePlaceholders(update.measConfig.controlId()))
				.withType(controlType)
				.withReadonly(false)
				.withValue(update.propertyValue != null ? update.propertyValue.toString() : null)
				.build();
		// @formatter:on
		return new SimpleNodeControlInfoDatum(info, Instant.now());
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		admin.postEvent(event);
	}

	/**
	 * Get the task scheduler.
	 *
	 * @return the task scheduler
	 */
	public final TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 *
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public final void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the startup delay.
	 *
	 * @return the startup delay, in seconds
	 */
	public final int getStartupDelay() {
		return startupDelay;
	}

	/**
	 * Set the startup delay.
	 *
	 * @param startupDelay
	 *        the delay to set, in seconds
	 */
	public final void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * Get the block configurations.
	 *
	 * @return the block configurations
	 */
	public final UnitConfig[] getUnitConfigs() {
		return unitConfigs;
	}

	/**
	 * Set the block configurations to use.
	 *
	 * @param unitConfigs
	 *        the configurations to use
	 */
	public final void setUnitConfigs(UnitConfig[] unitConfigs) {
		this.unitConfigs = unitConfigs;
	}

	/**
	 * Get the number of configured {@code unitConfigs} elements.
	 *
	 * @return the number of {@code unitConfigs} elements
	 */
	public final int getUnitConfigsCount() {
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
	public final void setUnitConfigsCount(int count) {
		this.unitConfigs = ArrayUtils.arrayWithLength(this.unitConfigs, count, UnitConfig.class, null);
	}

	/**
	 * Get the request throttle.
	 *
	 * @return the minimum time in milliseconds allowed between client requests,
	 *         or {@code 0} for no minimum
	 */
	public final long getRequestThrottle() {
		return handler.getRequestThrottle();
	}

	/**
	 * Set the request throttle.
	 *
	 * @param requestThrottle
	 *        the minimum time in milliseconds allowed between client requests,
	 *        or {@code 0} to disable
	 */
	public final void setRequestThrottle(long requestThrottle) {
		handler.setRequestThrottle(requestThrottle);
	}

	/**
	 * Get the toggle value to allow Modbus writes.
	 *
	 * @return {@literal true} to allow Modbus clients to write to holding/coil
	 *         registers
	 */
	public final boolean isAllowWrites() {
		return handler.isAllowWrites();
	}

	/**
	 * Toggle allowing Modbus writes.
	 *
	 * @param allowWrites
	 *        {@literal true} to allow Modbus clients to write to holding/coil
	 *        registers
	 */
	public final void setAllowWrites(boolean allowWrites) {
		handler.setAllowWrites(allowWrites);
	}

	/**
	 * Get the "wire logging" setting.
	 *
	 * @return {@literal true} to enable wire-level logging of all messages
	 */
	public final boolean isWireLogging() {
		return wireLogging;
	}

	/**
	 * Set the "wire logging" setting.
	 *
	 * @param wireLogging
	 *        {@literal true} to enable wire-level logging of all messages
	 */
	public final void setWireLogging(boolean wireLogging) {
		this.wireLogging = wireLogging;
	}

	/**
	 * Get the event admin service.
	 *
	 * @return the event admin
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set the event admin sevice.
	 *
	 * @param eventAdmin
	 *        the service to set
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Get the register DAO.
	 *
	 * @return the DAO
	 */
	public final OptionalService<ModbusRegisterDao> getRegisterDao() {
		return registerDao;
	}

	/**
	 * Set the register DAO.
	 *
	 * @param registerDao
	 *        the DAO to set
	 */
	public final void setRegisterDao(OptionalService<ModbusRegisterDao> registerDao) {
		this.registerDao = registerDao;
	}

	/**
	 * Get the "register DAO required" mode.
	 *
	 * @return {@literal true} to treat the lack of a {@link #getRegisterDao()}
	 *         service as an error state
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
	 */
	public final void setDaoRequired(boolean daoRequired) {
		this.daoRequired = daoRequired;
	}

	/**
	 * Get the restrict-unit-ids mode.
	 *
	 * @return {@code true} to deny requests for unit IDs that do not exist in
	 *         the registers map
	 * @since 1.1
	 */
	public boolean isRestrictUnitIds() {
		return handler.isRestrictUnitIds();
	}

	/**
	 * Set the restrict-unit-ids mode.
	 *
	 * @param restrictUnitIds
	 *        {@code true} to deny requests for unit IDs that do not exist in
	 *        the registers map
	 * @since 1.1
	 */
	public void setRestrictUnitIds(boolean restrictUnitIds) {
		handler.setRestrictUnitIds(restrictUnitIds);
	}

	/**
	 * Get the restrict-addresses mode.
	 *
	 * @return {@code} true to deny requests for data addresses that do not
	 *         already exist in the register data
	 * @since 1.1
	 */
	public boolean isRestrictAddresses() {
		return handler.isRestrictAddresses();
	}

	/**
	 * Set the restrict-addresses mode.
	 *
	 * <p>
	 * <b>Note</b> this mode only affects unit IDs that are created dynamically
	 * by this class, when {@code restrictUnitIds} is {@code false}.
	 * </p>
	 *
	 * @param restrictAddresses
	 *        {@code} true to deny requests for data addresses that do not
	 *        already exist in the register data
	 * @since 1.1
	 */
	public void setRestrictAddresses(boolean restrictAddresses) {
		handler.setRestrictAddresses(restrictAddresses);
	}

	/**
	 * Get the operational modes service to use.
	 *
	 * @return the service, or {@literal null}
	 * @since 1.2
	 */
	public OptionalService<OperationalModesService> getOpModesService() {
		return opModesService;
	}

	/**
	 * Set the operational modes service to use.
	 *
	 * @param opModesService
	 *        the service to use
	 * @since 1.2
	 */
	public void setOpModesService(OptionalService<OperationalModesService> opModesService) {
		this.opModesService = opModesService;
	}

	/**
	 * Get an operational mode that is required by this service.
	 *
	 * @return the required operational mode, or {@literal null} for none
	 * @since 1.2
	 */
	public String getRequiredOperationalMode() {
		return requiredOperationalMode;
	}

	/**
	 * Set an operational mode that is required by this service.
	 *
	 * @param requiredOperationalMode
	 *        the required operational mode, or {@literal null} or an empty
	 *        string that will be treated as {@literal null}
	 * @since 1.2
	 */
	public void setRequiredOperationalMode(String requiredOperationalMode) {
		if ( requiredOperationalMode != null && requiredOperationalMode.trim().isEmpty() ) {
			requiredOperationalMode = null;
		}
		this.requiredOperationalMode = requiredOperationalMode;
	}

}
