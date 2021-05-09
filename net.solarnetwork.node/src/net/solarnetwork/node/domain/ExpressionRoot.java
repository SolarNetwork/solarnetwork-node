/* ==================================================================
 * ExpressionRoot.java - 20/02/2019 10:21:55 am
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

package net.solarnetwork.node.domain;

import java.util.Collections;
import java.util.Map;
import net.solarnetwork.util.MapBeanProxy;

/**
 * An object to use as the "root" for
 * {@link net.solarnetwork.support.ExpressionService} evaluation.
 * 
 * <p>
 * This object extends {@link MapBeanProxy} to allow all datum sample properties
 * to be exposed as top-level expression properties.
 * </p>
 * 
 * @author matt
 * @version 1.2
 * @since 1.79
 */
public class ExpressionRoot extends MapBeanProxy implements DatumExpressionRoot {

	private final Datum datum;
	private final Map<String, ?> datumProps;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 */
	public ExpressionRoot(Datum datum) {
		this(null, datum);
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the map data
	 * @param datum
	 *        the datum currently being populated
	 * @since 1.2
	 */
	public ExpressionRoot(Map<String, ?> data, Datum datum) {
		super(data);
		this.datum = datum;
		this.datumProps = (datum != null ? datum.getSampleData() : Collections.emptyMap());
	}

	@Override
	public Datum getDatum() {
		return datum;
	}

	@Override
	public Map<String, ?> getProps() {
		return datumProps;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ExpressionRoot{");
		if ( datum != null ) {
			builder.append("datum=");
			builder.append(datum);
		}
		Map<String, ?> data = getData();
		if ( data != null ) {
			builder.append(", data=");
			builder.append(data);
		}
		Map<String, ?> props = getProps();
		if ( props != null && props != data ) {
			builder.append(", props=");
			builder.append(props);
		}
		builder.append("}");
		return builder.toString();
	}

}
