/* ==================================================================
 * DemandBalancer_UnchangedTimeoutTests.java - 12 Nov 2026 8:13:41 am
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.scheduling.TaskScheduler;
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
 * Test cases for the {@link DemandBalancer} unchanged timeout support.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings({ "static-access", "rawtypes", "unchecked" })
@RunWith(MockitoJUnitRunner.class)
public class DemandBalancer_UnchangedTimeoutTests {

	private static final String TEST_CONTROL_ID = "test/power/limit";

	private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);

	@Mock
	private InstructionExecutionService instructionService;

	@Mock
	private DatumDataSource consumptionDataSource;

	@Mock
	private NodeControlProvider powerControl;

	@Mock
	private TaskScheduler taskScheduler;

	@Mock
	private ScheduledFuture future1;

	@Mock
	private ScheduledFuture future2;

	@Captor
	private ArgumentCaptor<Instruction> instructionCaptor;

	@Captor
	private ArgumentCaptor<Runnable> taskCaptor;

	@Captor
	private ArgumentCaptor<Instant> dateCaptor;

	private DemandBalancer service;

	@Before
	public void setup() {
		service = new DemandBalancer(new StaticOptionalService<>(instructionService));
		service.setPowerControlId(TEST_CONTROL_ID);
		service.setPowerControl(new StaticOptionalService<>(powerControl));
		service.setConsumptionDataSource(
				new StaticOptionalServiceCollection<>(List.of(consumptionDataSource)));
		service.setTaskScheduler(taskScheduler);
		service.setUnchangedTimeout(TEST_TIMEOUT);
	}

	private void givenDemandAndLimit(int demandWatts, String limit) {
		final SimpleAcEnergyDatum consumption = new SimpleAcEnergyDatum("con", Instant.now(),
				new DatumSamples());
		consumption.setWatts(demandWatts);
		given(consumptionDataSource.readCurrentDatum()).willReturn(consumption);
		givenLimit(limit);
	}

	private void givenUnknownDemandAndLimit(String limit) {
		given(consumptionDataSource.readCurrentDatum()).willReturn(null);
		givenLimit(limit);
	}

	private void givenLimit(String limit) {
		// @formatter:off
		given(powerControl.getCurrentControlInfo(TEST_CONTROL_ID)).willReturn(
				BasicNodeControlInfo.builder()
					.withControlId(TEST_CONTROL_ID)
					.withType(NodeControlPropertyType.Percent)
					.withValue(limit)
					.build());
		// @formatter:on
	}

	private void givenInstructionsCompleted() {
		given(instructionService.executeInstruction(any()))
				.willAnswer(invocation -> createStatus(invocation.getArgument(0), Completed));
	}

	private void givenSchedules() {
		given(taskScheduler.schedule(any(), any(Instant.class))).willReturn(future1, future2);
	}

	private void thenInstructionsSent(String... limits) {
		then(instructionService).should(times(limits.length))
				.executeInstruction(instructionCaptor.capture());
		// @formatter:off
		and.then(instructionCaptor.getAllValues())
			.as("Demand balance instructions sent")
			.allSatisfy(i -> and.then(i.getTopic()).isEqualTo(InstructionHandler.TOPIC_DEMAND_BALANCE))
			.as("Limits sent in order")
			.map(i -> i.getParameterValue(TEST_CONTROL_ID))
			.containsExactly(limits)
			;
		// @formatter:on
	}

	@Test
	public void unchanged_noScheduler() {
		// GIVEN
		service.setTaskScheduler(null);

		// demand of 500 out of default 1000 W capacity is a 50% limit, same as current limit
		givenDemandAndLimit(500, "50");

		// WHEN
		service.evaluateBalance();

		// THEN
		then(instructionService).shouldHaveNoInteractions();
	}

	@Test
	public void unchanged_noTimeout() {
		// GIVEN
		service.setUnchangedTimeout(null);

		givenDemandAndLimit(500, "50");

		// WHEN
		service.evaluateBalance();

		// THEN
		then(instructionService).shouldHaveNoInteractions();
		then(taskScheduler).shouldHaveNoInteractions();
	}

	@Test
	public void unchanged_firstEvaluation() {
		// GIVEN
		givenDemandAndLimit(500, "50");
		givenInstructionsCompleted();
		givenSchedules();

		// WHEN
		final Instant start = Instant.now();
		service.evaluateBalance();
		final Instant end = Instant.now();

		// THEN
		thenInstructionsSent("50");

		then(taskScheduler).should().schedule(taskCaptor.capture(), dateCaptor.capture());
		// @formatter:off
		and.then(dateCaptor.getValue())
			.as("Timeout scheduled for unchanged timeout after instruction")
			.isBetween(start.plus(TEST_TIMEOUT), end.plus(TEST_TIMEOUT))
			;
		// @formatter:on
	}

	@Test
	public void unchanged_timeoutPending() {
		// GIVEN
		givenDemandAndLimit(500, "50");
		givenInstructionsCompleted();
		givenSchedules();

		// WHEN
		service.evaluateBalance();
		service.evaluateBalance();

		// THEN
		thenInstructionsSent("50");
		then(taskScheduler).should().schedule(any(), any(Instant.class));
		then(future1).shouldHaveNoInteractions();
	}

	@Test
	public void timeout() {
		// GIVEN
		givenDemandAndLimit(500, "50");
		givenInstructionsCompleted();
		givenSchedules();

		// WHEN
		service.evaluateBalance();
		then(taskScheduler).should().schedule(taskCaptor.capture(), any(Instant.class));
		final Instant start = Instant.now();
		taskCaptor.getValue().run();
		final Instant end = Instant.now();

		// THEN
		thenInstructionsSent("50", "50");

		then(taskScheduler).should(times(2)).schedule(taskCaptor.capture(), dateCaptor.capture());
		// @formatter:off
		and.then(dateCaptor.getValue())
			.as("Timeout re-scheduled for unchanged timeout after instruction")
			.isBetween(start.plus(TEST_TIMEOUT), end.plus(TEST_TIMEOUT))
			;
		// @formatter:on
	}

	@Test
	public void timeout_zeroLimit() {
		// GIVEN
		// demand of 0 is a 0% limit, same as current limit
		givenDemandAndLimit(0, "0");
		givenInstructionsCompleted();
		givenSchedules();

		// WHEN
		service.evaluateBalance();
		then(taskScheduler).should().schedule(taskCaptor.capture(), any(Instant.class));
		taskCaptor.getValue().run();

		// THEN
		thenInstructionsSent("0", "0");
	}

	@Test
	public void timeout_afterUnchangedEvaluation() {
		// GIVEN
		givenDemandAndLimit(500, "50");
		givenInstructionsCompleted();
		givenSchedules();

		// WHEN
		service.evaluateBalance();
		then(taskScheduler).should().schedule(taskCaptor.capture(), any(Instant.class));

		// demand now 600 (60%) but limit changed to match it elsewhere, so unchanged
		givenDemandAndLimit(600, "60");
		service.evaluateBalance();
		taskCaptor.getValue().run();

		// THEN
		thenInstructionsSent("50", "60");
	}

	@Test
	public void changed_reschedules() {
		// GIVEN
		givenDemandAndLimit(500, "50");
		givenInstructionsCompleted();
		givenSchedules();

		// WHEN
		service.evaluateBalance();
		givenDemandAndLimit(600, "50");
		service.evaluateBalance();

		then(taskScheduler).should(times(2)).schedule(taskCaptor.capture(), any(Instant.class));
		final List<Runnable> tasks = taskCaptor.getAllValues();
		tasks.get(0).run(); // was cancelled, so should do nothing
		tasks.get(1).run();

		// THEN
		thenInstructionsSent("50", "60", "60");
		then(future1).should().cancel(false);
		then(taskScheduler).should(times(3)).schedule(any(), any(Instant.class));
	}

	@Test
	public void noDesiredLimit_cancels() {
		// GIVEN
		givenDemandAndLimit(500, "50");
		givenInstructionsCompleted();
		givenSchedules();

		// WHEN
		service.evaluateBalance();
		then(taskScheduler).should().schedule(taskCaptor.capture(), any(Instant.class));

		// unknown demand with default strategy unknown demand limit of -1 means no limit
		givenUnknownDemandAndLimit("50");
		service.evaluateBalance();
		taskCaptor.getValue().run();

		// THEN
		thenInstructionsSent("50");
		then(future1).should().cancel(false);
		then(taskScheduler).should().schedule(any(), any(Instant.class));
	}

	@Test
	public void configurationChanged_cancels() {
		// GIVEN
		givenDemandAndLimit(500, "50");
		givenInstructionsCompleted();
		givenSchedules();

		// WHEN
		service.evaluateBalance();
		service.configurationChanged(null);
		service.evaluateBalance();

		// THEN
		then(future1).should().cancel(false);
		thenInstructionsSent("50", "50");
		then(taskScheduler).should(times(2)).schedule(any(), any(Instant.class));
	}

	@Test
	public void shutdown_cancels() {
		// GIVEN
		givenDemandAndLimit(500, "50");
		givenInstructionsCompleted();
		givenSchedules();

		// WHEN
		service.evaluateBalance();
		then(taskScheduler).should().schedule(taskCaptor.capture(), any(Instant.class));
		service.serviceDidShutdown();
		taskCaptor.getValue().run();

		// THEN
		then(future1).should().cancel(false);
		thenInstructionsSent("50");
	}

	@Test
	public void unchangedTimeoutValue() {
		// WHEN
		service.setUnchangedTimeoutValue("PT30S");

		// THEN
		and.then(service.getUnchangedTimeout()).as("Parsed ISO duration").isEqualTo(TEST_TIMEOUT);
		and.then(service.getUnchangedTimeoutValue()).as("Formatted ISO duration").isEqualTo("PT30S");

		// WHEN
		service.setUnchangedTimeoutValue(" pt1m ");

		// THEN
		and.then(service.getUnchangedTimeout()).as("Parsed lower case ISO duration")
				.isEqualTo(Duration.ofMinutes(1));

		// WHEN
		service.setUnchangedTimeoutValue("");

		// THEN
		and.then(service.getUnchangedTimeout()).as("Empty value clears").isNull();
		and.then(service.getUnchangedTimeoutValue()).as("Empty value cleared").isNull();

		// WHEN
		service.setUnchangedTimeoutValue("PT30S");
		service.setUnchangedTimeoutValue("30 seconds");

		// THEN
		and.then(service.getUnchangedTimeout()).as("Invalid value clears").isNull();
	}

}
