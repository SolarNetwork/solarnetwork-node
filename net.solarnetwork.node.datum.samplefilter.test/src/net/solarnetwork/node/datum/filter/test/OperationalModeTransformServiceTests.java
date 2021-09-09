/* ==================================================================
 * OperationalModeTransformServiceTests.java - 4/07/2021 2:51:24 PM
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

import static java.util.Collections.singleton;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.datum.filter.opmode.OperationalModeTransformConfig;
import net.solarnetwork.node.datum.filter.opmode.OperationalModeDatumFilterService;
import net.solarnetwork.node.datum.filter.std.ThrottlingDatumFilterService;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.support.ExpressionService;
import net.solarnetwork.util.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link OperationalModeDatumFilterService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OperationalModeTransformServiceTests {

	private static final String SOURCE_ID = "FILTER_ME";
	private static final String PROP_WATTS = "watts";
	private static final String OP_MODE = "foo";

	private OperationalModesService opModesService;
	private ExpressionService exprService;
	private OperationalModeDatumFilterService xform;

	@Before
	public void setup() {
		opModesService = EasyMock.createMock(OperationalModesService.class);
		ThrottlingDatumFilterService.clearSettingCache();
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

	private GeneralNodeDatum createTestGeneralNodeDatum(String sourceId) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId(sourceId);
		datum.putInstantaneousSampleValue(PROP_WATTS, 23.4);
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
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

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
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

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
		Capture<DateTime> expireCaptor = new Capture<>();
		expect(opModesService.enableOperationalModes(eq(singleton(OP_MODE)), capture(expireCaptor)))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		final long expire = System.currentTimeMillis()
				+ TimeUnit.SECONDS.toMillis(config.getExpireSeconds());
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Result unchanged", result, is(sameInstance(d.getSamples())));
		assertThat("Expire date set", expireCaptor.getValue().isBefore(expire), is(equalTo(false)));

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
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

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
		config.setDatumPropertyType(GeneralDatumSamplesType.Tag);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.enableOperationalModes(singleton(OP_MODE), null))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

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
		config.setDatumPropertyType(GeneralDatumSamplesType.Tag);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.disableOperationalModes(singleton(OP_MODE))).andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		d.addTag("FooMode");
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

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
		config.setDatumPropertyType(GeneralDatumSamplesType.Status);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.enableOperationalModes(singleton(OP_MODE), null))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result status set", result.getStatusSampleString("FooMode"), is(equalTo("true")));
	}

	@Test
	public void opModeDeactivated_statusProperty() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 100");
		config.setName("FooMode");
		config.setDatumPropertyType(GeneralDatumSamplesType.Status);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.disableOperationalModes(singleton(OP_MODE))).andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		d.addTag("FooMode");
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result status set", result.getStatusSampleString("FooMode"), is(equalTo("false")));
	}

	@Test
	public void opModeActivated_instProperty() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 10");
		config.setName("FooMode");
		config.setDatumPropertyType(GeneralDatumSamplesType.Instantaneous);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.enableOperationalModes(singleton(OP_MODE), null))
				.andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result instantaneous set", result.getInstantaneousSampleInteger("FooMode"),
				is(equalTo(1)));
	}

	@Test
	public void opModeDeactivated_instProperty() {
		// GIVEN
		OperationalModeTransformConfig config = new OperationalModeTransformConfig();
		config.setExpressionServiceId(exprService.getUid());
		config.setOperationalMode(OP_MODE);
		config.setExpression("watts > 100");
		config.setName("FooMode");
		config.setDatumPropertyType(GeneralDatumSamplesType.Instantaneous);
		xform.setExpressionConfigs(new OperationalModeTransformConfig[] { config });

		expect(opModesService.disableOperationalModes(singleton(OP_MODE))).andReturn(singleton(OP_MODE));

		// WHEN
		replayAll();
		GeneralNodeDatum d = createTestGeneralNodeDatum(SOURCE_ID);
		d.addTag("FooMode");
		GeneralDatumSamples result = xform.transformSamples(d, d.getSamples(), null);

		// THEN
		assertThat("Result changed to set tag", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Result instantaneous set", result.getInstantaneousSampleInteger("FooMode"),
				is(equalTo(0)));
	}

}
