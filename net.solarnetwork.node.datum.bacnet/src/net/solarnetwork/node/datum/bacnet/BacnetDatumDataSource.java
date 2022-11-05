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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.io.bacnet.BacnetConnection;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.io.bacnet.BacnetNetwork;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generic BACnet device datum data source.
 * 
 * @author matt
 * @version 1.0
 */
public class BacnetDatumDataSource extends DatumDataSourceSupport implements DatumDataSource,
		SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver {

	/** The {@code connectionCheckFrequency} property default value. */
	public static final long DEFAULT_CONNECTION_CHECK_FREQUENCY = 60_000L;

	/** The {@code reconnectDelay} property default value. */
	public static final long DEFAULT_RECONNECT_DELAY = 10_000L;

	private final OptionalService<BacnetNetwork> bacnetNetwork;
	private String sourceId;
	private long sampleCacheMs;
	private BacnetDeviceConfig[] deviceConfigs;

	private BacnetConnection connection;
	private ScheduledFuture<?> connectionCheckFuture;
	private long connectionCheckFrequency = DEFAULT_CONNECTION_CHECK_FREQUENCY;
	private long reconnectDelay = DEFAULT_RECONNECT_DELAY;

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
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.bacnet";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = basicIdentifiableSettings();
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier("bacnetNetworkUid", null));

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
			List<BacnetDeviceObjectPropertyRef> props = propertyRefs();
			if ( !props.isEmpty() ) {
				conn.covSubscribe(props, 5); // TODO maxDelay setting
			}
			connection = conn;
		}
		return conn;
	}

	protected synchronized void closeConnection() {
		if ( connection != null ) {
			log.info("BACnet connection closed for {}", getBacnetNetworkUid());
			// TODO: unsubscribe
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
					new Date(System.currentTimeMillis() + 10000L), connectionCheckFrequency);
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

	private List<BacnetDeviceObjectPropertyRef> propertyRefs() {
		final BacnetDeviceConfig[] deviceConfs = getDeviceConfigs();
		if ( deviceConfs == null || deviceConfs.length < 1 ) {
			return Collections.emptyList();
		}
		List<BacnetDeviceObjectPropertyRef> results = new ArrayList<>();
		for ( BacnetDeviceConfig deviceConf : deviceConfs ) {
			if ( !deviceConf.isValid() ) {
				continue;
			}
			for ( BacnetPropertyConfig propConf : deviceConf.getPropConfigs() ) {
				if ( !propConf.isValid() ) {
					continue;
				}
				results.add(propConf.toRef(deviceConf.getDeviceId()));
			}
		}
		return results;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		final String sourceId = resolvePlaceholders(getSourceId());
		if ( sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		BacnetNetwork network = OptionalService.service(bacnetNetwork);
		if ( network == null ) {
			return null;
		}
		try (BacnetConnection conn = network.createConnection()) {
			// TODO: something
			log.debug("Working with BacnetConnection {}", conn);
		} catch ( IOException e ) {
			log.warn("Communication error collecting source {} from BACnet network {}", sourceId,
					network);
		}
		// TODO
		return null;
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
}
