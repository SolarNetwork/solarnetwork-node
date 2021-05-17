/* ==================================================================
 * GeneralDatumSupport.java - Oct 20, 2014 12:01:24 PM
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

package net.solarnetwork.node.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesOperations;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;

/**
 * Base Datum implementation with {@link GeneralDatumSamples} support.
 * 
 * @author matt
 * @version 1.4
 */
public abstract class GeneralDatumSupport extends BaseDatum implements Datum, GeneralDatum, Cloneable {

	private GeneralDatumSamples samples;

	@Override
	public GeneralDatumSupport clone() {
		return (GeneralDatumSupport) super.clone();
	}

	/**
	 * Create a new {@link GeneralDatumSamples} instance.
	 * 
	 * <p>
	 * This method is called when a new samples instance is needed, so extending
	 * classes can return a custom class if needed.
	 * </p>
	 * 
	 * @return New instance.
	 * @since 1.1
	 */
	protected GeneralDatumSamples newSamplesInstance() {
		return new GeneralDatumSamples();
	}

	@Override
	public GeneralDatumSamplesOperations asSampleOperations() {
		return getSamples();
	}

	@Override
	public MutableGeneralDatumSamplesOperations asMutableSampleOperations() {
		GeneralDatumSamples s = getSamples();
		if ( s == null ) {
			s = newSamplesInstance();
			samples = s;

		}
		return s;
	}

	/**
	 * Put a general sample value.
	 * 
	 * <p>
	 * This is a short-cut for calling
	 * {@link GeneralDatumSamples#putSampleValue(GeneralDatumSamplesType, String, Object)}.
	 * </p>
	 * 
	 * @param type
	 *        the sample type
	 * @param key
	 *        the property key
	 * @param value
	 *        the property value
	 * @since 1.3
	 */
	public void putSampleValue(GeneralDatumSamplesType type, String key, Object value) {
		GeneralDatumSamples s = getSamples();
		if ( s == null ) {
			s = newSamplesInstance();
			setSamples(s);
		}
		s.putSampleValue(type, key, value);
	}

	/**
	 * Get a sample value.
	 * 
	 * @param <V>
	 *        the value type
	 * @param type
	 *        the type of sample data to get
	 * @param key
	 *        the key of the value, or tag name, to get
	 * @return the value cast as a {@code V}, or {@literal null} if not
	 *         available
	 * @see GeneralDatumSamples#getSampleValue(GeneralDatumSamplesType, String)
	 * @since 1.3
	 */
	public <V> V getSampleValue(GeneralDatumSamplesType type, String key) {
		GeneralDatumSamples s = getSamples();
		V result = null;
		if ( s != null ) {
			result = s.getSampleValue(type, key);
		}
		return result;
	}

	/**
	 * Put a value into the {@link GeneralDatumSamples#getInstantaneous()} map,
	 * creating the sample if it doesn't exist.
	 * 
	 * @param key
	 *        the key to put
	 * @param n
	 *        the value to put
	 */
	public void putInstantaneousSampleValue(String key, Number n) {
		GeneralDatumSamples s = samples;
		if ( s == null ) {
			s = newSamplesInstance();
			samples = s;
		}
		s.putInstantaneousSampleValue(key, n);
	}

	/**
	 * Put a value into the {@link GeneralDatumSamples#getAccumulating()} map,
	 * creating the sample if it doesn't exist.
	 * 
	 * @param key
	 *        the key to put
	 * @param n
	 *        the value to put
	 */
	public void putAccumulatingSampleValue(String key, Number n) {
		GeneralDatumSamples s = samples;
		if ( s == null ) {
			s = newSamplesInstance();
			samples = s;
		}
		s.putAccumulatingSampleValue(key, n);
	}

	/**
	 * Put a value into the {@link GeneralDatumSamples#getStatus()} map,
	 * creating the sample if it doesn't exist.
	 * 
	 * @param key
	 *        the key to put
	 * @param value
	 *        the value to put
	 */
	public void putStatusSampleValue(String key, Object value) {
		GeneralDatumSamples s = samples;
		if ( s == null ) {
			s = newSamplesInstance();
			samples = s;
		}
		s.putStatusSampleValue(key, value);
	}

	/**
	 * Get an Integer value from the
	 * {@link GeneralDatumSamples#getInstantaneous()} map, or <em>null</em> if
	 * not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Integer, or <em>null</em> if not available
	 */
	public Integer getInstantaneousSampleInteger(String key) {
		return (samples == null ? null : samples.getInstantaneousSampleInteger(key));
	}

	/**
	 * Get a Long value from the {@link GeneralDatumSamples#getInstantaneous()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Long, or <em>null</em> if not available
	 */
	public Long getInstantaneousSampleLong(String key) {
		return (samples == null ? null : samples.getInstantaneousSampleLong(key));
	}

	/**
	 * Get a Float value from the {@link GeneralDatumSamples#getInstantaneous()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Float, or <em>null</em> if not available
	 */
	public Float getInstantaneousSampleFloat(String key) {
		return (samples == null ? null : samples.getInstantaneousSampleFloat(key));
	}

	/**
	 * Get a Double value from the
	 * {@link GeneralDatumSamples#getInstantaneous()} map, or <em>null</em> if
	 * not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Double, or <em>null</em> if not available
	 */
	public Double getInstantaneousSampleDouble(String key) {
		return (samples == null ? null : samples.getInstantaneousSampleDouble(key));
	}

