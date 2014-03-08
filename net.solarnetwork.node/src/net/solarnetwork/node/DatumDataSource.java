/* ===================================================================
 * DatumDataSource.java
 * 
 * Created Nov 30, 2009 4:52:53 PM
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

package net.solarnetwork.node;

/**
 * API for collecting {@link Datum} objects from some device.
 * 
 * @author matt
 * @version 1.0
 * @param <T>
 *        the Datum type
 */
public interface DatumDataSource<T extends Datum> {

	/**
	 * Get the class supported by this DataSource.
	 * 
	 * @return class
	 */
	Class<? extends T> getDatumType();

	/**
	 * Read the current value from the data source, returning as an unpersisted
	 * {@link Datum} object.
	 * 
	 * @return Datum
	 */
	T readCurrentDatum();

}
