/* ==================================================================
 * DefaultControlCenterService.java - 7/08/2025 3:43:47 pm
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.automatak.dnp3.AnalogInput;
import com.automatak.dnp3.AnalogOutputStatus;
import com.automatak.dnp3.BinaryInput;
import com.automatak.dnp3.BinaryOutputStatus;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.Counter;
import com.automatak.dnp3.DNP3Exception;
import com.automatak.dnp3.DoubleBitBinaryInput;
import com.automatak.dnp3.FrozenCounter;
import com.automatak.dnp3.HeaderInfo;
import com.automatak.dnp3.IndexedValue;
import com.automatak.dnp3.Master;
import com.automatak.dnp3.MasterStackConfig;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.io.dnp3.ChannelService;
import net.solarnetwork.node.io.dnp3.ControlCenterService;
import net.solarnetwork.node.io.dnp3.domain.ControlCenterConfig;
import net.solarnetwork.node.io.dnp3.domain.LinkLayerConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementType;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Default implementation of {@link ControlCenterService}.
 *
 * @author matt
 * @version 1.0
 */
public class DefaultControlCenterService extends AbstractApplicationService<Master>
		implements ControlCenterService, SettingSpecifierProvider {

	private final Application app;
	private final SOEHandler handler;
	private final ControlCenterConfig controlCenterConfig;
	private final ConcurrentMap<MeasurementTypeIndex, DatumStreamSamples> datumStreamMapping;
	private MeasurementConfig[] measurementConfigs;

	private Master master;

	/**
	 * Constructor.
	 *
	 * @param dnp3Channel
	 *        the channel to use
	 */
	public DefaultControlCenterService(OptionalService<ChannelService> dnp3Channel) {
		super(dnp3Channel, new LinkLayerConfig(true));
		this.app = new Application();
		this.handler = new SOEHandler();
		this.controlCenterConfig = new ControlCenterConfig();
		this.datumStreamMapping = new ConcurrentHashMap<>(4, 0.9f, 2);
		setDisplayName("DNP3 Control Center");
	}

	/**
	 * A measurement type and index combination.
	 */
	private record MeasurementTypeIndex(MeasurementType type, Integer index) {

	}

	/**
	 * A runtime datum stream value.
	 */
	private record DatumStreamSamples(String sourceId, DatumSamples samples,
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
			synchronized ( samples ) {
				Number propVal = measConfig.applyTransformations(measurementValue);
				samples.putSampleValue(measConfig.getPropertyType(), measConfig.getPropertyName(),
						propVal);
				log.trace("Source [{}] samples [{}] updated to {}; samples now {}",
						streamSamples.sourceId, measConfig.getPropertyKey(), propVal, samples);
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
				processMeasurement(MeasurementType.BinaryOutputStatus, val.index, val.value.value);
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
		return "net.solarnetwork.node.io.dnp3.controlcenter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(16);

		result.add(new BasicTitleSettingSpecifier("status", controlCenterStatusMessage(), true));

		result.addAll(basicIdentifiableSettings());

		result.add(new BasicTextFieldSettingSpecifier("dnp3Channel.propertyFilters['uid']", null, false,
				"(&(objectClass=net.solarnetwork.node.io.dnp3.ChannelService)(function=client))"));

		result.addAll(linkLayerSettings("linkLayerConfig.", new LinkLayerConfig(true)));

		result.addAll(ControlCenterConfig.controlCenterSettings("controlCenterConfig.",
				new ControlCenterConfig()));

		MeasurementConfig[] measConfs = getMeasurementConfigs();
		List<MeasurementConfig> measConfsList = (measConfs != null ? Arrays.asList(measConfs)
				: Collections.<MeasurementConfig> emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("measurementConfigs", measConfsList,
				new SettingUtils.KeyedListCallback<MeasurementConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(MeasurementConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								MeasurementConfig.clientSettings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return result;
	}

	private synchronized String controlCenterStatusMessage() {
		final Master master = this.master;
		final StringBuilder buf = new StringBuilder();
		buf.append(master != null ? "Available" : "Offline");
		buf.append(
				stackStatusMessage(master != null ? master.getStatistics() : null, app.getLinkStatus()));
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
			return channel.addMaster(uid, handler, app, createMasterStackConfig());
		} catch ( DNP3Exception e ) {
			log.error("Error creating DNP3 control center application [{}]: {}", uid, e.getMessage(), e);
			return null;
		}
	}

	private MasterStackConfig createMasterStackConfig() {
		MasterStackConfig config = new MasterStackConfig();
		LinkLayerConfig.copySettings(getLinkLayerConfig(), config.link);
		ControlCenterConfig.copySettings(getControlCenterConfig(), config.master);

		Map<MeasurementTypeIndex, DatumStreamSamples> newDatumStreamMapping = new HashMap<>();
		final MeasurementConfig[] measConfigs = getMeasurementConfigs();
		if ( measConfigs != null && measConfigs.length > 0 ) {
			Map<String, DatumStreamSamples> sourceIdMapping = new HashMap<>(8);
			for ( MeasurementConfig measConfig : measConfigs ) {
				if ( !measConfig.isValidForClient() ) {
					continue;
				}

				// locate or create DatumStreamSamples for this source ID
				DatumStreamSamples streamSamples = sourceIdMapping.get(measConfig.getSourceId());
				if ( streamSamples == null ) {
					for ( DatumStreamSamples s : datumStreamMapping.values() ) {
						if ( s.sourceId.equals(measConfig.getSourceId()) ) {
							streamSamples = new DatumStreamSamples(measConfig.getSourceId(), s.samples,
									new ConcurrentHashMap<>(8, 0.9f, 2));
							sourceIdMapping.put(measConfig.getSourceId(), streamSamples);
							break;
						}
					}
				}
				if ( streamSamples == null ) {
					streamSamples = new DatumStreamSamples(measConfig.getSourceId(), new DatumSamples(),
							new ConcurrentHashMap<>(8, 0.9f, 2));
					sourceIdMapping.put(measConfig.getSourceId(), streamSamples);
				}

				MeasurementTypeIndex idx = new MeasurementTypeIndex(measConfig.getType(),
						measConfig.getIndex());
				streamSamples.measurementConfigs.put(idx, measConfig);
				newDatumStreamMapping.putIfAbsent(idx, streamSamples);
			}
		}

		datumStreamMapping.clear();
		datumStreamMapping.putAll(newDatumStreamMapping);

		return config;
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

}
