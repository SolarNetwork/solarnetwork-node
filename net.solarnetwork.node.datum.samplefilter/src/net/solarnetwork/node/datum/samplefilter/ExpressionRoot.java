/* ==================================================================
 * ExpressionRoot.java - 13/05/2021 8:31:37 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.domain.GeneralDatumSamplesOperations;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.DatumExpressionRoot;
import net.solarnetwork.node.domain.GeneralDatum;

/**
 * An expression root object implementation that acts like a composite map of
 * parameters, sample data, and datum properties.
 * 
 * @author matt
 * @version 1.0
 * @since 1.6
 */
public class ExpressionRoot extends AbstractMap<String, Object>
		implements DatumExpressionRoot, Map<String, Object> {

	private final Datum datum;
	private final GeneralDatumSamplesOperations datumOps;
	private final GeneralDatumSamplesOperations sample;
	private final Map<String, ?> parameters;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 * @param
	 */
	public ExpressionRoot(Datum datum, GeneralDatumSamplesOperations sample, Map<String, ?> parameters) {
		super();
		this.datum = datum;
		if ( datum instanceof GeneralDatum ) {
			this.datumOps = ((GeneralDatum) datum).asSampleOperations();
		} else {
			this.datumOps = null;
		}
		this.sample = sample;
		this.parameters = parameters;
	}

	@Override
	public Datum getDatum() {
		return datum;
	}

	@Override
	public Map<String, ?> getData() {
		return this;
	}

	@Override
	public Map<String, ?> getProps() {
		return this;
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public Object get(Object key) {
		if ( key == null ) {
			return null;
		}
		String k = key.toString();
		Object o = null;
		if ( parameters != null ) {
			o = parameters.get(key);
			if ( o != null ) {
				return o;
			}
		}
		o = getSampleValue(sample, k);
		if ( o != null ) {
			return o;
		}
		o = getSampleValue(datumOps, k);
		if ( o != null ) {
			return o;
		}
		return super.get(key);
	}

	private static final GeneralDatumSamplesType[] TYPES = new GeneralDatumSamplesType[] {
			GeneralDatumSamplesType.Instantaneous, GeneralDatumSamplesType.Accumulating,
			GeneralDatumSamplesType.Status };

	private Object getSampleValue(GeneralDatumSamplesOperations ops, String key) {
		if ( ops == null ) {
			return null;
		}
		for ( GeneralDatumSamplesType type : TYPES ) {
			Object o = ops.getSampleValue(type, key);
			if ( o != null ) {
				return o;
			}
		}
		return null;
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return new EntrySet();
	}

	private final class EntrySet extends AbstractSet<Entry<String, Object>>
			implements Set<Entry<String, Object>> {

		private final Set<Entry<String, Object>> delegate;

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private EntrySet() {
			super();
			delegate = new LinkedHashSet<>();
			if ( parameters != null ) {
				delegate.addAll((Set) parameters.entrySet());
			}
			if ( sample != null ) {
				for ( GeneralDatumSamplesType type : TYPES ) {
					Map<String, ?> data = sample.getSampleData(type);
					if ( data != null ) {
						delegate.addAll((Set) data);
					}
				}
			}
			if ( datumOps != null ) {
				for ( GeneralDatumSamplesType type : TYPES ) {
					Map<String, ?> data = datumOps.getSampleData(type);
					if ( data != null ) {
						delegate.addAll((Set) data);
					}
				}
			}
		}

		@Override
		public Iterator<Entry<String, Object>> iterator() {
			return delegate.iterator();
		}

		@Override
		public int size() {
			return delegate.size();
		}

	}

}
