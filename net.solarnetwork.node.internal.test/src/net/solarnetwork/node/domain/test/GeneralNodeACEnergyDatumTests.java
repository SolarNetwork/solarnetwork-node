/* ==================================================================
 * GeneralNodeACEnergyDatumTests.java - 25/02/2019 8:50:02 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.test;

import static net.solarnetwork.node.domain.ACPhase.PhaseA;
import static net.solarnetwork.node.domain.ACPhase.PhaseB;
import static net.solarnetwork.node.domain.ACPhase.PhaseC;
import static net.solarnetwork.node.domain.ACPhase.Total;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;

/**
 * Test cases for the {@link GeneralNodeACEnergyDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeACEnergyDatumTests {

	private static final Float F = 1.23f;
	private static final Float Fa = 2.34f;
	private static final Float Fb = 3.45f;
	private static final Float Fc = 4.56f;

	@Test
	public void lineVoltage() {
		GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
		d.setLineVoltage(F);

		assertThat("Accessor", d.getLineVoltage(), equalTo(F));
		assertThat("Direct", d.getInstantaneousSampleFloat(ACEnergyDatum.LINE_VOLTAGE_KEY), equalTo(F));
	}

	@Test
	public void lineVoltagePhased() {
		GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
		d.setLineVoltage(Fa, PhaseA);
		d.setLineVoltage(Fb, PhaseB);
		d.setLineVoltage(Fc, PhaseC);
		d.setLineVoltage(F, Total);

		assertThat("Accessor non-phased", d.getLineVoltage(), nullValue());
		assertThat("Accessor a", d.getLineVoltage(PhaseA), equalTo(Fa));
		assertThat("Accessor b", d.getLineVoltage(PhaseB), equalTo(Fb));
		assertThat("Accessor c", d.getLineVoltage(PhaseC), equalTo(Fc));
		assertThat("Accessor t", d.getLineVoltage(Total), equalTo(F));

		assertThat("Direct non-phased", d.getInstantaneousSampleFloat(ACEnergyDatum.LINE_VOLTAGE_KEY),
				nullValue());
		assertThat("Direct a",
				d.getInstantaneousSampleFloat(PhaseA.withLineKey(ACEnergyDatum.VOLTAGE_KEY)),
				equalTo(Fa));
		assertThat("Direct b",
				d.getInstantaneousSampleFloat(PhaseB.withLineKey(ACEnergyDatum.VOLTAGE_KEY)),
				equalTo(Fb));
		assertThat("Direct c",
				d.getInstantaneousSampleFloat(PhaseC.withLineKey(ACEnergyDatum.VOLTAGE_KEY)),
				equalTo(Fc));
		assertThat("Direct t",
				d.getInstantaneousSampleFloat(Total.withLineKey(ACEnergyDatum.VOLTAGE_KEY)), equalTo(F));
	}

	@Test
	public void phaseVoltage() {
		GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
		d.setPhaseVoltage(F);

		assertThat("Accessor", d.getPhaseVoltage(), equalTo(F));
		assertThat("Direct", d.getInstantaneousSampleFloat(ACEnergyDatum.PHASE_VOLTAGE_KEY), equalTo(F));
	}

	@Test
	public void phaseVoltagePhased() {
		GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
		d.setVoltage(Fa, PhaseA);
		d.setVoltage(Fb, PhaseB);
		d.setVoltage(Fc, PhaseC);
		d.setVoltage(F, Total);

		assertThat("Accessor non-phased", d.getPhaseVoltage(), nullValue());
		assertThat("Accessor a", d.getVoltage(PhaseA), equalTo(Fa));
		assertThat("Accessor b", d.getVoltage(PhaseB), equalTo(Fb));
		assertThat("Accessor c", d.getVoltage(PhaseC), equalTo(Fc));
		assertThat("Accessor t", d.getVoltage(Total), equalTo(F));

		assertThat("Direct non-phased", d.getInstantaneousSampleFloat(ACEnergyDatum.PHASE_VOLTAGE_KEY),
				nullValue());
		assertThat("Direct a", d.getInstantaneousSampleFloat(PhaseA.withKey(ACEnergyDatum.VOLTAGE_KEY)),
				equalTo(Fa));
		assertThat("Direct b", d.getInstantaneousSampleFloat(PhaseB.withKey(ACEnergyDatum.VOLTAGE_KEY)),
				equalTo(Fb));
		assertThat("Direct c", d.getInstantaneousSampleFloat(PhaseC.withKey(ACEnergyDatum.VOLTAGE_KEY)),
				equalTo(Fc));
		assertThat("Direct t", d.getInstantaneousSampleFloat(Total.withKey(ACEnergyDatum.VOLTAGE_KEY)),
				equalTo(F));
	}

	@Test
	public void current() {
		GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
		d.setCurrent(F);

		assertThat("Accessor", d.getCurrent(), equalTo(F));
		assertThat("Direct", d.getInstantaneousSampleFloat(ACEnergyDatum.CURRENT_KEY), equalTo(F));
	}

	@Test
	public void currentPhased() {
		GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
		d.setCurrent(Fa, PhaseA);
		d.setCurrent(Fb, PhaseB);
		d.setCurrent(Fc, PhaseC);
		d.setCurrent(F, Total);

		assertThat("Accessor non-phased", d.getCurrent(), nullValue());
		assertThat("Accessor a", d.getCurrent(PhaseA), equalTo(Fa));
		assertThat("Accessor b", d.getCurrent(PhaseB), equalTo(Fb));
		assertThat("Accessor c", d.getCurrent(PhaseC), equalTo(Fc));
		assertThat("Accessor t", d.getCurrent(Total), equalTo(F));

		assertThat("Direct non-phased", d.getInstantaneousSampleFloat(ACEnergyDatum.CURRENT_KEY),
				nullValue());
		assertThat("Direct a", d.getInstantaneousSampleFloat(PhaseA.withKey(ACEnergyDatum.CURRENT_KEY)),
				equalTo(Fa));
		assertThat("Direct b", d.getInstantaneousSampleFloat(PhaseB.withKey(ACEnergyDatum.CURRENT_KEY)),
				equalTo(Fb));
		assertThat("Direct c", d.getInstantaneousSampleFloat(PhaseC.withKey(ACEnergyDatum.CURRENT_KEY)),
				equalTo(Fc));
		assertThat("Direct t", d.getInstantaneousSampleFloat(Total.withKey(ACEnergyDatum.CURRENT_KEY)),
				equalTo(F));
	}
}
