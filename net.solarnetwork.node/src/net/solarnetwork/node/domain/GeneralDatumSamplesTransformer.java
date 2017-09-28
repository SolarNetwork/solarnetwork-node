/* ==================================================================
 * GeneralDatumSamplesTransformer.java - 28/10/2016 2:39:24 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import net.solarnetwork.domain.GeneralDatumSamples;

/**
 * API for taking a {@link GeneralDatumSamples} object and transforming it in
 * some way into a different {@link GeneralDatumSamples) object.
 * 
 * @author matt
 * @version 1.1
 */
public interface GeneralDatumSamplesTransformer {

	/**
	 * Transform a samples instance.
	 * 
	 * <p>
	 * Generally this method is not meant to make changes to the passed in
	 * {@code samples} instance. Rather it should apply changes to a copy of
	 * {@code samples} and return the copy. If no changes are necessary then the
	 * {@code samples} instance may be returned.
	 * </p>
	 * 
	 * <p>
	 * This method may also return {@literal null} to indicate the
	 * {@code samples} instance should not be processed, or that there is
	 * essentially no data to associate with this particular {@code datum}.
	 * </p>
	 * 
	 * @param datum
	 *        The {@link Datum} associated with {@code samples}.
	 * @param samples
	 *        The samples object to transform.
	 * @return The transformed samples instance, which may be the
	 *         {@code samples} instance or a new instance, or {@literal null} to
	 *         indicate the samples should not be processed.
	 */
	GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples);

}
