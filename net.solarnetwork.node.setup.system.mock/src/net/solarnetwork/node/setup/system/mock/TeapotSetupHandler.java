/* ==================================================================
 * TeapotSetupHandler.java - 20/08/2021 9:19:45 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.system.mock;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;

/**
 * Support for making tea.
 * 
 * @author matt
 * @version 1.0
 */
public class TeapotSetupHandler implements FeedbackInstructionHandler {

	public static final String SERVICE_COFFEE_ORDER = "/coffee/order";

	public static final String SERVICE_TEA_ORDER = "/tea/order";

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		return null;
	}

	@Override
	public InstructionStatus processInstructionWithFeedback(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		String service = instruction.getParameterValue(InstructionHandler.PARAM_SERVICE);
		if ( SERVICE_COFFEE_ORDER.equals(service) ) {
			return handleCoffeeOrder(instruction);
		} else if ( SERVICE_TEA_ORDER.equals(service) ) {
			return handleTeaOrder(instruction);
		}
		return null;
	}

	private InstructionStatus handleCoffeeOrder(Instruction instruction) {
		Map<String, Object> resultParams = new LinkedHashMap<>(2);
		resultParams.put(InstructionHandler.PARAM_STATUS_CODE, 418);
		resultParams.put(InstructionHandler.PARAM_MESSAGE, "I'm a tea pot.");
		return InstructionStatus.createStatus(instruction, InstructionState.Declined, new Date(),
				resultParams);
	}

	private InstructionStatus handleTeaOrder(Instruction instruction) {
		return InstructionStatus.createStatus(instruction, InstructionState.Completed, new Date(),
				Collections.singletonMap(InstructionHandler.PARAM_MESSAGE, "Order placed."));
	}

}
