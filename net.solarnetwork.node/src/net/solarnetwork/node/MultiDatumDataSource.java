/* ==================================================================
 * MultiDatumDataSource.java - Apr 8, 2010 7:32:57 AM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node;

import java.util.Collection;

/**
 * API for collecting multiple {@link Datum} objects from some device.
 * 
 * @author matt
 * @version $Id$
 */
public interface MultiDatumDataSource<T extends Datum> {

	/**
	 * Get the class supported by this DataSource.
	 * 
	 * @return class
	 */
	Class<? extends T> getMultiDatumType();
	
	/**
	 * Read multiple values from the data source, returning
	 * as a collection of unpersisted {@link Datum} objects.
	 * 
	 * @return Datum
	 */
	Collection<T> readMultipleDatum();

}
