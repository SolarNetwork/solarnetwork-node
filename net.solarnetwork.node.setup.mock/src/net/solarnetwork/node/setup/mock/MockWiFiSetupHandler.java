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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;

/**
 * Mock implementation of a setup handler for WiFi configuration.
 * 
 * @author matt
 * @version 2.0
 */
public class MockWiFiSetupHandler implements InstructionHandler {

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

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
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
		return InstructionUtils.createStatus(instruction, resultState, Instant.now(), resultParams);
	}

}
