/* ===================================================================
 * Datum.java
 * 
 * Created Nov 30, 2009 4:50:28 PM
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
import java.util.Map;

/**
 * Basic persistable domain object API.
 * 
 * @author matt
 * @version 1.4
 */
public interface Datum {

	/**
	 * A suffix to append to to data property keys that represent a logical
	 * reverse of the same key without this suffix. For example a
	 * <code>wattHoursReverse</code> key might represent energy exported, rather
	 * than imported, through a power meter.
	 * 
	 * @since 1.2
	 */
	String REVERSE_ACCUMULATING_SUFFIX_KEY = "Reverse";

	/**
	 * A property name for the string name of the <em>core</em> datum type a
	 * datum represents.
	 * 
	 * <p>
	 * The <em>core</em> data type is the most specific interface defined on a
	 * datum class, which will be the first value in the
	 * {@link #DATUM_TYPES_PROPERTY} property.
	 * </p>
	 * 
	 * @since 1.3
	 * @see #DATUM_TYPES_PROPERTY
	 */
	String DATUM_TYPE_PROPERTY = "_DatumType";

	/**
	 * A property name for an array of string names of all datum types
	 * associated with the event.
	 * 
	 * <p>
	 * Datum types are the fully qualified <em>interfaces</em> defined on the
	 * datum implementation class, and any superclass. All Java language
	 * interfaces are ignored, e.g. packages starting with {@literal java.} or
	 * {@literal javax.} are not included. The array is ordered in reverse class
	 * hierarchy order.
	 * </p>
	 * 
	 * @since 1.3
	 */
	String DATUM_TYPES_PROPERTY = "_DatumTypes";

	/**
	 * A property name for a {@code Datum} instance associated with an event.
	 * 
	 * @since 1.5
	 */
	String DATUM_PROPERTY = "_Datum";

	/**
	 * A {@link net.solarnetwork.domain.GeneralNodeDatumSamples} sample key for
	 * a {@link net.solarnetwork.domain.DeviceOperatingState#getCode()} value.
	 * 
	 * @since 1.4
	 */
	String OP_STATE = "opState";

	/**
	 * A {@link net.solarnetwork.domain.GeneralNodeDatumSamples} sample key for
	 * a bitmask of hardware-specific operating state values.
	 * 
	 * @since 1.4
	 */
	String OP_STATES = "opStates";

	/**
	 * A sample data key for a {@link Datum#getCreated()} value, as a
	 * {@code long} epoch value.
	 * 
	 * @since 1.4
	 */
	String TIMESTAMP = "created";

	/**
	 * A sample data key for a {@link Datum#getSourceId()} value.
	 * 
	 * @since 1.4
	 */
	String SOURCE_ID = "sourceId";

	/**
	 * A sample data key for a {@link Datum#getUploaded()} value, as a
	 * {@code long} epoch value.
	 * 
	 * @since 1.4
	 */
	String TIMESTAMP_UPLOAD = "uploaded";

	/**
	 * Get the date this object was created, which is often equal to either the
	 * date it was persisted or the date the associated data in this object was
	 * captured.
	 * 
	 * @return the created date
	 */
	Date getCreated();

	/**
	 * Get a unique source ID for this datum.
	 * 
	 * <p>
	 * A single datum type may collect data from many different sources.
	 * </p>
	 * 
	 * @return the source ID
	 */
	String getSourceId();

	/**
	 * Get the date this object was uploaded to SolarNet.
	 * 
	 * @return the upload date
	 */
	Date getUploaded();

	/**
	 * Get a map of all available data sampled or collected on this datum.
	 * 
	 * @return a map with all available sample data
	 * @since 1.3
	 */
	Map<String, ?> getSampleData();

	/**
	 * Get a simple {@link Map} view of this datum.
	 * 
	 * <p>
	 * The returned map should include all the available properties of this
	 * datum, in as flat of a structure as possible, i.e. without nested maps.
	 * The property values should be composed only if simple Java types like
	 * numbers, strings, and arrays or lists of those types. It should also
	 * include the {@link #DATUM_TYPE_PROPERTY} and
	 * {@link #DATUM_TYPES_PROPERTY} values.
	 * </p>
	 * 
	 * @return a Map view of this datum
	 * @since 1.3
	 */
	Map<String, ?> asSimpleMap();

}
