/* ==================================================================
 * MetricDao.java - 14/07/2024 7:40:55 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.metrics.dao;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import java.util.List;
import net.solarnetwork.dao.BatchableDao;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.domain.MetricKey;

/**
 * DAO API for {@link Metric} entities.
 *
 * @author matt
 * @version 1.1
 */
public interface MetricDao extends GenericDao<Metric, MetricKey>,
		FilterableDao<Metric, MetricKey, MetricFilter>, BatchableDao<Metric> {

	/** The sort key for ordering by timestamp. */
	String SORT_BY_DATE = "date";

	/** The sort key for ordering by timestamp. */
	String SORT_BY_TYPE = "type";

	/** The sort key for ordering by timestamp. */
	String SORT_BY_NAME = "name";

	/** The sort key for ordering by timestamp. */
	String SORT_BY_VALUE = "value";

	/** Sort descriptor to sort by date, descending. */
	SortDescriptor SORT_BY_DATE_DESC = new SimpleSortDescriptor(MetricDao.SORT_BY_DATE, true);

	/** Sort descriptor to sort by type, ascending. */
	SortDescriptor SORT_BY_TYPE_ASC = new SimpleSortDescriptor(MetricDao.SORT_BY_TYPE);

	/** Sort descriptor to sort by name, ascending. */
	SortDescriptor SORT_BY_NAME_ASC = new SimpleSortDescriptor(MetricDao.SORT_BY_NAME);

	/**
	 * Sort descriptor list to sort by date descending, then type, then name.
	 */
	List<SortDescriptor> SORT_BY_DATE_DESC_TYPE_NAME = unmodifiableList(
			asList(SORT_BY_DATE_DESC, SORT_BY_TYPE_ASC, SORT_BY_NAME_ASC));

	/** Sort descriptor list to sort by type then name. */
	List<SortDescriptor> SORT_BY_TYPE_NAME = unmodifiableList(
			asList(SORT_BY_TYPE_ASC, SORT_BY_NAME_ASC));

	/** A batch parameter for a {@link MetricFilter} value. */
	String BATCH_PARAM_FILTER = "filter";

	/**
	 * Delete metrics matching a filter.
	 *
	 * @param filter
	 *        the filter
	 * @return the number of rows deleted
	 */
	int deleteFiltered(MetricFilter filter);

}
