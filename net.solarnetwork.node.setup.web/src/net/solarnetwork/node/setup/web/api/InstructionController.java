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
import static net.solarnetwork.node.reactor.Instruction.LOCAL_INSTRUCTION_ID;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import net.solarnetwork.domain.Instruction;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.InstructionDao;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.service.IdentityService;
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

	@Autowired
	@Qualifier("instructionExecutionService")
	private OptionalService<InstructionExecutionService> instructionService;

	@Autowired
	@Qualifier("reactorService")
	private OptionalService<ReactorService> reactorService;

	@Autowired
	@Qualifier("instructionDao")
	private OptionalService<InstructionDao> instructionDao;

	private final IdentityService identityService;

	/**
	 * Constructor.
	 *
	 * @param identityService
	 *        the identity service
	 */
	public InstructionController(IdentityService identityService) {
		super();
		this.identityService = requireNonNullArgument(identityService, "identityService");
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
		BasicInstruction.copyParameters(localInstr, procInstr);
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
	 * View a single instruction, based on its primary key.
	 *
	 * @param instructionId
	 *        the ID of the instruction to view
	 * @param source
	 *        the optional instructor ID, or {@literal null} to look for SolarIn
	 *        and local instructions
	 * @return the instruction
	 */
	@GetMapping(value = "/view", params = "!ids")
	public Result<Instruction> viewInstruction(@RequestParam("id") Long instructionId,
			@RequestParam(value = "source", required = false) String source) {
		return success(findOne(instructionId, source));
	}

	/**
	 * View a set of instructions, based on their primary keys.
	 *
	 * @param instructionIds
	 *        the IDs of the instructions to view
	 * @param source
	 *        the optional instructor ID, or {@literal null} to look for SolarIn
	 *        and local instructions
	 * @return the instructions
	 */
	@GetMapping(value = "/view", params = "ids")
	public Result<List<Instruction>> viewInstructions(@RequestParam("ids") Set<Long> instructionIds,
			@RequestParam(value = "source", required = false) String source) {
		List<Instruction> result = new ArrayList<>(instructionIds.size());
		for ( Long id : instructionIds ) {
			Instruction inst = findOne(id, source);
			if ( inst != null ) {
				result.add(inst);
			}
		}
		return success(result.isEmpty() ? null : result);
	}

	private net.solarnetwork.node.reactor.Instruction findOne(Long instructionId, String source) {
		final InstructionDao dao = dao();
		if ( source != null ) {
			return dao.getInstruction(instructionId, source);
		}
		net.solarnetwork.node.reactor.Instruction result = dao.getInstruction(instructionId,
				solarInSource());
		if ( result != null ) {
			return result;
		}
		return dao.getInstruction(instructionId, LOCAL_INSTRUCTION_ID);
	}

	private InstructionDao dao() {
		InstructionDao dao = service(instructionDao);
		if ( dao != null ) {
			return dao;
		}
		throw new UnsupportedOperationException("Instruction DAO not available.");
	}

	private String solarInSource() {
		return identityService.getSolarInBaseUrl();
	}

	/**
	 * Update the state of an existing instruction.
	 *
	 * @param instructionId
	 *        the ID of the instruction to update
	 * @param source
	 *        the optional instructor ID, or {@literal null} to look for SolarIn
	 *        and local instructions
	 * @param state
	 *        the desired state
	 * @return the response
	 */
	@PostMapping(value = "/updateState", params = "!ids")
	public Result<Void> updateInstructionState(@RequestParam("id") Long instructionId,
			@RequestParam(value = "source", required = false) String source,
			@RequestParam("state") InstructionState state) {
		doUpdateState(instructionId, source, state);
		return success();
	}

	/**
	 * Update the state of an existing instruction.
	 *
	 * @param instructionIds
	 *        the IDs of the instructions to update
	 * @param source
	 *        the optional instructor ID, or {@literal null} to look for SolarIn
	 *        and local instructions
	 * @param state
	 *        the desired state
	 * @return the response
	 */
	@PostMapping(value = "/updateState", params = "ids")
	public Result<Void> updateInstructionStates(@RequestParam("ids") Set<Long> instructionIds,
			@RequestParam(value = "source", required = false) String source,
			@RequestParam("state") InstructionState state) {
		for ( Long instructionId : instructionIds ) {
			doUpdateState(instructionId, source, state);
		}
		return success();
	}

	private void doUpdateState(Long instructionId, String source, InstructionState state) {
		final InstructionDao dao = dao();
		final net.solarnetwork.node.reactor.Instruction instr = findOne(instructionId, source);
		if ( instr == null ) {
			return;
		}
		InstructionStatus status = InstructionUtils.createStatus(instr, state);
		dao.compareAndStoreInstructionStatus(instr.getId(), instr.getInstructorId(),
				instr.getInstructionState(), status);
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

	/**
	 * Set the instruction DAO.
	 *
	 * @param instructionDao
	 *        the DAO to set
	 */
	public void setInstructionDao(OptionalService<InstructionDao> instructionDao) {
		this.instructionDao = instructionDao;
	}

}
