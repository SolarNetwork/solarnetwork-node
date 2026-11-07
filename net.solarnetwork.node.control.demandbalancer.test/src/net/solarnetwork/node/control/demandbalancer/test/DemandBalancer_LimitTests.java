/* ==================================================================
 * DemandBalancer_LimitTests.java - 6 Nov 2026 4:12:31 pm
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

package net.solarnetwork.node.control.demandbalancer.test;

import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.control.demandbalancer.DemandBalancer;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for how the {@link DemandBalancer} class handles the current limit
 * control value.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class DemandBalancer_LimitTests {

	private static final String TEST_CONTROL_ID = "test/power/limit";

	@Mock
	private InstructionExecutionService instructionService;

	@Mock
	private DatumDataSource consumptionDataSource;

	@Mock
	private NodeControlProvider powerControl;

	@Captor
	private ArgumentCaptor<Instruction> instructionCaptor;

	private DemandBalancer service;

	@Before
	public void setup() {
		service = new DemandBalancer(new StaticOptionalService<>(instructionService));
		service.setPowerControlId(TEST_CONTROL_ID);
		service.setPowerControl(new StaticOptionalService<>(powerControl));
		service.setConsumptionDataSource(
				new StaticOptionalServiceCollection<>(List.of(consumptionDataSource)));
	}

	private void givenDemandAndLimit(int demandWatts, String limit) {
		final SimpleAcEnergyDatum consumption = new SimpleAcEnergyDatum("con", Instant.now(),
				new DatumSamples());
		consumption.setWatts(demandWatts);
		given(consumptionDataSource.readCurrentDatum()).willReturn(consumption);

		// @formatter:off
		given(powerControl.getCurrentControlInfo(TEST_CONTROL_ID)).willReturn(
				BasicNodeControlInfo.builder()
					.withControlId(TEST_CONTROL_ID)
					.withType(NodeControlPropertyType.Percent)
					.withValue(limit)
					.build());
		// @formatter:on
	}

	@Test
	public void decimalLimit_wholeNumber() {
		// GIVEN
		// demand of 500 out of default 1000 W capacity is a 50% limit, same as current limit
		givenDemandAndLimit(500, "50.0");

		// WHEN
		service.evaluateBalance();

		// THEN
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void decimalLimit_roundedDown() {
		// GIVEN
		// demand of 500 out of default 1000 W capacity is a 50% limit, same as current limit
		// after rounding down
		givenDemandAndLimit(500, "50.9");

		// WHEN
		service.evaluateBalance();

		// THEN
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void decimalLimit_adjusted() {
		// GIVEN
		// demand of 600 out of default 1000 W capacity is a 60% limit
		givenDemandAndLimit(600, "50.9");

		given(instructionService.executeInstruction(any()))
				.willAnswer(invocation -> createStatus(invocation.getArgument(0), Completed));

		// WHEN
		service.evaluateBalance();

		// THEN
		// @formatter:off
		then(instructionService).should().executeInstruction(instructionCaptor.capture());
		and.then(instructionCaptor.getValue())
			.as("Demand balance instruction sent")
			.returns(InstructionHandler.TOPIC_DEMAND_BALANCE, from(Instruction::getTopic))
			.as("New limit is whole percentage")
			.returns("60", from(i -> i.getParameterValue(TEST_CONTROL_ID)))
			;
		// @formatter:on
	}

}
