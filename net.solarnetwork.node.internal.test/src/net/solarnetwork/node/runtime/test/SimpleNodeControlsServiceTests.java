/* ==================================================================
 * SimpleNodeControlsServiceTests.java - 13/07/2023 2:24:37 pm
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

package net.solarnetwork.node.runtime.test;

import static java.util.Arrays.asList;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_SERVICE;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_SERVICE_RESULT;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SYSTEM_CONFIGURATION;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.node.runtime.SimpleNodeControlsService.CONTROLS_SERVICE_UID;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.Test;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.runtime.SimpleNodeControlsService;
import net.solarnetwork.node.service.NodeControlProvider;

/**
 * Test cases for the {@link SimpleNodeControlsService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleNodeControlsServiceTests {

	@Test
	public void handlesTopic() {
		// GIVEN
		SimpleNodeControlsService service = new SimpleNodeControlsService(Collections.emptyList());

		// WHEN
		boolean handlesSystemConfigurationTopic = service.handlesTopic(TOPIC_SYSTEM_CONFIGURATION);
		boolean handlesSetControlParameter = service.handlesTopic(TOPIC_SET_CONTROL_PARAMETER);

		// THEN
		assertThat("SystemConfiguration topic handled", handlesSystemConfigurationTopic, is(true));
		assertThat("Other topic not handled", handlesSetControlParameter, is(false));
	}

	@Test
	public void handleSystemConfigurationTopic() {
		NodeControlProvider p1 = EasyMock.createMock(NodeControlProvider.class);
		NodeControlProvider p2 = EasyMock.createMock(NodeControlProvider.class);
		NodeControlProvider p3 = EasyMock.createMock(NodeControlProvider.class);

		expect(p1.getAvailableControlIds()).andReturn(asList("a", "b"));
		expect(p2.getAvailableControlIds()).andReturn(null);
		expect(p3.getAvailableControlIds()).andReturn(asList("b", "c"));

		SimpleNodeControlsService service = new SimpleNodeControlsService(asList(p2, p3, p1));

		// WHEN
		replay(p1, p2, p3);
		Instruction instr = createLocalInstruction(TOPIC_SYSTEM_CONFIGURATION, PARAM_SERVICE,
				CONTROLS_SERVICE_UID);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Status result returned", result, is(notNullValue()));
		assertThat("State is Completed", result.getInstructionState(), is(equalTo(Completed)));
		assertThat("Result param provided", result.getResultParameters(),
				hasEntry(PARAM_SERVICE_RESULT, "a,b,c"));
		verify(p1, p2, p3);
	}

	@Test
	public void handleSystemConfigurationTopic_filtered() {
		NodeControlProvider p1 = EasyMock.createMock(NodeControlProvider.class);
		NodeControlProvider p2 = EasyMock.createMock(NodeControlProvider.class);
		NodeControlProvider p3 = EasyMock.createMock(NodeControlProvider.class);

		expect(p1.getAvailableControlIds()).andReturn(asList("a/b/1", "b/0/a/2"));
		expect(p2.getAvailableControlIds()).andReturn(asList("b/1", "b/2"));
		expect(p3.getAvailableControlIds()).andReturn(asList("c", "d"));

		SimpleNodeControlsService service = new SimpleNodeControlsService(asList(p2, p3, p1));

		// WHEN
		replay(p1, p2, p3);
		Map<String, String> instrParams = new HashMap<>(2);
		instrParams.put(PARAM_SERVICE, CONTROLS_SERVICE_UID);
		instrParams.put(SimpleNodeControlsService.PARAM_FILTER, "b/**");
		Instruction instr = createLocalInstruction(TOPIC_SYSTEM_CONFIGURATION, instrParams);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Status result returned", result, is(notNullValue()));
		assertThat("State is Completed", result.getInstructionState(), is(equalTo(Completed)));
		assertThat("Result param provided", result.getResultParameters(),
				hasEntry(PARAM_SERVICE_RESULT, "b/0/a/2,b/1,b/2"));
		verify(p1, p2, p3);
	}

}
