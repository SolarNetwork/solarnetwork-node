/* ==================================================================
 * OutstationService.java - 21/02/2019 11:05:32 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.dnp3.impl;

import static net.solarnetwork.node.reactor.InstructionUtils.createSetControlValueLocalInstruction;
import static net.solarnetwork.service.OptionalService.service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.springframework.core.task.TaskExecutor;
import com.automatak.dnp3.AnalogInput;
import com.automatak.dnp3.AnalogOutputDouble64;
import com.automatak.dnp3.AnalogOutputFloat32;
import com.automatak.dnp3.AnalogOutputInt16;
import com.automatak.dnp3.AnalogOutputInt32;
import com.automatak.dnp3.AnalogOutputStatus;
import com.automatak.dnp3.BinaryInput;
import com.automatak.dnp3.BinaryOutputStatus;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.ControlRelayOutputBlock;
import com.automatak.dnp3.Counter;
import com.automatak.dnp3.DNP3Exception;
import com.automatak.dnp3.DatabaseConfig;
import com.automatak.dnp3.DoubleBitBinaryInput;
import com.automatak.dnp3.EventBufferConfig;
import com.automatak.dnp3.FrozenCounter;
import com.automatak.dnp3.Outstation;
import com.automatak.dnp3.OutstationChangeSet;
import com.automatak.dnp3.OutstationStackConfig;
import com.automatak.dnp3.enums.AnalogOutputStatusQuality;
import com.automatak.dnp3.enums.AnalogQuality;
import com.automatak.dnp3.enums.BinaryOutputStatusQuality;
import com.automatak.dnp3.enums.BinaryQuality;
import com.automatak.dnp3.enums.CommandStatus;
import com.automatak.dnp3.enums.CounterQuality;
import com.automatak.dnp3.enums.DoubleBit;
import com.automatak.dnp3.enums.DoubleBitBinaryQuality;
import com.automatak.dnp3.enums.FrozenCounterQuality;
import com.automatak.dnp3.enums.OperateType;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.node.io.dnp3.ChannelService;
import net.solarnetwork.node.io.dnp3.domain.ControlConfig;
import net.solarnetwork.node.io.dnp3.domain.ControlType;
import net.solarnetwork.node.io.dnp3.domain.LinkLayerConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementType;
import net.solarnetwork.node.io.dnp3.domain.OutstationConfig;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * A DNP3 "outstation" server service that publishes SolarNode datum/control
 * events to DNP3.
 *
 * @author matt
 * @version 2.1
 */
