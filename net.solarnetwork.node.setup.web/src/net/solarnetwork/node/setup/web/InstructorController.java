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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller to act as a local Instructor to the local node.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/controls")
public class InstructorController {

	private static final String KEY_CONTROL_ID = "controlId";
	private static final String KEY_CONTROL_INFO = "info";
	private static final String KEY_CONTROL_IDS = "controlIds";

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Resource(name = "nodeControlProviderList")
	private Collection<NodeControlProvider> providers = Collections.emptyList();

	@Resource(name = "instructionHandlerList")
	private Collection<InstructionHandler> handlers = Collections.emptyList();

	@RequestMapping(value = "", method = RequestMethod.GET)
	public String settingsList(ModelMap model) {
		List<String> providerIds = new ArrayList<String>();
		for ( NodeControlProvider provider : providers ) {
			providerIds.addAll(provider.getAvailableControlIds());
		}
		model.put(KEY_CONTROL_IDS, providerIds);
		return "control/list";
	}

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

	@RequestMapping(value = "/setControlParameter", method = RequestMethod.POST)
	public String setControlParameter(SetControlParameterInstruction instruction, ModelMap model,
			HttpServletRequest request) {
		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER,
				new Date(), "LOCAL", "LOCAL", null);
		instr.addParameter(instruction.getControlId(), instruction.getParameterValue());
		InstructionStatus.InstructionState result = null;
		try {
			for ( InstructionHandler handler : handlers ) {
				if ( handler.handlesTopic(instr.getTopic()) ) {
					result = handler.processInstruction(instr);
				}
				if ( result != null ) {
					break;
				}
			}
		} catch ( RuntimeException e ) {
			log.error("Exception setting control parameter {} to {}", instruction.getControlId(),
					instruction.getParameterValue(), e);
		}
		if ( result == null ) {
			// nobody handled it!
			result = InstructionStatus.InstructionState.Declined;
		}
		String keyPrefix = (result == InstructionStatus.InstructionState.Completed ? "status" : "error");
		HttpSession session = request.getSession();
		session.setAttribute(keyPrefix + "MessageKey", "controls.manage.SetControlParameter.result");
		session.setAttribute(keyPrefix + "MessageParam0", result);
		return "redirect:/controls/manage?id=" + instruction.getControlId();
	}

	public void setProviders(Collection<NodeControlProvider> providers) {
		this.providers = providers;
	}

	public void setHandlers(Collection<InstructionHandler> handlers) {
		this.handlers = handlers;
	}

}
