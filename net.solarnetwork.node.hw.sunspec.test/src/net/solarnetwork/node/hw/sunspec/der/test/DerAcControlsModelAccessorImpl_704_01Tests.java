/* ==================================================================
 * DerAcControlsModelAccessorImpl_704_01Tests.java - 5/10/2026 3:20:44 pm
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

package net.solarnetwork.node.hw.sunspec.der.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.der.DerAcControlsModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerAcControlsModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerActivePowerSetpointMode;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerPowerFactorExcitation;
import net.solarnetwork.node.hw.sunspec.der.DerRampRateReference;
import net.solarnetwork.node.hw.sunspec.der.DerReactivePowerPriority;
import net.solarnetwork.node.hw.sunspec.der.DerReactivePowerSetpointMode;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;
import net.solarnetwork.node.hw.sunspec.test.RecordingModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Test cases for the {@link DerAcControlsModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerAcControlsModelAccessorImpl_704_01Tests {

	private static final String TEST_DATA = "test-data-der-01.txt";

	/** The model block address. */
	private static final int BLOCK_ADDRESS = 405;

	private DerAcControlsModelAccessor getTestModel() {
		return ModelDataUtils.getModelDataInstance(getClass(), TEST_DATA)
				.findTypedModel(DerAcControlsModelAccessor.class);
	}

	private RecordingModbusConnection writableConnection() {
		return ModelDataUtils.getWritableModbusConnection(getClass(), TEST_DATA);
	}

	private static DerAcControlsModelAccessor discoverModel(ModbusConnection conn) {
		return ModelDataUtils.getModelDataInstance(conn)
				.findTypedModel(DerAcControlsModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerAcControlsModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerAcControlsModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(403)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(BLOCK_ADDRESS)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.AcControls)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(65)));
		assertThat("Model length", model.getModelLength(), is(equalTo(65)));
	}

	@Test
	public void powerFactorWhenInjecting() {
		DerAcControlsModelAccessor model = getTestModel();
		assertThat("Enabled", model.isPowerFactorWhenInjectingEnabled(), is(equalTo(true)));
		assertThat("Power factor", model.getPowerFactorWhenInjecting(), is(equalTo(1.0f)));
		assertThat("Excitation", model.getPowerFactorExcitationWhenInjecting(),
				is(equalTo(DerPowerFactorExcitation.UnderExcited)));
		assertThat("Reversion enabled not implemented",
				model.isPowerFactorWhenInjectingReversionEnabled(), is(nullValue()));
		assertThat("Reversion time not implemented", model.getPowerFactorWhenInjectingReversionTime(),
				is(nullValue()));
		assertThat("Reversion time remaining not implemented",
				model.getPowerFactorWhenInjectingReversionTimeRemaining(), is(nullValue()));
		assertThat("Reversion power factor not implemented",
				model.getReversionPowerFactorWhenInjecting(), is(nullValue()));
		assertThat("Reversion excitation not implemented",
				model.getReversionPowerFactorExcitationWhenInjecting(), is(nullValue()));
	}

	@Test
	public void powerFactorWhenAbsorbing() {
		DerAcControlsModelAccessor model = getTestModel();
		assertThat("Enabled", model.isPowerFactorWhenAbsorbingEnabled(), is(equalTo(true)));
		assertThat("Power factor", model.getPowerFactorWhenAbsorbing(), is(equalTo(1.0f)));
		assertThat("Excitation", model.getPowerFactorExcitationWhenAbsorbing(),
				is(equalTo(DerPowerFactorExcitation.OverExcited)));
		assertThat("Reversion enabled not implemented",
				model.isPowerFactorWhenAbsorbingReversionEnabled(), is(nullValue()));
		assertThat("Reversion time not implemented", model.getPowerFactorWhenAbsorbingReversionTime(),
				is(nullValue()));
		assertThat("Reversion power factor not implemented",
				model.getReversionPowerFactorWhenAbsorbing(), is(nullValue()));
	}

	@Test
	public void activePowerLimit() {
		DerAcControlsModelAccessor model = getTestModel();
		assertThat("Enabled", model.isActivePowerLimitEnabled(), is(equalTo(true)));
		assertThat("Limit", model.getActivePowerLimitPercent(), is(equalTo(100.0f)));
		assertThat("Reversion limit not implemented", model.getReversionActivePowerLimitPercent(),
				is(nullValue()));
		assertThat("Reversion enabled not implemented", model.isActivePowerLimitReversionEnabled(),
				is(nullValue()));
		assertThat("Reversion time not implemented", model.getActivePowerLimitReversionTime(),
				is(nullValue()));
		assertThat("Reversion time remaining not implemented",
				model.getActivePowerLimitReversionTimeRemaining(), is(nullValue()));
	}

	@Test
	public void activePowerSetpoint() {
		DerAcControlsModelAccessor model = getTestModel();
		assertThat("Enabled", model.isActivePowerSetpointEnabled(), is(equalTo(false)));
		assertThat("Mode", model.getActivePowerSetpointMode(),
				is(equalTo(DerActivePowerSetpointMode.MaximumActivePowerPercent)));
		assertThat("Setpoint not implemented", model.getActivePowerSetpoint(), is(nullValue()));
		assertThat("Reversion setpoint not implemented", model.getReversionActivePowerSetpoint(),
				is(nullValue()));
		assertThat("Setpoint percent", model.getActivePowerSetpointPercent(), is(equalTo(-100.0f)));
		assertThat("Reversion setpoint percent not implemented",
				model.getReversionActivePowerSetpointPercent(), is(nullValue()));
		assertThat("Reversion enabled not implemented", model.isActivePowerSetpointReversionEnabled(),
				is(nullValue()));
		assertThat("Reversion time not implemented", model.getActivePowerSetpointReversionTime(),
				is(nullValue()));
	}

	@Test
	public void reactivePowerSetpoint() {
		DerAcControlsModelAccessor model = getTestModel();
		assertThat("Enabled", model.isReactivePowerSetpointEnabled(), is(equalTo(false)));
		assertThat("Mode", model.getReactivePowerSetpointMode(),
				is(equalTo(DerReactivePowerSetpointMode.MaximumActivePowerPercent)));
		assertThat("Priority", model.getReactivePowerPriority(),
				is(equalTo(DerReactivePowerPriority.ReactivePower)));
		assertThat("Setpoint not implemented", model.getReactivePowerSetpoint(), is(nullValue()));
		assertThat("Reversion setpoint not implemented", model.getReversionReactivePowerSetpoint(),
				is(nullValue()));
		assertThat("Setpoint percent", model.getReactivePowerSetpointPercent(), is(equalTo(100.0f)));
		assertThat("Reversion setpoint percent not implemented",
				model.getReversionReactivePowerSetpointPercent(), is(nullValue()));
		assertThat("Reversion enabled not implemented", model.isReactivePowerSetpointReversionEnabled(),
				is(nullValue()));
		assertThat("Reversion time remaining not implemented",
				model.getReactivePowerSetpointReversionTimeRemaining(), is(nullValue()));
	}

	@Test
	public void rampRatesAndAntiIslanding() {
		DerAcControlsModelAccessor model = getTestModel();
		assertThat("Active power ramp rate", model.getActivePowerRampRate(), is(equalTo(1000)));
		assertThat("Active power ramp rate reference", model.getActivePowerRampRateReference(),
				is(equalTo(DerRampRateReference.MaximumActivePower)));
		assertThat("Reactive power ramp rate", model.getReactivePowerRampRate(), is(equalTo(0)));
		assertThat("Anti-islanding enabled", model.isAntiIslandingEnabled(), is(equalTo(true)));
	}

	@Test
	public void writePowerFactor_singleRequest() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerAcControlsModelAccessor model = discoverModel(conn);

		// WHEN
		model.setPowerFactorWhenInjecting(conn, 0.95f, DerPowerFactorExcitation.OverExcited);

		// THEN
		assertThat("Power factor and excitation written together in one request", conn.getWrites(),
				is(equalTo(List.of(List.of(BLOCK_ADDRESS + 57, 2)))));

		DerAcControlsModelAccessor device = discoverModel(conn);
		assertThat("Power factor", device.getPowerFactorWhenInjecting(), is(equalTo(0.95f)));
		assertThat("Excitation", device.getPowerFactorExcitationWhenInjecting(),
				is(equalTo(DerPowerFactorExcitation.OverExcited)));
		assertThat("Absorbing power factor unchanged", device.getPowerFactorWhenAbsorbing(),
				is(equalTo(1.0f)));
	}

	@Test
	public void writeReversionPowerFactors() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerAcControlsModelAccessor model = discoverModel(conn);

		// WHEN
		model.setReversionPowerFactorWhenInjecting(conn, 0.9f, DerPowerFactorExcitation.OverExcited);
		model.setPowerFactorWhenAbsorbing(conn, 0.85f, DerPowerFactorExcitation.UnderExcited);
		model.setReversionPowerFactorWhenAbsorbing(conn, 0.8f, DerPowerFactorExcitation.OverExcited);

		// THEN
		assertThat("Each pair written in one request", conn.getWrites(),
				is(equalTo(List.of(List.of(BLOCK_ADDRESS + 59, 2), List.of(BLOCK_ADDRESS + 61, 2),
						List.of(BLOCK_ADDRESS + 63, 2)))));

		DerAcControlsModelAccessor device = discoverModel(conn);
		assertThat("Reversion power factor injecting", device.getReversionPowerFactorWhenInjecting(),
				is(equalTo(0.9f)));
		assertThat("Reversion excitation injecting",
				device.getReversionPowerFactorExcitationWhenInjecting(),
				is(equalTo(DerPowerFactorExcitation.OverExcited)));
		assertThat("Power factor absorbing", device.getPowerFactorWhenAbsorbing(), is(equalTo(0.85f)));
		assertThat("Excitation absorbing", device.getPowerFactorExcitationWhenAbsorbing(),
				is(equalTo(DerPowerFactorExcitation.UnderExcited)));
		assertThat("Reversion power factor absorbing", device.getReversionPowerFactorWhenAbsorbing(),
				is(equalTo(0.8f)));
		assertThat("Reversion excitation absorbing",
				device.getReversionPowerFactorExcitationWhenAbsorbing(),
				is(equalTo(DerPowerFactorExcitation.OverExcited)));
	}

	@Test
	public void writePowerFactor_outOfRange() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerAcControlsModelAccessor model = discoverModel(conn);

		// WHEN
		try {
			model.setPowerFactorWhenInjecting(conn, 70f, DerPowerFactorExcitation.OverExcited);
			fail("Scaled value larger than uint16 should be rejected.");
		} catch ( IllegalArgumentException e ) {
			// expected
		}

		// THEN
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

	@Test
	public void writeSettings() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerAcControlsModelAccessor model = discoverModel(conn);

		// WHEN
		model.setPowerFactorWhenInjectingEnabled(conn, false);
		model.setPowerFactorWhenInjectingReversionEnabled(conn, true);
		model.setPowerFactorWhenInjectingReversionTime(conn, 300);
		model.setPowerFactorWhenAbsorbingEnabled(conn, false);
		model.setPowerFactorWhenAbsorbingReversionTime(conn, 600);
		model.setActivePowerLimitEnabled(conn, false);
		model.setActivePowerLimitPercent(conn, 80.5f);
		model.setReversionActivePowerLimitPercent(conn, 100f);
		model.setActivePowerLimitReversionEnabled(conn, true);
		model.setActivePowerLimitReversionTime(conn, 900);
		model.setActivePowerSetpointEnabled(conn, true);
		model.setActivePowerSetpointMode(conn, DerActivePowerSetpointMode.Watts);
		model.setActivePowerSetpoint(conn, -2500);
		model.setReversionActivePowerSetpoint(conn, 0);
		model.setActivePowerSetpointPercent(conn, 50.5f);
		model.setReversionActivePowerSetpointPercent(conn, -25f);
		model.setActivePowerSetpointReversionEnabled(conn, true);
		model.setActivePowerSetpointReversionTime(conn, 60);
		model.setReactivePowerSetpointEnabled(conn, true);
		model.setReactivePowerSetpointMode(conn,
				DerReactivePowerSetpointMode.MaximumApparentPowerPercent);
		model.setReactivePowerPriority(conn, DerReactivePowerPriority.ActivePower);
		model.setReactivePowerSetpointPercent(conn, -30.5f);
		model.setReversionReactivePowerSetpointPercent(conn, 0f);
		model.setReactivePowerSetpointReversionEnabled(conn, false);
		model.setReactivePowerSetpointReversionTime(conn, 120);
		model.setActivePowerRampRate(conn, 20);
		model.setActivePowerRampRateReference(conn, DerRampRateReference.MaximumCurrent);
		model.setReactivePowerRampRate(conn, 15);
		model.setAntiIslandingEnabled(conn, false);

		// THEN
		DerAcControlsModelAccessor device = discoverModel(conn);
		assertThat("PF injecting enabled", device.isPowerFactorWhenInjectingEnabled(),
				is(equalTo(false)));
		assertThat("PF injecting reversion enabled", device.isPowerFactorWhenInjectingReversionEnabled(),
				is(equalTo(true)));
		assertThat("PF injecting reversion time", device.getPowerFactorWhenInjectingReversionTime(),
				is(equalTo(300L)));
		assertThat("PF injecting reversion time remaining unchanged",
				device.getPowerFactorWhenInjectingReversionTimeRemaining(), is(nullValue()));
		assertThat("PF absorbing enabled", device.isPowerFactorWhenAbsorbingEnabled(),
				is(equalTo(false)));
		assertThat("PF absorbing reversion time", device.getPowerFactorWhenAbsorbingReversionTime(),
				is(equalTo(600L)));
		assertThat("Limit enabled", device.isActivePowerLimitEnabled(), is(equalTo(false)));
		assertThat("Limit", device.getActivePowerLimitPercent(), is(equalTo(80.5f)));
		assertThat("Reversion limit", device.getReversionActivePowerLimitPercent(), is(equalTo(100.0f)));
		assertThat("Limit reversion enabled", device.isActivePowerLimitReversionEnabled(),
				is(equalTo(true)));
		assertThat("Limit reversion time", device.getActivePowerLimitReversionTime(), is(equalTo(900L)));
		assertThat("Set active power enabled", device.isActivePowerSetpointEnabled(), is(equalTo(true)));
		assertThat("Set active power mode", device.getActivePowerSetpointMode(),
				is(equalTo(DerActivePowerSetpointMode.Watts)));
		assertThat("Active power setpoint", device.getActivePowerSetpoint(), is(equalTo(-2500)));
		assertThat("Reversion active power setpoint", device.getReversionActivePowerSetpoint(),
				is(equalTo(0)));
		assertThat("Active power setpoint percent", device.getActivePowerSetpointPercent(),
				is(equalTo(50.5f)));
		assertThat("Reversion active power setpoint percent",
				device.getReversionActivePowerSetpointPercent(), is(equalTo(-25.0f)));
		assertThat("Set active power reversion enabled", device.isActivePowerSetpointReversionEnabled(),
				is(equalTo(true)));
		assertThat("Set active power reversion time", device.getActivePowerSetpointReversionTime(),
				is(equalTo(60L)));
		assertThat("Set reactive power enabled", device.isReactivePowerSetpointEnabled(),
				is(equalTo(true)));
		assertThat("Set reactive power mode", device.getReactivePowerSetpointMode(),
				is(equalTo(DerReactivePowerSetpointMode.MaximumApparentPowerPercent)));
		assertThat("Reactive power priority", device.getReactivePowerPriority(),
				is(equalTo(DerReactivePowerPriority.ActivePower)));
		assertThat("Reactive power setpoint percent", device.getReactivePowerSetpointPercent(),
				is(equalTo(-30.5f)));
		assertThat("Reversion reactive power setpoint percent",
				device.getReversionReactivePowerSetpointPercent(), is(equalTo(0.0f)));
		assertThat("Set reactive power reversion enabled",
				device.isReactivePowerSetpointReversionEnabled(), is(equalTo(false)));
		assertThat("Set reactive power reversion time", device.getReactivePowerSetpointReversionTime(),
				is(equalTo(120L)));
		assertThat("Active power ramp rate", device.getActivePowerRampRate(), is(equalTo(20)));
		assertThat("Active power ramp rate reference", device.getActivePowerRampRateReference(),
				is(equalTo(DerRampRateReference.MaximumCurrent)));
		assertThat("Reactive power ramp rate", device.getReactivePowerRampRate(), is(equalTo(15)));
		assertThat("Anti-islanding enabled", device.isAntiIslandingEnabled(), is(equalTo(false)));
	}

	@Test
	public void writeReactivePowerSetpoint_scaleFactorNotImplemented() throws IOException {
		// GIVEN
		RecordingModbusConnection conn = writableConnection();
		DerAcControlsModelAccessor model = discoverModel(conn);

		// WHEN
		try {
			model.setReactivePowerSetpoint(conn, 1000);
			fail("The not implemented VarSet_SF scale factor should prevent writing.");
		} catch ( IllegalStateException e ) {
			// expected
		}

		// THEN
		assertThat("Nothing written", conn.getWrites(), is(equalTo(List.of())));
	}

}