	/**
	 * Get a BigDecimal value from the
	 * {@link GeneralDatumSamples#getInstantaneous()} map, or <em>null</em> if
	 * not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an BigDecimal, or <em>null</em> if not available
	 */
	public BigDecimal getInstantaneousSampleBigDecimal(String key) {
		return (samples == null ? null : samples.getInstantaneousSampleBigDecimal(key));
	}

	/**
	 * Get an Integer value from the
	 * {@link GeneralDatumSamples#getAccumulating()} map, or <em>null</em> if
	 * not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Integer, or <em>null</em> if not available
	 */
	public Integer getAccumulatingSampleInteger(String key) {
		return (samples == null ? null : samples.getAccumulatingSampleInteger(key));
	}

	/**
	 * Get a Long value from the {@link GeneralDatumSamples#getAccumulating()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Long, or <em>null</em> if not available
	 */
	public Long getAccumulatingSampleLong(String key) {
		return (samples == null ? null : samples.getAccumulatingSampleLong(key));
	}

	/**
	 * Get a Float value from the {@link GeneralDatumSamples#getAccumulating()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Float, or <em>null</em> if not available
	 */
	public Float getAccumulatingSampleFloat(String key) {
		return (samples == null ? null : samples.getAccumulatingSampleFloat(key));
	}

	/**
	 * Get a Double value from the {@link GeneralDatumSamples#getAccumulating()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Double, or <em>null</em> if not available
	 */
	public Double getAccumulatingSampleDouble(String key) {
		return (samples == null ? null : samples.getAccumulatingSampleDouble(key));
	}

	/**
	 * Get a BigDecimal value from the
	 * {@link GeneralDatumSamples#getAccumulating()} map, or <em>null</em> if
	 * not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an BigDecimal, or <em>null</em> if not available
	 */
	public BigDecimal getAccumulatingSampleBigDecimal(String key) {
		return (samples == null ? null : samples.getAccumulatingSampleBigDecimal(key));
	}

	/**
	 * Get an Integer value from the {@link GeneralDatumSamples#getStatus()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Integer, or <em>null</em> if not available
	 */
	public Integer getStatusSampleInteger(String key) {
		return (samples == null ? null : samples.getStatusSampleInteger(key));
	}

	/**
	 * Get a Long value from the {@link GeneralDatumSamples#getStatus()} map, or
	 * <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Long, or <em>null</em> if not available
	 */
	public Long getStatusSampleLong(String key) {
		return (samples == null ? null : samples.getStatusSampleLong(key));
	}

	/**
	 * Get a Float value from the {@link GeneralDatumSamples#getStatus()} map,
	 * or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Float, or <em>null</em> if not available
	 */
	public Float getStatusSampleFloat(String key) {
		return (samples == null ? null : samples.getStatusSampleFloat(key));
	}

	/**
	 * Get a Double value from the {@link GeneralDatumSamples#getStatus()} map,
	 * or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Double, or <em>null</em> if not available
	 */
	public Double getStatusSampleDouble(String key) {
		return (samples == null ? null : samples.getStatusSampleDouble(key));
	}

	/**
	 * Get a BigDecimal value from the {@link GeneralDatumSamples#getStatus()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an BigDecimal, or <em>null</em> if not available
	 */
	public BigDecimal getStatusSampleBigDecimal(String key) {
		return (samples == null ? null : samples.getStatusSampleBigDecimal(key));
	}

	/**
	 * Get a String value from the {@link GeneralDatumSamples#getStatus()} map,
	 * or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as a String, or <em>null</em> if not available
	 */
	public String getStatusSampleString(String key) {
		return (samples == null ? null : samples.getStatusSampleString(key));
	}

	/**
	 * Return <em>true</em> if {@code GeneralDatumSamples#getTags()} contains
	 * {@code tag}.
	 * 
	 * @param tag
	 *        the tag value to test for existence
	 * @return boolean
	 */
	public boolean hasTag(String tag) {
		return (samples != null && samples.hasTag(tag));
	}

	/**
	 * Add a tag value via {@link GeneralDatumSamples#addTag(String)}.
	 * 
	 * @param tag
	 *        the tag value to add
	 */
	public void addTag(String tag) {
		if ( tag == null ) {
			return;
		}
		GeneralDatumSamples s = samples;
		if ( s == null ) {
			s = newSamplesInstance();
			samples = s;
		}
		s.addTag(tag);
	}

	/**
	 * Add a tag value via {@link GeneralDatumSamples#removeTag(String)}.
	 * 
	 * @param tag
	 *        the tag value to add
	 */
	public void removeTag(String tag) {
		if ( tag == null ) {
			return;
		}
		GeneralDatumSamples s = samples;
		if ( s != null ) {
			s.removeTag(tag);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.2
	 */
	@Override
	public Map<String, ?> getSampleData() {
		return (samples != null ? samples.getSampleData() : null);
	}

	@Override
	protected Map<String, Object> createSimpleMap() {
		Map<String, Object> map = super.createSimpleMap();

		// recreate tags as an array, instead of a Set
		if ( samples != null ) {
			Set<String> tags = samples.getTags();
			if ( tags != null ) {
				String[] tagArray = tags.toArray(new String[tags.size()]);
				if ( tagArray.length > 0 ) {
					map.put("tags", tagArray);
				} else {
					map.remove("tags");
				}
			}
		}

		return map;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append("{sourceId=");
		builder.append(getSourceId());
		builder.append(",samples=");
		if ( samples != null ) {
			builder.append(samples.getSampleData());
		}
		builder.append("}");
		return builder.toString();
	}

	public GeneralDatumSamples getSamples() {
		return samples;
	}

	public void setSamples(GeneralDatumSamples samples) {
		this.samples = samples;
	}

}
