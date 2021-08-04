/* ==================================================================
 * MockWiFiSetupHandler.java - 5/08/2021 9:02:06 AM
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

package net.solarnetwork.node.setup.mock;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;

/**
 * Mock implementation of a setup handler for WiFi configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class MockWiFiSetupHandler implements FeedbackInstructionHandler {

	/** The country parameter. */
	public static final String PARAM_COUNTRY = "country";

	/** The SSID parameter. */
	public static final String PARAM_SSID = "ssid";

	/** The password parameter. */
	public static final String PARAM_PASSWORD = "password";

	/**
	 * The {@literal service} instruction parameter value for WiFi
	 * configuration.
	 */
	public static final String WIFI_SERVICE_NAME = "wifi";

	private List<FeedbackInstructionHandler> feedbackHandlers = Collections.emptyList();

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		InstructionStatus status = processInstructionWithFeedback(instruction);
		return (status != null ? status.getInstructionState() : null);
	}

	@Override
	public InstructionStatus processInstructionWithFeedback(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic())
				|| !WIFI_SERVICE_NAME.equals(instruction.getParameterValue(PARAM_SERVICE)) ) {
			return null;
		}
		InstructionState resultState = InstructionState.Completed;
		Map<String, Object> resultParams = new LinkedHashMap<>(3);
		resultParams.put(PARAM_COUNTRY, instruction.getParameterValue(PARAM_COUNTRY));
		resultParams.put(PARAM_SSID, instruction.getParameterValue(PARAM_SSID));
		resultParams.put(PARAM_PASSWORD,
				instruction.isParameterAvailable(PARAM_PASSWORD) ? "*****" : "N/A");
		return InstructionStatus.createStatus(instruction, resultState, new Date(), resultParams);
	}

	/**
	 * Get the configured handlers.
	 * 
	 * @return the handlers, never {@literal null}
	 */
	public List<FeedbackInstructionHandler> getFeedbackHandlers() {
		return feedbackHandlers;
	}

	/**
	 * Set the configured handlers.
	 * 
	 * @param feedbackHandlers
	 *        the handlers to set
	 */
	public void setFeedbackHandlers(List<FeedbackInstructionHandler> feedbackHandlers) {
		if ( feedbackHandlers == null ) {
			feedbackHandlers = Collections.emptyList();
		}
		this.feedbackHandlers = feedbackHandlers;
	}

}
