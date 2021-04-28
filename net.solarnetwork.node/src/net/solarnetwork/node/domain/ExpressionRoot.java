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

/**
 * An object to use as the "root" for
 * {@link net.solarnetwork.support.ExpressionService} evaluation.
 * 
 * @author matt
 * @version 1.1
 * @since 1.79
 */
public class ExpressionRoot {

	private final GeneralNodeDatum datum;
	private final Map<String, ?> datumProps;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 */
	public ExpressionRoot(GeneralNodeDatum datum) {
		super();
		this.datum = datum;
		this.datumProps = (datum != null ? datum.getSampleData() : Collections.emptyMap());
	}

	/**
	 * Get the datum.
	 * 
	 * @return the datum
	 */
	public GeneralNodeDatum getDatum() {
		return datum;
	}

	/**
	 * Alias for {@code datum.getSampleData()}.
	 * 
	 * @return the datum sample data, never {@literal null}
	 */
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
			builder.append(", ");
		}
		if ( datumProps != null ) {
			builder.append("props=");
			builder.append(datumProps);
		}
		builder.append("}");
		return builder.toString();
	}

}
