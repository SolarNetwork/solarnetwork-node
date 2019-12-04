/* ==================================================================
 * CanbusDatumDataSourceSupport.java - 24/09/2019 8:54:44 pm
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

package net.solarnetwork.node.io.canbus.support;

import static java.util.stream.Collectors.toSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.measure.Quantity;
import javax.measure.Unit;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;
import net.solarnetwork.node.io.canbus.CanbusNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.support.ServiceLifecycleObserver;
import net.solarnetwork.util.OptionalService;

/**
 * A base helper class to support {@link CanbusNetwork} based
 * {@link DatumDataSource} implementations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class CanbusDatumDataSourceSupport extends DatumDataSourceSupport
		implements SettingsChangeObserver, ServiceLifecycleObserver, CanbusFrameListener {

	/** The default value for the {@code connectionCheckFrequency} property. */
	public static final long DEFAULT_CONNECTION_CHECK_FREQUENCY = 60000L;

	private final AtomicReference<CanbusConnection> connection = new AtomicReference<CanbusConnection>();
	private final AtomicReference<CanbusFrameListener> monitor = new AtomicReference<CanbusFrameListener>();
	private final ConcurrentMap<Integer, CanbusSubscription> subscriptions = new ConcurrentHashMap<>(16,
			0.9f, 1);

	private OptionalService<CanbusNetwork> canbusNetwork;
	private String busName;
	private TaskScheduler taskScheduler;
	private long connectionCheckFrequency = DEFAULT_CONNECTION_CHECK_FREQUENCY;
	private MeasurementHelper measurementHelper;

	private ScheduledFuture<?> connectionCheckFuture;

	/**
	 * Get setting specifiers for the
	 * {@literal canbusNetwork.propertyFilters['UID']} and {@literal busName}
	 * properties.
	 * 
	 * @return list of setting specifiers
	 */
	public static List<SettingSpecifier> canbusDatumDataSourceSettingSpecifiers(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "canbusNetwork.propertyFilters['UID']", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "busName", ""));
		return results;
	}

	private synchronized void rescheduleConnectionCheck() {
		if ( connectionCheckFuture != null ) {
			connectionCheckFuture.cancel(true);
			connectionCheckFuture = null;
		}
		if ( taskScheduler != null && connectionCheckFuture == null ) {
			log.info("Scheduling CAN bus [{}] connectivity check for {}ms", busName,
					connectionCheckFrequency);
			connectionCheckFuture = taskScheduler.scheduleWithFixedDelay(new ConnectionCheck(),
					new Date(System.currentTimeMillis() + 10000L), connectionCheckFrequency);
		}
	}

	/**
	 * Callback after properties have been changed.
	 * 
	 * <p>
	 * This method closes the shared connection if it is open, so that it is
	 * re-opened with the updated configuration.
	 * </p>
	 * 
	 * @param properties
	 *        the changed properties
	 */
	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		rescheduleConnectionCheck();
		closeSharedCanbusConnection();
	}

	@Override
	public synchronized void serviceDidStartup() {
		rescheduleConnectionCheck();
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( connectionCheckFuture != null ) {
			connectionCheckFuture.cancel(true);
			connectionCheckFuture = null;
		}
		closeSharedCanbusConnection();
	}

	/**
	 * Completely replace all subscriptions to be applied automatically to
	 * connections opened by this class.
	 * 
	 * <p>
	 * Any existing subscriptions will be unsubscribed. If a monitor has been
	 * registered already via {@link #registerMonitor(CanbusFrameListener)} then
	 * these subscriptions will still be registered, but not activated. When
	 * {@link #unregisterMonitor()} is called, the subscriptions will then be
	 * applied.
	 * </p>
	 * 
	 * @param subscriptions
	 *        the subscriptions to apply
	 * @throws IOException
	 *         if the CAN bus connection is currently open and the subscription
	 *         cannot be applied
	 */
	protected synchronized void configureSubscriptions(Iterable<CanbusSubscription> subscriptions)
			throws IOException {
		Set<Integer> subscribedAddresses = new HashSet<>(8);
		CanbusConnection conn = connection.get();
		for ( CanbusSubscription sub : subscriptions ) {
			subscribedAddresses.add(sub.getAddress());
			registerSubscription(conn, sub);
		}
		Set<Integer> toRemove = this.subscriptions.keySet().stream()
				.filter(k -> !subscribedAddresses.contains(k)).collect(toSet());
		for ( Integer k : toRemove ) {
			CanbusSubscription old = this.subscriptions.remove(k);
			if ( conn != null && !conn.isClosed() && !conn.isMonitoring() ) {
				try {
					conn.unsubscribe(old.getAddress(), old.isForceExtendedAddress());
				} catch ( IOException e ) {
					log.warn("Error unsubsubscribing from CAN bus {}: {}", getBusName(), old);
				}
			}
		}
	}

	/**
	 * Register a subscription to be applied automatically to connections opened
	 * by this class.
	 * 
	 * @param subscription
	 *        the subscription to register
	 * @throws IOException
	 *         if the CAN bus connection is currently open and the subscription
	 *         cannot be applied
	 */
	protected synchronized void registerSubscription(CanbusSubscription subscription)
			throws IOException {
		registerSubscription(connection.get(), subscription);
	}

	/**
	 * Register a monitor to be applied automatically when connections are
	 * opened by this class.
	 * 
	 * @param listener
	 *        the listener
	 * @throws IOException
	 *         if the CAN bus monitor cannot be applied
	 */
	protected synchronized void registerMonitor(CanbusFrameListener listener) throws IOException {
		CanbusFrameListener curr = monitor.get();
		if ( curr != listener && monitor.compareAndSet(curr, listener) ) {
			CanbusConnection conn = connection.get();
			if ( conn != null ) {
				conn.monitor(listener);
			}
		}
	}

	/**
	 * Unregister a monitor previously added via
	 * {@link #registerMonitor(CanbusFrameListener)}.
	 * 
	 * @throws IOException
	 *         if the monitor cannot be unregistered
	 */
	protected synchronized void unregisterMonitor() throws IOException {
		CanbusFrameListener curr = monitor.get();
		if ( curr != null && monitor.compareAndSet(curr, null) ) {
			CanbusConnection conn = connection.get();
			if ( conn != null ) {
				conn.unmonitor();
			}
		}
	}

	private synchronized void registerSubscription(CanbusConnection conn,
			CanbusSubscription subscription) throws IOException {
		CanbusSubscription old = subscriptions.put(subscription.getAddress(), subscription);
		if ( conn != null && !conn.isClosed() && subscription != old ) {
			if ( old != null && !conn.isMonitoring() ) {
				conn.unsubscribe(old.getAddress(), old.isForceExtendedAddress());
			}
			applySubscription(conn, subscription);
		}
	}

	private synchronized void applySubscriptions(CanbusConnection conn) throws IOException {
		CanbusFrameListener mon = monitor.get();
		if ( mon != null ) {
			conn.monitor(mon);
		} else {
			for ( CanbusSubscription sub : subscriptions.values() ) {
				applySubscription(conn, sub);
			}
		}
	}

	private synchronized void applySubscription(CanbusConnection conn, CanbusSubscription sub)
			throws IOException {
		if ( conn.isMonitoring() ) {
			// don't add/remove while monitoring
			log.debug("Not applying CAN bus {} subscription {} because in monitoring mode.",
					getBusName(), sub);
			return;
		}
		log.info("Applying registered subscription on CAN bus {}: {}", getBusName(), sub);
		if ( sub.isMultiplexFilter() ) {
			conn.subscribe(sub.getAddress(), sub.isForceExtendedAddress(), sub.getLimit(),
					sub.getDataFilter(), sub.getDataFilters(), sub.getListener());
		} else {
			conn.subscribe(sub.getAddress(), sub.isForceExtendedAddress(), sub.getLimit(),
					sub.getDataFilter(), sub.getListener());
		}
	}

	/**
	 * Get an <b>open</b> CAN bus connection, creating and opening a new
	 * connection if necessary.
	 * 
	 * <p>
	 * An existing shared connection is returned if possible, but should be
	 * treated like a normal, non-shared connection by calling
	 * {@link CanbusConnection#close()} when finished using it.
	 * </p>
	 * 
	 * @return the open connection, or {@literal null} if the connection could
	 *         not be created
	 */
	protected CanbusConnection canbusConnection() {
		CanbusConnection conn = sharedCanbusConnection();
		return (conn != null ? new NonClosingCanbusConnection(conn) : null);
	}

	@SuppressWarnings("resource")
	private synchronized CanbusConnection sharedCanbusConnection() {
		CanbusConnection curr, newConn = null;
		do {
			if ( newConn != null && !newConn.isClosed() ) {
				// previous compareAndSet failed, so close and re-try
				try {
					newConn.close();
				} catch ( Exception e ) {
					// ignore
				}
			}
			curr = connection.get();
			if ( curr != null && !curr.isClosed() ) {
				return curr;
			}

			CanbusNetwork network = canbusNetwork();
			if ( network == null ) {
				log.info("No CanbusNetwork available; cannot open connection to bus {}", getBusName());
				return null;
			}
			newConn = network.createConnection(getBusName());
			if ( newConn == null ) {
				return null;
			}
			try {
				newConn.open();
				applySubscriptions(newConn);
			} catch ( Exception e ) {
				log.error("Error opening CAN bus connection {}: {}", canbusNetworkName(), e.toString());
				return null;
			}
		} while ( !connection.compareAndSet(curr, newConn) );
		return newConn;
	}

	/**
	 * Close the shared {@link CanbusConnection} if possible.
	 */
	private synchronized void closeSharedCanbusConnection() {
		CanbusConnection conn = connection.get();
		if ( conn != null && !conn.isClosed() ) {
			try {
				conn.close();
			} catch ( Exception e ) {
				log.warn("Error closing CAN bus connection {}: {}", canbusNetworkName(), e.toString());
			} finally {
				connection.compareAndSet(conn, null);
			}
		}
	}

	private class ConnectionCheck implements Runnable {

		@Override
		public void run() {
			try {
				CanbusConnection conn = sharedCanbusConnection();
				if ( conn == null ) {
					log.info("No CAN bus connection available to {} (missing configuration?)",
							canbusNetworkName());
				} else {
					Future<Boolean> verification = conn.verifyConnectivity();
					Boolean result = verification.get(connectionCheckFrequency, TimeUnit.MILLISECONDS);
					if ( result != null && result.booleanValue() ) {
						log.info("Verified CAN bus connectivity to {}", canbusNetworkName());
					} else {
						log.warn("Failed to verify CAN bus connectivity to {}; closing connection now",
								canbusNetworkName());
						closeSharedCanbusConnection();
					}
				}
			} catch ( Exception e ) {
				log.error("Error checking CAN bus connection to {}: {}", canbusNetworkName(),
						e.toString());
			}
		}

	}

	/**
	 * Get a {@link Unit} instance from a unit string.
	 * 
	 * @param unit
	 *        the unit string
	 * @return the {@code Unit} instance, or {@literal null} if an instance
	 *         cannot be resolved, either from the lack of a
	 *         {@link #getMeasurementHelper()} or an unsupported unit string
	 *         value
	 */
	protected Unit<?> unitValue(String unit) {
		if ( unit == null || unit.isEmpty() ) {
			return null;
		}
		MeasurementHelper helper = getMeasurementHelper();
		if ( helper != null ) {
			return helper.unitValue(unit);
		}
		return null;
	}

	/**
	 * Get a "normalized" unit for an arbitrary unit.
	 * 
	 * @param unit
	 *        the unit to normalize
	 * @return the normalized unit, or {@literal null} if {@code unit} is
	 *         {@literal null}
	 */
	protected Unit<?> normalizedUnitValue(Unit<?> unit) {
		if ( unit == null ) {
			return null;
		}
		MeasurementHelper helper = getMeasurementHelper();
		if ( helper != null ) {
			return helper.normalizedUnit(unit);
		}
		return unit;
	}

	/**
	 * Get a "normalized" amount for an arbitrary amount.
	 * 
	 * @param amount
	 *        the amount
	 * @param unit
	 *        the amount's unit
	 * @param slope
	 *        a slope factor
	 * @param intercept
	 *        an intercept factor
	 * @return the amount, or {@literal null} if {@code amount} is
	 *         {@literal null}
	 */
	protected Number normalizedAmountValue(Number amount, String unit, Number slope, Number intercept) {
		if ( amount == null || unit == null ) {
			return amount;
		}
		MeasurementHelper helper = getMeasurementHelper();
		if ( helper == null ) {
			return amount;
		}
		Quantity<?> q = helper.quantityValue(amount, unit, slope, intercept);
		if ( q == null ) {
			return amount;
		}
		Quantity<?> n = helper.normalizedQuantity(q);
		if ( n == null ) {
			return amount;
		}
		return n.getValue();
	}

	/**
	 * Format a {@link Unit} instance as a unit string.
	 * 
	 * @param unit
	 *        the unit to format
	 * @return the formatted string, or {@literal null} if {@code unit} is
	 *         {@literal null}
	 */
	protected String formattedUnitValue(Unit<?> unit) {
		MeasurementHelper helper = getMeasurementHelper();
		if ( helper != null ) {
			return helper.formatUnit(unit);
		}
		return (unit != null ? unit.toString() : null);
	}

	/**
	 * Get the configured CAN bus network name.
	 * 
	 * @return the CAN bus network name
	 */
	public String canbusNetworkName() {
		return getBusName() + "@" + canbusNetwork();
	}

	/**
	 * Get the {@link CanbusNetwork} from the configured {@code canbusNetwork}
	 * service, or {@literal null} if not available or not configured.
	 * 
	 * @return the CanbusNetwork or {@literal null}
	 */
	protected final CanbusNetwork canbusNetwork() {
		return (canbusNetwork == null ? null : canbusNetwork.service());
	}

	/**
	 * Get the CAN bus network.
	 * 
	 * @return the network
	 */
	public OptionalService<CanbusNetwork> getCanbusNetwork() {
		return canbusNetwork;
	}

	/**
	 * Set the CAN bus network.
	 * 
	 * @param canbusNetwork
	 *        the network
	 */
	public void setCanbusNetwork(OptionalService<CanbusNetwork> canbusNetwork) {
		this.canbusNetwork = canbusNetwork;
	}

	/**
	 * Get the CAN bus name to use.
	 * 
	 * @return the CAN bus name
	 */
	public String getBusName() {
		return busName;
	}

	/**
	 * Set the CAN bus name to use.
	 * 
	 * @param busName
	 *        the CAN bus name
	 * @throws IllegalArgumentException
	 *         if {@code busName} is {@literal null} or empty
	 */
	public void setBusName(String busName) {
		if ( busName == null || busName.isEmpty() ) {
			throw new IllegalArgumentException("The CAN bus name must be provided.");
		}
		this.busName = busName;
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
	 *        the task scheduler
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
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
	 * Get the measurement helper.
	 * 
	 * @return the measurement helper, or {@literal null}
	 */
	public MeasurementHelper getMeasurementHelper() {
		return measurementHelper;
	}

	/**
	 * Set the measurement helper.
	 * 
	 * @param measurementHelper
	 *        the measurement helper to set
	 */
	public void setMeasurementHelper(MeasurementHelper measurementHelper) {
		this.measurementHelper = measurementHelper;
	}

}
