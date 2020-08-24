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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
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

	private final ConcurrentMap<String, List<GeneralDatumSamples>> subSamplesBySource = new ConcurrentHashMap<>(
			8, 0.9f, 4);

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, ?> parameters) {
		if ( datum == null || datum.getSourceId() == null || samples == null ) {
			return samples;
		}
		final boolean sub = (parameters != null && parameters.containsKey(SUB_SAMPLE_PROP));
		List<GeneralDatumSamples> subSamples = subSamplesBySource.computeIfAbsent(datum.getSourceId(),
				k -> new ArrayList<>(16));
		synchronized ( subSamples ) {
			subSamples.add(samples);
			if ( !sub ) {
				GeneralDatumSamples out = downsample(subSamples);
				subSamples.clear();
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

		results.add(new BasicTitleSettingSpecifier("uid", getUid()));

		return results;
	}

	private class Agg {

		private int count;
		private BigDecimal total;
		private BigDecimal min;
		private BigDecimal max;

		private Agg(BigDecimal val) {
			super();
			this.count = 1;
			this.total = (val == null ? BigDecimal.ZERO : val);
			this.min = total;
			this.max = total;
		}

		private void accumulate(BigDecimal val) {
			count++;
			if ( val == null ) {
				val = BigDecimal.ZERO;
			}
			total = total.add(val);
			if ( val.compareTo(min) < 0 ) {
				min = val;
			} else if ( val.compareTo(max) > 0 ) {
				max = val;
			}
		}

		private BigDecimal average() {
			return total.divide(new BigDecimal(count));
		}
	}

	private GeneralDatumSamples downsample(List<GeneralDatumSamples> subSamples) {
		Map<String, Agg> inst = null;
		Map<String, Number> accu = null;
		Map<String, Object> stat = null;
		GeneralDatumSamples out = new GeneralDatumSamples();
		for ( GeneralDatumSamples s : subSamples ) {
			if ( s.getInstantaneous() != null ) {
				for ( String name : s.getInstantaneous().keySet() ) {
					BigDecimal v = s.getInstantaneousSampleBigDecimal(name);
					if ( v != null ) {
						if ( inst == null ) {
							inst = new LinkedHashMap<>(8);
						}
						Agg a = inst.get(name);
						if ( a == null ) {
							a = new Agg(v);
							inst.put(name, a);
						} else {
							a.accumulate(v);
						}
					}
				}
			}
			if ( s.getAccumulating() != null ) {
				for ( Map.Entry<String, Number> me : s.getAccumulating().entrySet() ) {
					if ( accu == null ) {
						accu = new LinkedHashMap<>(8);
					}
					accu.put(me.getKey(), me.getValue());
				}
			}
			if ( s.getStatus() != null ) {
				for ( Map.Entry<String, Object> me : s.getStatus().entrySet() ) {
					if ( stat == null ) {
						stat = new LinkedHashMap<>(8);
					}
					stat.put(me.getKey(), me.getValue());
				}
			}
			if ( s.getTags() != null ) {
				if ( out.getTags() == null ) {
					out.setTags(new LinkedHashSet<>(8));
				}
				out.getTags().addAll(s.getTags());
			}
		}
		if ( inst != null ) {
			for ( Map.Entry<String, Agg> me : inst.entrySet() ) {
				out.putInstantaneousSampleValue(me.getKey(), me.getValue().average());
				out.putInstantaneousSampleValue(String.format("%s_min", me.getKey()), me.getValue().min);
				out.putInstantaneousSampleValue(String.format("%s_max", me.getKey()), me.getValue().max);
			}
		}
		if ( accu != null ) {
			out.setAccumulating(accu);
		}
		if ( stat != null ) {
			out.setStatus(stat);
		}
		return out;
	}

}
