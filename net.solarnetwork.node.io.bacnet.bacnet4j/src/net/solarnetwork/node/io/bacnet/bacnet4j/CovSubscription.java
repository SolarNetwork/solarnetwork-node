/* ==================================================================
 * CovSubscription.java - 6/11/2022 9:26:19 am
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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.ResponseConsumer;
import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.apdu.AckAPDU;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyMultipleRequest.CovSubscriptionSpecification;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyMultipleRequest.CovSubscriptionSpecification.CovReference;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyRequest;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVRequest;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;

/**
 * Internal COV subscription tracker.
 * 
 * @author matt
 * @version 1.0
 */
public class CovSubscription {

	private static final Logger log = LoggerFactory.getLogger(CovSubscription.class);

	private final int id;
	private final LocalDevice localDevice;
	private final UnsignedInteger lifetime;
	private final Bacnet4jNetworkOps networkOps;
	private final Map<Integer, CovSubscriptionType> deviceSubscriptionTypes;
	private final Map<Integer, Set<CovSubscriptionIdentifier>> deviceSubscriptionIds;
	private final List<BacnetDeviceObjectPropertyRef> refs;
	private final Map<Integer, Map<ObjectIdentifier, List<CovReference>>> devRefMap;
	private Instant expires;

	public CovSubscription(int id, LocalDevice localDevice, Bacnet4jNetworkOps networkOps,
			UnsignedInteger lifetime) {
		super();
		this.id = id;
		this.localDevice = requireNonNullArgument(localDevice, "localDevice");
		this.networkOps = requireNonNullArgument(networkOps, "networkOps");
		this.lifetime = requireNonNullArgument(lifetime, "lifetime");
		this.deviceSubscriptionTypes = new HashMap<>();
		this.deviceSubscriptionIds = new HashMap<>();
		this.refs = new ArrayList<>();
		this.devRefMap = new HashMap<>(8);
	}

	/**
	 * Reset all subscription state.
	 */
	public void reset() {
		deviceSubscriptionTypes.clear();
		deviceSubscriptionIds.clear();
		refs.clear();
		devRefMap.clear();
		expires = null;
	}

	/**
	 * Un-subscribe to all COV subscriptions.
	 * 
	 * @param timeout
	 *        the network timeout, in milliseconds
	 * @throws BACnetException
	 *         if any error occurs
	 */
	public void unsubscribe(long timeout) throws BACnetException {
		for ( Entry<Integer, Set<CovSubscriptionIdentifier>> subTypeEntry : deviceSubscriptionIds
				.entrySet() ) {
			final Integer deviceId = subTypeEntry.getKey();
			RemoteDevice dev = localDevice.getRemoteDeviceBlocking(deviceId, timeout);
			for ( CovSubscriptionIdentifier subIdent : subTypeEntry.getValue() ) {
				if ( subIdent.getSubType() == CovSubscriptionType.SubscribeProperties ) {
					SubscribeCOVPropertyMultipleRequest req = new SubscribeCOVPropertyMultipleRequest(
							new Unsigned32(subIdent.getSubId().bigIntegerValue()), Boolean.FALSE, null,
							null, new SequenceOf<>(0));
					localDevice.send(dev, req, new ResponseConsumer() {

						@Override
						public void success(AcknowledgementService ack) {
							log.info("Un-subscription {} on device {} properties successful", id,
									deviceId);
						}

						@Override
						public void fail(AckAPDU ack) {
							log.warn("Un-subscription {} on device {} properties failed: {}", id,
									deviceId, ack);
						}

						@Override
						public void ex(BACnetException e) {
							log.info("Un-subscription {} on device {} properties threw exception", id,
									deviceId, e);
						}

					});
				} else if ( subIdent.getReq() instanceof SubscribeCOVPropertyRequest ) {
					SubscribeCOVPropertyRequest req = new SubscribeCOVPropertyRequest(
							(SubscribeCOVPropertyRequest) subIdent.getReq());
					localDevice.send(dev, req, new ResponseConsumer() {

						@Override
						public void success(AcknowledgementService ack) {
							log.info("Un-subscription {} on device {} object {} property {} successful",
									id, deviceId, subIdent.getObjId(), subIdent.getPropRef());
						}

						@Override
						public void fail(AckAPDU ack) {
							log.warn("Un-subscription {} on device {} object {} property {} failed: {}",
									id, deviceId, subIdent.getObjId(), subIdent.getPropRef(), ack);
						}

						@Override
						public void ex(BACnetException e) {
							log.warn(
									"Un-subscription {} on device {} object {} property {} threw exception",
									id, deviceId, subIdent.getObjId(), subIdent.getPropRef(), e);
						}

					});
				} else if ( subIdent.getReq() instanceof SubscribeCOVRequest ) {
					SubscribeCOVRequest req = new SubscribeCOVRequest(
							(SubscribeCOVRequest) subIdent.getReq());
					localDevice.send(dev, req, new ResponseConsumer() {

						@Override
						public void success(AcknowledgementService ack) {
							log.info("Un-subscription {} on device {} object {} successful", id,
									deviceId, subIdent.getObjId());
						}

						@Override
						public void fail(AckAPDU ack) {
							log.warn("Un-subscription {} on device {} object {} failed: {}", id,
									deviceId, subIdent.getObjId(), ack);
						}

						@Override
						public void ex(BACnetException e) {
							log.warn("Un-subscription {} on device {} object {} threw exception", id,
									deviceId, subIdent.getObjId(), e);
						}

					});
				}
			}
		}
	}

