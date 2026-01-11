/* ==================================================================
 * BacnetDatumDataSource.java - 3/11/2022 2:34:46 pm
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

package net.solarnetwork.node.datum.bacnet;

import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.io.bacnet.BacnetConnection;
import net.solarnetwork.node.io.bacnet.BacnetCovHandler;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.io.bacnet.BacnetNetwork;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generic BACnet device datum data source.
 *
 * @author matt
 * @version 1.5
 */
public class BacnetDatumDataSource extends DatumDataSourceSupport implements DatumDataSource,
		SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver, BacnetCovHandler {

	/** The setting UID used by this service. */
	public static final String SETTING_UID = "net.solarnetwork.node.datum.bacnet";

	/** The {@code connectionCheckFrequency} property default value. */
	public static final long DEFAULT_CONNECTION_CHECK_FREQUENCY = 60_000L;

	/** The {@code reconnectDelay} property default value. */
	public static final long DEFAULT_RECONNECT_DELAY = 10_000L;

	/** The {@code sampleCacheMs} property default value. */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5_000L;

	/** The {@code datumMode} property default value. */
	public static final BacnetDatumMode DEFAULT_DATUM_MODE = BacnetDatumMode.PollOnly;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OptionalService<BacnetNetwork> bacnetNetwork;
	private String sourceId;
	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;
	private BacnetDatumMode datumMode = DEFAULT_DATUM_MODE;
	private BacnetDeviceConfig[] deviceConfigs;

	private BacnetConnection connection;
	private ScheduledFuture<?> connectionCheckFuture;
	private long connectionCheckFrequency = DEFAULT_CONNECTION_CHECK_FREQUENCY;
	private long reconnectDelay = DEFAULT_RECONNECT_DELAY;
	private Map<BacnetDeviceObjectPropertyRef, BacnetPropertyConfig> propertyRefs;

	/**
	 * Constructor.
	 *
	 * @param bacnetNetwork
	 *        the network to use
	 */
	public BacnetDatumDataSource(OptionalService<BacnetNetwork> bacnetNetwork) {
		super();
		this.bacnetNetwork = ObjectUtils.requireNonNullArgument(bacnetNetwork, "bacnetNetwork");
		setDisplayName("BACnet Device");
	}

	@Override
	public void serviceDidStartup() {
		rescheduleConnectionCheck();
	}

	@Override
	public void serviceDidShutdown() {
		if ( connectionCheckFuture != null ) {
			connectionCheckFuture.cancel(true);
			connectionCheckFuture = null;
		}
		closeConnection();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		rescheduleConnectionCheck();
		closeConnection();
		propertyRefs = null;
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = basicIdentifiableSettings();
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier("bacnetNetworkUid", null, false,
				"(objectClass=net.solarnetwork.node.io.bacnet.BacnetNetwork)"));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier datumModeSpec = new BasicMultiValueSettingSpecifier(
				"datumModeValue", DEFAULT_DATUM_MODE.name());
		Map<String, String> datumModeTitles = new LinkedHashMap<>(4);
		for ( BacnetDatumMode e : BacnetDatumMode.values() ) {
			datumModeTitles.put(e.name(), getMessageSource().getMessage("BacnetDatumMode." + e.name(),
					null, e.name(), Locale.getDefault()));
		}
		datumModeSpec.setValueTitles(datumModeTitles);
		results.add(datumModeSpec);

		BacnetDeviceConfig[] confs = getDeviceConfigs();
		List<BacnetDeviceConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("deviceConfigs", confsList,
				new SettingUtils.KeyedListCallback<BacnetDeviceConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(BacnetDeviceConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								value.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return results;
	}

	/**
	 * Get the BACNet connection, if available.
	 *
	 * @return the connection, or {@literal null} it not available
	 */
	protected synchronized BacnetConnection connection() {
		if ( connection != null && !connection.isClosed() ) {
			return connection;
		}
		final String networkUid = getBacnetNetworkUid();
		if ( networkUid == null ) {
			return null;
		}
		BacnetNetwork network = service(bacnetNetwork);
		if ( network == null ) {
			return null;
		}
		BacnetConnection conn = network.createConnection();
		if ( conn != null ) {
			log.info("BACnet connection created for {}", networkUid);
			try {
				conn.open();
				conn.addCovHandler(this);
				Set<BacnetDeviceObjectPropertyRef> refs = propertyRefs().keySet();
				if ( !refs.isEmpty() ) {
					network.setCachePolicy(refs, sampleCacheMs);
					conn.covSubscribe(refs, 5); // TODO maxDelay setting
				}
				connection = conn;
			} catch ( IOException e ) {
				throw new RemoteServiceException(String.format(
						"Error communicating with BACnet network %s: %s", network, e.getMessage()), e);
			}
		}
		return conn;
	}

	private synchronized void closeConnection() {
		if ( connection != null ) {
			log.info("BACnet connection closed for {}", getBacnetNetworkUid());
			try {
				connection.close();
			} catch ( IOException e ) {
				// ignore this
			}
			connection = null;
		}
	}

	private synchronized void rescheduleConnectionCheck() {
		if ( connectionCheckFuture != null ) {
			connectionCheckFuture.cancel(true);
			connectionCheckFuture = null;
		}
		final String networkUid = getBacnetNetworkUid();
		if ( networkUid == null ) {
			return;
		}
		final TaskScheduler taskScheduler = getTaskScheduler();
		if ( taskScheduler != null && connectionCheckFuture == null ) {
			log.info("Scheduling BACnet [{}] connectivity check for {}ms", networkUid,
					connectionCheckFrequency);
			connectionCheckFuture = taskScheduler.scheduleWithFixedDelay(new ConnectionCheck(),
					Instant.ofEpochMilli(System.currentTimeMillis() + 10000L),
					Duration.ofMillis(connectionCheckFrequency));
		}
	}

	private class ConnectionCheck implements Runnable {

		@Override
		public void run() {
			final String networkUid = getBacnetNetworkUid();
			try {
				if ( networkUid == null ) {
					log.debug("BACnet network UID not configured");
					return;
				}
				BacnetConnection conn = connection();
				if ( conn == null ) {
					log.info("No BACnet connection available to {} (missing configuration?)",
							networkUid);
				}
			} catch ( Exception e ) {
				log.error("Error checking BACnet connection to {}: {}", networkUid, e.toString());
			}
		}

	}

	private synchronized Map<BacnetDeviceObjectPropertyRef, BacnetPropertyConfig> propertyRefs() {
		if ( propertyRefs != null ) {
			return propertyRefs;
		}
		final BacnetDeviceConfig[] deviceConfs = getDeviceConfigs();
		if ( deviceConfs == null || deviceConfs.length < 1 ) {
			Map<BacnetDeviceObjectPropertyRef, BacnetPropertyConfig> results = Collections.emptyMap();
			propertyRefs = results;
			return results;
		}
		Map<BacnetDeviceObjectPropertyRef, BacnetPropertyConfig> results = new HashMap<>();
		int devNum = 0;
		for ( BacnetDeviceConfig deviceConf : deviceConfs ) {
			devNum++;
			if ( !deviceConf.isValid() ) {
				log.warn("Bacnet source [{}] device configuration {} is not valid, ignoring.",
						resolvePlaceholders(sourceId), devNum);
				continue;
			}
			int propNum = 0;
			for ( BacnetPropertyConfig propConf : deviceConf.getPropConfigs() ) {
				propNum++;
				if ( !propConf.isValid() ) {
					log.warn(
							"Bacnet source [{}] device {} property {} configuration is not valid, ignoring: {}",
							resolvePlaceholders(sourceId), devNum, propNum, propConf);
					continue;
				}
				results.put(propConf.toRef(deviceConf.getDeviceId()), propConf);
			}
		}
		propertyRefs = results;
		return results;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		if ( datumMode == BacnetDatumMode.EventOnly ) {
			return null;
		}
		final String sourceId = resolvePlaceholders(getSourceId());
		if ( sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		BacnetNetwork network = OptionalService.service(bacnetNetwork);
		if ( network == null ) {
			return null;
		}
		BacnetConnection conn = connection();
		if ( conn != null ) {
			log.debug("Working with BacnetConnection {}", conn);
			Map<BacnetDeviceObjectPropertyRef, BacnetPropertyConfig> refs = propertyRefs();
			Map<BacnetDeviceObjectPropertyRef, ?> values = conn.propertyValues(refs.keySet());
			if ( log.isDebugEnabled() ) {
				log.debug("Got property values: {}",
						values.entrySet().stream()
								.map(e -> String.format("%s: %s", e.getKey(), e.getValue()))
								.collect(Collectors.joining("\n  ", "\n  ", "")));
			}
			return createDatum(sourceId, refs, values);
		}
		return null;
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	private NodeDatum createDatum(final String sourceId,
			Map<BacnetDeviceObjectPropertyRef, BacnetPropertyConfig> refs,
			Map<BacnetDeviceObjectPropertyRef, ?> values) {
		DatumSamples s = new DatumSamples();
		for ( Entry<BacnetDeviceObjectPropertyRef, BacnetPropertyConfig> e : refs.entrySet() ) {
			BacnetPropertyConfig conf = e.getValue();
			if ( !conf.isValid() ) {
				continue;
			}
			Object val = values.get(e.getKey());
			if ( val == null ) {
				continue;
			}
			switch (conf.getPropertyType()) {
				case Accumulating:
				case Instantaneous:
					if ( val instanceof Number ) {
						s.putSampleValue(conf.getPropertyType(), conf.getPropertyKey(),
								conf.applyTransformations((Number) val));
					} else {
						log.warn(
								"Cannot use BACnet {} non-number property value [{}] for source [{}] property [{}]",
								e.getKey(), val, sourceId, conf.getPropertyKey());
					}
					break;

				case Status:
					s.putStatusSampleValue(conf.getPropertyKey(), val.toString());
					break;

				case Tag:
					s.addTag(val.toString());
					break;

				case Metadata:
					// ignore
					break;
			}
		}
		return (s.isEmpty() ? null : SimpleDatum.nodeDatum(sourceId, Instant.now(), s));
	}

	@Override
	public void accept(Integer subscriptionId, Map<BacnetDeviceObjectPropertyRef, ?> updates) {
		final String sourceId = resolvePlaceholders(getSourceId());
		if ( sourceId == null || sourceId.isEmpty() ) {
			return;
		}
		NodeDatum datum = createDatum(sourceId, propertyRefs(), updates);
		final BacnetDatumMode mode = getDatumMode();
		DatumQueue q = service(getDatumQueue());
		if ( q != null ) {
			q.offer(datum, mode != BacnetDatumMode.PollOnly);
		}
	}

	/**
	 * Get the BacnetNetwork service UID filter value.
	 *
	 * @return the BacnetNetwork UID filter value, if {@code bacnetNetwork} also
	 *         implements {@link FilterableService}
	 */
	public String getBacnetNetworkUid() {
		String uid = FilterableService.filterPropValue(bacnetNetwork, "uid");
		if ( uid != null && uid.trim().isEmpty() ) {
			uid = null;
		}
		return uid;
	}

	/**
	 * Set the BacnetNetwork service UID filter value.
	 *
	 * @param uid
	 *        the BacnetNetwork UID filter value to set, if
	 *        {@code bacnetNetwork} also implements {@link FilterableService}
	 */
	public void setBacnetNetworkUid(String uid) {
		FilterableService.setFilterProp(bacnetNetwork, "uid", uid);
	}

	/**
	 * Get the source ID to use for returned datum.
	 *
	 * @return the source ID to use
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to use for returned datum.
	 *
	 * @param sourceId
	 *        the source ID to use
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the sample cache maximum age, in milliseconds.
	 *
	 * @return the cache milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 *
	 * @param sampleCacheMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the connection check frequency.
	 *
	 * @return the check frequency, in milliseconds; defaults to
	 *         {@link #DEFAULT_CONNECTION_CHECK_FREQUENCY}
	 */
	public long getConnectionCheckFrequency() {
		return connectionCheckFrequency;
	}

	/**
	 * Set the connection check frequency.
	 *
	 * <p>
	 * A frequency at which to check that the CAN bus connection is still valid,
	 * or {@literal 0} to disable. Requires the
	 * {@link #setTaskScheduler(TaskScheduler)} to have been configured.
	 * </p>
	 *
	 * @param connectionCheckFrequency
	 *        the frequency to check for a valid connection, in milliseconds
	 */
	public void setConnectionCheckFrequency(long connectionCheckFrequency) {
		this.connectionCheckFrequency = connectionCheckFrequency;
	}

	/**
	 * Get the reconnect delay.
	 *
	 * @return the delay, in milliseconds
	 */
	public long getReconnectDelay() {
		return reconnectDelay;
	}

	/**
	 * Set the reconnect delay.
	 *
	 * @param reconnectDelay
	 *        the delay to set, in milliseconds
	 */
	public void setReconnectDelay(long reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
	}

	/**
	 * Get the device configurations.
	 *
	 * @return the device configurations
	 */
	public BacnetDeviceConfig[] getDeviceConfigs() {
		return deviceConfigs;
	}

	/**
	 * Set the device configurations to use.
	 *
	 * @param deviceConfigs
	 *        the configs to use
	 */
	public void setDeviceConfigs(BacnetDeviceConfig[] deviceConfigs) {
		this.deviceConfigs = deviceConfigs;
	}

	/**
	 * Get the number of configured {@code deviceConfigs} elements.
	 *
	 * @return the number of {@code deviceConfigs} elements
	 */
	public int getDeviceConfigsCount() {
		BacnetDeviceConfig[] confs = this.deviceConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code deviceConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link BacnetDeviceConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        the desired number of {@code deviceConfigs} elements
	 */
	public void setDeviceConfigsCount(int count) {
		this.deviceConfigs = ArrayUtils.arrayWithLength(this.deviceConfigs, count,
				BacnetDeviceConfig.class, null);
	}

	/**
	 * Get the datum mode.
	 *
	 * @return the datum mode, never {@literal null}; defaults to
	 *         {@link #DEFAULT_DATUM_MODE}
	 */
	public BacnetDatumMode getDatumMode() {
		return datumMode;
	}

	/**
	 * Set the datum mode.
	 *
	 * @param datumMode
	 *        the datum mode to set; if {@literal null} then
	 *        {@link #DEFAULT_DATUM_MODE} will be used
	 */
	public void setDatumMode(BacnetDatumMode datumMode) {
		if ( datumMode == null ) {
			datumMode = DEFAULT_DATUM_MODE;
		}
		this.datumMode = datumMode;
	}

	/**
	 * Get the datum mode as a string value.
	 *
	 * @return the datum mode value
	 */
	public String getDatumModeValue() {
		return getDatumMode().toString();
	}

	/**
	 * Set the datum mode as a string value.
	 *
	 * @param value
	 *        the value to set
	 */
	public void setDatumModeValue(String value) {
		BacnetDatumMode mode = null;
		try {
			mode = BacnetDatumMode.valueOf(value);
		} catch ( Exception e ) {
			log.warn("Unsupported BacnetDatumMode value [{}]", value);
		}
		setDatumMode(mode);
	}

}
