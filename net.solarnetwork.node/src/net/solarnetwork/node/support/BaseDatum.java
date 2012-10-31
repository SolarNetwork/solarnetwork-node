/* ===================================================================
 * BaseDatum.java
 * 
 * Created Dec 1, 2009 4:10:14 PM
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

/**
 * Abstract base class for {@link Datum} implementations.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public abstract class BaseDatum implements Datum, Cloneable {

	private Long id = null;
	private String sourceId = null;
	private Date created = null;
	private String errorMessage = null;
	
	/**
	 * Default constructor.
	 */
	public BaseDatum() {
		super();
	}
	
	/**
	 * Construct with an ID value.
	 * 
	 * @param id the ID value
	 */
	public BaseDatum(Long id) {
		setId(id);
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((created == null) ? 0 : created.hashCode());
		result = prime * result + ((errorMessage == null) ? 0 : errorMessage.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		BaseDatum other = (BaseDatum) obj;
		if ( created == null ) {
			if ( other.created != null ) {
				return false;
			}
		} else if ( !created.equals(other.created) ) {
			return false;
		}
		if ( errorMessage == null ) {
			if ( other.errorMessage != null ) {
				return false;
			}
		} else if ( !errorMessage.equals(other.errorMessage) ) {
			return false;
		}
		if ( id == null ) {
			if (other.id != null) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		if ( sourceId == null ) {
			if (other.sourceId != null) {
				return false;
			}
		} else if ( !sourceId.equals(other.sourceId) ) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see net.solarnetwork.node.Datum#getId()
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
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * @return the sourceId
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * @param sourceId the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
