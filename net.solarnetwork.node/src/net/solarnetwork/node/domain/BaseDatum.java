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

package net.solarnetwork.node.domain;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.util.ClassUtils;

/**
 * Abstract base class for {@link Datum} implementations.
 * 
 * @author matt
 * @version 1.2
 */
public abstract class BaseDatum implements Datum, Cloneable {

	private static final ConcurrentMap<Class<?>, String[]> DATUM_TYPE_CACHE = new ConcurrentHashMap<Class<?>, String[]>();

	private String sourceId = null;
	private Date created = null;
	private Date uploaded = null;

	/**
	 * Default constructor.
	 */
	public BaseDatum() {
		super();
		setSourceId("");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * This method returns the result of {@link #createSimpleMap()}.
	 * </p>
	 * 
	 * @since 1.2
	 */
	@Override
	final public Map<String, ?> asSimpleMap() {
		return createSimpleMap();
	}

	/**
	 * Create a map of simple property data out of this object.
	 * 
	 * <p>
	 * This method will populate the properties of this class and the
	 * {@link Datum#DATUM_TYPE_PROPERTY} and {@link Datum#DATUM_TYPES_PROPERTY}
	 * properties. It will then call {@link #getSampleData()} and add all those
	 * values to the returned result.
	 * </p>
	 * 
	 * @return a map of simple property data
	 * @since 1.2
	 */
	protected Map<String, Object> createSimpleMap() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		if ( created != null ) {
			map.put("created", created.getTime());
		}
		if ( sourceId != null ) {
			map.put("sourceId", sourceId);
		}
		String[] datumTypes = getDatumTypes(getClass());
		if ( datumTypes != null && datumTypes.length > 0 ) {
			map.put(DATUM_TYPE_PROPERTY, datumTypes[0]);
			map.put(DATUM_TYPES_PROPERTY, datumTypes);
		}
		if ( uploaded != null ) {
			map.put("uploaded", uploaded.getTime());
		}
		Map<String, ?> sampleData = getSampleData();
		if ( sampleData != null ) {
			map.putAll(sampleData);
		}
		return map;
	}

	/**
	 * Get an array of datum types for a class.
	 * 
	 * <p>
	 * This method caches the results for performance.
	 * </p>
	 * 
	 * @param clazz
	 *        the datum class to get the types for
	 * @return the types
	 * @since 1.2
	 */
	public static String[] getDatumTypes(Class<?> clazz) {
		String[] result = DATUM_TYPE_CACHE.get(clazz);
		if ( result != null ) {
			return result;
		}
		Set<Class<?>> interfaces = ClassUtils.getAllNonJavaInterfacesForClassAsSet(clazz);
		result = new String[interfaces.size()];
		int i = 0;
		for ( Class<?> intf : interfaces ) {
			result[i] = intf.getName();
			i++;
		}
		DATUM_TYPE_CACHE.putIfAbsent(clazz, result);
		return result;
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
		result = prime * result + ((created == null) ? 0 : created.hashCode());
		result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
		return result;
	}

	/**
	 * Compare for equality.
	 * 
	 * <p>
	 * This method compares the {@code created} and {@code sourceId} values for
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
		if ( created == null ) {
			if ( other.created != null ) {
				return false;
			}
		} else if ( !(created.getTime() == other.created.getTime()) ) {
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
	public Map<String, ?> getSampleData() {
		return null;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	@Override
	public Date getUploaded() {
		return uploaded;
	}

	public void setUploaded(Date uploaded) {
		this.uploaded = uploaded;
	}

}
