/* ==================================================================
 * InverterExtendedMeasurementsModelAccessorImplTests.java - 6/10/2026 9:52:10 am
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

package net.solarnetwork.node.hw.sunspec.inverter.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.inverter.InverterConnectionStatus;
import net.solarnetwork.node.hw.sunspec.inverter.InverterControlFunction;
import net.solarnetwork.node.hw.sunspec.inverter.InverterControlModelId;
import net.solarnetwork.node.hw.sunspec.inverter.InverterExtendedMeasurementsModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterExtendedMeasurementsModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.inverter.InverterRideThrough;
import net.solarnetwork.node.hw.sunspec.inverter.InverterSetpointLimit;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link InverterExtendedMeasurementsModelAccessorImpl}
 * class.
 *
 * @author matt
 * @version 1.0
 */
public class InverterExtendedMeasurementsModelAccessorImplTests {

	/** A Fronius IG Plus capture. */
	private static final String FRONIUS_TEST_DATA = "test-data-101-01.txt";

	/** An SMA capture. */
	private static final String SMA_TEST_DATA = "test-data-103-05.txt";

	/** A Fronius Symo capture. */
	private static final String FRONIUS_SYMO_TEST_DATA = "test-data-113-01.txt";

	private InverterExtendedMeasurementsModelAccessor getTestModel(String resource) {
		return ModelDataUtils.getModelDataInstance(getClass(), resource)
				.findTypedModel(InverterExtendedMeasurementsModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(FRONIUS_TEST_DATA),
				is(instanceOf(InverterExtendedMeasurementsModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		InverterExtendedMeasurementsModelAccessor model = getTestModel(FRONIUS_TEST_DATA);
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(181)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(183)));
		assertThat("Model ID", model.getModelId(),
				is(equalTo(InverterControlModelId.ExtendedMeasurements)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(44)));
		assertThat("Model length", model.getModelLength(), is(equalTo(44)));
	}

	@Test
	public void values_fronius() {
		InverterExtendedMeasurementsModelAccessor model = getTestModel(FRONIUS_TEST_DATA);
		assertThat("PV connection status", model.getPvConnectionStatus(), is(equalTo(
				EnumSet.of(InverterConnectionStatus.Available, InverterConnectionStatus.Operating))));
		assertThat("Storage connection status", model.getStorageConnectionStatus(),
				is(equalTo(Set.of())));
		assertThat("ECP connected", model.isEcpConnected(), is(equalTo(false)));
		assertThat("Active energy", model.getActiveEnergyExported(), is(equalTo(76476024L)));
		assertThat("Apparent energy not accumulated", model.getApparentEnergyExported(),
				is(nullValue()));
		assertThat("Reactive energy Q1 not accumulated", model.getReactiveEnergyQ1(), is(nullValue()));
		assertThat("Reactive energy Q2 not accumulated", model.getReactiveEnergyQ2(), is(nullValue()));
		assertThat("Reactive energy Q3 not accumulated", model.getReactiveEnergyQ3(), is(nullValue()));
		assertThat("Reactive energy Q4 not accumulated", model.getReactiveEnergyQ4(), is(nullValue()));
		assertThat("Reactive power available not implemented", model.getReactivePowerAvailable(),
				is(nullValue()));
		assertThat("Active power available not implemented", model.getActivePowerAvailable(),
				is(nullValue()));
		assertThat("Setpoint limits not implemented", model.getSetpointLimitsReached(),
				is(equalTo(Set.of())));
		assertThat("Active controls", model.getActiveControls(), is(equalTo(Set.of())));
		assertThat("Time source", model.getTimeSource(), is(equalTo("RTC")));
		assertThat("Device time is the SunSpec epoch", model.getDeviceTime(),
				is(equalTo(Instant.parse("2000-01-01T00:00:00Z"))));
		assertThat("Ride-throughs not implemented", model.getActiveRideThroughs(),
				is(equalTo(Set.of())));
		assertThat("Isolation resistance", model.getIsolationResistance(), is(equalTo(0.0f)));
	}

	@Test
	public void values_sma() {
		InverterExtendedMeasurementsModelAccessor model = getTestModel(SMA_TEST_DATA);
		assertThat("PV connection status", model.getPvConnectionStatus(), is(equalTo(
				EnumSet.of(InverterConnectionStatus.Connected, InverterConnectionStatus.Operating))));
		assertThat("Storage connection status not implemented", model.getStorageConnectionStatus(),
				is(equalTo(Set.of())));
		assertThat("ECP connected", model.isEcpConnected(), is(equalTo(true)));
		assertThat("Active energy", model.getActiveEnergyExported(), is(equalTo(4418970L)));
		assertThat("Apparent energy not accumulated", model.getApparentEnergyExported(),
				is(nullValue()));
		assertThat("Active controls not implemented", model.getActiveControls(), is(equalTo(Set.of())));
		assertThat("Time source not implemented", model.getTimeSource(), is(nullValue()));
		assertThat("Device time not implemented", model.getDeviceTime(), is(nullValue()));
		assertThat("Isolation resistance", model.getIsolationResistance(), is(equalTo(2040000.0f)));
	}

	@Test
	public void values_froniusSymo() {
		InverterExtendedMeasurementsModelAccessor model = getTestModel(FRONIUS_SYMO_TEST_DATA);
		assertThat("PV connection status", model.getPvConnectionStatus(),
				is(equalTo(EnumSet.of(InverterConnectionStatus.Connected,
						InverterConnectionStatus.Available, InverterConnectionStatus.Operating))));
		assertThat("ECP connected", model.isEcpConnected(), is(equalTo(true)));
		assertThat("Active energy", model.getActiveEnergyExported(), is(equalTo(11937020L)));
		assertThat("Time source", model.getTimeSource(), is(equalTo("RTC")));
		assertThat("Device time", model.getDeviceTime(),
				is(equalTo(Instant.parse("2019-10-05T16:36:59Z"))));
		assertThat("Isolation resistance not implemented", model.getIsolationResistance(),
				is(nullValue()));
	}

	@Test
	public void syntheticValues() {
		// GIVEN
		// replace the whole block with synthetic values
		InverterExtendedMeasurementsModelAccessor model = ModelDataUtils
				.getModelDataInstanceWithRegisters(getClass(), FRONIUS_TEST_DATA, 183,
				// @formatter:off
						0x0009,                         // PVConn
						0x0002,                         // StorConn
						0x8001,                         // ECPConn, MSB set
						0x8000, 0x0000, 0x0000, 0x0000, // ActWh, larger than a long
						0x0000, 0x0000, 0x0001, 0x0000, // ActVAh
						0x0000, 0x0000, 0x0000, 0x0001, // ActVArhQ1
						0x0000, 0x0000, 0x0000, 0x0002, // ActVArhQ2
						0x0000, 0x0000, 0x0000, 0x0003, // ActVArhQ3
						0x0000, 0x0000, 0x0000, 0x0004, // ActVArhQ4
						0xFB1E, 0x0000,                 // VArAval, VArAval_SF
						0x0159, 0x0001,                 // WAval, WAval_SF
						0x0000, 0x0481,                 // StSetLimMsk
						0x0000, 0x1804,                 // StActCtl, with undefined bit 11
						0x4E54, 0x5000, 0x0000, 0x0000, // TmSrc
						0x3257, 0x5FF8,                 // Tms
						0x000A,                         // RtSt
						0x05DC, 0x0003                  // Ris, Ris_SF
						// @formatter:on
				).findTypedModel(InverterExtendedMeasurementsModelAccessor.class);

		// THEN
		assertThat("PV connection status", model.getPvConnectionStatus(), is(
				equalTo(EnumSet.of(InverterConnectionStatus.Connected, InverterConnectionStatus.Test))));
		assertThat("Storage connection status", model.getStorageConnectionStatus(),
				is(equalTo(EnumSet.of(InverterConnectionStatus.Available))));
		assertThat("ECP connection status with MSB set not implemented", model.isEcpConnected(),
				is(nullValue()));
		assertThat("Active energy larger than a long not available", model.getActiveEnergyExported(),
				is(nullValue()));
		assertThat("Apparent energy", model.getApparentEnergyExported(), is(equalTo(65536L)));
		assertThat("Reactive energy Q1", model.getReactiveEnergyQ1(), is(equalTo(1L)));
		assertThat("Reactive energy Q2", model.getReactiveEnergyQ2(), is(equalTo(2L)));
		assertThat("Reactive energy Q3", model.getReactiveEnergyQ3(), is(equalTo(3L)));
		assertThat("Reactive energy Q4", model.getReactiveEnergyQ4(), is(equalTo(4L)));
		assertThat("Reactive power available", model.getReactivePowerAvailable(), is(equalTo(-1250)));
		assertThat("Active power available", model.getActivePowerAvailable(), is(equalTo(3450)));
		assertThat("Setpoint limits", model.getSetpointLimitsReached(),
				is(equalTo(EnumSet.of(InverterSetpointLimit.MaximumActivePower,
						InverterSetpointLimit.MinimumPowerFactorQ1,
						InverterSetpointLimit.MinimumPowerFactorQ4))));
		assertThat("Active controls", model.getActiveControls(), is(equalTo(EnumSet
				.of(InverterControlFunction.FixedPowerFactor, InverterControlFunction.Scheduled))));
		assertThat("Time source", model.getTimeSource(), is(equalTo("NTP")));
		assertThat("Device time", model.getDeviceTime(),
				is(equalTo(Instant.parse("2026-10-06T07:30:00Z"))));
		assertThat("Ride-throughs", model.getActiveRideThroughs(), is(equalTo(
				EnumSet.of(InverterRideThrough.HighVoltage, InverterRideThrough.HighFrequency))));
		assertThat("Isolation resistance", model.getIsolationResistance(), is(equalTo(1500000.0f)));
	}

}
