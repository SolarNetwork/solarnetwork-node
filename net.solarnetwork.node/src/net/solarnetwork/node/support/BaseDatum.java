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
 */

package net.solarnetwork.node.support;

import java.util.Date;
import net.solarnetwork.node.Datum;

/**
 * Abstract base class for {@link Datum} implementations.
 * 
 * @author matt
 * @version 1.1
 */
public abstract class BaseDatum implements Datum, Cloneable {

	private Long id = null;
	private String sourceId = null;
	private Date created = null;

	/**
	 * Default constructor.
	 */
	public BaseDatum() {
		super();
	}

	/**
	 * Construct with an ID value.
	 * 
	 * @param id
	 *        the ID value
	 */
	public BaseDatum(Long id) {
		setId(id);
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
		return result;
	}

	/**
	 * Compare for equality.
	 * 
	 * <p>
	 * This method compares the {@code id} and {@code sourceId} values for
	 * equality.
	 * </p>
	 * 
	 * @return <em>true</em> if the objects are equal
	 */
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
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		if ( sourceId == null ) {
			if ( other.sourceId != null ) {
				return false;
			}
		} else if ( !sourceId.equals(other.sourceId) ) {
			return false;
		}
		return true;
	}

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
