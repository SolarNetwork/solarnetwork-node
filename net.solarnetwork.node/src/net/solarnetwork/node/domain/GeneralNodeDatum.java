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

import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * General node datum.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatum extends BaseDatum implements Datum, Cloneable {

	private GeneralNodeDatumSamples samples;

	public GeneralNodeDatumSamples getSamples() {
		return samples;
	}

	public void setSamples(GeneralNodeDatumSamples samples) {
		this.samples = samples;
	}

}
