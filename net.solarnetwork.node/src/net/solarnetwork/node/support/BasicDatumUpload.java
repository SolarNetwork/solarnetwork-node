/* ===================================================================
 * BasicDatumUpload.java
 * 
 * Created Dec 1, 2009 4:13:29 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.support;

import java.util.Date;

import net.solarnetwork.node.Datum;
import net.solarnetwork.node.DatumUpload;

/**
 * Basic implementation of {@link DatumUpload}.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class BasicDatumUpload implements DatumUpload {

	private Long id = null;
	private Class<? extends Datum> datumClass = null;
	private Date created = null;
	private Long datumId = null;
	private String destination = null;
	private Long trackingId = null;
	
	/**
	 * Construct with a Datum class value.
	 * 
	 * @param datumClass the class of the Datum this upload is for
	 */
	public BasicDatumUpload(Class<? extends Datum> datumClass) {
		this.datumClass = datumClass;
	}

	/**
	 * Construct with values.
	 * 
	 * @param datum the datum that was uploaded
	 * @param id the primary key of this upload datum
	 * @param destination the destination
	 * @param trackingId the tracking ID
	 */
	public BasicDatumUpload(Datum datum, Long id, String destination, Long trackingId) {
		this((Class<? extends Datum>) datum.getClass());
		setId(id);
		setDatumId(datum.getId());
		setDestination(destination);
		setTrackingId(trackingId);
	}

	/**
	 * @return the datumClass
	 */
	public Class<? extends Datum> getDatumClass() {
		return datumClass;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the created
	 */
	public Date getCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Date created) {
		this.created = created;
	}

	/**
	 * @return the datumId
	 */
	public Long getDatumId() {
		return datumId;
	}

	/**
	 * @param datumId the datumId to set
	 */
	public void setDatumId(Long datumId) {
		this.datumId = datumId;
	}

	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * @param destination the destination to set
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}

	/**
	 * @return the trackingId
	 */
	public Long getTrackingId() {
		return trackingId;
	}

	/**
	 * @param trackingId the trackingId to set
	 */
	public void setTrackingId(Long trackingId) {
		this.trackingId = trackingId;
	}

}