public class OutstationService extends AbstractApplicationService
		implements EventHandler, SettingSpecifierProvider {

	/** The default event buffer size. */
	private static final int DEFAULT_EVENT_BUFFER_SIZE = 30;

	/** The default startup delay. */
	private static final int DEFAULT_STARTUP_DELAY_SECONDS = 5;

	/** The default uid value. */
	public static final String DEFAULT_UID = "DNP3 Outstation";

	private final Application app;
	private final CommandHandler commandHandler;
	private final OptionalService<InstructionExecutionService> instructionExecutionService;
	private final OutstationConfig outstationConfig;

	private MeasurementConfig[] measurementConfigs;
	private ControlConfig[] controlConfigs;
	private int eventBufferSize = DEFAULT_EVENT_BUFFER_SIZE;
	private int startupDelaySecs = DEFAULT_STARTUP_DELAY_SECONDS;

	private Outstation outstation;
	private Runnable initTask;

	/**
	 * Constructor.
	 *
	 * @param dnp3Channel
	 *        the channel to use
	 * @param instructionExecutionService
	 *        the execution service to handle control operations
	 */
	public OutstationService(OptionalService<ChannelService> dnp3Channel,
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		super(dnp3Channel);
		this.app = new Application();
		this.outstationConfig = new OutstationConfig();
		this.commandHandler = new CommandHandler();
		this.instructionExecutionService = instructionExecutionService;
	}

	@Override
	public synchronized void startup() {
		super.startup();
	}

	/**
	 * Callback after properties have been changed.
	 *
	 * @param properties
	 *        the changed properties
	 */
	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		super.configurationChanged(properties);
		if ( initTask != null ) {
			// init task already underway
			return;
		}
		TaskExecutor executor = getTaskExecutor();
		if ( executor != null ) {
			initTask = new Runnable() {

				@Override
				public void run() {
					try {
						log.info("Waiting {}s to start DNP3 outstation [{}]", getStartupDelaySecs(),
								getUid());
						Thread.sleep(getStartupDelaySecs() * 1000L);
					} catch ( InterruptedException e ) {
						// ignore
					} finally {
						synchronized ( OutstationService.this ) {
							initTask = null;
							getOutstation();
						}
					}
				}
			};
			executor.execute(initTask);
		} else {
			// no executor; init immediately
			getOutstation();
		}
	}

	@Override
	public synchronized void shutdown() {
		super.shutdown();
		if ( outstation != null ) {
			log.info("Shutting down DNP3 outstation [{}]", getUid());
			outstation.shutdown();
			this.outstation = null;
			log.info("DNP3 outstation [{}] shutdown", getUid());
		}
	}

	private synchronized Outstation getOutstation() {
		if ( outstation != null || initTask != null ) {
			return outstation;
		}
		outstation = createOutstation();
		if ( outstation != null ) {
			outstation.enable();
			log.info("DNP3 outstation [{}] enabled", getUid());
		}
		return outstation;
	}

	private Outstation createOutstation() {
		Channel channel = channel();
		if ( channel == null ) {
			log.info("DNP3 channel not available for outstation [{}]", getUid());
			return null;
		}
		log.info("Initializing DNP3 outstation [{}]", getUid());
		try {
			return channel.addOutstation(getUid(), commandHandler, app, createOutstationStackConfig());
		} catch ( DNP3Exception e ) {
			log.error("Error creating outstation application [{}]: {}", getUid(), e.getMessage(), e);
			return null;
		}
	}

	private OutstationStackConfig createOutstationStackConfig() {
		Map<MeasurementType, List<MeasurementConfig>> configs = measurementTypeMap(
				getMeasurementConfigs());
		Map<ControlType, List<ControlConfig>> controlConfigs = controlTypeMap(getControlConfigs());
		OutstationStackConfig config = new OutstationStackConfig(
				createDatabaseConfig(configs, controlConfigs),
				createEventBufferConfig(configs, controlConfigs));
		copySettings(getLinkLayerConfig(), config.linkConfig);
		copySettings(getOutstationConfig(), config.outstationConfig);
		return config;
	}

	private Map<MeasurementType, List<MeasurementConfig>> measurementTypeMap(
			MeasurementConfig[] configs) {
		Map<MeasurementType, List<MeasurementConfig>> map = new LinkedHashMap<>(
				configs != null ? configs.length : 0);
		if ( configs != null ) {
			for ( MeasurementConfig config : configs ) {
				MeasurementType type = config.getType();
				if ( type != null && config.getPropertyName() != null
						&& !config.getPropertyName().isEmpty() ) {
					map.computeIfAbsent(type, k -> new ArrayList<>(4)).add(config);
				}
			}
		}
		return map;
	}

	private Map<ControlType, List<ControlConfig>> controlTypeMap(ControlConfig[] configs) {
		Map<ControlType, List<ControlConfig>> map = new LinkedHashMap<>(
				configs != null ? configs.length : 0);
		if ( configs != null ) {
			for ( ControlConfig config : configs ) {
				ControlType type = config.getType();
				if ( type != null && config.getControlId() != null
						&& !config.getControlId().isEmpty() ) {
					map.computeIfAbsent(type, k -> new ArrayList<>(4)).add(config);
				}
			}
		}
		return map;
	}

	private void appendMeasurementInfos(StringBuilder buf, MeasurementType type,
			List<MeasurementConfig> list) {
		buf.append(type.getTitle()).append(" (").append(list != null ? list.size() : 0).append(")\n");
		if ( list != null ) {
			int i = 0;
			for ( MeasurementConfig conf : list ) {
				buf.append(String.format("  %3d: %s\n", i, conf.getSourceId()));
				i++;
			}
		}
	}

	private void appendControlInfos(StringBuilder buf, ControlType type, List<ControlConfig> list,
			int offset) {
		buf.append(type.getTitle()).append(" output status (").append(list != null ? list.size() : 0)
				.append(")\n");
		if ( list != null ) {
			int i = 0;
			for ( ControlConfig conf : list ) {
				buf.append(String.format("  %3d: %s\n", i + offset, conf.getControlId()));
				i++;
			}
		}
	}

	private DatabaseConfig createDatabaseConfig(Map<MeasurementType, List<MeasurementConfig>> configs,
			Map<ControlType, List<ControlConfig>> controlConfigs) {
		int analogCount = 0;
		int aoStatusCount = 0;
		int binaryCount = 0;
		int boStatusCount = 0;
		int counterCount = 0;
		int doubleBinaryCount = 0;
		int frozenCounterCount = 0;
		StringBuilder infoBuf = new StringBuilder();
		if ( configs != null ) {
			for ( Map.Entry<MeasurementType, List<MeasurementConfig>> me : configs.entrySet() ) {
				MeasurementType type = me.getKey();
				List<MeasurementConfig> list = me.getValue();
				if ( type == null || list == null || list.isEmpty() ) {
					continue;
				}
				switch (type) {
					case AnalogInput:
						analogCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;

					case AnalogOutputStatus:
						aoStatusCount = list.size();
						break;

					case BinaryInput:
						binaryCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;

					case BinaryOutputStatus:
						boStatusCount = list.size();
						break;

					case Counter:
						counterCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;

					case DoubleBitBinaryInput:
						doubleBinaryCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;

					case FrozenCounter:
						frozenCounterCount = list.size();
						appendMeasurementInfos(infoBuf, type, list);
						break;
				}
			}
		}
		if ( controlConfigs != null ) {
			for ( Map.Entry<ControlType, List<ControlConfig>> me : controlConfigs.entrySet() ) {
				ControlType type = me.getKey();
				List<ControlConfig> list = me.getValue();
				if ( type == null || list == null || list.isEmpty() ) {
					continue;
				}
				switch (type) {
					case Analog:
						appendControlInfos(infoBuf, type, list, aoStatusCount);
						aoStatusCount += list.size();
						break;

					case Binary:
						appendControlInfos(infoBuf, type, list, boStatusCount);
						boStatusCount += list.size();
						break;

				}
			}
		}
		log.info("DNP3 outstation [{}] database configured with following registers:\n{}", getUid(),
				infoBuf);
		return new DatabaseConfig(binaryCount, doubleBinaryCount, analogCount, counterCount,
				frozenCounterCount, boStatusCount, aoStatusCount);
	}

	private EventBufferConfig createEventBufferConfig(
			Map<MeasurementType, List<MeasurementConfig>> configs,
			Map<ControlType, List<ControlConfig>> controlConfigs) {
		EventBufferConfig config = EventBufferConfig.allTypes(0);
		final int size = getEventBufferSize();
		if ( configs != null ) {
			for ( Map.Entry<MeasurementType, List<MeasurementConfig>> me : configs.entrySet() ) {
				MeasurementType type = me.getKey();
				List<MeasurementConfig> list = me.getValue();
				if ( type == null || list == null || list.isEmpty() ) {
					continue;
				}
				switch (type) {
					case AnalogInput:
						config.maxAnalogEvents = size;
						break;

					case AnalogOutputStatus:
						config.maxAnalogOutputStatusEvents = size;
						break;

					case BinaryInput:
						config.maxBinaryEvents = size;
						break;

					case BinaryOutputStatus:
						config.maxBinaryOutputStatusEvents = size;
						break;

					case Counter:
						config.maxCounterEvents = size;
						break;

					case DoubleBitBinaryInput:
						config.maxDoubleBinaryEvents = size;
						break;

					case FrozenCounter:
						config.maxFrozenCounterEvents = size;
						break;
				}
			}
		}
		if ( controlConfigs != null ) {
			for ( Map.Entry<ControlType, List<ControlConfig>> me : controlConfigs.entrySet() ) {
				ControlType type = me.getKey();
				List<ControlConfig> list = me.getValue();
				if ( type == null || list == null || list.isEmpty() ) {
					continue;
				}
				switch (type) {
					case Analog:
						config.maxAnalogOutputStatusEvents = size;
						break;

					case Binary:
						config.maxBinaryOutputStatusEvents = size;
						break;

				}
			}
		}
		return config;
	}

	/*
	 * =========================================================================
	 * EventHandler implementation
	 * =========================================================================
	 */

	@Override
	public void handleEvent(Event event) {
		String topic = (event != null ? event.getTopic() : null);
		if ( DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED.equals(topic) ) {
			handleDatumCapturedEvent(event);
		} else if ( NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED.equals(topic)
				|| NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED.equals(topic) ) {
			handleControlInfoCapturedEvent(event);
		}
	}

	private void handleDatumCapturedEvent(Event event) {
		final Object datum = event.getProperty(DatumEvents.DATUM_PROPERTY);
		if ( !(datum instanceof Datum && ((Datum) datum).getSourceId() != null) ) {
			return;
		}

		TaskExecutor executor = getTaskExecutor();
		if ( executor != null ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					applyDatumCapturedUpdates((Datum) datum, event);
				}
			});
		} else {
			applyDatumCapturedUpdates((Datum) datum, event);
		}
	}

	private void handleControlInfoCapturedEvent(Event event) {
		final Object datum = event.getProperty(DatumEvents.DATUM_PROPERTY);
		if ( !(datum instanceof Datum && ((Datum) datum).getSourceId() != null) ) {
			return;
		}

		TaskExecutor executor = getTaskExecutor();
		if ( executor != null ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					applyDatumCapturedUpdates((Datum) datum, event);
				}
			});
		} else {
			applyDatumCapturedUpdates((Datum) datum, event);
		}
	}

	private void applyDatumCapturedUpdates(Datum datum, Event event) {
		OutstationChangeSet changes = changeSetForDatumCapturedEvent(datum, event);
		if ( changes == null ) {
			return;
		}
		synchronized ( this ) {
			Outstation station = getOutstation();
			if ( station != null ) {
				log.info("Applying changes to DNP3 [{}]", getUid());
				station.apply(changes);
			}
		}
	}

	private OutstationChangeSet changeSetForDatumCapturedEvent(final Datum datum, final Event event) {
		Map<MeasurementType, List<MeasurementConfig>> map = measurementTypeMap(getMeasurementConfigs());
		Map<ControlType, List<ControlConfig>> controlMap = controlTypeMap(getControlConfigs());
		if ( datum == null
				|| ((map == null || map.isEmpty()) && (controlMap == null || controlMap.isEmpty())) ) {
			return null;
		}
		final String sourceId = datum.getSourceId();
		final Instant timestamp = datum.getTimestamp();
		if ( timestamp == null ) {
			return null;
		}
		final long ts = timestamp.toEpochMilli();
		final Map<String, ?> datumProps = datum.getSampleData();
		OutstationChangeSet changes = null;
		if ( map != null ) {
			for ( Map.Entry<MeasurementType, List<MeasurementConfig>> me : map.entrySet() ) {
				MeasurementType type = me.getKey();
				List<MeasurementConfig> list = me.getValue();
				for ( ListIterator<MeasurementConfig> itr = list.listIterator(); itr.hasNext(); ) {
					MeasurementConfig config = itr.next();
					if ( sourceId.equals(config.getSourceId()) ) {
						Object propVal = datumProps.get(config.getPropertyName());
						if ( propVal != null ) {
							if ( propVal instanceof Number ) {
								if ( config.getUnitMultiplier() != null ) {
									propVal = applyUnitMultiplier((Number) propVal,
											config.getUnitMultiplier());
								}
								if ( config.getDecimalScale() >= 0 ) {
									propVal = applyDecimalScale((Number) propVal,
											config.getDecimalScale());
								}
							}
							if ( changes == null ) {
								changes = new OutstationChangeSet();
							}
							log.debug("Updating DNP3 {}[{}] from [{}].{} -> {}", type,
									itr.previousIndex(), sourceId, config.getPropertyName(), propVal);
							switch (type) {
								case AnalogInput:
									if ( propVal instanceof Number ) {
										changes.update(
												new AnalogInput(((Number) propVal).doubleValue(),
														(byte) AnalogQuality.ONLINE.toType(), ts),
												itr.previousIndex());
									}
									break;

								case AnalogOutputStatus:
									if ( propVal instanceof Number ) {
										changes.update(
												new AnalogOutputStatus(((Number) propVal).doubleValue(),
														(byte) AnalogOutputStatusQuality.ONLINE.toType(),
														ts),
												itr.previousIndex());
									}
									break;

								case BinaryInput:
									changes.update(
											new BinaryInput(booleanPropertyValue(propVal),
													(byte) BinaryQuality.ONLINE.toType(), ts),
											itr.previousIndex());
									break;

								case BinaryOutputStatus:
									changes.update(new BinaryOutputStatus(booleanPropertyValue(propVal),
											(byte) BinaryOutputStatusQuality.ONLINE.toType(), ts),
											itr.previousIndex());
									break;

								case Counter:
									if ( propVal instanceof Number ) {
										changes.update(
												new Counter(((Number) propVal).longValue(),
														(byte) CounterQuality.ONLINE.toType(), ts),
												itr.previousIndex());
									}
									break;

								case DoubleBitBinaryInput:
									changes.update(new DoubleBitBinaryInput(
											booleanPropertyValue(propVal) ? DoubleBit.DETERMINED_ON
													: DoubleBit.DETERMINED_OFF,
											(byte) DoubleBitBinaryQuality.ONLINE.toType(), ts),
											itr.previousIndex());
									break;

								case FrozenCounter:
									if ( propVal instanceof Number ) {
										changes.update(
												new FrozenCounter(((Number) propVal).longValue(),
														(byte) FrozenCounterQuality.ONLINE.toType(), ts),
												itr.previousIndex());
									}
									break;
							}
						}
					}
				}
			}
			if ( controlMap != null ) {
				int analogStatusOffset = typeConfigCount(MeasurementType.AnalogOutputStatus, map);
				int binaryStatusOffset = typeConfigCount(MeasurementType.BinaryOutputStatus, map);
				for ( Map.Entry<ControlType, List<ControlConfig>> me : controlMap.entrySet() ) {
					ControlType type = me.getKey();
					List<ControlConfig> list = me.getValue();
					for ( ListIterator<ControlConfig> itr = list.listIterator(); itr.hasNext(); ) {
						ControlConfig config = itr.next();
						if ( sourceId.equals(config.getControlId()) ) {
							if ( changes == null ) {
								changes = new OutstationChangeSet();
							}

							int index = (type == ControlType.Analog ? analogStatusOffset
									: binaryStatusOffset) + itr.previousIndex();

							Object propVal = datumProps.get("value");
							log.debug("Updating DNP3 control {}[{}] from [{}].value -> {}", type, index,
									sourceId, propVal);
							switch (type) {
								case Analog:
									try {
										Number n = null;
										if ( propVal instanceof Number ) {
											n = (Number) propVal;
										} else {
											n = new BigDecimal(propVal.toString());
										}
										changes.update(new AnalogOutputStatus(n.doubleValue(),
												(byte) AnalogOutputStatusQuality.ONLINE.toType(), ts),
												index);
									} catch ( NumberFormatException e ) {
										log.warn("Cannot convert control [{}] value [{}] to number: {}",
												sourceId, propVal, e.getMessage());
									}
									break;

								case Binary:
									changes.update(new BinaryOutputStatus(booleanPropertyValue(propVal),
											(byte) BinaryOutputStatusQuality.ONLINE.toType(), ts),
											index);
									break;

							}
						}
					}
				}
			}

		}

		return changes;

	}

	private <T, C> int typeConfigCount(T key, Map<T, List<C>> map) {
		if ( map == null || map.isEmpty() ) {
			return 0;
		}
		List<?> list = map.get(key);
		return (list != null ? list.size() : 0);
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

	private Number applyDecimalScale(Number value, int decimalScale) {
		if ( decimalScale < 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		if ( v.scale() > decimalScale ) {
			v = v.setScale(decimalScale, RoundingMode.HALF_UP);
		}
		return v;
	}

	private Number applyUnitMultiplier(Number value, BigDecimal multiplier) {
		if ( BigDecimal.ONE.compareTo(multiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.multiply(multiplier);
	}

	/*
	 * =========================================================================
	 * OutstationApplication implementation
	 * =========================================================================
	 */

	private class Application extends BaseOutstationApplication {

	}

	/*
	 * =========================================================================
	 * CommandHandler implementation
	 * =========================================================================
	 */

	private class CommandHandler extends BaseCommandHandler {

		private CommandHandler() {
			super(CommandStatus.SUCCESS);
		}

		@Override
		public CommandStatus operateCROB(ControlRelayOutputBlock command, int index,
				OperateType opType) {
			ControlConfig config = controlConfigForIndex(ControlType.Binary, index);
			if ( config == null ) {
				return CommandStatus.NOT_AUTHORIZED;
			}
			log.info("DNP3 outstation [{}] received CROB operation request {} on {}[{}] control [{}]",
					getUid(), command.function, config.getType(), index, config.getControlId());
			TaskExecutor executor = getTaskExecutor();
			if ( executor != null ) {
				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							operateBinaryControl(command, index, opType, config);
						} catch ( Exception e ) {
							log.error(
									"Error processing DNP3 outstation [{}] operate request {} on {}[{}] control [{}]",
									getUid(), command.function, config.getType(), index,
									config.getControlId(), e);
						}
					}
				});
			} else {
				operateBinaryControl(command, index, opType, config);
			}
			return CommandStatus.SUCCESS;
		}

		@Override
		public CommandStatus operateAOI16(AnalogOutputInt16 command, int index, OperateType opType) {
			return handleAnalogOperation(command, index, "AnalogOutputInt16", command.value);
		}

		@Override
		public CommandStatus operateAOI32(AnalogOutputInt32 command, int index, OperateType opType) {
			return handleAnalogOperation(command, index, "AnalogOutputInt32", command.value);
		}

		@Override
		public CommandStatus operateAOF32(AnalogOutputFloat32 command, int index, OperateType opType) {
			return handleAnalogOperation(command, index, "AnalogOutputFloat32", command.value);
		}

		@Override
		public CommandStatus operateAOD64(AnalogOutputDouble64 command, int index, OperateType opType) {
			return handleAnalogOperation(command, index, "AnalogOutputDouble64", command.value);
		}

		private CommandStatus handleAnalogOperation(Object command, int index, String opDescription,
				Number value) {
			ControlConfig config = controlConfigForIndex(ControlType.Analog, index);
			if ( config == null ) {
				return CommandStatus.NOT_AUTHORIZED;
			}
			log.info("DNP3 outstation [{}] received analog operation request {} on {}[{}] control [{}]",
					getUid(), opDescription, config.getType(), index, config.getControlId());
			TaskExecutor executor = getTaskExecutor();
			if ( executor != null ) {
				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							operateAnalogControl(command, index, opDescription, config, value);
						} catch ( Exception e ) {
							log.error(
									"Error processing DNP3 outstation [{}] analog operation request {} on {}[{}] control [{}]",
									getUid(), opDescription, config.getType(), index,
									config.getControlId(), e);
						}
					}
				});
			} else {
				operateAnalogControl(command, index, opDescription, config, value);
			}
			return CommandStatus.SUCCESS;
		}

	}

	private ControlConfig controlConfigForIndex(ControlType controlType, int index) {
		MeasurementType measType = (controlType == ControlType.Analog
				? MeasurementType.AnalogOutputStatus
				: MeasurementType.BinaryOutputStatus);
		int binaryStatusOffset = typeConfigCount(measType, measurementTypeMap(getMeasurementConfigs()));
		int controlConfigIndex = index - binaryStatusOffset;
		ControlConfig[] configs = getControlConfigs();
		if ( configs != null && controlConfigIndex < configs.length ) {
			return configs[controlConfigIndex];
		}
		return null;
	}

	private InstructionStatus operateBinaryControl(ControlRelayOutputBlock command, int index,
			OperateType opType, ControlConfig config) {
		final InstructionExecutionService service = OptionalService.service(instructionExecutionService);
		Instruction instr = null;
		switch (command.function) {
			case LATCH_ON:
				instr = InstructionUtils.createSetControlValueLocalInstruction(config.getControlId(),
						Boolean.TRUE);
				break;

			case LATCH_OFF:
				instr = InstructionUtils.createSetControlValueLocalInstruction(config.getControlId(),
						Boolean.FALSE);
				break;

			default:
				// nothing
		}
		InstructionStatus result = null;
		if ( service != null ) {
			try {
				if ( instr != null ) {
					result = service.executeInstruction(instr);
				} else {
					result = InstructionUtils.createStatus(instr, InstructionState.Declined);
				}
			} finally {
				log.info(
						"DNP3 outstation [{}] CROB operation request {} on {}[{}] control [{}] result: {}",
						getUid(), command.function, config.getType(), index, config.getControlId(),
						result);
			}
		}
		return result;
	}

	private InstructionStatus operateAnalogControl(Object command, int index, String opDescription,
			ControlConfig config, Number value) {
		final InstructionExecutionService service = service(instructionExecutionService);
		Instruction instr = createSetControlValueLocalInstruction(config.getControlId(),
				value.toString());
		InstructionStatus result = null;
		try {
			if ( service != null ) {
				result = service.executeInstruction(instr);
			} else {
				result = InstructionUtils.createStatus(instr, InstructionState.Declined);
			}
		} finally {
			log.info(
					"DNP3 outstation [{}] analog operation request {} on {}[{}] control [{}] result: {}",
					getUid(), opDescription, config.getType(), index, config.getControlId(), result);
		}
		return result;
	}

	/*
	 * =========================================================================
	 * SettingSpecifierProvider implementation
	 * =========================================================================
	 */

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(16);

		result.add(new BasicTitleSettingSpecifier("status", getStackStatusMessage(), true));

		result.add(new BasicTextFieldSettingSpecifier("uid", null));
		result.add(new BasicTextFieldSettingSpecifier("groupUid", ""));
		result.add(new BasicTextFieldSettingSpecifier("eventBufferSize",
				String.valueOf(DEFAULT_EVENT_BUFFER_SIZE)));
		result.add(new BasicTextFieldSettingSpecifier("dnp3Channel.propertyFilters['uid']", null, false,
				"(objectClass=net.solarnetwork.node.io.dnp3.ChannelService)"));

		result.addAll(linkLayerSettings("linkLayerConfig.", new LinkLayerConfig(false)));

		result.addAll(outstationSettings("outstationConfig.", new OutstationConfig()));

		MeasurementConfig[] measConfs = getMeasurementConfigs();
		List<MeasurementConfig> measConfsList = (measConfs != null ? Arrays.asList(measConfs)
				: Collections.<MeasurementConfig> emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("measurementConfigs", measConfsList,
				new SettingUtils.KeyedListCallback<MeasurementConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(MeasurementConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								MeasurementConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		ControlConfig[] cntrlConfs = getControlConfigs();
		List<ControlConfig> cntrlConfsList = (cntrlConfs != null ? Arrays.asList(cntrlConfs)
				: Collections.<ControlConfig> emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("controlConfigs", cntrlConfsList,
				new SettingUtils.KeyedListCallback<ControlConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(ControlConfig value, int index,
							String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								ControlConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return result;
	}

	@Override
	public String getDisplayName() {
		return DEFAULT_UID;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.dnp3.outstation";
	}

	private synchronized String getStackStatusMessage() {
		StringBuilder buf = new StringBuilder();
		buf.append(outstation != null ? "Available" : "Offline");
		/*- TODO stats are crashing JVM for some reason
		StackStatistics stackStats = outstation != null ? outstation.getStatistics() : null;
		if ( stackStats == null ) {
			buf.append("N/A");
		} else {
			buf.append(app.getLinkStatus() == LinkStatus.RESET ? "Online" : "Offline");
			TransportStatistics stats = stackStats.transport;
			if ( stats != null ) {
				buf.append("; ").append(stats.numTransportRx).append(" in");
				buf.append("; ").append(stats.numTransportTx).append(" out");
				buf.append("; ").append(stats.numTransportErrorRx).append(" in errors");
				buf.append("; ").append(stats.numTransportBufferOverflow).append(" buffer overflows");
				buf.append("; ").append(stats.numTransportDiscard).append(" discarded");
				buf.append("; ").append(stats.numTransportIgnore).append(" ignored");
			}
		}
		*/
		return buf.toString();
	}

	/*
	 * =========================================================================
	 * Accessors
	 * =========================================================================
	 */

	/**
	 * Get the internal {@link com.automatak.dnp3.CommandHandler}
	 * implementation.
	 *
	 * <p>
	 * This is exposed primarily for testing purposes.
	 * </p>
	 *
	 * @return the handler, never {@literal null}
	 */
	protected com.automatak.dnp3.CommandHandler getCommandHandler() {
		return commandHandler;
	}

	/**
	 * Get the measurement configurations.
	 *
	 * @return the measurement configurations
	 */
	public MeasurementConfig[] getMeasurementConfigs() {
		return measurementConfigs;
	}

	/**
	 * Set the measurement configurations to use.
	 *
	 * @param measurementConfigs
	 *        the configs to use
	 */
	public void setMeasurementConfigs(MeasurementConfig[] measurementConfigs) {
		this.measurementConfigs = measurementConfigs;
	}

	/**
	 * Get the number of configured {@code measurementConfigs} elements.
	 *
	 * @return the number of {@code measurementConfigs} elements
	 */
	public int getMeasurementConfigsCount() {
		MeasurementConfig[] confs = this.measurementConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code MeasurementConfig} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link MeasurementConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code measurementConfigs} elements.
	 */
	public void setMeasurementConfigsCount(int count) {
		this.measurementConfigs = ArrayUtils.arrayWithLength(this.measurementConfigs, count,
				MeasurementConfig.class, null);
	}

	/**
	 * Get the control configurations.
	 *
	 * @return the control configurations
	 */
	public ControlConfig[] getControlConfigs() {
		return controlConfigs;
	}

	/**
	 * Set the control configurations to use.
	 *
	 * @param controlConfigs
	 *        the configs to use
	 */
	public void setControlConfigs(ControlConfig[] controlConfigs) {
		this.controlConfigs = controlConfigs;
	}

	/**
	 * Get the number of configured {@code controlConfigs} elements.
	 *
	 * @return the number of {@code controlConfigs} elements
	 */
	public int getControlConfigsCount() {
		ControlConfig[] confs = this.controlConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code ControlConfig} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new {@link ControlConfig}
	 * instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code controlConfigs} elements.
	 */
	public void setControlConfigsCount(int count) {
		this.controlConfigs = ArrayUtils.arrayWithLength(this.controlConfigs, count, ControlConfig.class,
				null);
	}

	/**
	 * Get the event buffer size.
	 *
	 * <p>
	 * This buffer is used by DNP3 to hold updated values.
	 * </p>
	 *
	 * @return the buffer size, defaults to {@link #DEFAULT_EVENT_BUFFER_SIZE}
	 */
	public int getEventBufferSize() {
		return eventBufferSize;
	}

	/**
	 * Set the event buffer size.
	 *
	 * @param eventBufferSize
	 *        the buffer size to set
	 */
	public void setEventBufferSize(int eventBufferSize) {
		if ( eventBufferSize < 0 ) {
			return;
		}
		this.eventBufferSize = eventBufferSize;
	}

	/**
	 * Get the startup delay, in seconds.
	 *
	 * @return the delay; defaults to {@link #DEFAULT_STARTUP_DELAY_SECONDS}
	 */
	public int getStartupDelaySecs() {
		return startupDelaySecs;
	}

	/**
	 * Set the startup delay, in seconds.
	 *
	 * <p>
	 * This delay is used to allow the class to be configured fully before
	 * starting.
	 * </p>
	 *
	 * @param startupDelaySecs
	 *        the delay
	 */
	public void setStartupDelaySecs(int startupDelaySecs) {
		this.startupDelaySecs = startupDelaySecs;
	}

	/**
	 * Get the outstation configuration.
	 *
	 * @return the configuration
	 * @since 1.2
	 */
	public OutstationConfig getOutstationConfig() {
		return outstationConfig;
	}

	/**
	 * Copy the link outstation configuration from one object to another.
	 *
	 * @param from
	 *        the settings to copy
	 * @param to
	 *        the destination to copy the settings to
	 * @since 1.2
	 */
	public static void copySettings(com.automatak.dnp3.OutstationConfig from,
			com.automatak.dnp3.OutstationConfig to) {
		to.allowUnsolicited = from.allowUnsolicited;
		to.indexMode = from.indexMode;
		to.maxControlsPerRequest = from.maxControlsPerRequest;
		to.maxRxFragSize = from.maxRxFragSize;
		to.maxTxFragSize = from.maxTxFragSize;
		to.selectTimeout = from.selectTimeout;
		to.solConfirmTimeout = from.solConfirmTimeout;
		to.unsolRetryTimeout = from.unsolRetryTimeout;
	}

	/**
	 * Get settings suitable for configuring an instance of
	 * {@link OutstationConfig}.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @param defaults
	 *        the default settings
	 * @return the settings, never {@literal null}
	 * @since 1.1
	 */
	public static List<SettingSpecifier> outstationSettings(String prefix, OutstationConfig defaults) {
		List<SettingSpecifier> results = new ArrayList<>(8);
		OutstationConfig config = new OutstationConfig();
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxControlsPerRequest",
				String.valueOf(config.maxControlsPerRequest)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxRxFragSize",
				String.valueOf(config.maxRxFragSize)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "maxTxFragSize",
				String.valueOf(config.maxTxFragSize)));
		return results;
	}

}
