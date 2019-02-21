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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.springframework.core.task.TaskExecutor;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.DNP3Exception;
import com.automatak.dnp3.DatabaseConfig;
import com.automatak.dnp3.EventBufferConfig;
import com.automatak.dnp3.Outstation;
import com.automatak.dnp3.OutstationStackConfig;
import com.automatak.dnp3.StackStatistics;
import com.automatak.dnp3.enums.CommandStatus;
import com.automatak.dnp3.enums.LinkStatus;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.io.dnp3.ChannelService;
import net.solarnetwork.node.io.dnp3.domain.MeasurementConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementType;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.OptionalService;

/**
 * A DNP3 "outstation" server service that publishes SolarNode datum/control
 * events to DNP3.
 * 
 * @author matt
 * @version 1.0
 */
public class OutstationService extends AbstractApplicationService
		implements EventHandler, SettingSpecifierProvider {

	/** The default event buffer size. */
	private static final int DEFAULT_EVENT_BUFFER_SIZE = 30;

	/** The default uid value. */
	public static final String DEFAULT_UID = "DNP3 Outstation";

	private final Application app;
	private final CommandHandler commandHandler;

	private MeasurementConfig[] measurementConfigs;
	private int eventBufferSize = DEFAULT_EVENT_BUFFER_SIZE;

	private Outstation outstation;

	/**
	 * Constructor.
	 * 
	 * @param dnp3Channel
	 *        the channel to use
	 */
	public OutstationService(OptionalService<ChannelService> dnp3Channel) {
		super(dnp3Channel);
		setUid(DEFAULT_UID);
		this.app = new Application();
		this.commandHandler = new CommandHandler();
	}

	@Override
	public synchronized void startup() {
		super.startup();
		outstation = createOutstation();
		outstation.enable();
	}

	@Override
	public synchronized void shutdown() {
		super.shutdown();
		if ( outstation != null ) {
			outstation.shutdown();
			this.outstation = null;
		}
	}

	private Outstation createOutstation() {
		Channel channel = channel();
		if ( channel == null ) {
			return null;
		}
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
		return new OutstationStackConfig(createDatabaseConfig(configs), createEventBufferConig(configs));
	}

	private Map<MeasurementType, List<MeasurementConfig>> measurementTypeMap(
			MeasurementConfig[] configs) {
		Map<MeasurementType, List<MeasurementConfig>> map = new LinkedHashMap<>(
				configs != null ? configs.length : 0);
		if ( configs != null ) {
			for ( MeasurementConfig config : configs ) {
				MeasurementType type = config.getType();
				if ( type != null ) {
					map.computeIfAbsent(type, k -> new ArrayList<>(4)).add(config);
				}
			}
		}
		return map;
	}

	private DatabaseConfig createDatabaseConfig(Map<MeasurementType, List<MeasurementConfig>> configs) {
		int analogCount = 0;
		int aoStatusCount = 0;
		int binaryCount = 0;
		int boStatusCount = 0;
		int counterCount = 0;
		int doubleBinaryCount = 0;
		int frozenCounterCount = 0;
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
						break;

					case AnalogOutputStatus:
						aoStatusCount = list.size();
						break;

					case BinaryInput:
						binaryCount = list.size();
						break;

					case BinaryOutputStatus:
						boStatusCount = list.size();
						break;

					case Counter:
						counterCount = list.size();
						break;

					case DoubleBitBinaryInput:
						doubleBinaryCount = list.size();
						break;

					case FrozenCounter:
						frozenCounterCount = list.size();
						break;
				}
			}
		}
		return new DatabaseConfig(binaryCount, doubleBinaryCount, analogCount, counterCount,
				frozenCounterCount, boStatusCount, aoStatusCount);
	}

	private EventBufferConfig createEventBufferConig(
			Map<MeasurementType, List<MeasurementConfig>> configs) {
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
		}
		// TODO Auto-generated method stub
	}

	private void handleDatumCapturedEvent(Event event) {
		Map<String, Object> data = mapForEvent(event);
		TaskExecutor executor = getTaskExecutor();
		if ( executor != null ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					// TODO post prop changes
				}
			});
		} else {
			// TODO post prop changes
		}
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
	}

	/*
	 * =========================================================================
	 * SettingSpecifierProvider implementation
	 * =========================================================================
	 */

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTitleSettingSpecifier("status", getStackStatusMessage(), true));

		result.add(new BasicTextFieldSettingSpecifier("uid", DEFAULT_UID));
		result.add(new BasicTextFieldSettingSpecifier("groupUID", ""));
		result.add(new BasicTextFieldSettingSpecifier("eventBufferSize",
				String.valueOf(DEFAULT_EVENT_BUFFER_SIZE)));

		MeasurementConfig[] measConfs = getMeasurementConfigs();
		List<MeasurementConfig> measConfsList = (measConfs != null ? Arrays.asList(measConfs)
				: Collections.<MeasurementConfig> emptyList());
		result.add(SettingsUtil.dynamicListSettingSpecifier("measurementConfigs", measConfsList,
				new SettingsUtil.KeyedListCallback<MeasurementConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(MeasurementConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								MeasurementConfig.settings(key + "."));
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
	public String getSettingUID() {
		return "net.solarnetwork.node.io.dnp3.outstation";
	}

	private synchronized String getStackStatusMessage() {
		StringBuilder buf = new StringBuilder();
		StackStatistics stackStats = outstation != null ? outstation.getStatistics() : null;
		if ( stackStats == null ) {
			buf.append("N/A");
		} else {
			buf.append(app.getLinkStatus() == LinkStatus.RESET ? "Online" : "Offline");
			/*- stats are crashing JVM for some reason
			TransportStatistics stats = stackStats.transport;
			if ( stats != null ) {
				buf.append("; ").append(stats.numTransportRx).append(" in");
				buf.append("; ").append(stats.numTransportTx).append(" out");
				buf.append("; ").append(stats.numTransportErrorRx).append(" in errors");
				buf.append("; ").append(stats.numTransportBufferOverflow).append(" buffer overflows");
				buf.append("; ").append(stats.numTransportDiscard).append(" discarded");
				buf.append("; ").append(stats.numTransportIgnore).append(" ignored");
			}
			*/
		}
		return buf.toString();
	}
	/*
	 * =========================================================================
	 * Accessors
	 * =========================================================================
	 */

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
	 * Get the event buffer size.
	 * 
	 * <p>
	 * This buffer is used by DNP3 to hold updated values.
	 * </p>
	 * 
	 * @return the buffer size
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

}
