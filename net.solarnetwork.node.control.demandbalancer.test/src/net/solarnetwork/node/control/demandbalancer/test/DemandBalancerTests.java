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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.control.demandbalancer.DemandBalanceStrategy;
import net.solarnetwork.node.control.demandbalancer.DemandBalancer;
import net.solarnetwork.node.control.demandbalancer.SimpleDemandBalanceStrategy;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StaticOptionalService;
import net.solarnetwork.util.StaticOptionalServiceCollection;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link DemandBalancer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DemandBalancerTests extends AbstractNodeTest {

	private DemandBalancer demandBalancer;

	private SimpleDemandBalanceStrategy strategy;
	private DatumDataSource<GeneralNodeACEnergyDatum> consumptionDataSource;
	private NodeControlProvider pcmControl;
	private InstructionHandler pcmHandler;

	private void replayAll() {
		replay(consumptionDataSource, pcmControl, pcmHandler);
	}

	private void verifyAll() {
		verify(consumptionDataSource, pcmControl, pcmHandler);
	}

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		demandBalancer = new DemandBalancer();

		consumptionDataSource = EasyMock.createMock(DatumDataSource.class);
		Collection<DatumDataSource<? extends EnergyDatum>> consumptions = Collections
				.<DatumDataSource<? extends EnergyDatum>> singleton(consumptionDataSource);
		OptionalServiceCollection<DatumDataSource<? extends EnergyDatum>> consumptionsService = new StaticOptionalServiceCollection<DatumDataSource<? extends EnergyDatum>>(
				consumptions);
		demandBalancer.setConsumptionDataSource(consumptionsService);

		pcmControl = EasyMock.createMock(NodeControlProvider.class);
		demandBalancer.setPowerControl(new StaticOptionalService<NodeControlProvider>(pcmControl));

		strategy = new SimpleDemandBalanceStrategy();
		strategy.setUnknownDemandLimit(3);
		demandBalancer.setBalanceStrategy(new StaticOptionalService<DemandBalanceStrategy>(strategy));

		pcmHandler = EasyMock.createMock(InstructionHandler.class);
		demandBalancer.setInstructionHandlers(Collections.singleton(pcmHandler));
	}

	@Test
	public void negativePowerWithUnknownLimit() {
		final Integer originalLimit = 100;
		final NodeControlInfoDatum originalPcmInfo = new NodeControlInfoDatum();
		originalPcmInfo.setCreated(new Date());
		originalPcmInfo.setSourceId(demandBalancer.getPowerControlId());
		originalPcmInfo.setValue(originalLimit.toString());

		final GeneralNodeACEnergyDatum consumption = new GeneralNodeACEnergyDatum();
		consumption.setWatts(-123);

		expect(pcmControl.getCurrentControlInfo(demandBalancer.getPowerControlId())).andReturn(
				originalPcmInfo);

		expect(consumptionDataSource.readCurrentDatum()).andReturn(consumption);

		expect(pcmHandler.handlesTopic(InstructionHandler.TOPIC_DEMAND_BALANCE)).andReturn(true);

		Capture<Instruction> instructionCapture = new Capture<Instruction>();
		expect(pcmHandler.processInstruction(EasyMock.capture(instructionCapture))).andReturn(
				InstructionState.Completed);

		replayAll();

		demandBalancer.evaluateBalance();

		verifyAll();

		Instruction instr = instructionCapture.getValue();
		Assert.assertEquals(InstructionHandler.TOPIC_DEMAND_BALANCE, instr.getTopic());
		Assert.assertEquals(String.valueOf(strategy.getUnknownDemandLimit()),
				instr.getParameterValue(demandBalancer.getPowerControlId()));
	}

}
