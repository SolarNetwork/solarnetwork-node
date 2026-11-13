/* ==================================================================
 * BaseSolarQueryController.java - 13 Nov 2026 3:41:19 pm
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

package net.solarnetwork.node.setup.web.api;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import jakarta.servlet.ServletRequest;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.service.FilterableDatumHistorian;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.setup.web.domain.DatumFilterCommand;
import net.solarnetwork.node.setup.web.support.DatumHistorianSelector;
import net.solarnetwork.node.setup.web.support.LenientAggregationFormatter;
import net.solarnetwork.node.setup.web.support.TemporalFormatter;

/**
 * Base class for controllers of the SolarQuery compatible API.
 *
 * @author matt
 * @version 1.0
 * @since 5.4
 */
public abstract class BaseSolarQueryController {

	/** The CSV media type value. */
	public static final String TEXT_CSV_VALUE = "text/csv";

	/** The identity service. */
	protected final IdentityService identityService;

	/** The datum historian selector. */
	protected final DatumHistorianSelector historianSelector;

	/**
	 * A datum historian and the filter to query it with.
	 *
	 * @param historian
	 *        the historian
	 * @param filter
	 *        the filter
	 */
	protected record HistorianQuery(FilterableDatumHistorian historian, BasicNodeDatumFilter filter) {

	}

	/**
	 * Constructor.
	 *
	 * @param identityService
	 *        the identity service
	 * @param historianSelector
	 *        the datum historian selector
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	protected BaseSolarQueryController(IdentityService identityService,
			DatumHistorianSelector historianSelector) {
		super();
		this.identityService = requireNonNullArgument(identityService, "identityService");
		this.historianSelector = requireNonNullArgument(historianSelector, "historianSelector");
	}

	/**
	 * Configure request parameter binding.
	 *
	 * <p>
	 * Dates are parsed in the same formats as SolarQuery, and an unknown
	 * aggregation is ignored.
	 * </p>
	 *
	 * @param binder
	 *        the binder
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
		binder.addCustomFormatter(TemporalFormatter.SOLARNET_INSTANT_FORMATTER, Instant.class);
		binder.addCustomFormatter(TemporalFormatter.SOLARNET_LOCAL_DATE_TIME_FORMATTER,
				LocalDateTime.class);
		binder.addCustomFormatter(LenientAggregationFormatter.INSTANCE, Aggregation.class);
	}

	/**
	 * Get the historian query for a request.
	 *
	 * <p>
	 * The query is limited to the node's own datum, using the time zone of
	 * {@link #zone()} for any local dates.
	 * </p>
	 *
	 * @param criteria
	 *        the request criteria
	 * @param request
	 *        the request
	 * @return the query, or {@code null} if the request can match nothing: the
	 *         node is not associated, the criteria are for other nodes, or the
	 *         historian of the request is not available
	 */
	protected @Nullable HistorianQuery historianQuery(DatumFilterCommand criteria,
			ServletRequest request) {
		final Long nodeId = identityService.getNodeId();
		if ( nodeId == null || !criteria.includesNode(nodeId) ) {
			return null;
		}
		final FilterableDatumHistorian historian = historianSelector.historian(request);
		if ( historian == null ) {
			return null;
		}
		return new HistorianQuery(historian, criteria.toNodeDatumFilter(nodeId, zone()));
	}

	/**
	 * Get the node's time zone.
	 *
	 * @return the system time zone
	 */
	protected ZoneId zone() {
		return ZoneId.systemDefault();
	}

}
