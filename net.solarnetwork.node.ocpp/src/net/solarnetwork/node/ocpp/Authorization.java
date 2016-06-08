/* ==================================================================
 * Authorization.java - 8/06/2015 9:51:59 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp;

import java.util.Date;
import ocpp.v15.cs.AuthorizationStatus;
import ocpp.v15.cs.IdTagInfo;

/**
 * Extension of {@link IdTagInfo} to associate an ID tag value with the info
 * itself.
 * 
 * @author matt
 * @version 1.0
 */
public class Authorization extends IdTagInfo {

	private Date created = null;
	private String idTag;

	/**
	 * Default constructor.
	 */
	public Authorization() {
		super();
	}

	/**
	 * Construct from an ID tag and associated info. This constructor copies the
	 * values out of the provided {@link IdTagInfo} into this object.
	 * 
	 * @param idTag
	 *        The ID tag value.
	 * @param info
	 *        The associated info.
	 */
	public Authorization(String idTag, IdTagInfo info) {
		super();
		setIdTag(idTag);
		if ( info != null ) {
			setExpiryDate(info.getExpiryDate());
			setParentIdTag(info.getParentIdTag());
			setStatus(info.getStatus());
		}
	}

	/**
	 * Test if this authorization is expired. An authorization is expired if it
	 * has an {@code expiryDate} and that {@code expiryDate} is not earlier than
	 * the current time.
	 * 
	 * @return Expired flag.
	 */
	public boolean isExpired() {
		return (expiryDate != null && expiryDate.toGregorianCalendar().getTimeInMillis() < System
				.currentTimeMillis());
	}

	/**
	 * Test if this authorization has an {@link AuthorizationStatus#ACCEPTED}
	 * state and is not exipred.
	 * 
	 * @return If accepted and not expired then <em>true</em>, otherwise
	 *         <em>false</em>.
	 */
	public boolean isAccepted() {
		return (AuthorizationStatus.ACCEPTED.equals(getStatus()) && !isExpired());
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getIdTag() {
		return idTag;
	}

	public void setIdTag(String idTag) {
		this.idTag = idTag;
	}

}
