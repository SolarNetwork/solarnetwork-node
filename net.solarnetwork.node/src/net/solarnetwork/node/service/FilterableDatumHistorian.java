/* ==================================================================
 * FilterableDatumHistorian.java - 11 Nov 2026 8:46:23 am
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

import java.util.List;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.node.dao.NodeDatumFilter;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * Extension of {@link DatumHistorian} with DAO filtering support.
 *
 * <p>
 * The {@code SORT_BY_*} constants are the supported sort keys. Results are
 * sorted by {@link #DEFAULT_SORTS} when no sorts are given.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public interface FilterableDatumHistorian
		extends DatumHistorian, FilterableDao<NodeDatum, DatumId, NodeDatumFilter> {

	/** A sort key for the node ID. */
	String SORT_BY_NODE = "node";

	/** A sort key for the location ID. */
	String SORT_BY_LOCATION = "loc";

	/** A sort key for the source ID. */
	String SORT_BY_SOURCE = "source";

	/** A sort key for the datum timestamp. */
	String SORT_BY_TIME = "time";

	/**
	 * The default sorts: by node, location, source, and then time.
	 *
	 * <p>
	 * This keeps the datum of each stream together, oldest first.
	 * </p>
	 */
	List<SortDescriptor> DEFAULT_SORTS = List.of(new SimpleSortDescriptor(SORT_BY_NODE),
			new SimpleSortDescriptor(SORT_BY_LOCATION), new SimpleSortDescriptor(SORT_BY_SOURCE),
			new SimpleSortDescriptor(SORT_BY_TIME));

}
