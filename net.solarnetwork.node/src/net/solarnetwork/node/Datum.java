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

package net.solarnetwork.node;

import java.util.Date;

/**
 * Basic persistable domain object API.
 * 
 * @author matt
 * @version 1.1
 */
public interface Datum {

	/**
	 * Get the date this object was created, which is often equal to either the
	 * date it was persisted or the date the associated data in this object was
	 * captured.
	 * 
	 * @return the created date
	 */
	public Date getCreated();

	/**
	 * Get a unique source ID for this datum.
	 * 
	 * <p>
	 * A single datum type may collect data from many different sources.
	 * </p>
	 * 
	 * @return the source ID
	 */
	public String getSourceId();

	/**
	 * Get teh date this object was uploaded to SolarNet.
	 * 
	 * @return the upload date
	 */
	public Date getUploaded();

}
