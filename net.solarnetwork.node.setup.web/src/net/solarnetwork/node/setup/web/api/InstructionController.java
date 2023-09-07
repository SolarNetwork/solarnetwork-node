/* ==================================================================
 * InstructionController.java - 7/09/2023 5:10:49 pm
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

package net.solarnetwork.node.setup.web.api;

import static net.solarnetwork.domain.Result.error;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.service.OptionalService.service;
import javax.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import net.solarnetwork.domain.Instruction;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.setup.web.support.GlobalExceptionRestController;
import net.solarnetwork.service.OptionalService;

/**
 * API controller for local instruction handling.
 * 
 * @author matt
 * @version 1.0
 * @since 3.3
 */
@GlobalExceptionRestController
@RequestMapping("/api/v1/sec/instr")
public class InstructionController {

	@Resource(name = "instructionExecutionService")
	private OptionalService<InstructionExecutionService> instructionService;

	@Resource(name = "reactorService")
	private OptionalService<ReactorService> reactorService;

	/**
	 * Constructor.
	 */
	public InstructionController() {
		super();
	}

	/**
	 * Enqueue a new instruction.
	 * 
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instruction
	 */
	@PostMapping(value = "/add", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public Result<InstructionStatus> queueInstruction(Instruction input) {
		return doHandleInstruction(input);
	}

	/**
	 * Enqueue a new instruction.
	 * 
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instruction
	 */
	@PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE)
	public Result<InstructionStatus> queueInstructionBody(@RequestBody Instruction input) {
		return doHandleInstruction(input);
	}

	/**
	 * Enqueue a new instruction.
	 * 
	 * <p>
	 * This API call exists to help with API path-based security policy
	 * restrictions, to allow a policy to restrict which topics can be enqueued.
	 * </p>
	 * 
	 * @param topic
	 *        the instruction topic
	 * @param input
	 *        the other instruction data
	 * @return the node instruction
	 */
	@PostMapping(value = "/add/{topic}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public Result<InstructionStatus> queueInstructionBody(@PathVariable("topic") String topic,
			@RequestBody Instruction input) {
		net.solarnetwork.node.reactor.Instruction localInstr = InstructionUtils
				.localInstructionFrom(input);

		BasicInstruction procInstr = new BasicInstruction(null, topic, localInstr.getInstructionDate(),
				topic, localInstr.getStatus());
		return doHandleInstruction(procInstr);
	}

	private Result<InstructionStatus> doHandleInstruction(Instruction input) {
		if ( input.getTopic() == null || input.getTopic().isEmpty() ) {
			throw new IllegalArgumentException("The topic parameter is required.");
		}

		net.solarnetwork.node.reactor.Instruction localInstr = InstructionUtils
				.localInstructionFrom(input);

		InstructionStatus result = null;

		// first try to handle immediately
		InstructionExecutionService execService = service(instructionService);
		if ( execService != null ) {
			result = execService.executeInstruction(localInstr);
		}

		if ( result == null ) {
			// wasn't handled, so queue
			ReactorService reactor = service(reactorService);
			if ( reactor != null ) {
				result = reactor.processInstruction(localInstr);
			}
		}
		if ( result != null ) {
			return success(result);
		}

		return error("INST.0001", "Instruction was not accepted.");
	}

	/**
	 * Set the instruction service.
	 * 
	 * @param instructionService
	 *        the service to set
	 */
	public void setInstructionService(OptionalService<InstructionExecutionService> instructionService) {
		this.instructionService = instructionService;
	}

	/**
	 * Set the reactor service.
	 * 
	 * @param reactorService
	 *        the service to set
	 */
	public void setReactorService(OptionalService<ReactorService> reactorService) {
		this.reactorService = reactorService;
	}

}
