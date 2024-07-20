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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
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
import net.solarnetwork.node.metrics.domain.BasicMetricAggregate;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.domain.MetricAggregate;
import net.solarnetwork.node.metrics.domain.MetricKey;
import net.solarnetwork.node.metrics.domain.ParameterizedMetricAggregate;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.DateUtils;

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
	 * @param metricDao
	 *        the metric DAO
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
	public String metricsUi() {
		return "metrics";
	}

	/**
	 * List command.
	 */
	public static final class MetricListCommand {

		private String start;
		private String end;
		private String type;
		private String name;
		private Integer offset;
		private Integer max;
		private boolean mostRecent;
		private List<MutableSortDescriptor> sorts;
		private Set<String> aggs;

		/**
		 * Constructor.
		 */
		public MetricListCommand() {
			super();
		}

		/**
		 * Get the start date.
		 *
		 * @return the start
		 */
		public final String getStart() {
			return start;
		}

		/**
		 * Set the start date.
		 *
		 * @param start
		 *        the start to set
		 */
		public final void setStart(String start) {
			this.start = start;
		}

		/**
		 * Get the end date.
		 *
		 * @return the end
		 */
		public final String getEnd() {
			return end;
		}

		/**
		 * Set the end date.
		 *
		 * @param end
		 *        the end to set
		 */
		public final void setEnd(String end) {
			this.end = end;
		}

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
		 * Get the "most recent" flag.
		 *
		 * @return the "most recent" flag
		 */
		public boolean isMostRecent() {
			return mostRecent;
		}

		/**
		 * Set the "most recent" flag.
		 *
		 * @param mostRecent
		 *        the "most recent" flag to set
		 */
		public void setMostRecent(boolean mostRecent) {
			this.mostRecent = mostRecent;
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

		/**
		 * Get the set of aggregates.
		 *
		 * @return the aggregates
		 */
		public final Set<String> getAggs() {
			return aggs;
		}

		/**
		 * Set the set of aggregates.
		 *
		 * <p>
		 * This is a set of {@link MetricAggregate} key values. All the values
		 * in {@link BasicMetricAggregate} are supported, along with {@code q:x}
		 * quantiles where {@code x} is an integer percent, like {@code q:25}
		 * for the 25th quantile.
		 * </p>
		 *
		 * @param aggregates
		 *        the aggregates to set
		 */
		public final void setAggs(Set<String> aggs) {
			this.aggs = aggs;
		}

		private Instant startDate() {
			if ( start == null || start.isEmpty() ) {
				return null;
			}
			ZonedDateTime date = DateUtils.parseIsoTimestamp(start, TimeZone.getDefault().toZoneId());
			return (date != null ? date.toInstant() : null);
		}

		private Instant endDate() {
			if ( end == null || end.isEmpty() ) {
				return null;
			}
			ZonedDateTime date = DateUtils.parseIsoTimestamp(end, TimeZone.getDefault().toZoneId());
			return (date != null ? date.toInstant() : null);
		}

		private MetricAggregate[] aggregates() {
			if ( aggs == null || aggs.isEmpty() ) {
				return null;
			}
			List<MetricAggregate> result = new ArrayList<>(aggs.size());
			for ( String key : aggs ) {
				if ( MetricAggregate.METRIC_TYPE_MINIMUM.equalsIgnoreCase(key) ) {
					result.add(BasicMetricAggregate.Minimum);
				} else if ( MetricAggregate.METRIC_TYPE_MAXIMUM.equalsIgnoreCase(key) ) {
					result.add(BasicMetricAggregate.Maximum);
				} else if ( MetricAggregate.METRIC_TYPE_AVERAGE.equalsIgnoreCase(key) ) {
					result.add(BasicMetricAggregate.Average);
				} else if ( key.startsWith("q:") || key.startsWith("Q:") && key.length() > 2 ) {
					try {
						Integer p = Integer.valueOf(key.substring(2));
						result.add(new ParameterizedMetricAggregate(MetricAggregate.METRIC_TYPE_QUANTILE,
								new Object[] { p / 100.0 },
								ParameterizedMetricAggregate.INTEGER_PERCENT_KEY));
					} catch ( NumberFormatException e ) {
						// ignore
					}
				}
			}
			return (result.isEmpty() ? null : result.toArray(new MetricAggregate[result.size()]));
		}

		/**
		 * Create a filter instance from this command.
		 *
		 * @return the filter instance
		 */
		public BasicMetricFilter toFilter() {
			final BasicMetricFilter filter = new BasicMetricFilter();
			filter.setType(type);
			filter.setName(name);
			filter.setOffset(offset);
			filter.setMax(max);

			filter.setStartDate(startDate());
			filter.setEndDate(endDate());

			MetricAggregate[] aggregates = aggregates();
			if ( aggregates != null && aggregates.length > 0 ) {
				filter.setAggregates(aggregates);
			} else if ( mostRecent ) {
				filter.setMostRecent(true);
			} else {
				filter.setWithoutTotalResultsCount(false);
			}
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
	public Result<FilterResults<Metric, MetricKey>> listMetrics(MetricListCommand cmd) {
		final MetricDao dao = OptionalService.service(this.metricDao);
		if ( dao == null ) {
			return Result.success(new BasicFilterResults<>(Collections.emptyList()));
		}

		final BasicMetricFilter filter = (cmd != null ? cmd.toFilter() : new BasicMetricFilter());

		FilterResults<Metric, MetricKey> results = dao.findFiltered(filter);

		return success(results);
	}

}
