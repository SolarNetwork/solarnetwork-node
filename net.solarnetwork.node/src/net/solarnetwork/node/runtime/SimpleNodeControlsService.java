/* ==================================================================
 * SimpleNodeControlsService.java - 13/07/2023 2:07:49 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;

/**
 * Service to support node control management.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleNodeControlsService extends BaseIdentifiable implements InstructionHandler {

	/** The default UID for this service. */
	public static final String CONTROLS_SERVICE_UID = "net.solarnetwork.node.controls";

	/**
	 * An optional instruction parameter containing an Ant-style path pattern to
	 * filter the returned control IDs by.
	 */
	public static final String PARAM_FILTER = "filter";

	private final Collection<NodeControlProvider> providers;

	/**
	 * Constructor.
	 * 
	 * @param providers
	 *        the control providers
	 */
	public SimpleNodeControlsService(Collection<NodeControlProvider> providers) {
		super();
		this.providers = requireNonNullArgument(providers, "providers");
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURATION.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		final String uid = getUid() != null ? getUid() : CONTROLS_SERVICE_UID;
		final String serviceId = instruction.getParameterValue(PARAM_SERVICE);
		if ( !uid.equals(serviceId) ) {
			return null;
		}

		final String filter = instruction.getParameterValue(PARAM_FILTER);
		final PathMatcher pathMatcher = filter != null ? new AntPathMatcher() : null;

		// return list of all available control IDs
		SortedSet<String> controlIds = new TreeSet<>();
		for ( NodeControlProvider provider : providers ) {
			List<String> ids = provider.getAvailableControlIds();
			if ( ids != null ) {
				if ( pathMatcher == null ) {
					controlIds.addAll(ids);
				} else {
					for ( String id : ids ) {
						if ( pathMatcher.match(filter, id) ) {
							controlIds.add(id);
						}
					}
				}
			}
		}

		return createStatus(instruction, Completed,
				singletonMap(PARAM_SERVICE_RESULT, commaDelimitedStringFromCollection(controlIds)));
	}

}
