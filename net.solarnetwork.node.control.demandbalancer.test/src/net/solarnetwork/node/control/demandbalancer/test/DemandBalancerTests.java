/* ==================================================================
 * DemandBalancerTests.java - 15/09/2015 9:05:45 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.control.demandbalancer.DemandBalancer;
import net.solarnetwork.node.control.demandbalancer.SimpleDemandBalanceStrategy;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.reactor.SimpleInstructionExecutionService;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link DemandBalancer} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DemandBalancerTests {

	private DemandBalancer demandBalancer;

	private SimpleDemandBalanceStrategy strategy;
	private DatumDataSource consumptionDataSource;
	private NodeControlProvider pcmControl;
	private InstructionHandler pcmHandler;

	private void replayAll() {
		replay(consumptionDataSource, pcmControl, pcmHandler);
	}

	private void verifyAll() {
		verify(consumptionDataSource, pcmControl, pcmHandler);
	}

	@Before
	public void setup() {
		pcmHandler = EasyMock.createMock(InstructionHandler.class);
		demandBalancer = new DemandBalancer(new StaticOptionalService<>(
				new SimpleInstructionExecutionService(singletonList(pcmHandler))));

		consumptionDataSource = EasyMock.createMock(DatumDataSource.class);
		demandBalancer.setConsumptionDataSource(
				new StaticOptionalServiceCollection<>(singletonList(consumptionDataSource)));

		pcmControl = EasyMock.createMock(NodeControlProvider.class);
		demandBalancer.setPowerControl(new StaticOptionalService<>(pcmControl));

		strategy = new SimpleDemandBalanceStrategy();
		strategy.setUnknownDemandLimit(3);
		demandBalancer.setBalanceStrategy(new StaticOptionalService<>(strategy));

	}

	@Test
	public void negativePowerWithUnknownLimit() {
		// GIVEN
		final Integer originalLimit = 100;
		final NodeControlInfo originalPcmInfo = BasicNodeControlInfo.builder()
				.withControlId(demandBalancer.getPowerControlId())
				.withType(NodeControlPropertyType.Integer).withReadonly(false)
				.withValue(originalLimit.toString()).build();

		final SimpleAcEnergyDatum consumption = new SimpleAcEnergyDatum("foo", Instant.now(),
				new DatumSamples());
		consumption.setWatts(-123);

		expect(pcmControl.getCurrentControlInfo(demandBalancer.getPowerControlId()))
				.andReturn(originalPcmInfo);

		expect(consumptionDataSource.readCurrentDatum()).andReturn(consumption);

		expect(pcmHandler.handlesTopic(InstructionHandler.TOPIC_DEMAND_BALANCE)).andReturn(true);

		Capture<Instruction> instructionCapture = Capture.newInstance();
		expect(pcmHandler.processInstruction(EasyMock.capture(instructionCapture)))
				.andAnswer(new IAnswer<InstructionStatus>() {

					@Override
					public InstructionStatus answer() throws Throwable {
						// TODO Auto-generated method stub
						return InstructionUtils.createStatus(instructionCapture.getValue(),
								InstructionState.Completed);
					}
				});

		// WHEN
		replayAll();
		demandBalancer.evaluateBalance();

		verifyAll();

		Instruction instr = instructionCapture.getValue();
		assertThat("Instruction topic", instr.getTopic(), is(InstructionHandler.TOPIC_DEMAND_BALANCE));
		assertThat("Instruction control ID", instr.getParameterValue(demandBalancer.getPowerControlId()),
				is(String.valueOf(strategy.getUnknownDemandLimit())));
	}

}
