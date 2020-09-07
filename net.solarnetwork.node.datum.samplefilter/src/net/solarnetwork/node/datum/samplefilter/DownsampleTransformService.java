/* ==================================================================
 * DownsampleTransformService.java - 24/08/2020 3:47:12 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.samplefilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.domain.AggregateDatumProperty;
import net.solarnetwork.domain.AggregateDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;

/**
 * Samples transform service that accumulates "sub-sample" values and then
 * produces a down-sampled average with min/max added.
 *
 * <p>
 * Sub-samples are signaled by passing the {@link #SUB_SAMPLE_PROP} key in the
 * {@code parameter} map passed to
 * {@link #transformSamples(Datum, GeneralDatumSamples, Map)}. When invoked in
 * this way the method will always return {@literal null}. Then when a
 * down-sampled output value is needed call
 * {@link #transformSamples(Datum, GeneralDatumSamples, Map)} again but without
 * the {@link #SUB_SAMPLE_PROP} key. Then a computed value derived from the
 * collected sub-samples will be returned:
 * </p>
 * 
 * <ul>
 * <li>Each instantaneous sample property will be transformed into a simple
 * average</li>
 * <li>Each instantaneous sample property will have <i>_min</i> and <i>_max</i>
 * property names, added as a suffix to the original property name, with
 * associated minimum and maximum property values.
 * <li>
 * <li>Each accumulating sample property will be transformed into the last seen
 * value.
 * <li>
 * <li>Each status sample property will be transformed into the last seen
 * value.</li>
 * </ul>
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public class DownsampleTransformService extends BaseIdentifiable
		implements GeneralDatumSamplesTransformService, SettingSpecifierProvider {

	/** The "sub sample" transform property flag. */
	public static final String SUB_SAMPLE_PROP = "subsample";

	/**
	 * A transform properties instance that can be used to signal "sub-sampling"
	 * mode to the transform service.2
	 */
	public static final Map<String, Object> SUB_SAMPLE_PROPS = Collections.singletonMap(SUB_SAMPLE_PROP,
			Boolean.TRUE);

	/** The {@code decimalScale} property default value. */
	public static final int DEFAULT_DECIMAL_SCALE = 3;

	/** The {@code minPropertyFormat} property default value. */
	public static final String DEFAULT_MIN_FORMAT = "%s_min";

	/** The {@code maxPropertyFormat} property default value. */
	public static final String DEFAULT_MAX_FORMAT = "%s_max";

	private final ConcurrentMap<String, AggregateDatumSamples> subSamplesBySource = new ConcurrentHashMap<>(
			8, 0.9f, 4);

	private int decimalScale = DEFAULT_DECIMAL_SCALE;
	private String minPropertyFormat = DEFAULT_MIN_FORMAT;
	private String maxPropertyFormat = DEFAULT_MAX_FORMAT;

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, ?> parameters) {
		if ( datum == null || datum.getSourceId() == null || samples == null ) {
			return samples;
		}
		final boolean sub = (parameters != null && parameters.containsKey(SUB_SAMPLE_PROP));
		AggregateDatumSamples agg = subSamplesBySource.computeIfAbsent(datum.getSourceId(),
				k -> new AggregateDatumSamples());
		synchronized ( agg ) {
			agg.addSample(samples);
			if ( !sub ) {
				GeneralDatumSamples out = downsample(agg);
				subSamplesBySource.remove(datum.getSourceId());
				return out;
			}
		}
		return null;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.samplefilter.downsample";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);

		results.add(new BasicTitleSettingSpecifier("status", statusValue()));
		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));
		results.add(new BasicTextFieldSettingSpecifier("minPropertyFormat", DEFAULT_MIN_FORMAT));
		results.add(new BasicTextFieldSettingSpecifier("maxPropertyFormat", DEFAULT_MAX_FORMAT));

		return results;
	}

	public String statusValue() {
		StringBuffer buf = new StringBuffer();
		for ( Map.Entry<String, AggregateDatumSamples> me : subSamplesBySource.entrySet() ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			AggregateDatumSamples agg = me.getValue();
			synchronized ( agg ) {
				buf.append(me.getKey()).append(": ").append(agg.addedSampleCount()).append(" samples");
			}
		}
		return buf.toString();
	}

	private GeneralDatumSamples downsample(AggregateDatumSamples s) {
		GeneralDatumSamples out = new GeneralDatumSamples();
		if ( s.getInstantaneous() != null ) {
			for ( Map.Entry<String, AggregateDatumProperty> me : s.getInstantaneous().entrySet() ) {
				out.putInstantaneousSampleValue(me.getKey(), me.getValue().average(decimalScale));
				out.putInstantaneousSampleValue(String.format(minPropertyFormat, me.getKey()),
						me.getValue().getMin());
				out.putInstantaneousSampleValue(String.format(maxPropertyFormat, me.getKey()),
						me.getValue().getMax());
			}
		}
		if ( s.getAccumulating() != null ) {
			for ( Map.Entry<String, AggregateDatumProperty> me : s.getAccumulating().entrySet() ) {
				out.putAccumulatingSampleValue(me.getKey(), me.getValue().last());
			}
		}
		if ( s.getStatus() != null ) {
			out.setStatus(s.getStatus());
		}
		if ( s.getTags() != null ) {
			out.setTags(s.getTags());
		}

		return out;
	}

	/**
	 * Get the decimal scale.
	 * 
	 * @return the scale
	 */
	public int getDecimalScale() {
		return decimalScale;
	}

	/**
	 * Set the decimal scale.
	 * 
	 * <p>
	 * This is the maximum decimal scale to round averaged results to.
	 * </p>
	 * 
	 * @param decimalScale
	 *        the scale to set; if less than {@literal 0} then {@literal 0} will
	 *        be used
	 */
	public void setDecimalScale(int decimalScale) {
		if ( decimalScale < 0 ) {
			decimalScale = 0;
		}
		this.decimalScale = decimalScale;
	}

	/**
	 * Get the property name format for "minimum" computed values.
	 * 
	 * @return the format; defaults to {@link #DEFAULT_MIN_FORMAT}
	 * @since 1.1
	 */
	public String getMinPropertyFormat() {
		return minPropertyFormat;
	}

	/**
	 * Set the property name format for "minimum" computed values.
	 * 
	 * <p>
	 * This accepts a standard {@link String#format(String, Object...)} style
	 * formatting template.
	 * 
	 * @param minPropertyFormat
	 *        the format to set
	 * @since 1.1
	 */
	public void setMinPropertyFormat(String minPropertyFormat) {
		this.minPropertyFormat = minPropertyFormat;
	}

	/**
	 * Get the property name format for "maximum" computed values.
	 * 
	 * @return the format; defaults to {@link #DEFAULT_MIN_FORMAT}
	 * @since 1.1
	 */
	public String getMaxPropertyFormat() {
		return maxPropertyFormat;
	}

	/**
	 * Set the property name format for "maximum" computed values.
	 * 
	 * <p>
	 * This accepts a standard {@link String#format(String, Object...)} style
	 * formatting template.
	 * 
	 * @param maxPropertyFormat
	 *        the format to set
	 * @since 1.1
	 */
	public void setMaxPropertyFormat(String maxPropertyFormat) {
		this.maxPropertyFormat = maxPropertyFormat;
	}

}
