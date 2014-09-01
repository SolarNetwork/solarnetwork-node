/* ==================================================================
 * GeneralNodeDatum.java - Aug 25, 2014 10:48:30 AM
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
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * General node datum.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatum extends BaseDatum implements Datum, Cloneable {

	private GeneralNodeDatumSamples samples;

	/**
	 * Put a value into the {@link GeneralNodeDatumSamples#getInstantaneous()}
	 * map, creating the sample if it doesn't exist.
	 * 
	 * @param key
	 *        the key to put
	 * @param n
	 *        the value to put
	 */
	public void putInstantaneousSampleValue(String key, Number n) {
		GeneralNodeDatumSamples s = samples;
		if ( s == null ) {
			s = new GeneralNodeDatumSamples();
			samples = s;
		}
		s.putInstantaneousSampleValue(key, n);
	}

	/**
	 * Put a value into the {@link GeneralNodeDatumSamples#getAccumulating()}
	 * map, creating the sample if it doesn't exist.
	 * 
	 * @param key
	 *        the key to put
	 * @param n
	 *        the value to put
	 */
	public void putAccumulatingSampleValue(String key, Number n) {
		GeneralNodeDatumSamples s = samples;
		if ( s == null ) {
			s = new GeneralNodeDatumSamples();
			samples = s;
		}
		s.putAccumulatingSampleValue(key, n);
	}

	/**
	 * Put a value into the {@link GeneralNodeDatumSamples#getStatus()} map,
	 * creating the sample if it doesn't exist.
	 * 
	 * @param key
	 *        the key to put
	 * @param value
	 *        the value to put
	 */
	public void putStatusSampleValue(String key, Object value) {
		GeneralNodeDatumSamples s = samples;
		if ( s == null ) {
			s = new GeneralNodeDatumSamples();
			samples = s;
		}
		s.putStatusSampleValue(key, value);
	}

	/**
	 * Get an Integer value from the
	 * {@link GeneralNodeDatumSamples#getInstantaneous()} map, or <em>null</em>
	 * if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Integer, or <em>null</em> if not available
	 */
	public Integer getInstantaneousSampleInteger(String key) {
		return (samples == null ? null : samples.getInstantaneousSampleInteger(key));
	}

	/**
	 * Get a Long value from the
	 * {@link GeneralNodeDatumSamples#getInstantaneous()} map, or <em>null</em>
	 * if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Long, or <em>null</em> if not available
	 */
	public Long getInstantaneousSampleLong(String key) {
		return (samples == null ? null : samples.getInstantaneousSampleLong(key));
	}

	/**
	 * Get a Float value from the
	 * {@link GeneralNodeDatumSamples#getInstantaneous()} map, or <em>null</em>
	 * if not available.
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
	 * {@link GeneralNodeDatumSamples#getInstantaneous()} map, or <em>null</em>
	 * if not available.
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
	 * {@link GeneralNodeDatumSamples#getInstantaneous()} map, or <em>null</em>
	 * if not available.
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
	 * {@link GeneralNodeDatumSamples#getAccumulating()} map, or <em>null</em>
	 * if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Integer, or <em>null</em> if not available
	 */
	public Integer getAccumulatingSampleInteger(String key) {
		return (samples == null ? null : samples.getAccumulatingSampleInteger(key));
	}

	/**
	 * Get a Long value from the
	 * {@link GeneralNodeDatumSamples#getAccumulating()} map, or <em>null</em>
	 * if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Long, or <em>null</em> if not available
	 */
	public Long getAccumulatingSampleLong(String key) {
		return (samples == null ? null : samples.getAccumulatingSampleLong(key));
	}

	/**
	 * Get a Float value from the
	 * {@link GeneralNodeDatumSamples#getAccumulating()} map, or <em>null</em>
	 * if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Float, or <em>null</em> if not available
	 */
	public Float getAccumulatingSampleFloat(String key) {
		return (samples == null ? null : samples.getAccumulatingSampleFloat(key));
	}

	/**
	 * Get a Double value from the
	 * {@link GeneralNodeDatumSamples#getAccumulating()} map, or <em>null</em>
	 * if not available.
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
	 * {@link GeneralNodeDatumSamples#getAccumulating()} map, or <em>null</em>
	 * if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an BigDecimal, or <em>null</em> if not available
	 */
	public BigDecimal getAccumulatingSampleBigDecimal(String key) {
		return (samples == null ? null : samples.getAccumulatingSampleBigDecimal(key));
	}

	/**
	 * Get an Integer value from the {@link GeneralNodeDatumSamples#getStatus()}
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
	 * Get a Long value from the {@link GeneralNodeDatumSamples#getStatus()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Long, or <em>null</em> if not available
	 */
	public Long getStatusSampleLong(String key) {
		return (samples == null ? null : samples.getStatusSampleLong(key));
	}

	/**
	 * Get a Float value from the {@link GeneralNodeDatumSamples#getStatus()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Float, or <em>null</em> if not available
	 */
	public Float getStatusSampleFloat(String key) {
		return (samples == null ? null : samples.getStatusSampleFloat(key));
	}

	/**
	 * Get a Double value from the {@link GeneralNodeDatumSamples#getStatus()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an Double, or <em>null</em> if not available
	 */
	public Double getStatusSampleDouble(String key) {
		return (samples == null ? null : samples.getStatusSampleDouble(key));
	}

	/**
	 * Get a BigDecimal value from the
	 * {@link GeneralNodeDatumSamples#getStatus()} map, or <em>null</em> if not
	 * available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as an BigDecimal, or <em>null</em> if not available
	 */
	public BigDecimal getStatusSampleBigDecimal(String key) {
		return (samples == null ? null : samples.getStatusSampleBigDecimal(key));
	}

	/**
	 * Get a String value from the {@link GeneralNodeDatumSamples#getStatus()}
	 * map, or <em>null</em> if not available.
	 * 
	 * @param key
	 *        the key of the value to get
	 * @return the value as a String, or <em>null</em> if not available
	 */
	public String getStatusSampleString(String key) {
		return (samples == null ? null : samples.getStatusSampleString(key));
	}

	/**
	 * Return <em>true</em> if {@code GeneralNodeDatumSamples#getTags()}
	 * contains {@code tag}.
	 * 
	 * @param tag
	 *        the tag value to test for existence
	 * @return boolean
	 */
	public boolean hasTag(String tag) {
		return (samples != null && samples.hasTag(tag));
	}

	/**
	 * Add a tag value via {@link GeneralNodeDatumSamples#addTag(String)}.
	 * 
	 * @param tag
	 *        the tag value to add
	 */
	public void addTag(String tag) {
		if ( tag == null ) {
			return;
		}
		GeneralNodeDatumSamples s = samples;
		if ( s == null ) {
			s = new GeneralNodeDatumSamples();
			samples = s;
		}
		s.addTag(tag);
	}

	/**
	 * Add a tag value via {@link GeneralNodeDatumSamples#removeTag(String)}.
	 * 
	 * @param tag
	 *        the tag value to add
	 */
	public void removeTag(String tag) {
		if ( tag == null ) {
			return;
		}
		GeneralNodeDatumSamples s = samples;
		if ( s != null ) {
			s.removeTag(tag);
		}
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

	public GeneralNodeDatumSamples getSamples() {
		return samples;
	}

	public void setSamples(GeneralNodeDatumSamples samples) {
		this.samples = samples;
	}

}