	/**
	 * Subscribe to an object COV.
	 * 
	 * @param dev
	 *        the device
	 * @param objId
	 *        the object ID
	 * @throws BACnetException
	 *         if any error occurs
	 */
	public void subscribeCov(final RemoteDevice dev, final ObjectIdentifier objId)
			throws BACnetException {
		UnsignedInteger subId = new UnsignedInteger(
				BigInteger.valueOf(toUnsignedLong(networkOps.nextSubscriptionId())));
		SubscribeCOVRequest req = new SubscribeCOVRequest(subId, objId, Boolean.FALSE, lifetime);
		ServiceFuture f = localDevice.send(dev, req);
		f.get();
		log.info("Subscription {} on device {} object {} successful", id, dev.getInstanceNumber(),
				objId);
		deviceSubscriptionIds.computeIfAbsent(dev.getInstanceNumber(), k -> new HashSet<>(8))
				.add(CovSubscriptionIdentifier.objectSubscription(subId, req));
	}

	/**
	 * Subscribe to a property COV.
	 * 
	 * @param dev
	 *        the device
	 * @param objId
	 *        the object ID
	 * @param cov
	 *        the COV property reference
	 * @throws BACnetException
	 *         if any error occurs
	 */
	public void subscribeCovProperty(final RemoteDevice dev, final ObjectIdentifier objId,
			CovReference cov) throws BACnetException {
		UnsignedInteger subId = new UnsignedInteger(
				BigInteger.valueOf(toUnsignedLong(networkOps.nextSubscriptionId())));
		SubscribeCOVPropertyRequest req = new SubscribeCOVPropertyRequest(subId, objId, Boolean.FALSE,
				lifetime, cov.getMonitoredProperty(), cov.getCovIncrement());
		ServiceFuture f = localDevice.send(dev, req);
		f.get();
		log.info("Subscription {} on device {} object {} property {} successful", id,
				dev.getInstanceNumber(), objId, cov.getMonitoredProperty().getPropertyIdentifier());
		deviceSubscriptionIds.computeIfAbsent(dev.getInstanceNumber(), k -> new HashSet<>(8))
				.add(CovSubscriptionIdentifier.propertySubscription(subId, req));

	}

	/**
	 * Subscribe to multiple properties COV.
	 * 
	 * @param dev
	 *        the device
	 * @param covMapping
	 *        the COV mapping
	 * @throws BACnetException
	 *         if any error occurs
	 */
	public void subscribeCovPropertyMultiple(final RemoteDevice dev,
			Map<ObjectIdentifier, List<CovReference>> covMapping) throws BACnetException {
		UnsignedInteger subId = new UnsignedInteger(BigInteger.valueOf(toUnsignedLong(id)));
		UnsignedInteger maxNotificationDelay = new UnsignedInteger(5);
		List<CovSubscriptionSpecification> specs = covMapping.entrySet().stream().map(e -> {
			return new CovSubscriptionSpecification(e.getKey(), new SequenceOf<>(e.getValue()));
		}).collect(Collectors.toList());
		SubscribeCOVPropertyMultipleRequest req = new SubscribeCOVPropertyMultipleRequest(
				new Unsigned32(subId.bigIntegerValue()), Boolean.FALSE, lifetime, maxNotificationDelay,
				new SequenceOf<>(specs));
		ServiceFuture f = localDevice.send(dev, req);
		f.get();
		log.info("Subscription {} on device {} properties successful", id, dev.getInstanceNumber());
		deviceSubscriptionIds.computeIfAbsent(dev.getInstanceNumber(), k -> new HashSet<>(8))
				.add(CovSubscriptionIdentifier.propertiesSubscription(subId, req));

	}

