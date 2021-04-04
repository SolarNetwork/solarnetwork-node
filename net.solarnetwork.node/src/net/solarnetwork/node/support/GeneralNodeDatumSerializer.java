/* ==================================================================
 * GeneralNodeDatumSerializer.java - Aug 25, 2014 2:05:01 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;
import net.solarnetwork.node.domain.GeneralLocationDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.util.OptionalService;

/**
 * Serialize {@link GeneralNodeDatum} and {@link GeneralLocationDatum} to JSON.
 * 
 * @author matt
 * @version 1.3
 * @since 1.58
 */
public class GeneralNodeDatumSerializer extends StdScalarSerializer<GeneralNodeDatum>
		implements Serializable {

	private static final long serialVersionUID = 1564431501442940772L;

	private List<GeneralDatumSamplesTransformer> sampleTransformers;
	private OptionalService<SimpleGeneralDatumSamplesTransformService> samplesTransformService;

	/**
	 * Default constructor.
	 */
	public GeneralNodeDatumSerializer() {
		super(GeneralNodeDatum.class);
	}

	@Override
	public void serialize(GeneralNodeDatum datum, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonGenerationException {
		GeneralDatumSamples samples = datum.getSamples();

		SimpleGeneralDatumSamplesTransformService xformService = (samplesTransformService != null
				? samplesTransformService.service()
				: null);
		if ( samples != null && xformService != null ) {
			samples = xformService.transformSamples(datum, samples, null);
		}

		List<GeneralDatumSamplesTransformer> xforms = sampleTransformers;
		if ( samples != null && xforms != null ) {
			for ( GeneralDatumSamplesTransformer xform : xforms ) {
				samples = xform.transformSamples(datum, samples);
				if ( samples == null ) {
					break;
				}
			}
		}

		if ( samples == null ) {
			return;
		}

		generator.writeStartObject();
		if ( datum.getCreated() != null ) {
			generator.writeNumberField("created", datum.getCreated().getTime());
		}
		if ( datum instanceof GeneralLocationDatum ) {
			GeneralLocationDatum loc = (GeneralLocationDatum) datum;
			generator.writeNumberField("locationId", loc.getLocationId());
		}

		if ( datum.getSourceId() != null ) {
			generator.writeStringField("sourceId", datum.getSourceId());
		}

		generator.writeObjectField("samples", samples);

		generator.writeEndObject();
	}

	/**
	 * Set a list of sample transformers to apply when serializing.
	 * 
	 * @param sampleTransformers
	 *        The sample transformers to apply. If a transformer returns
	 *        {@literal null} then the datum will not be serialized.
	 * @since 1.2
	 */
	public void setSampleTransformers(
			List<net.solarnetwork.node.domain.GeneralDatumSamplesTransformer> sampleTransformers) {
		this.sampleTransformers = sampleTransformers;
	}

	/**
	 * Set a samples transform service to use when serializing.
	 * 
	 * <p>
	 * If this service is configured, it will be invoked <em>before</em> any
	 * samples transformers configured via {@link #setSampleTransformers(List)}.
	 * </p>
	 * 
	 * @param samplesTransformService
	 *        The service to use; if the service returns {@literal null} then
	 *        the datum will not be serialized.
	 * @since 1.3
	 */
	public void setSamplesTransformService(
			OptionalService<SimpleGeneralDatumSamplesTransformService> samplesTransformService) {
		this.samplesTransformService = samplesTransformService;
	}

}
