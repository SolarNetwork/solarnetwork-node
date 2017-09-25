/* ===================================================================
 * DatumDao.java
 * 
 * Created Nov 30, 2009 4:56:25 PM
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

package net.solarnetwork.node.dao;

import java.util.Date;
import java.util.List;
import net.solarnetwork.node.domain.Datum;

/**
 * Data Access Object (DAO) API for {@link Datum} objects.
 * 
 * @author matt
 * @version 1.2
 * @param <T>
 *        the type of Datum this DAO supports
 */
public interface DatumDao<T extends Datum> {

	/**
	 * An {@link org.osgi.service.event.Event} property for the string name of
	 * the <em>core</em> datum type associated with the event.
	 * 
	 * <p>
	 * The <em>core<em> data type is the most specific interface defined on the
	 * {@link #getDatumType()} class, which will be the first value in the
	 * {@link #EVENT_PROP_DATUM_TYPES} property.
	 * </p>
	 * 
	 * @since 1.2
	 * @see #EVENT_PROP_DATUM_TYPES
	 */
	public static final String EVENT_PROP_DATUM_TYPE = "_DatumType";

	/**
	 * An {@link org.osgi.service.event.Event} property for an array of string
	 * names of all datum types associated with the event.
	 * 
	 * <p>
	 * Datum types are the fully qualified <em>interfaces</em> defined on the
	 * {@link #getDatumType()} class, and any superclass. All Java language
	 * interfaces are ignored, e.g. packages starting with {@literal java.} or
	 * {@literal javax.} are not included. The array is ordered in reverse class
	 * hierarchy order.
	 * </p>
	 * 
	 * @since 1.2
	 */
	public static final String EVENT_PROP_DATUM_TYPES = "_DatumTypes";

	/**
	 * An {@link org.osgi.service.event.Event} topic for when a {@link Datum}
	 * has been persisted.
	 * 
	 * <p>
	 * The properties of the event shall be any of the JavaBean properties of
	 * the Datum supported by events (i.e. any simple Java property such as
	 * numbers and strings). In addition, the {@link #EVENT_PROP_DATUM_TYPE}
	 * property shall be populated with the name of the <em>core</em> class name
	 * of the datum type.
	 * </p>
	 * 
	 * @since 1.2
	 */
	public static final String EVENT_TOPIC_DATUM_STORED = "net/solarnetwork/node/dao/DATUM_STORED";

	/**
	 * Get the class supported by this Dao.
	 * 
	 * @return class
	 */
	Class<? extends T> getDatumType();

	/**
	 * Store (create or update) a datum.
	 * 
	 * @param datum
	 *        the datum to persist
	 * @return the generated primary key
	 */
	void storeDatum(T datum);

	/**
	 * Get a List of Datum instances that have not been uploaded yet to a
	 * specific destination.
	 * 
	 * <p>
	 * This does not need to return all data, it can limit the amount returned
	 * at one time to conserve memory. This method can be called repeatedly if
	 * needed.
	 * </p>
	 * 
	 * @param destination
	 *        the destination to check
	 * @return list of Datum, or empty List if none available
	 */
	List<T> getDatumNotUploaded(String destination);

	/**
	 * Persist a {@link DatumUpload} instance.
	 * 
	 * @param datum
	 *        the Datum that has been uploaded successfully
	 * @param date
	 *        the date it was uploaded
	 * @param destination
	 *        the destination the Datum was uploaded to
	 * @param trackingId
	 *        the remote tracking ID assigned to the uploaded Datum
	 */
	void setDatumUploaded(T datum, Date date, String destination, String trackingId);

	/**
	 * Delete both Datum and DatumUpload objects that have been successfully
	 * uploaded to at least one destination and are older than the specified
	 * number of hours.
	 * 
	 * <p>
	 * This is designed to free up space from local database storage for devices
	 * with limited storage capacity. It will not delete any Datum objects that
	 * have not been successfully uploaded anywhere.
	 * </p>
	 * 
	 * @param hours
	 *        the minimum number of hours old the data must be to delete
	 * @return the number of Datum (and associated DatumUpload) entities deleted
	 */
	int deleteUploadedDataOlderThan(int hours);

}
