/* ==================================================================
 * AppendableDatumHistorian.java - 10 Nov 2026 3:00:10 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service;

import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * API for a {@link DatumHistorian} that can be added to.
 *
 * @author matt
 * @version 1.0
 */
public interface AppendableDatumHistorian extends DatumHistorian {

	/**
	 * Add a datum to the history.
	 *
	 * @param datum
	 *        the datum
	 */
	void add(NodeDatum datum);

}