	/**
	 * Re-subscribe to all COV subscriptions.
	 * 
	 * @param timeout
	 *        the network timeout, in milliseconds
	 * @throws BACnetException
	 *         if any error occurs
	 */
	public void resubscribe(long timeout) throws BACnetException {
		final Instant newExpire = Instant.now().plusSeconds(lifetime.longValue());
		for ( Entry<Integer, Set<CovSubscriptionIdentifier>> subTypeEntry : deviceSubscriptionIds
				.entrySet() ) {
			final Integer deviceId = subTypeEntry.getKey();
			RemoteDevice dev = localDevice.getRemoteDeviceBlocking(deviceId, timeout);
			for ( CovSubscriptionIdentifier subIdent : subTypeEntry.getValue() ) {
				if ( subIdent.getSubType() == CovSubscriptionType.SubscribeProperties ) {
					SubscribeCOVPropertyMultipleRequest req = new SubscribeCOVPropertyMultipleRequest(
							new Unsigned32(subIdent.getSubId().bigIntegerValue()), Boolean.FALSE,
							lifetime, null, new SequenceOf<>(0));
					ServiceFuture f = localDevice.send(dev, req);
					f.get();
					log.info("Re-subscription {} on device {} properties successful", id, deviceId);
				} else if ( subIdent.getReq() instanceof SubscribeCOVPropertyRequest ) {
					SubscribeCOVPropertyRequest req = new SubscribeCOVPropertyRequest(
							subIdent.getSubId(), subIdent.getObjId(), Boolean.FALSE, lifetime,
							subIdent.getPropRef(),
							((SubscribeCOVPropertyRequest) subIdent.getReq()).getCovIncrement());
					ServiceFuture f = localDevice.send(dev, req);
					f.get();
					log.info("Re-subscription {} on device {} object {} property {} successful", id,
							deviceId, subIdent.getObjId(), subIdent.getPropRef());
				} else if ( subIdent.getReq() instanceof SubscribeCOVRequest ) {
					SubscribeCOVRequest req = new SubscribeCOVRequest(subIdent.getSubId(),
							subIdent.getObjId(), Boolean.FALSE, lifetime);
					ServiceFuture f = localDevice.send(dev, req);
					f.get();
					log.info("Re-subscription {} on device {} object {} successful", id, deviceId,
							subIdent.getObjId());
				}
			}
		}
		expireAt(newExpire);
	}

	/**
	 * Get the lifetime value.
	 * 
	 * @return the lifetime
	 */
	public UnsignedInteger getLifetime() {
		return lifetime;
	}

	/**
	 * Get the device subscription type mapping.
	 * 
	 * @return the device subscription types
	 */
	public Map<Integer, CovSubscriptionType> getDeviceSubscriptionTypes() {
		return deviceSubscriptionTypes;
	}

	/**
	 * Get the device subscription identifier mapping.
	 * 
	 * @return the device subscription identifiers
	 */
	public Map<Integer, Set<CovSubscriptionIdentifier>> getDeviceSubscriptionIds() {
		return deviceSubscriptionIds;
	}

	/**
	 * Get the device object property references.
	 * 
	 * @return the references
	 */
	public List<BacnetDeviceObjectPropertyRef> getRefs() {
		return refs;
	}

	/**
	 * Get the device reference mapping.
	 * 
	 * @return the device reference mapping
	 */
	public Map<Integer, Map<ObjectIdentifier, List<CovReference>>> getDevRefMap() {
		return devRefMap;
	}

	/**
	 * Set the expiration date.
	 * 
	 * @param date
	 *        the expiration date
	 */
	public void expireAt(Instant date) {
		this.expires = date;
	}

	/**
	 * Get the internal subscription ID.
	 * 
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get the expiration date.
	 * 
	 * @return the expiration date
	 */
	public Instant getExpires() {
		return expires;
	}

}
