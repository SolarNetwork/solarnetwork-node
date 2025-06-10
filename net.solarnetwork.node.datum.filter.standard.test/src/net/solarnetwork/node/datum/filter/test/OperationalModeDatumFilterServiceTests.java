/* ==================================================================
 * OperationalModeDatumFilterServiceTests.java - 4/07/2021 2:51:24 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.test;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.filter.opmode.OperationalModeDatumFilterService;
import net.solarnetwork.node.datum.filter.opmode.OperationalModeTransformConfig;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link OperationalModeDatumFilterService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OperationalModeDatumFilterServiceTests {

	private static final String SOURCE_ID = "FILTER_ME";
	private static final String PROP_WATTS = "watts";
	private static final String OP_MODE = "foo";

	private OperationalModesService opModesService;
	private ExpressionService exprService;
	private OperationalModeDatumFilterService xform;

	@Before
	public void setup() {
		opModesService = EasyMock.createMock(OperationalModesService.class);
		xform = new OperationalModeDatumFilterService();
		xform.setSourceId("^F");
		xform.setOpModesService(opModesService);
		exprService = new SpelExpressionService();
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));
	}

	@After
	public void teardown() {
		EasyMock.verify(opModesService);
	}

	private void replayAll() {
		EasyMock.replay(opModesService);
	}

	private SimpleDatum createTestSimpleDatum(String sourceId) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId);
		datum.getSamples().putInstantaneousSampleValue(PROP_WATTS, 23.4);
		return datum;
	}

	@Test
	public void opModeActivated() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 10");
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.enableOperationalModes(singleton(OP_MODE), null))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result unchanged", result, is(sameInstance(d.getSamples())));
	}

	@Test
	public void opModeDeactivated() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 100");
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.disableOperationalModes(singleton(OP_MODE))).andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result unchanged", result, is(sameInstance(d.getSamples())));
	}

	@Test
	public void opModeActivated_withExpire() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 10");
		config.setExpireSeconds(60);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		// not already active, so activate now
		Capture<Instant> expireCaptor = Capture.newInstance();
		expect(opModesService.enableOperationalModes(eq(singleton(OP_MODE)), capture(expireCaptor)))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		final long expire = System.currentTimeMillis()
				+ TimeUnit.SECONDS.toMillis(config.getExpireSeconds());
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result unchanged", result, is(sameInstance(d.getSamples())));
		assertThat("Expire date set", expireCaptor.getValue().isBefore(Instant.ofEpochMilli(expire)),
				is(equalTo(false)));

	}

	@Test
	public void opModeDeactivated_withExpire() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 100");
		config.setExpireSeconds(60);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result unchanged", result, is(sameInstance(d.getSamples())));
	}

	@Test
	public void opModeActivated_tagProperty() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 10");
		config.setName("FooMode");
		config.setDatumPropertyType(DatumSamplesType.Tag);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.enableOperationalModes(singleton(OP_MODE), null))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result tag set", result.hasTag("FooMode"), is(equalTo(true)));
	}

	@Test
	public void opModeDeactivated_tagProperty() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 100");
		config.setName("FooMode");
		config.setDatumPropertyType(DatumSamplesType.Tag);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.disableOperationalModes(singleton(OP_MODE))).andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		d.addTag("FooMode");
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result tag removed", result.hasTag("FooMode"), is(equalTo(false)));
	}

	@Test
	public void opModeActivated_statusProperty() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 10");
		config.setName("FooMode");
		config.setDatumPropertyType(DatumSamplesType.Status);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.enableOperationalModes(singleton(OP_MODE), null))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result status set", result.getSampleString(DatumSamplesType.Status, "FooMode"),
				is(equalTo("true")));
	}

	@Test
	public void opModeDeactivated_statusProperty() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 100");
		config.setName("FooMode");
		config.setDatumPropertyType(DatumSamplesType.Status);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.disableOperationalModes(singleton(OP_MODE))).andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		d.addTag("FooMode");
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result status set", result.getSampleString(DatumSamplesType.Status, "FooMode"),
				is(equalTo("false")));
	}

	@Test
	public void opModeActivated_instProperty() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 10");
		config.setName("FooMode");
		config.setDatumPropertyType(DatumSamplesType.Instantaneous);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.enableOperationalModes(singleton(OP_MODE), null))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result instantaneous set",
				result.getSampleInteger(DatumSamplesType.Instantaneous, "FooMode"), is(equalTo(1)));
	}

	@Test
	public void opModeDeactivated_instProperty() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 100");
		config.setName("FooMode");
		config.setDatumPropertyType(DatumSamplesType.Instantaneous);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.disableOperationalModes(singleton(OP_MODE))).andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		d.addTag("FooMode");
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result instantaneous set",
				result.getSampleInteger(DatumSamplesType.Instantaneous, "FooMode"), is(equalTo(0)));
	}

	@Test
	public void notOrElse_lhsTrue() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression(
				"!has('vehStat_s') || (has('vchgDcPow_i') && vehStat_s == 1 && vchgDcPow_i <= 100)");
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.enableOperationalModes(singleton(OP_MODE), null))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamples s = d.getSamples();

		// no vehStat_s property, so LHS is true
		s.putInstantaneousSampleValue("vchgDcPow_i", 1000);

		xform.filter(d, s, null);

		// THEN
	}

	@Test
	public void notOrElse_rhsTrue() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression(
				"!has('vehStat_s') || (has('vchgDcPow_i') && vehStat_s == 1 && vchgDcPow_i <= 100)");
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.enableOperationalModes(singleton(OP_MODE), null))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamples s = d.getSamples();

		// vehStat_s == 1.0, vchgDcPow_i == 0, so RHS is true
		s.putStatusSampleValue("vehStat_s", 1);
		s.putInstantaneousSampleValue("vchgDcPow_i", 0);

		xform.filter(d, s, null);

		// THEN
	}

	@Test
	public void notOrElse_rhsFalse() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression(
				"!has('vehStat_s') || (has('vchgDcPow_i') && vehStat_s == 1 && vchgDcPow_i <= 100)");
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.disableOperationalModes(singleton(OP_MODE))).andReturn(emptySet());

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamples s = d.getSamples();

		// vehStat_s == 0, vchgDcPow_i == 0, so LHS is false and RHS is false
		s.putStatusSampleValue("vehStat_s", 0);
		s.putInstantaneousSampleValue("vchgDcPow_i", 0);

		xform.filter(d, s, null);

		// THEN
	}

	@Test
	public void notOrElse_rhsFalse2() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression(
				"!has('vehStat_s') || (has('vchgDcPow_i') && vehStat_s == 1 && vchgDcPow_i <= 100)");
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.disableOperationalModes(singleton(OP_MODE))).andReturn(emptySet());

		// WHEN
		replayAll();
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID);
		DatumSamples s = d.getSamples();

		// vehStat_s == 1, vchgDcPow_i == 101, so LHS is false and RHS is false
		s.putStatusSampleValue("vehStat_s", 1);
		s.putInstantaneousSampleValue("vchgDcPow_i", 101);

		xform.filter(d, s, null);

		// THEN
	}
}
