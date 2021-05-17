/* ==================================================================
 * GeneralDatumSamplesTransformService.java - 15/03/2019 9:41:46 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node;

import java.util.Map;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;

/**
 * A service API for working with {@link GeneralDatumSamplesTransformer}
 * objects.
 * 
 * <p>
 * This API is similar to {@link GeneralDatumSamplesTransformer} but designed to
 * work with potentially multiple {@link GeneralDatumSamplesTransformer}
 * instances in combination.
 * </p>
 * 
 * @author matt
 * @version 1.2
 * @since 1.66
 */
public interface GeneralDatumSamplesTransformService
		extends Identifiable, net.solarnetwork.domain.Identifiable {

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
	 * <p>
	 * See the standard parameter keys defined in
	 * {@link GeneralDatumSamplesTransformer}, like
	 * {@link GeneralDatumSamplesTransformer#PARAM_TEST_ONLY}.
	 * </p>
	 * 
	 * @param datum
	 *        The {@link Datum} associated with {@code samples}.
	 * @param samples
	 *        The samples object to transform.
	 * @param parameters
	 *        Optional implementation-specific parameters to pass to the
	 *        transformer.
	 * @return The transformed samples instance, which may be the
	 *         {@code samples} instance or a new instance, or {@literal null} to
	 *         indicate the samples should not be processed.
	 */
	GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, Object> parameters);

}
