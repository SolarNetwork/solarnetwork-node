/* ==================================================================
 * SubscriptionIdentifier.java - 6/11/2022 9:18:59 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Objects;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyRequest;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVRequest;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;

/**
 * A identifier for a COV subscription.
 * 
 * @author matt
 * @version 1.0
 */
public class CovSubscriptionIdentifier {

	private final CovSubscriptionType subType;
	private final Unsigned32 subId;
	private final ConfirmedRequestService req;
	private final ObjectIdentifier objId;
	private final PropertyReference propRef;

	/**
	 * Get an instance for a multiple-properties subscription.
	 * 
	 * @param subId
	 *        the COV subscription ID
	 * @param req
	 *        the request
	 * @return the new instance
	 */
	public static CovSubscriptionIdentifier propertiesSubscription(Unsigned32 subId,
			SubscribeCOVPropertyMultipleRequest req) {
		return new CovSubscriptionIdentifier(CovSubscriptionType.SubscribeProperties, subId, req, null,
				null);
	}

	/**
	 * Get an instance for a property subscription.
	 * 
	 * @param subId
	 *        the COV subscription ID
	 * @param req
	 *        the request
	 * @return the new instance
	 */
	public static CovSubscriptionIdentifier propertySubscription(Unsigned32 subId,
			SubscribeCOVPropertyRequest req) {
		return new CovSubscriptionIdentifier(CovSubscriptionType.SubscribeProperty, subId, req,
				req.getMonitoredObjectIdentifier(), req.getMonitoredPropertyIdentifier());
	}

	/**
	 * Get an instance for an object subscription.
	 * 
	 * @param subId
	 *        the COV subscription ID
	 * @param req
	 *        the request
	 * @return the new instance
	 */
	public static CovSubscriptionIdentifier objectSubscription(Unsigned32 subId,
			SubscribeCOVRequest req) {
		return new CovSubscriptionIdentifier(CovSubscriptionType.SubscribeObject, subId, req,
				req.getMonitoredObjectIdentifier(), null);
	}

	private CovSubscriptionIdentifier(CovSubscriptionType subType, Unsigned32 subId,
			ConfirmedRequestService req, ObjectIdentifier objId, PropertyReference propRef) {
		super();
		this.subType = requireNonNullArgument(subType, "subType");
		this.subId = requireNonNullArgument(subId, "subId");
		this.req = requireNonNullArgument(req, "req");
		this.objId = objId; // can be null
		this.propRef = propRef; // can be null
	}

	@Override
	public int hashCode() {
		return Objects.hash(subType, subId, objId);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof CovSubscriptionIdentifier) ) {
			return false;
		}
		CovSubscriptionIdentifier other = (CovSubscriptionIdentifier) obj;
		return Objects.equals(subType, other.subType) && Objects.equals(subId, other.subId)
				&& Objects.equals(objId, other.objId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SubscriptionIdentifier{subType=");
		builder.append(subType);
		builder.append(", subId=");
		builder.append(subId);
		builder.append(", objId=");
		builder.append(objId);
		if ( propRef != null ) {
			builder.append(", propId=");
			builder.append(propRef.getPropertyIdentifier());
			if ( propRef.getPropertyArrayIndex() != null ) {
				builder.append(", propIndex=");
				builder.append(propRef.getPropertyArrayIndex());
			}
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the subscription type.
	 * 
	 * @return the subscription type
	 */
	public CovSubscriptionType getSubType() {
		return subType;
	}

	/**
	 * Get the COV subscription ID.
	 * 
	 * @return the COV subscription ID
	 */
	public Unsigned32 getSubId() {
		return subId;
	}

	/**
	 * Get the original subscription request.
	 * 
	 * @return the reqquest
	 */
	public ConfirmedRequestService getReq() {
		return req;
	}

	/**
	 * Get the object ID (if available).
	 * 
	 * @return the objId the object ID, or {@literal null}
	 */
	public ObjectIdentifier getObjId() {
		return objId;
	}

	/**
	 * Get the property reference (if available).
	 * 
	 * @return the property reference, or {@literal null}
	 */
	public PropertyReference getPropRef() {
		return propRef;
	}

}
