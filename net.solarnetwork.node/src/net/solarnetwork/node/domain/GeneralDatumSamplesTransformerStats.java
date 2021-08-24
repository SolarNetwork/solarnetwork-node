/* ==================================================================
 * GeneralDatumSamplesTransformerStats.java - 23/08/2021 2:53:39 PM
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

package net.solarnetwork.node.domain;

import net.solarnetwork.util.StatCounter;

/**
 * Transformer statistics.
 * 
 * @author matt
 * @version 1.0
 * @since 1.89
 */
public enum GeneralDatumSamplesTransformerStats implements StatCounter.Stat {

	/** Count of datum passed into a transform service. */
	Input("input"),

	/** Count of datum unchanged by a transform service. */
	Ignored("ignored"),

	/** Count of datum removed by a transform service. */
	Filtered("filtered"),

	/** Count of datum modified by a transform service. */
	Modified("modified"),

	/** Count the number of errors encountered. */
	Errors("errors"),

	/** Milliseconds spent processing all input datum. */
	ProcessingTimeTotal("0rocessing ms"),

	/**
	 * Milliseconds spent processing all input datum that were not counted as
	 * ignored.
	 */
	ProcessingTimeNotIgnoredTotal("processing not-ignored ms"),

	;

	private String description;

	private GeneralDatumSamplesTransformerStats(String description) {
		this.description = description;
	}

	@Override
	public int getIndex() {
		return ordinal();
	}

	@Override
	public String getDescription() {
		return description;
	}

}
