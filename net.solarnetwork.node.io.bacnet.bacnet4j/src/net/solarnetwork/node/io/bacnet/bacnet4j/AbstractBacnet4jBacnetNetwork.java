/* ==================================================================
 * Bacnet4jBacnetNetwork.java - 1/11/2022 6:13:09 pm
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

package net.solarnetwork.node.io.bacnet.bacnet4j;

import static java.lang.Integer.toUnsignedLong;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.cache.CachePolicies;
import com.serotonin.bacnet4j.cache.RemoteEntityCachePolicy;
import com.serotonin.bacnet4j.cache.RemoteEntityCachePolicy.TimedExpiry;
import com.serotonin.bacnet4j.event.DeviceEventListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyMultipleRequest.CovSubscriptionSpecification.CovReference;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.WriteAccessSpecification;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyReferences;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyValues;
import com.serotonin.bacnet4j.util.PropertyUtils;
import com.serotonin.bacnet4j.util.RequestUtils;
import net.solarnetwork.node.io.bacnet.BacnetConnection;
import net.solarnetwork.node.io.bacnet.BacnetCovHandler;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyCovRef;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.io.bacnet.BacnetNetwork;
import net.solarnetwork.node.io.bacnet.SimpleBacnetDeviceObjectPropertyRef;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Base implementation of {@link BacnetNetwork} for other implementations to
 * extend.
 *
 * <p>
 * This implementation is designed to work with connections that are kept open
 * for long periods of time. Each connection returned from
 * {@link #createConnection()} will be tracked internally, and if the
 * {@link #configurationChanged(Map)} method is called any open connections will
 * be automatically closed, so that clients using the connection can re-open the
 * connection with the new configuration.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public abstract class AbstractBacnet4jBacnetNetwork extends BasicIdentifiable
		implements BacnetNetwork, Bacnet4jNetworkOps, DeviceEventListener, SettingSpecifierProvider,
		SettingsChangeObserver, ServiceLifecycleObserver {

	/** The {@code deviceId} default value. */
	public static final int DEFAULT_DEVICE_ID = 1;

	/** The {@code startupDelay} property default value. */
	public static final long DEFAULT_STARTUP_DELAY = 5000L;

	/**
	 * The period at which to manage subscription re-subscribe tasks, in
	 * milliseconds.
	 */
	private static final long SUBSCRIPTION_CHECK_PERIOD = 15_000L;

	/** The subscription lifetime to use, in seconds. */
	private static final UnsignedInteger DEFAULT_SUBSCRIPTION_LIFETIME = new UnsignedInteger(120);

	private final AtomicInteger connectionCounter = new AtomicInteger(0);
	private final AtomicInteger subscriptionCounter = new AtomicInteger(0);

	// a mapping of internal connection IDs to associated connection instances
	private final ConcurrentMap<Integer, Bacnet4jBacnetConnection> connections = new ConcurrentHashMap<>(
			8, 0.9f, 2);

	// a mapping of internal subscription IDs to associated subscription instances
	private final ConcurrentMap<Integer, CovSubscription> covSubscriptions = new ConcurrentHashMap<>(8,
			0.9f, 2);

	// a mapping of BACnet subscription process identifiers to associated internal subscription instances
	private final ConcurrentMap<Integer, CovSubscription> bacnetCovSubscriptions = new ConcurrentHashMap<>(
			8, 0.9f, 2);

	// a list of BACnet COV handlers
	private final Set<BacnetCovHandler> covHandlers = new CopyOnWriteArraySet<>();

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private TaskScheduler taskScheduler;
	private int deviceId = DEFAULT_DEVICE_ID;
	private int timeout = Transport.DEFAULT_TIMEOUT;
	private int segmentTimeout = Transport.DEFAULT_SEG_TIMEOUT;
	private int segmentWindow = Transport.DEFAULT_SEG_WINDOW;
	private int retries = Transport.DEFAULT_RETRIES;
	private long startupDelay = DEFAULT_STARTUP_DELAY;
	private String applicationSoftwareVersion;
	private UnsignedInteger subscriptionLifetime = DEFAULT_SUBSCRIPTION_LIFETIME;

	private Future<?> startupFuture;
	private ScheduledFuture<?> subscriptionFuture;

	/** The local BACnet device. */
	private LocalDevice localDevice;

	/**
	 * Constructor.
	 */
	public AbstractBacnet4jBacnetNetwork() {
		super();
	}

	@Override
	public void serviceDidStartup() {
		configurationChanged(null);
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( startupFuture != null ) {
			startupFuture.cancel(true);
			startupFuture = null;
		}
		if ( subscriptionFuture != null ) {
			subscriptionFuture.cancel(true);
			subscriptionFuture = null;
		}
		closeAllConnections();
		if ( localDevice != null ) {
			localDevice.getEventHandler().removeListener(this);
			localDevice.terminate();
		}
		connections.clear();
		covSubscriptions.clear();
		bacnetCovSubscriptions.clear();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		final Future<?> f = this.startupFuture;
		serviceDidShutdown();
		Runnable task = () -> {
			synchronized ( AbstractBacnet4jBacnetNetwork.this ) {
				localDevice();
			}
		};
		final TaskScheduler scheduler = getTaskScheduler();
		if ( scheduler != null ) {
			if ( f == null ) {
				log.info("Initializing BACnet network {} in {}ms", getNetworkDescription(),
						startupDelay);
			}
			startupFuture = scheduler.schedule(task,
					Instant.ofEpochMilli(System.currentTimeMillis() + startupDelay));
		} else {
			task.run();
		}
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = basicIdentifiableSettings();
		results.add(new BasicTextFieldSettingSpecifier("deviceId", String.valueOf(DEFAULT_DEVICE_ID)));
		results.add(new BasicTextFieldSettingSpecifier("timeout",
				String.valueOf(Transport.DEFAULT_TIMEOUT)));
		results.add(new BasicTextFieldSettingSpecifier("segmentWindow",
				String.valueOf(Transport.DEFAULT_SEG_WINDOW)));
		results.add(new BasicTextFieldSettingSpecifier("segmentTimeout",
				String.valueOf(Transport.DEFAULT_SEG_TIMEOUT)));
		results.add(new BasicTextFieldSettingSpecifier("retries",
				String.valueOf(Transport.DEFAULT_RETRIES)));
		results.add(new BasicTextFieldSettingSpecifier("subscriptionLifetimeSeconds",
				String.valueOf(DEFAULT_SUBSCRIPTION_LIFETIME.longValue())));
		return results;
	}

	@Override
	public void setCachePolicy(Collection<BacnetDeviceObjectPropertyRef> refs, long cacheMs) {
		if ( refs == null || refs.isEmpty() ) {
			return;
		}
		LocalDevice device = localDevice();
		if ( device == null ) {
			return;
		}
		CachePolicies policies = localDevice.getCachePolicies();
		RemoteEntityCachePolicy policy = (cacheMs < 1 ? RemoteEntityCachePolicy.NEVER_CACHE
				: cacheMs == Long.MAX_VALUE ? RemoteEntityCachePolicy.NEVER_EXPIRE
						: new TimedExpiry(Duration.ofMillis(cacheMs)));
		for ( BacnetDeviceObjectPropertyRef ref : refs ) {
			policies.putPropertyPolicy(ref.getDeviceId(),
					new ObjectIdentifier(ref.getObjectType(), ref.getObjectNumber()),
					PropertyIdentifier.forId(ref.getPropertyId()), policy);
		}
	}

	@Override
	public synchronized BacnetConnection createConnection() {
		final LocalDevice device = localDevice();
		if ( device == null ) {
			return null;
		}
		final Integer id = connectionCounter.incrementAndGet();
		final Bacnet4jBacnetConnection conn = new Bacnet4jBacnetConnection(id, this, device);
		connections.put(id, conn);
		return conn;
	}

	private synchronized LocalDevice localDevice() {
		if ( localDevice == null ) {
			localDevice = createLocalDevice();
		}
		if ( localDevice != null ) {
			if ( !localDevice.isInitialized() ) {
				try {
					localDevice.initialize();
				} catch ( Exception e ) {
					log.error("Error initializing BACnet network {}: {}", getNetworkDescription(),
							e.toString());
				}
			}
		}
		return localDevice;
	}

	private LocalDevice createLocalDevice() {
		Network network = createNetwork();
		if ( network == null ) {
			return null;
		}

		Transport transport = new DefaultTransport(network);
		transport.setTimeout(timeout);
		transport.setSegTimeout(segmentTimeout);
		transport.setSegWindow(segmentWindow);
		transport.setRetries(retries);

		LocalDevice device = new LocalDevice(deviceId, transport);
		device.writePropertyInternal(PropertyIdentifier.objectName,
				new CharacterString("SolarNode device " + deviceId));
		device.writePropertyInternal(PropertyIdentifier.modelName, new CharacterString("SolarNode"));
		device.writePropertyInternal(PropertyIdentifier.vendorName, new CharacterString("SolarNetwork"));
		if ( applicationSoftwareVersion != null ) {
			device.writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion,
					new CharacterString(applicationSoftwareVersion));
		}
		device.getEventHandler().addListener(this);
		return device;
	}

	/**
	 * Create a new network instance.
	 *
	 * @return the network, or {@literal null} if the network cannot be created
	 *         (i.e. from lack of configuration)
	 */
	protected abstract Network createNetwork();

	private void closeAllConnections() {
		for ( BacnetConnection conn : connections.values() ) {
			if ( !conn.isClosed() ) {
				try {
					log.info("Closing BACnet connection {} after network configuration change.", conn);
					conn.close();
				} catch ( Exception e ) {
					// ignore
				}
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + getNetworkDescription() + '}';
	}

	/**
	 * Get a description of this network.
	 *
	 * <p>
	 * This implementation simply calls {@code toString()} on this object.
	 * Extending classes may want to provide something more meaningful.
	 * </p>
	 *
	 * @return a description of this network
	 */
	@Override
	public String getNetworkDescription() {
		return this.toString();
	}

	@Override
	public void releaseConnection(BacnetConnection conn) {
		if ( conn instanceof Bacnet4jBacnetConnection ) {
			Bacnet4jBacnetConnection c = (Bacnet4jBacnetConnection) conn;
			connections.remove(c.getId());
		}
	}

	@Override
	public int nextSubscriptionId() {
		return subscriptionCounter.incrementAndGet();
	}

	@Override
	public void covSubscribe(int subscriptionId, Collection<BacnetDeviceObjectPropertyRef> refs,
			int maxDelay) {
		final UnsignedInteger lifetime = subscriptionLifetime;
		final CovSubscription subscription = covSubscriptions.computeIfAbsent(subscriptionId,
				k -> new CovSubscription(subscriptionId, localDevice, this, lifetime));
		final Map<Integer, Map<ObjectIdentifier, List<CovReference>>> devRefMap = deviceReferenceMap(
				refs);
		synchronized ( subscription ) {
			try {
				// first unsubscribe to any existing subscriptions
				subscription.unsubscribe(timeout);

				// then (re)subscribe
				for ( Set<CovSubscriptionIdentifier> idents : subscription.getDeviceSubscriptionIds()
						.values() ) {
					for ( CovSubscriptionIdentifier ident : idents ) {
						bacnetCovSubscriptions.remove(ident.getSubId().intValue());
					}
				}
				subscription.reset();
				subscription.expireAt(Instant.now().plusSeconds(lifetime.longValue()));
				subscription.getRefs().addAll(refs);
				subscription.getDevRefMap().putAll(devRefMap);

				for ( Entry<Integer, Map<ObjectIdentifier, List<CovReference>>> devEntry : devRefMap
						.entrySet() ) {
					final Integer deviceId = devEntry.getKey();
					RemoteDevice dev = localDevice.getRemoteDeviceBlocking(deviceId, timeout);
					ServicesSupported ss = deviceServicesSupported(dev);
					log.debug("Got device {} services supported: {}", deviceId, ss);
					CovSubscriptionType subType = null;
					if ( ss.isSubscribeCovPropertyMultiple() ) {
						subType = CovSubscriptionType.SubscribeProperties;
						subscription.subscribeCovPropertyMultiple(dev, devEntry.getValue());
					} else if ( ss.isSubscribeCovProperty() ) {
						subType = CovSubscriptionType.SubscribeProperty;
						Set<ObjectIdentifier> fallbackCovObjIds = new LinkedHashSet<>(8);
						for ( Entry<ObjectIdentifier, List<CovReference>> objEntry : devEntry.getValue()
								.entrySet() ) {
							ObjectIdentifier objId = objEntry.getKey();
							for ( CovReference cov : objEntry.getValue() ) {
								if ( cov.getCovIncrement() == null ) {
									// can't subscribe to property without an increment; fall back to whole-object COV
									fallbackCovObjIds.add(objId);
									continue;
								}
								subscription.subscribeCovProperty(dev, objId, cov);
							}
						}
						if ( !fallbackCovObjIds.isEmpty() && ss.isSubscribeCov() ) {
							// for refs where CovIncrement not provided, fall back to whole-object COV
							if ( fallbackCovObjIds.equals(devEntry.getValue().keySet()) ) {
								subType = CovSubscriptionType.SubscribeObject;
							}
							for ( ObjectIdentifier objId : fallbackCovObjIds ) {
								subscription.subscribeCov(dev, objId);
							}
						}
					} else if ( ss.isSubscribeCov() ) {
						subType = CovSubscriptionType.SubscribeObject;
						for ( ObjectIdentifier objId : devEntry.getValue().keySet() ) {
							subscription.subscribeCov(dev, objId);
						}
					} else if ( ss.isReadPropertyMultiple() ) {
						// have to poll for values :-(
						subType = CovSubscriptionType.PollProperties;
					} else if ( ss.isReadProperty() ) {
						// have to poll for values individually :-( :-( :-(
						subType = CovSubscriptionType.PollProperty;
					} else {
						throw new RemoteServiceException(String.format(
								"BACnet device %d does not support mandatory read-property service.",
								deviceId));
					}
					subscription.getDeviceSubscriptionTypes().put(deviceId, subType);
				}
			} catch ( BACnetException ex ) {
				throw new RemoteServiceException(
						String.format("Error subscribing to properties COV with subscription ID %d",
								subscriptionId),
						ex);
			}
			for ( Set<CovSubscriptionIdentifier> idents : subscription.getDeviceSubscriptionIds()
					.values() ) {
				for ( CovSubscriptionIdentifier ident : idents ) {
					bacnetCovSubscriptions.put(ident.getSubId().intValue(), subscription);
				}
			}
		}
		synchronized ( this ) {
			if ( subscriptionFuture == null ) {
				subscriptionFuture = taskScheduler.scheduleAtFixedRate(new SubscriptionResubscriber(),
						Duration.ofMillis(SUBSCRIPTION_CHECK_PERIOD));
			}
		}
	}

	@Override
	public void covUnsubscribe(int subscriptionId) {
		CovSubscription sub = covSubscriptions.remove(subscriptionId);
		if ( sub != null ) {
			try {
				sub.unsubscribe(timeout);
			} catch ( BACnetException e ) {
				log.warn("Error unsubscribing COV subscription {}: {}", subscriptionId, e.toString(), e);
			}
		}
	}

	@Override
	public void addCovHandler(BacnetCovHandler handler) {
		covHandlers.add(handler);
	}

	@Override
	public void removeCovHandler(BacnetCovHandler handler) {
		covHandlers.remove(handler);
	}

	@Override
	public Map<BacnetDeviceObjectPropertyRef, ?> propertyValues(
			Collection<BacnetDeviceObjectPropertyRef> refs) {
		if ( refs == null || refs.isEmpty() ) {
			return Collections.emptyMap();
		}

		DeviceObjectPropertyReferences q = new DeviceObjectPropertyReferences();
		for ( BacnetDeviceObjectPropertyRef ref : refs ) {
			if ( ref.hasPropertyIndex() ) {
				q.addIndex(ref.getDeviceId(), ObjectType.forId(ref.getObjectType()),
						ref.getObjectNumber(), PropertyIdentifier.forId(ref.getPropertyId()),
						ref.getPropertyIndex());
			} else {
				q.add(ref.getDeviceId(), ObjectType.forId(ref.getObjectType()), ref.getObjectNumber(),
						PropertyIdentifier.forId(ref.getPropertyId()));
			}
		}
		final Map<BacnetDeviceObjectPropertyRef, Object> result = new LinkedHashMap<>(refs.size());
		DeviceObjectPropertyValues values = PropertyUtils.readProperties(localDevice, q,
				(double progress, int deviceId, ObjectIdentifier oid, PropertyIdentifier pid,
						UnsignedInteger pin, Encodable value) -> {
					log.trace("{}% progress reading {} properties", progress / 100.0, refs.size());
					return false;
				}, timeout);
		for ( BacnetDeviceObjectPropertyRef ref : refs ) {
			Encodable val = null;
			if ( ref.hasPropertyIndex() ) {
				val = values.getIndex(ref.getDeviceId(), ObjectType.forId(ref.getObjectType()),
						ref.getObjectNumber(), PropertyIdentifier.forId(ref.getPropertyId()),
						ref.getPropertyIndex());
			} else {
				val = values.get(ref.getDeviceId(), ObjectType.forId(ref.getObjectType()),
						ref.getObjectNumber(), PropertyIdentifier.forId(ref.getPropertyId()));
			}
			Object value = decodeEncodable(val);
			result.put(ref, value);
		}
		return result;
	}

	@Override
	public Map<BacnetDeviceObjectPropertyRef, java.lang.Boolean> updatePropertyValues(
			Map<BacnetDeviceObjectPropertyRef, ?> values) {
		if ( values == null || values.isEmpty() ) {
			return Collections.emptyMap();
		}

		// group updates by device ID
		final Map<Integer, Map<BacnetDeviceObjectPropertyRef, Object>> deviceValues = new HashMap<>(
				values.size());
		for ( Entry<BacnetDeviceObjectPropertyRef, ?> e : values.entrySet() ) {
			final Integer deviceId = e.getKey().getDeviceId();
			deviceValues.computeIfAbsent(deviceId, k -> new HashMap<>(8)).put(e.getKey(), e.getValue());
		}
		final Map<BacnetDeviceObjectPropertyRef, java.lang.Boolean> result = new LinkedHashMap<>(
				values.size());
		try {
			for ( Entry<Integer, Map<BacnetDeviceObjectPropertyRef, Object>> deviceEntry : deviceValues
					.entrySet() ) {
				final Integer deviceId = deviceEntry.getKey();
				final Map<BacnetDeviceObjectPropertyRef, Object> vals = deviceEntry.getValue();
				final List<WriteAccessSpecification> specs = new ArrayList<>(vals.size());
				for ( Entry<BacnetDeviceObjectPropertyRef, Object> e : vals.entrySet() ) {
					BacnetDeviceObjectPropertyRef ref = e.getKey();
					Encodable enc = BacnetPropertyUtils.encodeValue(ref, e.getValue());
					if ( enc == null ) {
						log.warn("Unsupported write property type for {} : {}", ref, e.getValue());
						result.put(ref, false);
						continue;
					}
					PropertyValue val = new PropertyValue(PropertyIdentifier.forId(ref.getPropertyId()),
							ref.hasPropertyIndex()
									? new UnsignedInteger(Integer.toUnsignedLong(ref.getPropertyIndex()))
									: null,
							enc, ref.hasPriority() ? new UnsignedInteger(ref.getPriority()) : null);
					WriteAccessSpecification spec = new WriteAccessSpecification(
							new ObjectIdentifier(ref.getObjectType(), ref.getObjectNumber()),
							new SequenceOf<>(val));
					specs.add(spec);
				}
				RemoteDevice dev = localDevice.getRemoteDeviceBlocking(deviceId, timeout);
				ServicesSupported ss = deviceServicesSupported(dev);
				log.debug("Got device {} services supported: {}", deviceId, ss);
				if ( ss.isWritePropertyMultiple() || ss.isWriteProperty() ) {
					RequestUtils.writeProperties(localDevice, dev, specs);
				} else {
					for ( Entry<BacnetDeviceObjectPropertyRef, Object> e : vals.entrySet() ) {
						log.error(
								"BACnet device {} does not support write-property or write-property-multiple services, cannot set control value {} to {}",
								deviceId, e.getKey(), e.getValue());
						result.put(e.getKey(), false);
					}
				}
			}
			for ( BacnetDeviceObjectPropertyRef ref : values.keySet() ) {
				result.putIfAbsent(ref, true);
			}
		} catch ( BACnetException e ) {
			log.error("Error writing properties {}: {}", values, e.toString());
			// TODO: extract which properties failed; for now report that all failed
			for ( BacnetDeviceObjectPropertyRef ref : values.keySet() ) {
				result.put(ref, false);
			}
		}
		return result;
	}

	private Object decodeEncodable(Encodable val) {
		Object value = BacnetPropertyUtils.numberValue(val);
		if ( value == null ) {
			value = BacnetPropertyUtils.bitSetValue(val);
			if ( value == null ) {
				value = BacnetPropertyUtils.stringValue(val);
			}
		}
		return value;
	}

	private Map<Integer, Map<ObjectIdentifier, List<CovReference>>> deviceReferenceMap(
			Collection<BacnetDeviceObjectPropertyRef> refs) {
		Map<Integer, Map<ObjectIdentifier, List<CovReference>>> devRefMap = new LinkedHashMap<>(
				refs.size());
		for ( BacnetDeviceObjectPropertyRef ref : refs ) {
			Map<ObjectIdentifier, List<CovReference>> refMap = devRefMap
					.computeIfAbsent(ref.getDeviceId(), k -> new LinkedHashMap<>(8));
			List<CovReference> l = refMap.computeIfAbsent(
					new ObjectIdentifier(ref.getObjectType(), ref.getObjectNumber()),
					k -> new ArrayList<>(8));
			Real covIncrement = null;
			if ( ref instanceof BacnetDeviceObjectPropertyCovRef ) {
				BacnetDeviceObjectPropertyCovRef covRef = (BacnetDeviceObjectPropertyCovRef) ref;
				if ( covRef.getCovIncrement() != null ) {
					covIncrement = new Real(covRef.getCovIncrement());
				}
			}
			l.add(new CovReference(new PropertyReference(PropertyIdentifier.forId(ref.getPropertyId()),
					(ref.hasPropertyIndex() ? new UnsignedInteger(toUnsignedLong(ref.getPropertyIndex()))
							: null)),
					covIncrement, Boolean.FALSE));
		}
		return devRefMap;
	}

	private ServicesSupported deviceServicesSupported(RemoteDevice dev) {
		final int deviceId = dev.getInstanceNumber();
		final ObjectIdentifier objId = new ObjectIdentifier(ObjectType.device, deviceId);
		DeviceObjectPropertyReferences servicesSupported = new DeviceObjectPropertyReferences();
		servicesSupported.add(deviceId, objId, PropertyIdentifier.protocolServicesSupported);
		DeviceObjectPropertyValues vals = PropertyUtils.readProperties(localDevice, servicesSupported,
				null, timeout);
		Encodable propVal = vals.get(deviceId, objId, PropertyIdentifier.protocolServicesSupported);
		if ( propVal instanceof ServicesSupported ) {
			ServicesSupported supported = (ServicesSupported) propVal;
			return supported;
		} else if ( propVal instanceof ErrorClassAndCode ) {
			ErrorClassAndCode err = (ErrorClassAndCode) propVal;
			String msg = String.format("Error getting device %d services supported: %s", deviceId, err);
			log.error(msg);
			throw new RemoteServiceException(msg);
		}
		String msg = String.format("Error getting device %d services supported: %s", deviceId, propVal);
		log.error(msg);
		throw new RemoteServiceException(msg);
	}

	private boolean shouldResubscribe(CovSubscription subscription) {
		Instant expires = subscription.getExpires();
		return expires == null || Instant.now().plusMillis(SUBSCRIPTION_CHECK_PERIOD)
				.isAfter(subscription.getExpires());
	}

	private final class SubscriptionResubscriber implements Runnable {

		@Override
		public void run() {
			for ( CovSubscription subscription : covSubscriptions.values() ) {
				synchronized ( subscription ) {
					if ( shouldResubscribe(subscription) ) {
						try {
							subscription.resubscribe(timeout);
						} catch ( Exception e ) {
							log.error("Error managing COV re-subscriptions: {}", e.toString(), e);
						}
					}
				}
			}
		}

	}

	@Override
	public void listenerException(Throwable e) {
		log.error("BACnet [{}] listener exception: {}", getNetworkDescription(), e.toString(), e);
	}

	@Override
	public void iAmReceived(RemoteDevice d) {
		// nothing here
	}

	@Override
	public boolean allowPropertyWrite(Address from, BACnetObject obj, PropertyValue pv) {
		return false;
	}

	@Override
	public void propertyWritten(Address from, BACnetObject obj, PropertyValue pv) {
		// nothing here
	}

	@Override
	public void iHaveReceived(RemoteDevice d, RemoteObject o) {
		// nothing here
	}

	@Override
	public void covNotificationReceived(UnsignedInteger subscriberProcessIdentifier,
			ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier monitoredObjectIdentifier,
			UnsignedInteger timeRemaining, SequenceOf<PropertyValue> listOfValues) {
		log.trace("COV notification received for subscription {} object {} remaining time {}: {}",
				subscriberProcessIdentifier, monitoredObjectIdentifier, timeRemaining, listOfValues);
		final int subscriptionId = subscriberProcessIdentifier.intValue();
		final CovSubscription subscription = bacnetCovSubscriptions.get(subscriptionId);
		if ( subscription != null ) {
			Map<BacnetDeviceObjectPropertyRef, Object> updates = new LinkedHashMap<>(8);
			for ( PropertyValue val : listOfValues ) {
				UnsignedInteger propArrayIndex = null;
				Set<CovSubscriptionIdentifier> idents = subscription.getDeviceSubscriptionIds()
						.get(initiatingDeviceIdentifier.getInstanceNumber());
				for ( CovSubscriptionIdentifier ident : idents ) {
					if ( subscriberProcessIdentifier.equals(ident.getSubId())
							&& monitoredObjectIdentifier.equals(ident.getObjId()) ) {
						if ( ident.getSubType() == CovSubscriptionType.SubscribeProperty
								&& val.getPropertyIdentifier()
										.equals(ident.getPropRef().getPropertyIdentifier()) ) {
							propArrayIndex = ident.getPropRef().getPropertyArrayIndex();
						}
						break;
					}
				}
				updates.put(
						new SimpleBacnetDeviceObjectPropertyRef(
								initiatingDeviceIdentifier.getInstanceNumber(),
								monitoredObjectIdentifier.getObjectType().intValue(),
								monitoredObjectIdentifier.getInstanceNumber(),
								val.getPropertyIdentifier().intValue(),
								propArrayIndex != null ? propArrayIndex.intValue()
										: BacnetDeviceObjectPropertyRef.NOT_INDEXED,
								val.getPriority() != null ? val.getPriority().intValue()
										: BacnetDeviceObjectPropertyRef.NO_PRIORITY),
						decodeEncodable(val.getValue()));
				localDevice.setCachedRemoteProperty(initiatingDeviceIdentifier.getInstanceNumber(),
						monitoredObjectIdentifier, val.getPropertyIdentifier(), propArrayIndex,
						val.getValue());
			}
			for ( BacnetCovHandler handler : covHandlers ) {
				handler.accept(subscription.getId(), updates);
			}
		}
	}

	@Override
	public void eventNotificationReceived(UnsignedInteger processIdentifier,
			ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier eventObjectIdentifier,
			TimeStamp timeStamp, UnsignedInteger notificationClass, UnsignedInteger priority,
			EventType eventType, CharacterString messageText, NotifyType notifyType, Boolean ackRequired,
			EventState fromState, EventState toState, NotificationParameters eventValues) {
		// nothing here
	}

	@Override
	public void textMessageReceived(ObjectIdentifier textMessageSourceDevice, Choice messageClass,
			MessagePriority messagePriority, CharacterString message) {
		// nothing here
	}

	@Override
	public void synchronizeTime(Address from, DateTime dateTime, boolean utc) {
		// nothing here
	}

	@Override
	public void requestReceived(Address from, Service service) {
		// nothing here
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
	 * Set the network timeout.
	 *
	 * @return the timeout, in milliseconds
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Set the network timeout.
	 *
	 * @param timeout
	 *        the timeout to set, in milliseconds
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Get the network segment timeout.
	 *
	 * @return the segment timeout, in milliseconds
	 */
	public int getSegmentTimeout() {
		return segmentTimeout;
	}

	/**
	 * Set the network segment timeout.
	 *
	 * @param segmentTimeout
	 *        the segment timeout to set, in milliseconds
	 */
	public void setSegmentTimeout(int segmentTimeout) {
		this.segmentTimeout = segmentTimeout;
	}

	/**
	 * Get the network segment window.
	 *
	 * @return the segment window
	 */
	public int getSegmentWindow() {
		return segmentWindow;
	}

	/**
	 * Set the network segment window.
	 *
	 * @param segmentWindow
	 *        the segment window to set
	 */
	public void setSegmentWindow(int segmentWindow) {
		this.segmentWindow = segmentWindow;
	}

	/**
	 * Get the network retry count.
	 *
	 * @return the retries
	 */
	public int getRetries() {
		return retries;
	}

	/**
	 * Set the network retry count.
	 *
	 * @param retries
	 *        the retries to set
	 */
	public void setRetries(int retries) {
		this.retries = retries;
	}

	/**
	 * Get the device ID.
	 *
	 * @return the device ID
	 */
	public int getDeviceId() {
		return deviceId;
	}

	/**
	 * Set the device ID to use.
	 *
	 * @param deviceId
	 *        the device ID to set
	 */
	public void setDeviceId(int deviceId) {
		this.deviceId = deviceId;
	}

	/**
	 * Get the startup delay.
	 *
	 * @return the startup delay, in milliseconds
	 */
	public long getStartupDelay() {
		return startupDelay;
	}

	/**
	 * Set the startup delay.
	 *
	 * @param startupDelay
	 *        the startup delay to set, in milliseconds
	 */
	public void setStartupDelay(long startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * Get the application software version.
	 *
	 * @return the application software version
	 */
	public String getApplicationSoftwareVersion() {
		return applicationSoftwareVersion;
	}

	/**
	 * Set the application software version.
	 *
	 * @param applicationSoftwareVersion
	 *        the application software version to set
	 */
	public void setApplicationSoftwareVersion(String applicationSoftwareVersion) {
		this.applicationSoftwareVersion = applicationSoftwareVersion;
	}

	/**
	 * Get the subscription lifetime, in seconds.
	 *
	 * @return the subscription lifetime seconds
	 * @since 1.1
	 */
	public long getSubscriptionLifetimeSeconds() {
		return subscriptionLifetime.longValue();
	}

	/**
	 * Set the subscription lifetime, in seconds.
	 *
	 * @param subscriptionLifetimeSeconds
	 *        the subscription lifetime seconds to set
	 * @since 1.1
	 */
	public void setSubscriptionLifetimeSeconds(long subscriptionLifetimeSeconds) {
		this.subscriptionLifetime = new UnsignedInteger(subscriptionLifetimeSeconds);
	}

}
