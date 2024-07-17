/* ==================================================================
 * MetricController.java - 17/07/2024 8:21:08 am
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

package net.solarnetwork.node.setup.web;

import static java.util.stream.Collectors.toList;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.MutableSortDescriptor;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.node.metrics.dao.BasicMetricFilter;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.domain.MetricKey;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;

/**
 * Web controller for metrics support.
 *
 * @author matt
 * @version 1.0
 */
@ServiceAwareController
@RequestMapping("/a/metrics")
public class MetricController {

	private final OptionalService<MetricDao> metricDao;

	/**
	 * Constructor.
	 *
	 * @param serviceRegistry
	 *        the service registry
	 * @param settingsService
	 *        the settings service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MetricController(@Qualifier("metricDao") OptionalService<MetricDao> metricDao) {
		super();
		this.metricDao = requireNonNullArgument(metricDao, "metricDao");
	}

	/**
	 * Metrics UI.
	 *
	 * @return the Metrics view name
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public String datumDataSourcesUi() {
		return "metrics";
	}

	/**
	 * List command.
	 */
	public static final class MetricListCommand {

		private String type;
		private String name;
		private Integer offset;
		private Integer max;
		private List<MutableSortDescriptor> sorts;

		/**
		 * Get the type.
		 *
		 * @return the type
		 */
		public final String getType() {
			return type;
		}

		/**
		 * Set the type.
		 *
		 * @param type
		 *        the type to set
		 */
		public final void setType(String type) {
			this.type = type;
		}

		/**
		 * Get the name.
		 *
		 * @return the name
		 */
		public final String getName() {
			return name;
		}

		/**
		 * Set the name.
		 *
		 * @param name
		 *        the name to set
		 */
		public final void setName(String name) {
			this.name = name;
		}

		/**
		 * Get the offset.
		 *
		 * @return the offset
		 */
		public final Integer getOffset() {
			return offset;
		}

		/**
		 * Set the offset.
		 *
		 * @param offset
		 *        the offset to set
		 */
		public final void setOffset(Integer offset) {
			this.offset = offset;
		}

		/**
		 * Get the max.
		 *
		 * @return the max
		 */
		public final Integer getMax() {
			return max;
		}

		/**
		 * Set the max.
		 *
		 * @param max
		 *        the max to set
		 */
		public final void setMax(Integer max) {
			this.max = max;
		}

		/**
		 * Get the sorts.
		 *
		 * @return the sorts
		 */
		public final List<MutableSortDescriptor> getSorts() {
			return sorts;
		}

		/**
		 * Set the sorts.
		 *
		 * @param sorts
		 *        the sorts to set
		 */
		public final void setSorts(List<MutableSortDescriptor> sorts) {
			this.sorts = sorts;
		}

		public BasicMetricFilter toFilter() {
			final BasicMetricFilter filter = new BasicMetricFilter();
			filter.setType(type);
			filter.setName(name);
			filter.setOffset(offset);
			filter.setMax(max);
			filter.setWithoutTotalResultsCount(false);
			if ( sorts != null ) {
				filter.setSorts(sorts.stream().map(s -> (SortDescriptor) s).collect(toList()));
			}
			return filter;
		}

	}

	/**
	 * List metrics.
	 *
	 * @param cmd
	 *        the command arguments
	 * @return the resulting list of metrics
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<FilterResults<Metric, MetricKey>> listDatumDataSources(MetricListCommand cmd) {
		final MetricDao dao = OptionalService.service(this.metricDao);
		if ( dao == null ) {
			return Result.success(new BasicFilterResults<>(Collections.emptyList()));
		}

		final BasicMetricFilter filter = (cmd != null ? cmd.toFilter() : new BasicMetricFilter());

		FilterResults<Metric, MetricKey> results = dao.findFiltered(filter);

		return success(results);
	}

}
