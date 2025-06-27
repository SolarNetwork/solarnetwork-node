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

package net.solarnetwork.node.domain.datum.test;

import static net.solarnetwork.domain.AcPhase.PhaseA;
import static net.solarnetwork.domain.AcPhase.PhaseB;
import static net.solarnetwork.domain.AcPhase.PhaseC;
import static net.solarnetwork.domain.AcPhase.Total;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;

/**
 * Test cases for the {@link SimpleAcEnergyDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleAcEnergyDatumTests {

	private static final Float F = 1.23f;
	private static final Float Fa = 2.34f;
	private static final Float Fb = 3.45f;
	private static final Float Fc = 4.56f;

	@Test
	public void lineVoltage() {
		SimpleAcEnergyDatum d = new SimpleAcEnergyDatum("foo", Instant.now(), new DatumSamples());
		d.setLineVoltage(F);

		assertThat("Accessor", d.getLineVoltage(), equalTo(F));
		assertThat("Direct",
				d.asSampleOperations().getSampleFloat(Instantaneous, AcEnergyDatum.LINE_VOLTAGE_KEY),
				equalTo(F));
	}

	@Test
	public void lineVoltagePhased() {
		SimpleAcEnergyDatum d = new SimpleAcEnergyDatum("foo", Instant.now(), new DatumSamples());
		d.setLineVoltage(PhaseA, Fa);
		d.setLineVoltage(PhaseB, Fb);
		d.setLineVoltage(PhaseC, Fc);
		d.setLineVoltage(Total, F);

		assertThat("Accessor non-phased", d.getLineVoltage(), nullValue());
		assertThat("Accessor a", d.getLineVoltage(PhaseA), equalTo(Fa));
		assertThat("Accessor b", d.getLineVoltage(PhaseB), equalTo(Fb));
		assertThat("Accessor c", d.getLineVoltage(PhaseC), equalTo(Fc));
		assertThat("Accessor t", d.getLineVoltage(Total), equalTo(F));

		assertThat("Direct non-phased",
				d.asSampleOperations().getSampleFloat(Instantaneous, AcEnergyDatum.LINE_VOLTAGE_KEY),
				nullValue());
		assertThat("Direct a", d.asSampleOperations().getSampleFloat(Instantaneous,
				PhaseA.withLineKey(AcEnergyDatum.VOLTAGE_KEY)), equalTo(Fa));
		assertThat("Direct b", d.asSampleOperations().getSampleFloat(Instantaneous,
				PhaseB.withLineKey(AcEnergyDatum.VOLTAGE_KEY)), equalTo(Fb));
		assertThat("Direct c", d.asSampleOperations().getSampleFloat(Instantaneous,
				PhaseC.withLineKey(AcEnergyDatum.VOLTAGE_KEY)), equalTo(Fc));
		assertThat("Direct t", d.asSampleOperations().getSampleFloat(Instantaneous,
				Total.withLineKey(AcEnergyDatum.VOLTAGE_KEY)), equalTo(F));
	}

	@Test
	public void phaseVoltage() {
		SimpleAcEnergyDatum d = new SimpleAcEnergyDatum("foo", Instant.now(), new DatumSamples());
		d.setPhaseVoltage(F);

		assertThat("Accessor", d.getPhaseVoltage(), equalTo(F));
		assertThat("Direct",
				d.asSampleOperations().getSampleFloat(Instantaneous, AcEnergyDatum.PHASE_VOLTAGE_KEY),
				equalTo(F));
	}

	@Test
	public void phaseVoltagePhased() {
		SimpleAcEnergyDatum d = new SimpleAcEnergyDatum("foo", Instant.now(), new DatumSamples());
		d.setVoltage(PhaseA, Fa);
		d.setVoltage(PhaseB, Fb);
		d.setVoltage(PhaseC, Fc);
		d.setVoltage(Total, F);

		assertThat("Accessor non-phased", d.getPhaseVoltage(), nullValue());
		assertThat("Accessor a", d.getVoltage(PhaseA), equalTo(Fa));
		assertThat("Accessor b", d.getVoltage(PhaseB), equalTo(Fb));
		assertThat("Accessor c", d.getVoltage(PhaseC), equalTo(Fc));
		assertThat("Accessor t", d.getVoltage(Total), equalTo(F));

		assertThat("Direct non-phased",
				d.asSampleOperations().getSampleFloat(Instantaneous, AcEnergyDatum.PHASE_VOLTAGE_KEY),
				nullValue());
		assertThat("Direct a", d.asSampleOperations().getSampleFloat(Instantaneous,
				PhaseA.withKey(AcEnergyDatum.VOLTAGE_KEY)), equalTo(Fa));
		assertThat("Direct b", d.asSampleOperations().getSampleFloat(Instantaneous,
				PhaseB.withKey(AcEnergyDatum.VOLTAGE_KEY)), equalTo(Fb));
		assertThat("Direct c", d.asSampleOperations().getSampleFloat(Instantaneous,
				PhaseC.withKey(AcEnergyDatum.VOLTAGE_KEY)), equalTo(Fc));
		assertThat("Direct t", d.asSampleOperations().getSampleFloat(Instantaneous,
				Total.withKey(AcEnergyDatum.VOLTAGE_KEY)), equalTo(F));
	}

	@Test
	public void current() {
		SimpleAcEnergyDatum d = new SimpleAcEnergyDatum("foo", Instant.now(), new DatumSamples());
		d.setCurrent(F);

		assertThat("Accessor", d.getCurrent(), equalTo(F));
		assertThat("Direct",
				d.asSampleOperations().getSampleFloat(Instantaneous, AcEnergyDatum.CURRENT_KEY),
				equalTo(F));
	}

	@Test
	public void currentPhased() {
		SimpleAcEnergyDatum d = new SimpleAcEnergyDatum("foo", Instant.now(), new DatumSamples());
		d.setCurrent(PhaseA, Fa);
		d.setCurrent(PhaseB, Fb);
		d.setCurrent(PhaseC, Fc);
		d.setCurrent(Total, F);

		assertThat("Accessor non-phased", d.getCurrent(), nullValue());
		assertThat("Accessor a", d.getCurrent(PhaseA), equalTo(Fa));
		assertThat("Accessor b", d.getCurrent(PhaseB), equalTo(Fb));
		assertThat("Accessor c", d.getCurrent(PhaseC), equalTo(Fc));
		assertThat("Accessor t", d.getCurrent(Total), equalTo(F));

		assertThat("Direct non-phased",
				d.asSampleOperations().getSampleFloat(Instantaneous, AcEnergyDatum.CURRENT_KEY),
				nullValue());
		assertThat("Direct a", d.asSampleOperations().getSampleFloat(Instantaneous,
				PhaseA.withKey(AcEnergyDatum.CURRENT_KEY)), equalTo(Fa));
		assertThat("Direct b", d.asSampleOperations().getSampleFloat(Instantaneous,
				PhaseB.withKey(AcEnergyDatum.CURRENT_KEY)), equalTo(Fb));
		assertThat("Direct c", d.asSampleOperations().getSampleFloat(Instantaneous,
				PhaseC.withKey(AcEnergyDatum.CURRENT_KEY)), equalTo(Fc));
		assertThat("Direct t", d.asSampleOperations().getSampleFloat(Instantaneous,
				Total.withKey(AcEnergyDatum.CURRENT_KEY)), equalTo(F));
	}
}
