/* ===================================================================
 * NodeDatum.java
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

package net.solarnetwork.node.domain.datum;

import java.time.Instant;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamplesOperations;

/**
 * Basic persistable domain object API.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface NodeDatum extends Datum, Cloneable {

	/**
	 * A sample data key for a {@link NodeDatum#getUploaded()} value, as a
	 * {@code long} epoch value.
	 * 
	 * @since 1.4
	 */
	String TIMESTAMP_UPLOAD = "uploaded";

	/**
	 * Get the date this object was uploaded to SolarNet.
	 * 
	 * @return the upload date
	 */
	Instant getUploaded();

	/**
	 * Public clone method.
	 * 
	 * @return a copy of this instance
	 */
	NodeDatum clone();

	/**
	 * Create a copy of this instance with the sample properties replaced by a
	 * given samples instance.
	 * 
	 * @param samples
	 *        the samples to use for the copy
	 * @return a new copy of this instance
	 */
	@Override
	NodeDatum copyWithSamples(DatumSamplesOperations samples);

	/**
	 * Get a copy of this datum with a new ID
	 * 
	 * @return the copy with the given ID
	 */
	@Override
	NodeDatum copyWithId(DatumId id);

}
