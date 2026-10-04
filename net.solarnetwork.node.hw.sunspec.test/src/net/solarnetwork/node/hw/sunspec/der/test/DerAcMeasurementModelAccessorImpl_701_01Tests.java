/* ==================================================================
 * DerAcMeasurementModelAccessorImpl_701_01Tests.java - 5/10/2026 8:27:22 am
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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.der.DerAcMeasurementModelAccessor;
import net.solarnetwork.node.hw.sunspec.der.DerAcMeasurementModelAccessorImpl;
import net.solarnetwork.node.hw.sunspec.der.DerAcWiringType;
import net.solarnetwork.node.hw.sunspec.der.DerAlarm;
import net.solarnetwork.node.hw.sunspec.der.DerGridConnectionState;
import net.solarnetwork.node.hw.sunspec.der.DerInverterState;
import net.solarnetwork.node.hw.sunspec.der.DerModelId;
import net.solarnetwork.node.hw.sunspec.der.DerOperatingState;
import net.solarnetwork.node.hw.sunspec.der.DerOperationalCharacteristic;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link DerAcMeasurementModelAccessorImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class DerAcMeasurementModelAccessorImpl_701_01Tests {

	private ModelData getTestDataInstance() {
		return ModelDataUtils.getModelDataInstance(getClass(), "test-data-der-01.txt");
	}

	private DerAcMeasurementModelAccessor getTestModel() {
		return getTestDataInstance().findTypedModel(DerAcMeasurementModelAccessor.class);
	}

	@Test
	public void findTypedModel() {
		assertThat(getTestModel(), is(instanceOf(DerAcMeasurementModelAccessorImpl.class)));
	}

	@Test
	public void findTypedModel_inverter() {
		// GIVEN
		ModelData data = getTestDataInstance();

		// WHEN
		InverterModelAccessor model = data.findTypedModel(InverterModelAccessor.class);

		// THEN
		assertThat("DER AC measurement model found as inverter model", model,
				is(instanceOf(DerAcMeasurementModelAccessorImpl.class)));
	}

	@Test
	public void block() {
		DerAcMeasurementModelAccessor model = getTestModel();
		assertThat("Model base address", model.getBaseAddress(), is(equalTo(177)));
		assertThat("Model block address", model.getBlockAddress(), is(equalTo(179)));
		assertThat("Model ID", model.getModelId(), is(equalTo(DerModelId.AcMeasurement)));
		assertThat("Model fixed length", model.getFixedBlockLength(), is(equalTo(153)));
		assertThat("Model length", model.getModelLength(), is(equalTo(153)));
		assertThat("Model repeating instance count", model.getRepeatingBlockInstanceCount(),
				is(equalTo(0)));
	}

	@Test
	public void deviceInfo() {
		Map<String, Object> info = getTestModel().getDeviceInfo();
		assertThat("Manufacturer", info,
				hasEntry(DataAccessor.INFO_KEY_DEVICE_MANUFACTURER, "OutBack Power"));
		assertThat("Model", info,
				hasEntry(DataAccessor.INFO_KEY_DEVICE_MODEL, "OGHI8048A (version 1.0.20.3812)"));
		assertThat("Serial number", info,
				hasEntry(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, "OGHI2232F0100079"));
	}

	@Test
	public void states() {
		DerAcMeasurementModelAccessor model = getTestModel();
		assertThat("AC wiring type", model.getAcWiringType(), is(equalTo(DerAcWiringType.SplitPhase)));
		assertThat("DER operating state", model.getDerOperatingState(),
				is(equalTo(DerOperatingState.On)));
		assertThat("Inverter state", model.getInverterState(), is(equalTo(DerInverterState.Running)));
		assertThat("Inverter operating state", model.getOperatingState(),
				is(equalTo(InverterOperatingState.Normal)));
		assertThat("Grid connection state", model.getGridConnectionState(),
				is(equalTo(DerGridConnectionState.Disconnected)));
		assertThat("Operational characteristics", model.getOperationalCharacteristics(),
				is(equalTo(Set.of(DerOperationalCharacteristic.GridForming))));
	}

	@Test
	public void alarms() {
		DerAcMeasurementModelAccessor model = getTestModel();
		assertThat("Alarms", model.getEvents(), is(equalTo(Set.<ModelEvent> of(DerAlarm.GridDisconnect,
				DerAlarm.UnderFrequency, DerAlarm.AcUnderVoltage))));
		assertThat("Vendor events", model.getVendorEvents(), is(nullValue()));
		assertThat("Manufacturer alarm info", model.getManufacturerAlarmInfo(), is(nullValue()));
	}

	@Test
	public void throttling() {
		DerAcMeasurementModelAccessor model = getTestModel();
		assertThat("Throttle percent", model.getThrottlePercent(), is(equalTo(0)));
		assertThat("Throttle sources 0xFFFF9AC8 not implemented, as the MSB is set",
				model.getThrottleSources(), is(equalTo(Set.of())));
	}

	@Test
	public void totals() {
		DerAcMeasurementModelAccessor model = getTestModel();
		assertThat("Active power", model.getActivePower(), is(equalTo(0)));
		assertThat("Apparent power", model.getApparentPower(), is(equalTo(0)));
		assertThat("Reactive power", model.getReactivePower(), is(equalTo(0)));
		assertThat("Power factor", model.getPowerFactor(), is(equalTo(0.0f)));
		assertThat("Current", model.getCurrent(), is(equalTo(0.0f)));
		assertThat("Neutral current", model.getNeutralCurrent(), is(nullValue()));
		assertThat("Line to neutral voltage", model.getVoltage(), is(equalTo(120.0f)));
		assertThat("Line to line voltage", model.getLineVoltage(), is(equalTo(240.0f)));
		assertThat("Frequency", model.getFrequency(), is(equalTo(60.0f)));
	}

	@Test
	public void energy() {
		DerAcMeasurementModelAccessor model = getTestModel();
		assertThat("Active energy exported is injected", model.getActiveEnergyExported(),
				is(equalTo(0L)));
		assertThat("Active energy delivered is injected", model.getActiveEnergyDelivered(),
				is(equalTo(0L)));
		assertThat("Active energy received is absorbed", model.getActiveEnergyReceived(),
				is(equalTo(0L)));
		assertThat("Reactive energy delivered is injected", model.getReactiveEnergyDelivered(),
				is(equalTo(0L)));
		assertThat("Reactive energy received is absorbed", model.getReactiveEnergyReceived(),
				is(equalTo(0L)));
		assertThat("Apparent energy delivered", model.getApparentEnergyDelivered(), is(nullValue()));
		assertThat("Apparent energy received", model.getApparentEnergyReceived(), is(nullValue()));
	}

	@Test
	public void dc() {
		DerAcMeasurementModelAccessor model = getTestModel();
		assertThat("DC current", model.getDcCurrent(), is(nullValue()));
		assertThat("DC voltage", model.getDcVoltage(), is(nullValue()));
		assertThat("DC power", model.getDcPower(), is(nullValue()));
	}

	@Test
	public void temperatures() {
		DerAcMeasurementModelAccessor model = getTestModel();
		assertThat("Ambient", model.getAmbientTemperature(), is(nullValue()));
		assertThat("Cabinet", model.getCabinetTemperature(), is(nullValue()));
		assertThat("Heat sink", model.getHeatSinkTemperature(), is(nullValue()));
		assertThat("Transformer", model.getTransformerTemperature(), is(equalTo(57.0f)));
		assertThat("Switch", model.getSwitchTemperature(), is(equalTo(31.0f)));
		assertThat("Other", model.getOtherTemperature(), is(nullValue()));
	}

	@Test
	public void phaseTotal() {
		DerAcMeasurementModelAccessor model = getTestModel();
		assertThat("Total phase is model", model.accessorForPhase(AcPhase.Total),
				is(sameInstance(model)));
	}

	@Test
	public void phaseA() {
		InverterModelAccessor phase = getTestModel().accessorForPhase(AcPhase.PhaseA);
		assertThat("Active power", phase.getActivePower(), is(nullValue()));
		assertThat("Apparent power", phase.getApparentPower(), is(nullValue()));
		assertThat("Reactive power", phase.getReactivePower(), is(nullValue()));
		assertThat("Power factor", phase.getPowerFactor(), is(equalTo(-0.001f)));
		assertThat("Current", phase.getCurrent(), is(equalTo(0.0f)));
		assertThat("Line voltage", phase.getLineVoltage(), is(nullValue()));
		assertThat("Voltage", phase.getVoltage(), is(equalTo(120.04f)));
		assertThat("Active energy delivered", phase.getActiveEnergyDelivered(), is(nullValue()));
		assertThat("Active energy received", phase.getActiveEnergyReceived(), is(nullValue()));
		assertThat("Reactive energy delivered", phase.getReactiveEnergyDelivered(), is(nullValue()));
		assertThat("Reactive energy received", phase.getReactiveEnergyReceived(), is(nullValue()));
		assertThat("Frequency is total", phase.getFrequency(), is(equalTo(60.0f)));
		assertThat("Operating state is total", phase.getOperatingState(),
				is(equalTo(InverterOperatingState.Normal)));
	}

	@Test
	public void phaseB() {
		InverterModelAccessor phase = getTestModel().accessorForPhase(AcPhase.PhaseB);
		assertThat("Active power", phase.getActivePower(), is(nullValue()));
		assertThat("Power factor", phase.getPowerFactor(), is(equalTo(-0.001f)));
		assertThat("Current", phase.getCurrent(), is(equalTo(0.0f)));
		assertThat("Line voltage", phase.getLineVoltage(), is(nullValue()));
		assertThat("Voltage", phase.getVoltage(), is(equalTo(119.96f)));
		assertThat("Active energy delivered", phase.getActiveEnergyDelivered(), is(nullValue()));
	}

	@Test
	public void phaseC() {
		InverterModelAccessor phase = getTestModel().accessorForPhase(AcPhase.PhaseC);
		assertThat("Active power", phase.getActivePower(), is(nullValue()));
		assertThat("Power factor", phase.getPowerFactor(), is(equalTo(-0.001f)));
		assertThat("Current", phase.getCurrent(), is(nullValue()));
		assertThat("Line voltage", phase.getLineVoltage(), is(nullValue()));
		assertThat("Voltage", phase.getVoltage(), is(nullValue()));
		assertThat("Active energy delivered", phase.getActiveEnergyDelivered(), is(nullValue()));
	}

}
