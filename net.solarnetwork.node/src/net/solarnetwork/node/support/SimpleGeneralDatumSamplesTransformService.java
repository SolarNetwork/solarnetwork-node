/* ==================================================================
 * SimpleGeneralDatumSamplesTransformService.java - 15/03/2019 9:52:49 am
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

package net.solarnetwork.node.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;

/**
 * Basic implementation of {@link GeneralDatumSamplesTransformService}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.66
 */
public class SimpleGeneralDatumSamplesTransformService extends BaseIdentifiable
		implements GeneralDatumSamplesTransformService {

	private final Map<String, Object> staticParameters;
	private List<GeneralDatumSamplesTransformer> sampleTransformers;

	/**
	 * Constructor.
	 */
	public SimpleGeneralDatumSamplesTransformService() {
		this(null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param staticParameters
	 *        optional static properties to pass to the transformers
	 */
	public SimpleGeneralDatumSamplesTransformService(Map<String, Object> staticParameters) {
		this(null, staticParameters);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code staticParameters} can be used to pass a fixed set of
	 * parameters to every invocation of
	 * {@link GeneralDatumSamplesTransformer#transformSamples(Datum, GeneralDatumSamples, Map)}.
	 * Any parameters passed to
	 * {@link #transformSamples(Datum, GeneralDatumSamples, Map)} will be added
	 * to the static parameters, overriding duplicate values.
	 * </p>
	 * 
	 * @param sampleTransformers
	 *        the transformers
	 * @param staticParameters
	 *        optional static properties to pass to the transformers
	 */
	public SimpleGeneralDatumSamplesTransformService(
			List<GeneralDatumSamplesTransformer> sampleTransformers,
			Map<String, Object> staticParameters) {
		super();
		if ( staticParameters != null ) {
			staticParameters = Collections.unmodifiableMap(new LinkedHashMap<>(staticParameters));
		}
		this.staticParameters = staticParameters;
		setSampleTransformers(sampleTransformers);
	}

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, ?> parameters) {
		GeneralDatumSamples result = samples;
		List<GeneralDatumSamplesTransformer> xforms = sampleTransformers;
		Map<String, ?> xformParams = xformParameterMap(parameters);
		if ( result != null && xforms != null ) {
			for ( GeneralDatumSamplesTransformer xform : xforms ) {
				result = xform.transformSamples(datum, result, xformParams);
				if ( result == null ) {
					break;
				}
			}
		}
		return result;
	}

	private Map<String, ?> xformParameterMap(Map<String, ?> parameters) {
		if ( parameters == null || parameters.isEmpty() ) {
			return staticParameters;
		} else if ( staticParameters == null || staticParameters.isEmpty() ) {
			return parameters;
		}
		Map<String, Object> combined = new HashMap<>(staticParameters);
		combined.putAll(parameters);
		return combined;
	}

	/**
	 * Set the sample transformers to use.
	 * 
	 * @param sampleTransformers
	 *        the transformers to use
	 */
	public void setSampleTransformers(List<GeneralDatumSamplesTransformer> sampleTransformers) {
		this.sampleTransformers = sampleTransformers;
	}

}
