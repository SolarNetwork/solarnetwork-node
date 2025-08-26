/* ==================================================================
 * DatumControlCenterService.java - 7/08/2025 3:43:47 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static com.automatak.dnp3.Header.Range16;
import static com.automatak.dnp3.Header.Range8;
import static java.time.Instant.now;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.MessageSource;
import com.automatak.dnp3.AnalogInput;
import com.automatak.dnp3.AnalogOutputStatus;
import com.automatak.dnp3.BinaryInput;
import com.automatak.dnp3.BinaryOutputStatus;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.Counter;
import com.automatak.dnp3.DNP3Exception;
import com.automatak.dnp3.DoubleBitBinaryInput;
import com.automatak.dnp3.FrozenCounter;
import com.automatak.dnp3.Header;
import com.automatak.dnp3.HeaderInfo;
import com.automatak.dnp3.IndexedValue;
import com.automatak.dnp3.Master;
import com.automatak.dnp3.MasterStackConfig;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.io.dnp3.ChannelService;
import net.solarnetwork.node.io.dnp3.ControlCenterService;
import net.solarnetwork.node.io.dnp3.domain.ClassType;
import net.solarnetwork.node.io.dnp3.domain.ControlCenterConfig;
import net.solarnetwork.node.io.dnp3.domain.DatumConfig;
import net.solarnetwork.node.io.dnp3.domain.LinkLayerConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementType;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;
import net.solarnetwork.util.StringUtils;

/**
 * Default implementation of {@link ControlCenterService}.
 *
 * @author matt
 * @version 1.0
 */
