/* ==================================================================
 * GeneralDatum.java - 23/03/2018 9:22:50 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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
 */

package net.solarnetwork.node.domain;

import net.solarnetwork.domain.GeneralDatumSamplesOperations;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;

/**
 * API for a general datum.
 * 
 * @author matt
 * @version 1.0
 * @since 1.57
 */
public interface GeneralDatum extends Datum {

	/**
	 * Get a general accessor for the sample data.
	 * 
	 * @return the operations instance, or {@literal null} if no samples are
	 *         available
	 */
	GeneralDatumSamplesOperations asSampleOperations();

	/**
	 * Get a mutable general accessor for the sample data.
	 * 
	 * @return the operations instance, never {@literal null}
	 * @throws UnsupportedOperationException
	 *         if mutation is not supported
	 */
	MutableGeneralDatumSamplesOperations asMutableSampleOperations();

}
