/* ==================================================================
 * InstructorController.java - Jul 10, 2013 4:00:40 PM
 *
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;

/**
 * Controller to act as a local Instructor to the local node.
 *
 * @author matt
 * @version 2.2
 */
@ServiceAwareController
@RequestMapping("/a/controls")
public class InstructorController {

	private static final String KEY_CONTROL_ID = "controlId";
	private static final String KEY_CONTROL_INFO = "info";
	private static final String KEY_CONTROL_IDS = "controlIds";

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Resource(name = "nodeControlProviderList")
	private Collection<NodeControlProvider> providers = Collections.emptyList();

	@Resource(name = "instructionExecutionService")
	private OptionalService<InstructionExecutionService> instructionService;

	/**
	 * Default constructor.
	 */
	public InstructorController() {
		super();
	}

	/**
	 * List controls.
	 *
	 * @param model
	 *        the model
	 * @return the result view
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String controlsList(ModelMap model) {
		List<String> providerIds = new ArrayList<String>();
		for ( NodeControlProvider provider : providers ) {
			providerIds.addAll(provider.getAvailableControlIds());
		}
		Collections.sort(providerIds, CASE_INSENSITIVE_NATURAL_SORT);
		model.put(KEY_CONTROL_IDS, providerIds);
		return "control/list";
	}

	/**
	 * Manage a control.
	 *
	 * @param controlId
	 *        the control ID to manage
	 * @param model
	 *        the model
	 * @return the result view name
	 */
	@RequestMapping(value = "/manage", method = RequestMethod.GET)
	public String manage(@RequestParam("id") String controlId, ModelMap model) {
		NodeControlProvider provider = null;
		for ( NodeControlProvider p : providers ) {
			for ( String s : p.getAvailableControlIds() ) {
				if ( s.equals(controlId) ) {
					provider = p;
					break;
				}
			}
			if ( provider != null ) {
				break;
			}
		}
		if ( provider != null ) {
			model.put(KEY_CONTROL_ID, controlId);
			NodeControlInfo info = null;
			try {
				info = provider.getCurrentControlInfo(controlId);
			} catch ( RuntimeException e ) {
				log.warn("Error getting control {} info: {}", controlId, e.getMessage());
			}
			if ( info != null ) {
				model.put(KEY_CONTROL_INFO, info);
			}
		}
		return "control/manage";
	}

	/**
	 * Set a control parameter.
	 *
	 * @param instruction
	 *        the instruction.
	 * @param model
	 *        the model
	 * @param request
	 *        the request
	 * @return the result view name
	 */
	@RequestMapping(value = "/setControlParameter", method = RequestMethod.POST)
	public String setControlParameter(SetControlParameterInstruction instruction, ModelMap model,
			HttpServletRequest request) {

		Instruction instr = InstructionUtils.createLocalInstruction(
				InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, instruction.getControlId(),
				instruction.getParameterValue());
		InstructionExecutionService service = OptionalService.service(instructionService);
		InstructionStatus result = null;
		try {
			if ( service == null ) {
				result = InstructionUtils.createStatus(instr, InstructionState.Declined);
			} else {
				result = service.executeInstruction(instr);
			}
		} catch ( RuntimeException e ) {
			log.error("Exception setting control parameter {} to {}", instruction.getControlId(),
					instruction.getParameterValue(), e);
		}
		if ( result == null ) {
			// nobody handled it!
			result = InstructionUtils.createStatus(instr, InstructionState.Declined);
		}
		String keyPrefix = (result.getInstructionState() == InstructionStatus.InstructionState.Completed
				? "status"
				: "error");
		HttpSession session = request.getSession();
		session.setAttribute(keyPrefix + "MessageKey", "controls.manage.SetControlParameter.result");
		session.setAttribute(keyPrefix + "MessageParam0", result.getInstructionState());
		return "redirect:/a/controls/manage?id=" + instruction.getControlId();
	}

	/**
	 * Set the available control providers.
	 *
	 * @param providers
	 *        the providers to set
	 */
	public void setProviders(Collection<NodeControlProvider> providers) {
		this.providers = providers;
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

}
