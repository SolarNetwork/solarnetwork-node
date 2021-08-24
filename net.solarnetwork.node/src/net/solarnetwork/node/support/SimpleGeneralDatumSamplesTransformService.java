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
import java.util.stream.Collectors;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;
import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.MappableSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * Basic implementation of {@link GeneralDatumSamplesTransformService} that
 * adapts a collection of {@link GeneralDatumSamplesTransformer} into a single
 * service.
 * 
 * @author matt
 * @version 1.1
 * @since 1.66
 */
public class SimpleGeneralDatumSamplesTransformService extends BaseIdentifiable implements
		GeneralDatumSamplesTransformService, SettingsChangeObserver, SettingSpecifierProvider {

	private final String settingUid;
	private final Map<String, Object> staticParameters;
	private List<GeneralDatumSamplesTransformer> sampleTransformers;

	/**
	 * Constructor.
	 */
	public SimpleGeneralDatumSamplesTransformService() {
		this(null, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param settingUid
	 *        the setting UID
	 */
	public SimpleGeneralDatumSamplesTransformService(String settingUid) {
		this(settingUid, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param staticParameters
	 *        optional static properties to pass to the transformers
	 */
	public SimpleGeneralDatumSamplesTransformService(Map<String, Object> staticParameters) {
		this(null, null, staticParameters);
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
	 * @param settingUid
	 *        the setting UID
	 * @param sampleTransformers
	 *        the transformers
	 * @param staticParameters
	 *        optional static properties to pass to the transformers
	 */
	public SimpleGeneralDatumSamplesTransformService(String settingUid,
			List<GeneralDatumSamplesTransformer> sampleTransformers,
			Map<String, Object> staticParameters) {
		super();
		this.settingUid = settingUid;
		if ( staticParameters != null ) {
			staticParameters = Collections.unmodifiableMap(new LinkedHashMap<>(staticParameters));
		}
		this.staticParameters = staticParameters;
		setSampleTransformers(sampleTransformers);
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		final List<GeneralDatumSamplesTransformer> xforms = sampleTransformers;
		for ( GeneralDatumSamplesTransformer xform : xforms ) {
			if ( xform instanceof SettingsChangeObserver ) {
				((SettingsChangeObserver) xform).configurationChanged(properties);
			}
		}
	}

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, Object> parameters) {
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

	@Override
	public String getSettingUID() {
		return settingUid;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		GeneralDatumSamplesTransformer xform = getSampleTransformer();
		if ( xform instanceof SettingSpecifierProvider ) {
			List<SettingSpecifier> settings = ((SettingSpecifierProvider) xform).getSettingSpecifiers();
			result.addAll(MappableSpecifier.mapTo(settings, "sampleTransformer."));
		}

		// move transient to top (assume read-only titles and status)
		List<SettingSpecifier> reordered = result.stream().filter(
				s -> s instanceof KeyedSettingSpecifier && ((KeyedSettingSpecifier<?>) s).isTransient())
				.collect(Collectors.toList());
		result.removeAll(reordered);
		reordered.addAll(result);
		return reordered;
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

	/**
	 * Get the first available transformer.
	 * 
	 * @return the first available transformer
	 * @since 1.1
	 */
	public GeneralDatumSamplesTransformer getSampleTransformer() {
		return (sampleTransformers != null && sampleTransformers.size() > 0 ? sampleTransformers.get(0)
				: null);
	}

	/**
	 * Set a sample transformer to use.
	 * 
	 * @param xform
	 *        the transformer
	 * @since 1.1
	 */
	public void setSampleTransformer(GeneralDatumSamplesTransformer xform) {
		setSampleTransformers(Collections.singletonList(xform));
	}

}