public class DatumControlCenterService extends AbstractApplicationService<Master>
		implements ControlCenterService, MultiDatumDataSource, SettingSpecifierProvider {

	/** The setting UID. */
	public static final String SETTING_UID = "net.solarnetwork.node.io.dnp3.controlcenter";

	private final Application app;
	private final SOEHandler handler;
	private final ControlCenterConfig controlCenterConfig;
	private final ConcurrentMap<MeasurementTypeIndex, DatumStreamSamples> datumStreamMapping;
	private OptionalService<DatumQueue> datumQueue;
	private DatumConfig[] datumConfigs;
	private Set<ClassType> unsolicitedEventClasses;

	/**
	 * Constructor.
	 *
	 * @param dnp3Channel
	 *        the channel to use
	 */
	public DatumControlCenterService(OptionalService<ChannelService> dnp3Channel) {
		super(dnp3Channel, new LinkLayerConfig(true));
		this.app = new Application();
		this.handler = new SOEHandler();
		this.datumStreamMapping = new ConcurrentHashMap<>(4, 0.9f, 2);
		setDisplayName("DNP3 Control Center");

		var ccConfig = new ControlCenterConfig();
		ccConfig.setupUnsolicitedEvents(null);
		this.controlCenterConfig = ccConfig;
	}

	@Override
	public Collection<String> publishedSourceIds() {
		Set<String> publishedSourceIds = new LinkedHashSet<>();
		final DatumConfig[] confs = getDatumConfigs();
		if ( confs != null ) {
			for ( DatumConfig conf : confs ) {
				if ( conf.isValid() ) {
					publishedSourceIds.add(resolvePlaceholders(conf.getSourceId()));
				}
			}
		}
		return publishedSourceIds;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return NodeDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		Map<String, GeneralDatum> datumBySourceId = new LinkedHashMap<>();
		for ( DatumStreamSamples dsSamples : datumStreamMapping.values() ) {
			if ( datumBySourceId.containsKey(dsSamples.sourceId) ) {
				continue;
			}

			// copy samples for snapshot
			DatumSamples samples;
			synchronized ( dsSamples.samples ) {
				samples = new DatumSamples(dsSamples.samples);
			}

			SimpleDatum d = SimpleDatum.nodeDatum(resolvePlaceholders(dsSamples.sourceId), now(),
					samples);
			datumBySourceId.put(dsSamples.sourceId, d);
		}
		return datumBySourceId.values().stream().map(NodeDatum.class::cast).toList();
	}

	/**
	 * A measurement type and index combination.
	 */
	private static record MeasurementTypeIndex(MeasurementType type, Integer index)
			implements Comparable<MeasurementTypeIndex> {

		@Override
		public int compareTo(MeasurementTypeIndex o) {
			int result = type.compareTo(o.type);
			if ( result == 0 ) {
				result = index.compareTo(o.index);
			}
			return result;
		}

	}

	/**
	 * A runtime datum stream value.
	 */
	private static record DatumStreamSamples(String sourceId, DatumConfig datumConfig,
			DatumSamples samples,
			ConcurrentMap<MeasurementTypeIndex, MeasurementConfig> measurementConfigs) {

	}

	/*
	 * =========================================================================
	 * Control Center application implementation
	 * =========================================================================
	 */

	private class Application extends BaseControlCenterApplication {

	}

	private class SOEHandler extends BaseSOEHandler {

		private void processMeasurement(MeasurementType type, Integer index, Number measurementValue) {
			var idx = new MeasurementTypeIndex(type, index);
			DatumStreamSamples streamSamples = datumStreamMapping.get(idx);
			if ( streamSamples == null ) {
				return;
			}
			MeasurementConfig measConfig = streamSamples.measurementConfigs.get(idx);
			if ( measConfig == null ) {
				// shouldn't really happen
				return;
			}
			final DatumSamples samples = streamSamples.samples;
			DatumSamples publishSamples = null;
			synchronized ( samples ) {
				Number propVal = measConfig.applyTransformations(measurementValue);
				samples.putSampleValue(measConfig.getPropertyType(), measConfig.getPropertyName(),
						propVal);
				log.trace("Source [{}] samples [{}] updated to {}; samples now {}",
						streamSamples.sourceId, measConfig.getPropertyKey(), propVal, samples);
				if ( streamSamples.datumConfig.isGenerateDatumOnEvents() ) {
					publishSamples = new DatumSamples(samples);
				}
			}
			if ( publishSamples != null ) {
				DatumQueue queue = OptionalService.service(getDatumQueue());
				if ( queue != null ) {
					SimpleDatum d = SimpleDatum.nodeDatum(streamSamples.sourceId, now(), samples);
					queue.offer(d);
				}
			}
		}

		@Override
		public void processAI(HeaderInfo info, Iterable<IndexedValue<AnalogInput>> values) {
			if ( values == null ) {
				return;
			}
			log.trace("Got AnalogInput updates: {}", values);
			for ( IndexedValue<AnalogInput> val : values ) {
				processMeasurement(MeasurementType.AnalogInput, val.index, val.value.value);
			}
		}

		@Override
		public void processAOS(HeaderInfo info, Iterable<IndexedValue<AnalogOutputStatus>> values) {
			log.trace("Got AnalogOutputStatus updates: {}", values);
			for ( IndexedValue<AnalogOutputStatus> val : values ) {
				processMeasurement(MeasurementType.AnalogOutputStatus, val.index, val.value.value);
			}
		}

		@Override
		public void processBI(HeaderInfo info, Iterable<IndexedValue<BinaryInput>> values) {
			log.trace("Got BinaryInput updates: {}", values);
			for ( IndexedValue<BinaryInput> val : values ) {
				processMeasurement(MeasurementType.BinaryInput, val.index, val.value.value ? 1 : 0);
			}
		}

		@Override
		public void processBOS(HeaderInfo info, Iterable<IndexedValue<BinaryOutputStatus>> values) {
			log.trace("Got BinaryOutputStatus updates: {}", values);
			for ( IndexedValue<BinaryOutputStatus> val : values ) {
				processMeasurement(MeasurementType.BinaryOutputStatus, val.index,
						val.value.value ? 1 : 0);
			}
		}

		@Override
		public void processC(HeaderInfo info, Iterable<IndexedValue<Counter>> values) {
			log.trace("Got Counter updates: {}", values);
			for ( IndexedValue<Counter> val : values ) {
				processMeasurement(MeasurementType.Counter, val.index, val.value.value);
			}
		}

		@Override
		public void processDBI(HeaderInfo info, Iterable<IndexedValue<DoubleBitBinaryInput>> values) {
			log.trace("Got DoubleBitBinaryInput updates: {}", values);
			for ( IndexedValue<DoubleBitBinaryInput> val : values ) {
				processMeasurement(MeasurementType.DoubleBitBinaryInput, val.index,
						val.value.value.toType());
			}
		}

		@Override
		public void processFC(HeaderInfo info, Iterable<IndexedValue<FrozenCounter>> values) {
			log.trace("Got FrozenCounter updates: {}", values);
			for ( IndexedValue<FrozenCounter> val : values ) {
				processMeasurement(MeasurementType.FrozenCounter, val.index, val.value.value);
			}
		}

	}

	/*
	 * =========================================================================
	 * Settings
	 * =========================================================================
	 */

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(16);

		result.add(new BasicTitleSettingSpecifier("status", controlCenterStatusMessage(), true));

		result.add(new BasicTitleSettingSpecifier("info",
				controlCenterDatumInfo(getMessageSource(), Locale.getDefault()), true, true));

		result.addAll(basicIdentifiableSettings());

		result.add(new BasicTextFieldSettingSpecifier("dnp3Channel.propertyFilters['uid']", null, false,
				"(&(objectClass=net.solarnetwork.node.io.dnp3.ChannelService)(function=client))"));

		result.addAll(linkLayerSettings("linkLayerConfig.", new LinkLayerConfig(true)));

		result.addAll(ControlCenterConfig.controlCenterSettings("controlCenterConfig.",
				new ControlCenterConfig()));

		result.add(new BasicTextFieldSettingSpecifier("unsolicitedEventClassesValue", null));

		final DatumConfig[] datumConfs = getDatumConfigs();
		final List<DatumConfig> datumConfsList = (datumConfs != null ? Arrays.asList(datumConfs)
				: Collections.emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("datumConfigs", datumConfsList,
				new SettingUtils.KeyedListCallback<>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(DatumConfig value, int index,
							String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								value.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return result;
	}

	private synchronized String controlCenterStatusMessage() {
		final Master master = getDnp3Stack();
		final StringBuilder buf = new StringBuilder();
		buf.append(master != null ? "Available" : "Offline");

		final ChannelService channelService = channelService();
		if ( master != null || channelService != null ) {
			buf.append("; ");
			buf.append(stackStatusMessage(master != null ? master.getStatistics() : null,
					channelService != null ? channelService.getChannelState() : null));
		}
		return buf.toString();
	}

	@Override
	protected synchronized Master createDnp3Stack() {
		final String uid = getUid();
		if ( uid == null || uid.isBlank() ) {
			log.warn("Missing UID: can not start DNP3 control center.");
			return null;
		}
		Channel channel = channel();
		if ( channel == null ) {
			log.info("DNP3 channel not available for control center [{}]", uid);
			return null;
		}
		log.info("Initializing DNP3 control center [{}]", uid);
		try {
			Master master = channel.addMaster(uid, handler, app, createMasterStackConfig());

			// add periodic scans
			DatumConfig[] datumConfs = getDatumConfigs();
			if ( datumConfs != null && datumConfs.length > 0 ) {
				for ( DatumConfig datumConf : datumConfs ) {
					if ( !(datumConf.isValid() && datumConf.getPollFrequency() != null
							&& datumConf.getPollFrequency().compareTo(Duration.ZERO) > 0) ) {
						continue;
					}
					List<Header> pollHeaders = headers(datumConf, true);
					if ( pollHeaders != null && !pollHeaders.isEmpty() ) {
						master.addPeriodicScan(datumConf.getPollFrequency(), pollHeaders, handler);
					}
				}
			}

			return master;
		} catch ( DNP3Exception e ) {
			log.error("Error creating DNP3 control center application [{}]: {}", uid, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Generate a list of DNP3 headers for polling or event registration.
	 *
	 * @param datumConf
	 *        the configuration to generate headers from
	 * @param forPoll
	 *        {@code true} if "scan" polling headers are desired, {@code false}
	 *        for event registration
	 * @return the headers, never {@code null}
	 */
	private static List<Header> headers(final DatumConfig datumConf, final boolean forPoll) {
		if ( datumConf == null || !datumConf.isValid() ) {
			return Collections.emptyList();
		}
		Map<MeasurementType, IntRangeSet> ranges = new HashMap<>();

		for ( MeasurementConfig measConf : datumConf.getMeasurementConfigs() ) {
			if ( !measConf.isValidForClient() ) {
				continue;
			}
			ranges.computeIfAbsent(measConf.getType(), k -> new IntRangeSet()).add(measConf.getIndex());
		}
		if ( ranges.isEmpty() ) {
			return Collections.emptyList();
		}

		final List<Header> result = new ArrayList<>();
		for ( Entry<MeasurementType, IntRangeSet> e : ranges.entrySet() ) {
			MeasurementType measType = e.getKey();
			IntRangeSet typeRanges = e.getValue();
			for ( IntRange range : typeRanges.ranges() ) {
				// we assume variation 1 for all types here
				if ( range.getMin() > 0xFF || range.getMax() > 0xFF ) {
					result.add(Range16(forPoll ? measType.getStaticGroup() : measType.getEventGroup(),
							(byte) 1, range.getMin(), range.getMax()));
				} else {
					result.add(Range8(forPoll ? measType.getStaticGroup() : measType.getEventGroup(),
							(byte) 1, (short) range.getMin(), (short) range.getMax()));
				}
			}
		}
		return result;
	}

	private MasterStackConfig createMasterStackConfig() {
		final MasterStackConfig config = new MasterStackConfig();
		LinkLayerConfig.copySettings(getLinkLayerConfig(), config.link);
		ControlCenterConfig.copySettings(controlCenterConfig, config.master);

		final Map<MeasurementTypeIndex, DatumStreamSamples> newDatumStreamMapping = new HashMap<>();
		final DatumConfig[] datumConfs = getDatumConfigs();
		if ( datumConfs != null && datumConfs.length > 0 ) {
			final Map<String, DatumStreamSamples> sourceIdMapping = new HashMap<>(8);
			for ( DatumConfig datumConfig : datumConfs ) {
				if ( !datumConfig.isValid() ) {
					continue;
				}
				final String sourceId = datumConfig.getSourceId();
				final MeasurementConfig[] measConfs = datumConfig.getMeasurementConfigs();
				if ( measConfs != null && measConfs.length > 0 ) {
					for ( MeasurementConfig measConfig : measConfs ) {
						if ( !measConfig.isValidForClient() ) {
							continue;
						}

						// locate or create DatumStreamSamples for this source ID
						DatumStreamSamples streamSamples = sourceIdMapping.get(sourceId);
						if ( streamSamples == null ) {
							for ( DatumStreamSamples s : datumStreamMapping.values() ) {
								if ( s.sourceId.equals(sourceId) ) {
									streamSamples = new DatumStreamSamples(sourceId, datumConfig,
											s.samples, new ConcurrentHashMap<>(8, 0.9f, 2));
									sourceIdMapping.put(sourceId, streamSamples);
									break;
								}
							}
						}
						if ( streamSamples == null ) {
							streamSamples = new DatumStreamSamples(sourceId, datumConfig,
									new DatumSamples(), new ConcurrentHashMap<>(8, 0.9f, 2));
							sourceIdMapping.put(sourceId, streamSamples);
						}

						MeasurementTypeIndex idx = new MeasurementTypeIndex(measConfig.getType(),
								measConfig.getIndex());
						streamSamples.measurementConfigs.put(idx, measConfig);
						newDatumStreamMapping.putIfAbsent(idx, streamSamples);
					}
				}
			}
		}

		datumStreamMapping.clear();
		datumStreamMapping.putAll(newDatumStreamMapping);

		return config;
	}

	private String controlCenterDatumInfo(MessageSource messageSource, Locale locale) {
		final DatumConfig[] datumConfs = getDatumConfigs();
		if ( datumConfs == null || datumConfs.length < 1 ) {
			return "";
		}

		final StringBuilder buf = new StringBuilder();

		// render section per source ID

		for ( DatumConfig datumConf : datumConfs ) {
			if ( !datumConf.isValid() ) {
				continue;
			}
			buf.append(messageSource.getMessage("datumInfo.title",
					new Object[] { datumConf.getSourceId() }, locale));

			// render table of measurements per measurement type

			final MeasurementConfig[] measConfs = datumConf.getMeasurementConfigs();

			// @formatter:off
			SortedMap<MeasurementType, SortedSet<MeasurementTypeIndex>> measGroups = Arrays.stream(measConfs)
					.filter(c -> c.isValidForClient())
					.collect(groupingBy(MeasurementConfig::getType, TreeMap::new,
							mapping(c -> new MeasurementTypeIndex(c.getType(), c.getIndex()),
									toCollection(TreeSet::new))));
			// @formatter:on

			for ( Entry<MeasurementType, SortedSet<MeasurementTypeIndex>> measGroupEntry : measGroups
					.entrySet() ) {
				final MeasurementType type = measGroupEntry.getKey();
				// start a new measurements block for the type
				buf.append(messageSource.getMessage("datumInfoMeasurementBlock.start",
						new Object[] { messageSource.getMessage(
								"measurementType.%s.title".formatted(type.name()), null, locale) },
						locale));
				buf.append(messageSource.getMessage("datumInfoMeasurementTable.start", null, locale));

				// render measurements table for the block
				for ( MeasurementTypeIndex measKey : measGroupEntry.getValue() ) {

					DatumStreamSamples dsSamples = datumStreamMapping.get(measKey);
					MeasurementConfig measConf = (dsSamples != null
							? dsSamples.measurementConfigs.get(measKey)
							: null);
					buf.append(messageSource.getMessage("datumInfoMeasurementTable.row", new Object[] {
							measKey.index, "0x" + Integer.toHexString(measKey.index),
							(measConf != null ? measConf.getPropertyName() : ""),
							(measConf != null && measConf.getPropertyName() != null
									&& measConf.getPropertyType() != null
									&& dsSamples.samples.hasSampleValue(measConf.getPropertyType(),
											measConf.getPropertyName())
													? dsSamples.samples.getSampleValue(
															measConf.getPropertyType(),
															measConf.getPropertyName())
													: "") },
							locale));
				}

				buf.append(messageSource.getMessage("datumInfoMeasurementTable.end", null, locale));
				buf.append(messageSource.getMessage("datumInfoMeasurementBlock.end", null, locale));
			}
		}

		return buf.toString();
	}

	/**
	 * Get the control center configuration.
	 *
	 * @return the configuration
	 */
	public ControlCenterConfig getControlCenterConfig() {
		return controlCenterConfig;
	}

	/**
	 * Get the measurement configurations.
	 *
	 * @return the measurement configurations
	 */
	public DatumConfig[] getDatumConfigs() {
		return datumConfigs;
	}

	/**
	 * Set the measurement configurations to use.
	 *
	 * @param measurementConfigs
	 *        the configurations to use
	 */
	public void setDatumConfigs(DatumConfig[] measurementConfigs) {
		this.datumConfigs = measurementConfigs;
	}

	/**
	 * Get the number of configured {@code datumConfigs} elements.
	 *
	 * @return the number of {@code datumConfigs} elements
	 */
	public int getDatumConfigsCount() {
		final DatumConfig[] confs = getDatumConfigs();
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
	 *        The desired number of {@code datumConfigs} elements.
	 */
	public void setDatumConfigsCount(int count) {
		this.datumConfigs = ArrayUtils.arrayWithLength(this.datumConfigs, count, DatumConfig.class,
				null);
	}

	/**
	 * Get the unsolicited event classes to support.
	 *
	 * @return the classes
	 */
	public Set<ClassType> getUnsolicitedEventClasses() {
		return unsolicitedEventClasses;
	}

	/**
	 * Set the unsolicited event classes to support.
	 *
	 * @param unsolicitedEventClasses
	 *        the classes, or {@code null} or empty set to disable unsolicited
	 *        events
	 */
	public void setUnsolicitedEventClasses(Set<ClassType> unsolicitedEventClasses) {
		this.unsolicitedEventClasses = unsolicitedEventClasses;
		this.controlCenterConfig.setupUnsolicitedEvents(unsolicitedEventClasses);
	}

	/**
	 * Get the unsolicited event classes to support as a comma-delimited list of
	 * class type codes.
	 *
	 * @return the comma-delimited list of class type codes
	 */
	public String getUnsolicitedEventClassesValue() {
		final Set<ClassType> classes = getUnsolicitedEventClasses();
		if ( classes == null || classes.isEmpty() ) {
			return null;
		}
		return StringUtils.commaDelimitedStringFromCollection(
				classes.stream().map(c -> String.valueOf(c.getCode())).toList());
	}

	/**
	 * Set the unsolicited event classes to support as a comma-delimited list of
	 * class type codes.
	 *
	 * @param value
	 *        the classes as a comma-delimited list of class type codes, or
	 *        {@code null} or empty set to disable unsolicited events
	 */
	public void setUnsolicitedEventClassesValue(String value) {
		final Set<String> set = StringUtils.commaDelimitedStringToSet(value);
		final Set<ClassType> classes = new TreeSet<>();
		if ( set != null ) {
			for ( String classTypeValue : set ) {
				try {
					ClassType classType = ClassType.forValue(classTypeValue);
					if ( classType != ClassType.Static ) {
						classes.add(classType);
					}
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
				}
			}
		}
		setUnsolicitedEventClasses(classes.isEmpty() ? null : classes);
	}

	/**
	 * Get the datum queue.
	 *
	 * @return the queue
	 */
	public OptionalService<DatumQueue> getDatumQueue() {
		return datumQueue;
	}

	/**
	 * Set the datum queue.
	 *
	 * @param datumQueue
	 *        the queue to set
	 */
	public void setDatumQueue(OptionalService<DatumQueue> datumQueue) {
		this.datumQueue = datumQueue;
	}

}
