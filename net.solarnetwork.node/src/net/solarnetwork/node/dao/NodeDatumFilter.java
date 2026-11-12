/* ==================================================================
 * NodeDatumFilter.java - 11 Nov 2026 8:43:22 am
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

package net.solarnetwork.node.dao;

import net.solarnetwork.dao.AggregationCriteria;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.DatumObjectCriteria;
import net.solarnetwork.dao.LocalDateRangeCriteria;
import net.solarnetwork.dao.OptimizedQueryCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.dao.RecentCriteria;
import net.solarnetwork.dao.SourceCriteria;
import net.solarnetwork.dao.StreamCriteria;
import net.solarnetwork.dao.TypeCriteria;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * Filter API for {@link NodeDatum} related entities.
 *
 * <p>
 * The {@link AggregationCriteria} ask for aggregated results. A historian that
 * does not support aggregation can ignore them and return datum that are not
 * aggregated.
 * </p>
 *
 * <p>
 * The {@link StreamCriteria} stream IDs are those a historian gives its
 * streams, such as in the stream metadata returned by
 * {@link net.solarnetwork.node.service.FilterableDatumHistorian#findDatumStreamMetadata(NodeDatumFilter)}.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public interface NodeDatumFilter extends DateRangeCriteria, LocalDateRangeCriteria, AggregationCriteria,
		DatumObjectCriteria, SourceCriteria, StreamCriteria, TypeCriteria, PaginationCriteria,
		OptimizedQueryCriteria, RecentCriteria {

}
