/* ==================================================================
 * ControlGroupConsensusStrategy.java - 5 Nov 2026 3:20:33 pm
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

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;

/**
 * API for handling control group member state change consensus.
 *
 * @author matt
 * @version 1.0
 */
public interface ControlGroupConsensusStrategy {

	/**
	 * Determine the group state change result consensus.
	 *
	 * @param group
	 *        the group distributing on behalf of; this instance is meant to be
	 *        used for information about the control style only, and the
	 *        {@code value} is not expected to be available
	 * @param memberConfigs
	 *        member configurations
	 * @param request
	 *        the group control instruction
	 * @param results
	 *        the member results, with control ID keys to associated instruction
	 *        status
	 * @param messageSource
	 *        a message source to resolve messages
	 * @return the group instruction status consensus
	 */
	@Nullable
	InstructionStatus evaluate(NodeControlInfo group, List<MemberControlConfig> memberConfigs,
			Instruction request, Map<String, InstructionExecution> results);

}
