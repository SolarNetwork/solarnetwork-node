/* ==================================================================
 * ControlGroupAllocationStrategy.java - 4 Nov 2026 4:23:30 pm
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

package net.solarnetwork.node.control.group;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.Identifiable;

/**
 * API for evaluating control group member state.
 *
 * <p>
 * The member configurations passed to this API include only the valid members
 * of the group, with any placeholders in their control IDs already resolved. A
 * member that resolves to the group's own control ID is not included.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public interface ControlGroupCoordinationStrategy extends Identifiable {

	/**
	 * Combine a collection of controls into a group value.
	 *
	 * @param group
	 *        the group combining on behalf of; this instance is meant to be
	 *        used for information about the control style only, and the
	 *        {@code value} is not expected to be available
	 * @param memberConfigs
	 *        the member configurations, with control ID placeholders resolved
	 * @param values
	 *        the member control values to combine
	 * @return the combined control value
	 */
	NodeControlInfo combine(NodeControlInfo group, List<MemberControlConfig> memberConfigs,
			Collection<NodeControlInfo> values);

	/**
	 * Distribute a control request across a collection of controls.
	 *
	 * @param group
	 *        the group distributing on behalf of; this instance is meant to be
	 *        used for information about the control style only, and the
	 *        {@code value} is not expected to be available
	 * @param memberConfigs
	 *        the member configurations, with placeholders resolved
	 * @param request
	 *        the control instruction
	 * @param controls
	 *        all available controls
	 * @return the instructions to execute, grouped by member control ID, or
	 *         {@code null} if the request was not handled
	 */
	@Nullable
	Map<String, Instruction> distribute(NodeControlInfo group, List<MemberControlConfig> memberConfigs,
			Instruction request, Collection<NodeControlProvider> controls);

}
