/* ==================================================================
 * NodesController.java - 12 Nov 2026 1:19:47 pm
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

import static org.springframework.http.MediaType.APPLICATION_CBOR_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.setup.web.domain.DatumFilterCommand;
import net.solarnetwork.node.setup.web.domain.NodeSourcePK;
import net.solarnetwork.node.setup.web.support.DatumHistorianSelector;
import net.solarnetwork.node.setup.web.support.GlobalExceptionRestController;

/**
 * API for node related data.
 *
 * <p>
 * This controller offers some compatibility with the SolarQuery API, under both
 * the {@code /api/v1/sec} and {@code /solarquery/api/v1/sec} paths.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.4
 */
@GlobalExceptionRestController
@RequestMapping({ "/api/v1/sec/nodes", "/solarquery/api/v1/sec/nodes" })
public class NodesController extends BaseSolarQueryController {

	/**
	 * Constructor.
	 *
	 * @param identityService
	 *        the identity service to use
	 * @param historianSelector
	 *        the datum historian selector to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public NodesController(IdentityService identityService, DatumHistorianSelector historianSelector) {
		super(identityService, historianSelector);
	}

	/**
	 * Get a listing of nodes for the active user.
	 *
	 * @param req
	 *        the HTTP request
	 * @return The list of nodes available to the active user.
	 */
	@GetMapping(value = { "", "/" },
			produces = { APPLICATION_JSON_VALUE, APPLICATION_CBOR_VALUE, TEXT_CSV_VALUE })
	@ResponseBody
	public Result<Set<Long>> getAvailableNodes(final HttpServletRequest req) {
		final Long nodeId = identityService.getNodeId();
		return Result.success(nodeId != null ? Set.of(nodeId) : Set.of());
	}

	/**
	 * Find available node sources that match a search criteria.
	 *
	 * <p>
	 * The sources are those of the node streams in the datum history of the
	 * request, ordered by source ID. Date criteria are ignored.
	 * </p>
	 *
	 * @param req
	 *        the HTTP request
	 * @param criteria
	 *        the search criteria
	 * @return the matching set of node sources
	 */
	@ResponseBody
	@GetMapping(value = "/sources",
			produces = { APPLICATION_JSON_VALUE, APPLICATION_CBOR_VALUE, TEXT_CSV_VALUE })
	public Result<Set<NodeSourcePK>> findAvailableSources(final HttpServletRequest req,
			final DatumFilterCommand criteria) {
		final HistorianQuery query = historianQuery(criteria, req);
		if ( query == null ) {
			return Result.success(Set.of());
		}
		final SortedSet<NodeSourcePK> result = new TreeSet<>();
		for ( ObjectDatumStreamMetadata meta : query.historian()
				.findDatumStreamMetadata(query.filter()) ) {
			result.add(new NodeSourcePK(meta.getObjectId(), meta.getSourceId()));
		}
		return Result.success(result);
	}

}
