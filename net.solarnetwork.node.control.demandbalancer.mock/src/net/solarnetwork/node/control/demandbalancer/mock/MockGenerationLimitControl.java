/* ==================================================================
 * MockGenerationLimitControl.java - Mar 25, 2014 10:49:57 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.demandbalancer.mock;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link NodeControlProvider} that acts like a
 * generation limiting device, to support testing of demand balancing. This
 * control supports an <em>integer percentage</em> value that represents a
 * generation output limit, e.g. {@code 0} means no generation output is allowed
 * while {@code 100} means full output is allowed.
 * 
 * Both the {@link InstructionHandler#TOPIC_DEMAND_BALANCE} and
 * {@link InstructionHandler#TOPIC_SET_CONTROL_PARAMETER} topics are supported,
 * both of which expect a {@code controlId} key associated with an integer
 * percentage value.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>controlId</dt>
 * <dd>The control ID to use. Defaults to {@code /power/limit/mock}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class MockGenerationLimitControl implements NodeControlProvider, InstructionHandler {

	private final AtomicInteger limit = new AtomicInteger(100);

	private String controlId = "/power/limit/mock";

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public String getUID() {
		return controlId;
	}

	@Override
	public String getGroupUID() {
		return "Mock";
	}

	@Override
	public List<String> getAvailableControlIds() {
		return Collections.singletonList(controlId);
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		NodeControlInfoDatum info = new NodeControlInfoDatum();
		info.setCreated(new Date());
		info.setSourceId(controlId);
		info.setType(NodeControlPropertyType.Integer);
		info.setReadonly(false);
		info.setValue(limit.toString());
		return info;
	}

	private InstructionState setLimitValue(final int value) {
		if ( value < 0 || value > 100 ) {
			return InstructionState.Declined;
		}
		limit.set(value);
		return InstructionState.Completed;
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return (InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic) || InstructionHandler.TOPIC_DEMAND_BALANCE
				.equals(topic));
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		// look for a parameter name that matches a control ID
		InstructionState result = null;
		log.debug("Inspecting instruction {} against control {}", instruction.getId(), controlId);
		for ( String paramName : instruction.getParameterNames() ) {
			log.trace("Got instruction parameter {}", paramName);
			if ( controlId.equals(paramName) ) {
				String str = instruction.getParameterValue(controlId);
				Integer desiredValue = Integer.parseInt(str);
				log.info("Percent output limit request of {}%", desiredValue);
				result = setLimitValue(desiredValue);
				break;
			}
		}
		return result;
	}

	// Accessors

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

}
